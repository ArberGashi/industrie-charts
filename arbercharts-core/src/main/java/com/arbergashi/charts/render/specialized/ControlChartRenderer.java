package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.CompositePool;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * ControlChartRenderer (Shewhart Chart / SPC).
 * Visualizes process stability with center line (CL) and control limits (UCL/LCL).
 * Optimized for large datasets with adaptive rendering modes.
 * Highlights outliers beyond control limits.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class ControlChartRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("control_chart", new RendererDescriptor("control_chart", "renderer.control_chart", "/icons/control_chart.svg"), ControlChartRenderer::new);
    }

    // Reusable mapping buffer (avoid Point2D allocations in non-heavy path).
    private final double[] mapBuf = new double[2];
    // Cached resources (avoid allocations in draw)
    private transient float cachedDashWidth = -1f;
    private transient Stroke cachedLimitStroke;
    private transient Font cachedLabelFont;
    private transient float cachedLabelFontSize = -1f;
    // Reusable path for the main series line
    private transient Path2D path;
    // Reusable pixel buffers
    private transient double[] px;

    // Bucketing buffers for extreme point counts
    // (removed: older intermediate bucketing buffers; heavy mode now uses direct series arrays below)

    // Outlier bucketing buffers (for extreme outlier rates)
    // (removed: older intermediate outlier buffers)

    // Line bucketing buffers (min/max series y per pixel column)
    // (removed: older intermediate line buffers)
    private transient double[] py;
    // Fast-mapping parameters (valid only when heavy mode is active for the current render)
    private transient boolean fastMappingActive;
    private transient double fastMinY;
    private transient double fastMaxY;
    // Direct-series bucketing buffers (avoid per-point px/py arrays in heavy mode)
    private transient boolean[] seriesUsed;
    private transient double[] seriesMinY;
    private transient double[] seriesMaxY;
    private transient boolean[] seriesOutUsed;
    private transient double[] seriesOutMinY;
    private transient double[] seriesOutMaxY;
    // Cached stats/bounds to avoid O(n) scans on every repaint.
    private transient int statsKey;
    private transient double statsMean;
    private transient double statsUcl;
    private transient double statsLcl;
    private transient double statsMinX;
    private transient double statsMaxX;
    private transient double statsMinY;
    private transient double statsMaxY;
    private transient boolean statsValid;
    // Theme cache for derived colors to avoid per-paint theme lookups.
    private transient int themeColorKey;
    private transient Color cachedLimitColor;
    private transient Color cachedMeanColor;
    private transient Color cachedLineColor;
    private transient Color cachedNormalColor;
    private transient Color cachedOutlierColor;

    // Touched-column optimization for heavy mode resets.
    private transient int[] touchedCols;
    private transient int touchedCount;
    private transient int[] touchedOutCols;
    private transient int touchedOutCount;

    public ControlChartRenderer() {
        super("control_chart");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // Skip work if not visible.
        Rectangle clipBounds = g2.getClipBounds();
        if (clipBounds != null && !clipBounds.intersects(context.plotBounds().getBounds())) return;


        // Update cached stats only when data changes.
        ensureStats(xData, yData, n);

        double mean = statsMean;
        double ucl = statsUcl;
        double lcl = statsLcl;

        boolean heavy = n >= 5000;
        fastMappingActive = heavy;

        Shape oldClip = g2.getClip();
        g2.clip(context.plotBounds());
        try {
        ensureThemeColors(context);

            if (!heavy) {
                // High-fidelity mapping path (allocation-free)
                ensurePixelBuffers(n);
                for (int i = 0; i < n; i++) {
                    context.mapToPixel(xData[i], yData[i], mapBuf);
                    px[i] = mapBuf[0];
                    py[i] = mapBuf[1];
                }

                drawControlZones(g2, mean, ucl, lcl, context);
                drawDataLineFast(g2, n);
                drawPointsAndOutliersFast(g2, yData, n, ucl, lcl);
                return;
            }

            // Heavy mode: one-pass direct bucketing (no px/py fill for every point)
            drawControlZones(g2, mean, ucl, lcl, context);
            drawSeriesAndOutliersBounded(g2, xData, yData, n, context, ucl, lcl);

        } finally {
            g2.setClip(oldClip);
            fastMappingActive = false;
        }
    }

    private void ensureThemeColors(PlotContext context) {
        ChartTheme theme = resolveTheme(context);
        int key = System.identityHashCode(theme) * 31 + getLayerIndex() + (isMultiColor() ? 1 : 0);
        if (key == themeColorKey && cachedLimitColor != null) return;
        themeColorKey = key;

        if (isMultiColor()) {
            cachedLineColor = theme.getSeriesColor(getLayerIndex());
            cachedMeanColor = theme.getSeriesColor(getLayerIndex() + 1);
            cachedLimitColor = theme.getSeriesColor(getLayerIndex() + 2);
            if (cachedMeanColor == null) cachedMeanColor = theme.getAccentColor();
            if (cachedLimitColor == null) cachedLimitColor = theme.getBearishColor();
        } else {
            cachedLimitColor = theme.getBearishColor();
            cachedMeanColor = theme.getAccentColor();
            cachedLineColor = theme.getSeriesColor(getLayerIndex());
        }
        cachedNormalColor = cachedLineColor;
        cachedOutlierColor = cachedLimitColor;
    }

    private double mapYFast(double yValue, PlotContext context) {
        double by = context.plotBounds().getY();
        double bh = Math.max(1e-9, context.plotBounds().getHeight());
        double dy = (fastMaxY - fastMinY);
        if (dy == 0) dy = 1.0;
        double t = (yValue - fastMinY) / dy;
        return by + bh - t * bh;
    }

    private void ensurePixelBuffers(int n) {
        if (px == null || px.length < n) {
            px = new double[n];
            py = new double[n];
        }
    }

    private void ensureCaches(Graphics2D g2) {
        float dashWidth = ChartScale.scale(5f);
        if (cachedLimitStroke == null || cachedDashWidth != dashWidth) {
            cachedDashWidth = dashWidth;
            float[] dash = {dashWidth, dashWidth};
            cachedLimitStroke = new BasicStroke(
                    ChartScale.scale(1.5f),
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f,
                    dash,
                    0.0f);
        }

        float fontSize = ChartScale.scale(10f);
        if (cachedLabelFont == null || cachedLabelFontSize != fontSize) {
            cachedLabelFontSize = fontSize;
            cachedLabelFont = g2.getFont().deriveFont(Font.BOLD, fontSize);
        }

        if (path == null) {
            path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        }
    }

    private void drawControlZones(Graphics2D g2, double mean, double ucl, double lcl, PlotContext context) {
        ensureCaches(g2);

        double xStart = context.plotBounds().getX();
        double xEnd = context.plotBounds().getMaxX();

        // y locations
        final double yMean;
        final double yUcl;
        final double yLcl;
        if (fastMappingActive) {
            yMean = mapYFast(mean, context);
            yUcl = mapYFast(ucl, context);
            yLcl = mapYFast(lcl, context);
        } else {
            context.mapToPixel(0, mean, mapBuf);
            yMean = mapBuf[1];
            context.mapToPixel(0, ucl, mapBuf);
            yUcl = mapBuf[1];
            context.mapToPixel(0, lcl, mapBuf);
            yLcl = mapBuf[1];
        }

        Stroke prevStroke = g2.getStroke();
        Color prevColor = g2.getColor();

        g2.setColor(cachedLimitColor);
        g2.setStroke(cachedLimitStroke);
        g2.draw(getLine(xStart, yUcl, xEnd, yUcl));
        g2.draw(getLine(xStart, yLcl, xEnd, yLcl));

        g2.setColor(cachedMeanColor);
        g2.setStroke(getCachedStroke(ChartScale.scale(2.0f)));
        g2.draw(getLine(xStart, yMean, xEnd, yMean));

        Font labelFont = cachedLabelFont;
        Color labelColor = g2.getColor();
        drawI18nLabel(g2, "renderer.control_chart.ucl", labelFont, labelColor, (float) xStart + 5, (float) yUcl - 5);
        drawI18nLabel(g2, "renderer.control_chart.lcl", labelFont, labelColor, (float) xStart + 5, (float) yLcl + 15);
        drawI18nLabel(g2, "renderer.control_chart.cl", labelFont, labelColor, (float) xStart + 5, (float) yMean - 5);

        g2.setColor(prevColor);
        g2.setStroke(prevStroke);
    }

    private void drawDataLineFast(Graphics2D g2, int n) {
        ensureCaches(g2);

        g2.setColor(cachedLineColor);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f)));

        // Decimate for very large n (polyline cost dominates) while preserving overall shape.
        int step = 1;
        if (n > 20_000) step = Math.max(1, n / 8_000);
        if (n > 100_000) step = Math.max(step, n / 12_000);

        path.reset();
        boolean started = false;
        for (int i = 0; i < n; i += step) {
            double x = px[i];
            double y = py[i];
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        // ensure last point is included
        if (n > 1 && ((n - 1) % step) != 0) {
            path.lineTo(px[n - 1], py[n - 1]);
        }
        g2.draw(path);
    }


    private void drawPointsAndOutliersFast(Graphics2D g2, double[] yData, int n, double ucl, double lcl) {
        double dotSize = ChartScale.scale(6.0);
        Color outlierColor = cachedOutlierColor;

        int step = 1;
        if (n > 30_000) step = Math.max(1, n / 10_000);

        Composite prevComp = g2.getComposite();
        final Composite outlierGlow = CompositePool.get(0.3f);

        for (int i = 0; i < n; i += step) {
            double val = yData[i];
            double x = px[i];
            double y = py[i];
            boolean isOutlier = val > ucl || val < lcl;

            if (isOutlier) {
                g2.setComposite(outlierGlow);
                g2.setColor(outlierColor);
                double outerSize = dotSize * 2.5;
                g2.fill(getEllipse(x - outerSize / 2, y - outerSize / 2, outerSize, outerSize));
                g2.setComposite(prevComp);
                g2.setColor(outlierColor);
            } else {
                g2.setComposite(prevComp);
                g2.setColor(cachedNormalColor);
            }

            g2.fill(getEllipse(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize));
        }

        // ensure outliers are always rendered even if sampling skipped them
        if (step > 1) {
            for (int i = 0; i < n; i++) {
                double val = yData[i];
                if (val <= ucl && val >= lcl) continue;
                double x = px[i];
                double y = py[i];
                g2.setComposite(outlierGlow);
                g2.setColor(outlierColor);
                double outerSize = dotSize * 2.5;
                g2.fill(getEllipse(x - outerSize / 2, y - outerSize / 2, outerSize, outerSize));
                g2.setComposite(prevComp);
                g2.setColor(outlierColor);
                g2.fill(getEllipse(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize));
            }
        }

        g2.setComposite(prevComp);
    }

    private void ensureStats(double[] xData, double[] yData, int n) {
        int key = System.identityHashCode(yData) * 31 + n;
        if (statsValid && key == statsKey) {
            return;
        }

        // One pass using Welford's algorithm: mean + variance + min/max (stable and fast).
        double mean = 0.0;
        double m2 = 0.0;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            double x = xData[i];
            double y = yData[i];

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;

            double delta = y - mean;
            mean += delta / (i + 1);
            double delta2 = y - mean;
            m2 += delta * delta2;
        }

        double variance = (n > 1) ? (m2 / n) : 0.0;
        double stdDev = Math.sqrt(variance);

        double ucl = mean + 3 * stdDev;
        double lcl = mean - 3 * stdDev;

        // Expand Y bounds to include control limits
        if (lcl < minY) minY = lcl;
        if (ucl > maxY) maxY = ucl;

        statsKey = key;
        statsMean = mean;
        statsUcl = ucl;
        statsLcl = lcl;
        statsMinX = minX;
        statsMaxX = maxX;
        statsMinY = minY;
        statsMaxY = maxY;
        statsValid = true;
    }

    private void drawSeriesAndOutliersBounded(Graphics2D g2, double[] xData, double[] yData, int n, PlotContext context, double ucl, double lcl) {
        Rectangle bounds = context.plotBounds().getBounds();
        int w = Math.max(1, bounds.width);
        if (w <= 1) return;

        if (seriesUsed == null || seriesUsed.length != w) {
            seriesUsed = new boolean[w];
            seriesMinY = new double[w];
            seriesMaxY = new double[w];

            seriesOutUsed = new boolean[w];
            seriesOutMinY = new double[w];
            seriesOutMaxY = new double[w];

            touchedCols = new int[w];
            touchedOutCols = new int[w];
        }

        // reset only touched columns from previous frame (avoids O(width) clear spikes)
        for (int ti = 0; ti < touchedCount; ti++) {
            int col = touchedCols[ti];
            seriesUsed[col] = false;
            seriesMinY[col] = Double.POSITIVE_INFINITY;
            seriesMaxY[col] = Double.NEGATIVE_INFINITY;
        }
        for (int ti = 0; ti < touchedOutCount; ti++) {
            int col = touchedOutCols[ti];
            seriesOutUsed[col] = false;
            seriesOutMinY[col] = Double.POSITIVE_INFINITY;
            seriesOutMaxY[col] = Double.NEGATIVE_INFINITY;
        }
        touchedCount = 0;
        touchedOutCount = 0;

        double minX = statsMinX;
        double maxX = statsMaxX;
        double minY = statsMinY;
        double maxY = statsMaxY;

        double bx = context.plotBounds().getX();
        double by = context.plotBounds().getY();
        double bw = Math.max(1e-9, context.plotBounds().getWidth());
        double bh = Math.max(1e-9, context.plotBounds().getHeight());

        double dx = (maxX - minX);
        double dy = (maxY - minY);
        if (dx == 0) dx = 1.0;
        if (dy == 0) dy = 1.0;

        double invDx = 1.0 / dx;
        double invDy = 1.0 / dy;

        int left = bounds.x;
        int right = bounds.x + bounds.width;

        // One pass: bucket series and outliers
        for (int i = 0; i < n; i++) {
            double x = xData[i];
            double yv = yData[i];

            // map x -> pixel column
            double xPix = bx + (x - minX) * bw * invDx;
            if (xPix < left || xPix >= right) continue;
            int col = (int) (xPix - left);
            if (col < 0 || col >= w) continue;

            // map y -> pixel y
            double yPix = by + bh - (yv - minY) * bh * invDy;

            if (yv > ucl || yv < lcl) {
                if (!seriesOutUsed[col]) {
                    seriesOutUsed[col] = true;
                    touchedOutCols[touchedOutCount++] = col;
                    seriesOutMinY[col] = yPix;
                    seriesOutMaxY[col] = yPix;
                } else {
                    if (yPix < seriesOutMinY[col]) seriesOutMinY[col] = yPix;
                    if (yPix > seriesOutMaxY[col]) seriesOutMaxY[col] = yPix;
                }
            } else {
                if (!seriesUsed[col]) {
                    seriesUsed[col] = true;
                    touchedCols[touchedCount++] = col;
                    seriesMinY[col] = yPix;
                    seriesMaxY[col] = yPix;
                } else {
                    if (yPix < seriesMinY[col]) seriesMinY[col] = yPix;
                    if (yPix > seriesMaxY[col]) seriesMaxY[col] = yPix;
                }
            }
        }

        // Draw the series line as vertical fibers (bounded by width)
        g2.setColor(cachedLineColor);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f)));
        for (int col = 0; col < w; col++) {
            if (!seriesUsed[col]) continue;
            double x = left + col + 0.5;
            double y1 = seriesMinY[col];
            double y2 = seriesMaxY[col];
            g2.draw(getLine(x, y1, x, y2));
        }

        // Draw normal min/max points (bounded by width)
        double dotSize = ChartScale.scale(6.0);
        double halfDot = dotSize / 2.0;
        g2.setColor(cachedNormalColor);
        for (int col = 0; col < w; col++) {
            if (!seriesUsed[col]) continue;
            double x = left + col + 0.5;
            double yMin = seriesMinY[col];
            double yMax = seriesMaxY[col];
            g2.fill(getEllipse(x - halfDot, yMin - halfDot, dotSize, dotSize));
            if (yMax != yMin) {
                g2.fill(getEllipse(x - halfDot, yMax - halfDot, dotSize, dotSize));
            }
        }

        // Draw outliers (bounded by width)
        double outerSize = dotSize * 2.5;
        double outerHalf = outerSize / 2.0;
        Composite prevComp = g2.getComposite();
        final Composite outlierGlow = CompositePool.get(0.3f);

        for (int col = 0; col < w; col++) {
            if (!seriesOutUsed[col]) continue;
            double x = left + col + 0.5;
            double yMin = seriesOutMinY[col];
            double yMax = seriesOutMaxY[col];

            g2.setComposite(outlierGlow);
            g2.setColor(cachedOutlierColor);
            g2.fill(getEllipse(x - outerHalf, yMin - outerHalf, outerSize, outerSize));
            if (yMax != yMin) {
                g2.fill(getEllipse(x - outerHalf, yMax - outerHalf, outerSize, outerSize));
            }

            g2.setComposite(prevComp);
            g2.setColor(cachedOutlierColor);
            g2.fill(getEllipse(x - halfDot, yMin - halfDot, dotSize, dotSize));
            if (yMax != yMin) {
                g2.fill(getEllipse(x - halfDot, yMax - halfDot, dotSize, dotSize));
            }
        }

        g2.setComposite(prevComp);

        // Keep fast-mapping info consistent for any subsequent calls
        fastMinY = minY;
        fastMaxY = maxY;
    }
}
