package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Horizon chart renderer: compact, multi-band representation of a time series.
 *
 * <p><b>Performance contract</b>:
 * <ul>
 *   <li>No object allocations in the hot rendering loop (uses {@code PlotContext.mapToPixel(..., double[])}).</li>
 *   <li>For very large datasets, aggregates into pixel columns (O(width) draw cost).</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class HorizonRenderer extends BaseRenderer {

    private static final int MAX_BANDS = 4;

    static {
        RendererRegistry.register("horizon", new RendererDescriptor("horizon", "renderer.horizon", "/icons/horizon.svg"), HorizonRenderer::new);
    }

    private final double[] map = new double[2];
    private transient Path2D.Double poly;
    // reusable mapping buffers
    private transient double[] xs;
    private transient double[] vals; // value in data space (y)
    // Cached x-pixels for large datasets to avoid mapping x every frame.
    private transient double[] xPix;
    private transient long xPixKey;
    // pixel-column aggregation buffers
    private transient double[] colSum;
    private transient int[] colCount;
    private transient double[] colX;
    private transient double[] colVal;
    private transient int[] touched;
    private transient int touchedSize;
    // cached colors per band and UI state
    private transient int uiKey;
    private transient Color[] bandColors;
    // Decimation output buffers (reused): min/max per pixel bucket -> at most ~2*width + 2 points.
    private transient double[] decX;
    private transient double[] decY;
    // Reused paths for aggregated rendering (one per band and sign).
    private transient Path2D.Double posBand;
    private transient Path2D.Double negBand;
    // Reused per-column band index buffers.
    private transient byte[] posIdx;
    private transient byte[] negIdx;

    public HorizonRenderer() {
        super("horizon");
    }

    /**
     * Computes a stable signature for caching pixel-x arrays across frames.
     */
    private static long xPixKey(PlotContext context, Rectangle bounds, int n) {
        long h = 1469598103934665603L;
        h ^= Double.doubleToLongBits(context.minX());
        h *= 1099511628211L;
        h ^= Double.doubleToLongBits(context.maxX());
        h *= 1099511628211L;
        h ^= bounds.x;
        h *= 1099511628211L;
        h ^= bounds.width;
        h *= 1099511628211L;
        h ^= n;
        h *= 1099511628211L;
        return h;
    }

    private static void buildBandStrip(Path2D.Double path,
                                       double[] xs,
                                       byte[] idx,
                                       int n,
                                       int bandLevel,
                                       double by,
                                       double bandH,
                                       int bands) {
        path.reset();

        // bandLevel is 1..bands
        final double yTop = by + (bands - bandLevel) * bandH;
        final double yBot = yTop + bandH;

        boolean inRun = false;
        double runStartX = 0;
        double lastX = 0;

        for (int i = 0; i < n; i++) {
            boolean on = (idx[i] >= bandLevel);
            double x = xs[i];

            if (on) {
                if (!inRun) {
                    inRun = true;
                    runStartX = x;
                    lastX = x;
                } else {
                    lastX = x;
                }
                continue;
            }

            if (!inRun) {
                continue;
            }

            // Close current run as a rectangle.
            double x0 = runStartX;
            double x1 = lastX;
            if (x1 < x0) {
                double t = x0;
                x0 = x1;
                x1 = t;
            }

            // Avoid zero/near-zero width geometry (derive a minimal width from neighbors when available).
            if (x1 - x0 < 0.5) {
                double dx = 1.0;
                // i is always > 0 here because a run can only end when we've advanced.
                double prev = xs[i - 1];
                dx = Math.max(dx, Math.abs(x - prev));
                if (i + 1 < n) {
                    double next = xs[i + 1];
                    dx = Math.max(dx, Math.abs(next - x));
                }
                x1 = x0 + Math.max(1.0, dx);
            }

            path.moveTo(x0, yTop);
            path.lineTo(x1, yTop);
            path.lineTo(x1, yBot);
            path.lineTo(x0, yBot);
            path.closePath();

            inRun = false;
        }

        if (inRun) {
            double x0 = runStartX;
            double x1 = lastX;
            if (x1 < x0) {
                double t = x0;
                x0 = x1;
                x1 = t;
            }
            if (x1 - x0 < 0.5) {
                x1 = x0 + 1.0;
            }

            path.moveTo(x0, yTop);
            path.lineTo(x1, yTop);
            path.lineTo(x1, yBot);
            path.lineTo(x0, yBot);
            path.closePath();
        }

        // If nothing was added, keep it empty (currentPoint == null can be used by caller).
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    private static int lowerBoundDataX(double[] xs, int n, double x) {
        int lo = 0;
        int hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (xs[mid] < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private Path2D.Double poly() {
        if (poly == null) poly = new Path2D.Double(Path2D.WIND_NON_ZERO);
        return poly;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle bounds = context.plotBounds().getBounds();
        Rectangle clip = g2.getClipBounds();
        if (clip != null && !clip.intersects(bounds)) return;
        int w = Math.max(1, bounds.width);
        boolean aggregate = n > 12000;
        if (aggregate) {
            int budget = Math.max(64, w * 2 + 8);
            int dn = decimateMinMaxPerPixelToBuffers(xData, yData, n, context, budget, w);
            if (dn < 2) return;

            ensureAggregationBuffers(w);
            ensureXPixelsFromDataX(decX, dn, context, bounds);

            // Determine visible index range and clip bounds.
            final double clipLeft = (clip != null) ? clip.getX() : bounds.getX();
            final double clipRight = (clip != null) ? (clip.getX() + clip.getWidth()) : (bounds.getX() + bounds.getWidth());

            double dataLeft = context.minX() + ((clipLeft - bounds.getX()) / Math.max(1.0, bounds.getWidth())) * context.rangeX();
            double dataRight = context.minX() + ((clipRight - bounds.getX()) / Math.max(1.0, bounds.getWidth())) * context.rangeX();
            if (dataLeft > dataRight) {
                double t = dataLeft;
                dataLeft = dataRight;
                dataRight = t;
            }

            int from = lowerBoundDataX(decX, dn, dataLeft) - 1;
            if (from < 0) from = 0;
            int to = lowerBoundDataX(decX, dn, dataRight) + 1;
            if (to > dn) to = dn;
            if (to - from < 2) return;

            // clear only touched columns
            for (int i = 0; i < touchedSize; i++) {
                int idx = touched[i];
                colSum[idx] = 0.0;
                colCount[idx] = 0;
            }
            touchedSize = 0;

            final double bx = bounds.getX();
            final int xiMin = Math.max(0, (int) Math.floor(clipLeft - bx) - 1);
            final int xiMax = Math.min(w - 1, (int) Math.ceil(clipRight - bx) + 1);

            // Aggregate only the visible subset using cached x pixels (computed from decX).
            for (int i = from; i < to; i++) {
                double px = xPix[i];
                if (px < clipLeft || px > clipRight) continue;
                int xi = (int) (px - bx);
                if (xi < xiMin || xi > xiMax) continue;

                if (colCount[xi] == 0) {
                    touched[touchedSize++] = xi;
                }
                colSum[xi] += decY[i];
                colCount[xi]++;
            }

            if (touchedSize < 2) return;

            // Build compact arrays for rendering in x-order without scanning full width.
            java.util.Arrays.sort(touched, 0, touchedSize);
            double minAgg = Double.POSITIVE_INFINITY;
            double maxAgg = Double.NEGATIVE_INFINITY;
            for (int ti = 0; ti < touchedSize; ti++) {
                int xi = touched[ti];
                colX[ti] = bx + xi + 0.5;
                double v = colSum[xi] / colCount[xi];
                colVal[ti] = v;
                if (v < minAgg) minAgg = v;
                if (v > maxAgg) maxAgg = v;
            }

            renderHorizonAggregated(g2, model, context, bounds, colX, colVal, touchedSize, minAgg, maxAgg);
            return;
        }

        // Non-aggregated path (smaller datasets): use high precision mapping.
        ensureSeriesBuffers(n);
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], map);
            xs[i] = map[0];
            vals[i] = yData[i];
        }
        renderHorizon(g2, model, context, bounds, xs, vals, n);
    }

    private void ensureSeriesBuffers(int n) {
        if (xs == null || xs.length < n) {
            xs = new double[n];
            vals = new double[n];
        }
    }

    private void ensureAggregationBuffers(int w) {
        if (colSum == null || colSum.length < w) {
            colSum = new double[w];
            colCount = new int[w];
            colX = new double[w];
            colVal = new double[w];
            touched = new int[w];
        }
        if (posIdx == null || posIdx.length < w) {
            posIdx = new byte[w];
            negIdx = new byte[w];
        }
        if (posBand == null) posBand = new Path2D.Double(Path2D.WIND_NON_ZERO);
        if (negBand == null) negBand = new Path2D.Double(Path2D.WIND_NON_ZERO);
    }

    private void ensureXPixelsFromDataX(double[] dataX, int n, PlotContext context, Rectangle bounds) {
        if (xPix == null || xPix.length < n) {
            xPix = new double[n];
            xPixKey = 0;
        }

        long key = xPixKey(context, bounds, n);
        if (key == xPixKey) return;
        xPixKey = key;

        double minX = context.minX();
        double maxX = context.maxX();
        double rangeX = maxX - minX;
        if (rangeX == 0) rangeX = 1.0;

        double bx = bounds.getX();
        double w = bounds.getWidth();
        double sx = w / rangeX;

        for (int i = 0; i < n; i++) {
            xPix[i] = Math.fma((dataX[i] - minX), sx, bx);
        }
    }

    private int decimateMinMaxPerPixelToBuffers(double[] xData, double[] yData, int n,
                                                PlotContext context,
                                                int maxOutputPoints,
                                                int plotWidthPx) {
        if (n == 0) return 0;

        if (n <= 1) {
            ensureDecimationBuffers(1);
            if (n == 1) {
                decX[0] = xData[0];
                decY[0] = yData[0];
                return 1;
            }
            return 1;
        }
        double minX = context.minX();
        double maxX = context.maxX();
        double rangeX = maxX - minX;
        if (!(rangeX > 0)) {
            ensureDecimationBuffers(2);
            decX[0] = xData[0];
            decY[0] = yData[0];
            decX[1] = xData[n - 1];
            decY[1] = yData[n - 1];
            return 2;
        }
        int buckets = Math.max(1, plotWidthPx);
        int budget = (maxOutputPoints > 0) ? maxOutputPoints : Integer.MAX_VALUE;
        ensureDecimationBuffers(Math.min(budget, buckets * 2 + 2));
        int out = 0;
        int effectiveBudget = Math.max(0, budget - 2);
        decX[out] = xData[0];
        decY[out] = yData[0];
        out++;
        int lastBucket = -1;
        double minY = 0, maxY = 0;
        boolean have = false;
        double minPX = 0, maxPX = 0;
        for (int i = 0; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            int bucket = (int) (((x - minX) / rangeX) * buckets);
            bucket = (int) MathUtils.clamp(bucket, 0, buckets - 1);
            if (bucket != lastBucket) {
                if (have) {
                    if (out < effectiveBudget) {
                        decX[out] = minPX;
                        decY[out] = minY;
                        out++;
                    }
                    if (out < effectiveBudget && maxY != minY) {
                        decX[out] = maxPX;
                        decY[out] = maxY;
                        out++;
                    }
                    if (out >= effectiveBudget) break;
                }
                lastBucket = bucket;
                minY = y;
                maxY = y;
                minPX = x;
                maxPX = x;
                have = true;
            } else {
                if (y < minY) {
                    minY = y;
                    minPX = x;
                }
                if (y > maxY) {
                    maxY = y;
                    maxPX = x;
                }
            }
        }
        if (have && out < effectiveBudget) {
            decX[out] = minPX;
            decY[out] = minY;
            out++;
            if (out < effectiveBudget && maxY != minY) {
                decX[out] = maxPX;
                decY[out] = maxY;
                out++;
            }
        }
        if (out < budget) {
            decX[out] = xData[n - 1];
            decY[out] = yData[n - 1];
            out++;
        } else {
            decX[out - 1] = xData[n - 1];
            decY[out - 1] = yData[n - 1];
        }
        for (int i = 1; i < out; i++) {
            if (decX[i] < decX[i - 1]) {
                double tx = decX[i - 1];
                double ty = decY[i - 1];
                decX[i - 1] = decX[i];
                decY[i - 1] = decY[i];
                decX[i] = tx;
                decY[i] = ty;
            }
        }

        return out;
    }

    private void ensureDecimationBuffers(int minSize) {
        if (decX == null || decX.length < minSize) {
            decX = new double[minSize];
            decY = new double[minSize];
        }
    }

    private void ensureBandColors(ChartModel model, PlotContext context) {
        int key = System.identityHashCode(Toolkit.getDefaultToolkit());
        if (bandColors != null && uiKey == key) return;
        uiKey = key;

        Color[] colors = new Color[MAX_BANDS];
        if (isMultiColor()) {
            for (int b = 0; b < MAX_BANDS; b++) {
                Color c = themeSeries(context, b);
                if (c == null) c = getSeriesColor(model);
                colors[b] = c;
            }
        } else {
            Color base = getSeriesColor(model);
            for (int b = 0; b < MAX_BANDS; b++) {
                int r = clamp255(base.getRed() - b * 18);
                int g = clamp255(base.getGreen() - b * 14);
                int bl = clamp255(base.getBlue() - b * 10);
                colors[b] = com.arbergashi.charts.util.ColorRegistry.of(r, g, bl, 255);
            }
        }
        bandColors = colors;
    }

    private void renderHorizon(Graphics2D g2, ChartModel model, PlotContext context, Rectangle bounds,
                               double[] xsArr, double[] vArr, int n) {
        ensureBandColors(model, context);

        // Find min/max of values (data-space).
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double v = vArr[i];
            if (v < minV) minV = v;
            if (v > maxV) maxV = v;
        }
        if (!Double.isFinite(minV) || !Double.isFinite(maxV)) return;
        double range = maxV - minV;
        if (range <= 0) return;

        // Horizon charts fold values into bands. Use baseline at mid.
        double base = (minV + maxV) * 0.5;
        double halfRange = Math.max(1e-9, Math.max(maxV - base, base - minV));

        int bands = MAX_BANDS;
        if (n > 20000) bands = 3;
        if (n > 50000) bands = 2;

        g2.setStroke(getSeriesStroke());

        Path2D.Double path = poly();
        double by = bounds.getY();
        double bh = bounds.getHeight();

        // Progressive bands: higher magnitude => higher band (folded towards center).
        for (int b = 0; b < bands; b++) {
            double thresholdLo = (b / (double) bands) * halfRange;
            double thresholdHi = ((b + 1) / (double) bands) * halfRange;

            // Positive side
            buildBandPath(path, xsArr, vArr, n, base, thresholdLo, thresholdHi, true, by, bh);
            g2.setColor(bandColors[b]);
            g2.fill(path);

            // Negative side (slightly darker)
            buildBandPath(path, xsArr, vArr, n, base, thresholdLo, thresholdHi, false, by, bh);
            g2.setColor(bandColors[Math.min(bandColors.length - 1, b)]);
            g2.fill(path);
        }
    }

    private void buildBandPath(Path2D.Double path,
                               double[] xsArr, double[] vArr, int n,
                               double base, double lo, double hi,
                               boolean positive,
                               double by, double bh) {
        path.reset();

        // We map a band slice into the full plot height by shifting towards center.
        // Convert the "excess over lo" (clamped to band height) into pixel y.
        boolean started = false;
        double lastX = xsArr[0];

        for (int i = 0; i < n; i++) {
            double v = vArr[i] - base;
            if (!positive) v = -v;
            if (v <= lo) {
                lastX = xsArr[i];
                continue;
            }

            double vv = Math.min(hi, v);
            double t = (vv - lo) / Math.max(1e-9, (hi - lo));

            // Folded y position: top for high magnitude, center for low.
            // Use data-space mapping only for x; y is computed directly.
            double y = by + bh - (t * bh);

            double x = xsArr[i];
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
            lastX = x;
        }

        if (!started) {
            // empty band
            path.moveTo(xsArr[0], by + bh);
            path.lineTo(xsArr[n - 1], by + bh);
            path.closePath();
            return;
        }

        // Close down to baseline.
        path.lineTo(lastX, by + bh);
        path.lineTo(xsArr[0], by + bh);
        path.closePath();
    }

    private void renderHorizonAggregated(Graphics2D g2, ChartModel model, PlotContext context, Rectangle bounds,
                                         double[] xsArr, double[] vArr, int n, double minV, double maxV) {
        ensureBandColors(model, context);

        if (!Double.isFinite(minV) || !Double.isFinite(maxV)) return;
        double range = maxV - minV;
        if (range <= 0) return;

        double base = (minV + maxV) * 0.5;
        double halfRange = Math.max(1e-9, Math.max(maxV - base, base - minV));

        int bands = MAX_BANDS;
        if (n > 20000) bands = 3;
        if (n > 50000) bands = 2;

        final double by = bounds.getY();
        final double bh = bounds.getHeight();
        final double bandH = bh / (double) bands;

        // Compute per-column band index for positive/negative magnitudes.
        final double invBand = bands / halfRange;
        for (int i = 0; i < n; i++) {
            double dv = vArr[i] - base;
            if (dv > 0) {
                double mag = dv;
                if (mag > halfRange) mag = halfRange;
                int b = (int) (mag * invBand);
                if (b >= bands) b = bands - 1;
                posIdx[i] = (byte) (b + 1); // 1..bands
                negIdx[i] = 0;
            } else if (dv < 0) {
                double mag = -dv;
                if (mag > halfRange) mag = halfRange;
                int b = (int) (mag * invBand);
                if (b >= bands) b = bands - 1;
                negIdx[i] = (byte) (b + 1);
                posIdx[i] = 0;
            } else {
                posIdx[i] = 0;
                negIdx[i] = 0;
            }
        }

        // Fill from low bands to high bands. Each band is one filled strip.
        // This makes the render cost O(bands) instead of O(width).
        for (int b = 0; b < bands; b++) {
            // Positive
            buildBandStrip(posBand, xsArr, posIdx, n, b + 1, by, bandH, bands);
            if (posBand.getCurrentPoint() != null) {
                g2.setColor(bandColors[b]);
                g2.fill(posBand);
            }

            // Negative
            buildBandStrip(negBand, xsArr, negIdx, n, b + 1, by, bandH, bands);
            if (negBand.getCurrentPoint() != null) {
                g2.setColor(bandColors[b]);
                g2.fill(negBand);
            }
        }
    }
}
