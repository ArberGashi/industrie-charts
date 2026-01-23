package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Legacy horizon chart renderer.
 *
 * <p>This class is kept for backward compatibility but is no longer auto-registered (see {@link HorizonRenderer}).
 * It is still safe to instantiate directly.
 *
 * <p><b>Performance notes</b>:
 * <ul>
 *   <li>Uses allocation-free pixel mapping via {@link PlotContext#mapToPixel(double, double, double[])}.</li>
 *   <li>Reuses arrays and cached colors to avoid GC pressure in {@code paintComponent}.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class HorizonChartRenderer extends BaseRenderer {

    // Reusable map buffer to avoid Point2D allocations.
    private final double[] map = new double[2];
    private transient double[] xs;
    private transient double[] ys;
    // Cache colors per band (recomputed only if base color changes).
    private transient int colorKey;
    private transient Color[] bandFill;

    // Reused rectangle to avoid per-segment Shape churn.
    private transient Rectangle2D.Double rect;

    public HorizonChartRenderer() {
        super("horizon");
    }

    private static int lowerBound(double[] a, int n, double x) {
        int lo = 0;
        int hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static int lowerBoundX(ChartModel model, double x) {
        int lo = 0;
        int hi = model.getPointCount();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (model.getX(mid) < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static int upperBoundX(ChartModel model, double x) {
        int lo = 0;
        int hi = model.getPointCount();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (model.getX(mid) <= x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        Rectangle2D bounds = context.plotBounds();
        Rectangle clip = g2.getClipBounds();
        if (clip != null && !clip.intersects(bounds)) return;

        // Decimate for very large datasets – this renderer is legacy and prioritizes responsiveness.
        int step = 1;
        if (n > 50_000) step = Math.max(1, n / 12_000);
        if (n > 200_000) step = Math.max(step, n / 20_000);

        // If we have a clip, we can avoid mapping the full series in many cases.
        // We first find an approximate data-space x-range (min/max) and map only potential candidates.
        int from = 0;
        int to = n;
        if (clip != null && n > 2000) {
            // Cheap min/max scan on data X (no allocations)
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i += step) {
                double x = model.getX(i);
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
            }
            if (Double.isFinite(minX) && Double.isFinite(maxX) && maxX > minX) {
                // Map clip pixel x-range back to x-domain proportionally (approximation).
                double t0 = (clip.getX() - bounds.getX()) / Math.max(1e-9, bounds.getWidth());
                double t1 = ((clip.getX() + clip.getWidth()) - bounds.getX()) / Math.max(1e-9, bounds.getWidth());
                if (t0 < 0) t0 = 0;
                if (t0 > 1) t0 = 1;
                if (t1 < 0) t1 = 0;
                if (t1 > 1) t1 = 1;
                if (t1 < t0) {
                    double tmp = t0;
                    t0 = t1;
                    t1 = tmp;
                }

                double x0 = minX + t0 * (maxX - minX);
                double x1 = minX + t1 * (maxX - minX);

                // Expand a bit to be safe.
                double padding = (maxX - minX) * 0.01;
                x0 -= padding;
                x1 += padding;

                // If the model is a monotonic time series, this prunes most points.
                // Otherwise we just keep full range.
                if (model.getX(0) <= model.getX(n - 1)) {
                    from = lowerBoundX(model, x0);
                    to = upperBoundX(model, x1);
                }
            }
        }

        if (from < 0) from = 0;
        if (to > n) to = n;
        if (to - from < 2) return;

        int m = ((to - from) + step - 1) / step;
        ensureBuffers(m);

        int mi = 0;
        for (int i = from; i < to; i += step) {
            context.mapToPixel(model.getX(i), model.getY(i), map);
            xs[mi] = map[0];
            ys[mi] = map[1];
            mi++;
        }
        if (mi < 2) return;

        final int bands = 4;
        double bandHeight = bounds.getHeight() / (double) bands;

        Color base = seriesOrBase(model, context, 0);
        ensureBandColors(base, context);

        final double by = bounds.getY();
        final double invBh = 1.0 / Math.max(1e-9, bounds.getHeight());

        // Clip-aware x range (avoid scanning all segments when only a portion is visible)
        final double clipLeft = (clip != null) ? clip.getX() : bounds.getX();
        final double clipRight = (clip != null) ? (clip.getX() + clip.getWidth()) : (bounds.getX() + bounds.getWidth());

        // Find first/last visible segment indices; if xs are monotonic, use binary search.
        int iStart = 0;
        int iEnd = mi - 2;
        if (mi > 2 && xs[0] <= xs[mi - 1]) {
            iStart = lowerBound(xs, mi, clipLeft) - 1;
            if (iStart < 0) iStart = 0;
            iEnd = lowerBound(xs, mi, clipRight);
            if (iEnd > mi - 2) iEnd = mi - 2;
        }
        if (iEnd < iStart) iEnd = iStart;

        // draw positive and negative bands shifted vertically
        for (int b = 0; b < bands; b++) {
            g2.setColor(bandFill[b]);

            double top = by + (bands - b - 1) * bandHeight;
            for (int i = iStart; i <= iEnd; i++) {
                double x1 = xs[i];
                double x2 = xs[i + 1];

                // quick reject outside clip
                if (x2 < clipLeft || x1 > clipRight) continue;

                double wSeg = x2 - x1;
                if (wSeg <= 0) continue;

                double y = ys[i];
                // Normalize y within the plot bounds and scale to band height.
                double rel = (y - by) * invBh;
                if (rel < 0) rel = -rel;
                if (rel > 1) rel = 1;

                double rectH = Math.max(1.0, bandHeight * rel);

                Rectangle2D r = RendererAllocationCache.getRectangle(this, "rect");
                r.setRect(x1, top + (bandHeight - rectH), wSeg, rectH);
                g2.fill(r);
            }
        }
    }

    private void ensureBuffers(int n) {
        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ys", n);
        }
    }

    private void ensureBandColors(Color base, PlotContext context) {
        final int bands = 4;
        int key = (base.getRGB() * 31) ^ bands ^ (isMultiColor() ? 1 : 0);
        if (bandFill != null && colorKey == key) return;
        colorKey = key;

        Color[] fill = new Color[bands];
        if (isMultiColor()) {
            for (int b = 0; b < bands; b++) {
                Color c = themeSeries(context, b);
                if (c == null) c = base;
                fill[b] = c;
            }
        } else {
            for (int b = 0; b < bands; b++) {
                float t = (b + 1) / (float) bands;
                fill[b] = new Color(
                        Math.min(255, (int) (base.getRed() * (0.5 + t / 2.0))),
                        Math.min(255, (int) (base.getGreen() * (0.5 + t / 2.0))),
                        Math.min(255, (int) (base.getBlue() * (0.5 + t / 2.0))));
            }
        }
        bandFill = fill;
    }
}
