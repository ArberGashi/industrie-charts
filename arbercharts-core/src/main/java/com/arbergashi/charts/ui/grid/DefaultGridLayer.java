package com.arbergashi.charts.ui.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.NiceScale;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Default grid layer for general-purpose charts.
 * Renders a clean, adaptive grid using NiceScale for tick positioning.
 *
 * <p>All styling is theme-driven via UIManager properties (e.g., FlatLaf themes).
 * This enables Bloomberg/SciChart-grade grid quality without code changes.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class DefaultGridLayer implements GridLayer {

    // Theme property keys
    private static final String KEY_MINOR_ALPHA = "Chart.defaultGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.defaultGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.defaultGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.defaultGrid.majorStrokeWidth";
    private static final String KEY_MINOR_DASH_ON = "Chart.defaultGrid.minorDash.on";
    private static final String KEY_MINOR_DASH_OFF = "Chart.defaultGrid.minorDash.off";
    private static final String KEY_FRAME_ALPHA = "Chart.defaultGrid.frameAlpha";
    private static final String KEY_FRAME_STROKE = "Chart.defaultGrid.frameStrokeWidth";
    private static final String KEY_ZERO_ALPHA = "Chart.defaultGrid.zeroLineAlpha";
    private static final String KEY_ZERO_STROKE = "Chart.defaultGrid.zeroLineStrokeWidth";

    // Reusable line and buffer to avoid per-frame allocations
    private final Line2D.Double line = new Line2D.Double();
    private final double[] buf = new double[2];
    private final NiceScale yMinorScale = new NiceScale(0, 1);
    private final NiceScale yMajorScale = new NiceScale(0, 1);
    private final NiceScale xMinorScale = new NiceScale(0, 1);
    private final NiceScale xMajorScale = new NiceScale(0, 1);

    // Cached strokes to avoid per-frame allocations
    private float cachedMinorStrokeWidth = Float.NaN;
    private float cachedMajorStrokeWidth = Float.NaN;
    private float cachedFrameStrokeWidth = Float.NaN;
    private float cachedZeroStrokeWidth = Float.NaN;
    private BasicStroke minorStroke;
    private BasicStroke majorStroke;
    private BasicStroke frameStroke;
    private BasicStroke zeroStroke;

    private float cachedMinorDashWidth = Float.NaN;
    private float cachedMajorDashWidth = Float.NaN;
    private float cachedMinorDashOn = Float.NaN;
    private float cachedMinorDashOff = Float.NaN;
    private float cachedMajorDashOn = Float.NaN;
    private float cachedMajorDashOff = Float.NaN;
    private final float[] minorDashPattern = new float[]{1f, 1f};
    private final float[] majorDashPattern = new float[]{1f, 1f};
    private BasicStroke minorDashStroke;
    private BasicStroke majorDashStroke;

    // Cached colors to avoid per-frame allocations
    private int cachedGridRgb = 0;
    private int cachedAxisRgb = 0;
    private float cachedMinorAlpha = Float.NaN;
    private float cachedMajorAlpha = Float.NaN;
    private float cachedFrameAlpha = Float.NaN;
    private float cachedZeroAlpha = Float.NaN;
    private Color minorColor;
    private Color majorColor;
    private Color frameColor;
    private Color zeroLineColor;

    @Override
    public void renderGrid(Graphics2D g, PlotContext context) {
        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();
        Rectangle2D bounds = context.plotBounds();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        // Pixel-aware density to avoid overcrowding
        int w = Math.max(1, (int) Math.ceil(bounds.getWidth()));
        int h = Math.max(1, (int) Math.ceil(bounds.getHeight()));

        // Target spacing in pixels (roughly): ~80px major, ~40px minor
        int majorXTicks = Math.max(3, Math.min(12, w / 90));
        int majorYTicks = Math.max(3, Math.min(10, h / 70));

        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));

        // Crisp grid lines
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldStroke = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color base = theme.getGridColor();

        // Theme-driven alpha and stroke values
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.22f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        float minorDashOn = ChartAssets.getUIFloat(KEY_MINOR_DASH_ON, 2.0f);
        float minorDashOff = ChartAssets.getUIFloat(KEY_MINOR_DASH_OFF, 5.0f);
        float frameAlpha = ChartAssets.getUIFloat(KEY_FRAME_ALPHA, 0.52f);
        float frameStrokeWidth = ChartAssets.getUIFloat(KEY_FRAME_STROKE, 1.0f);
        float zeroAlpha = ChartAssets.getUIFloat(KEY_ZERO_ALPHA, 0.45f);
        float zeroStrokeWidth = ChartAssets.getUIFloat(KEY_ZERO_STROKE, 1.1f);

        updateCachedColors(base, theme.getAxisLabelColor(), minorAlpha, majorAlpha, frameAlpha, zeroAlpha);

        // --- Y grid: minor then major ---
        yMinorScale.setRange(context.minY(), context.maxY());
        yMinorScale.setMaxTicks(minorYTicks);
        g.setColor(minorColor);
        g.setStroke(getMinorStroke(ChartScale.scale(minorStrokeWidth)));
        double yMinorSpacing = yMinorScale.getTickSpacing();
        if (yMinorSpacing > 0) {
            double tick = yMinorScale.getNiceMin();
            double end = yMinorScale.getNiceMax();
            double epsilon = yMinorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMinorSpacing) {
                context.mapToPixel(context.minX(), tick, buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    line.setLine(minX, y, maxX, y);
                    g.draw(line);
                }
            }
        }

        yMajorScale.setRange(context.minY(), context.maxY());
        yMajorScale.setMaxTicks(majorYTicks);
        g.setColor(majorColor);
        g.setStroke(getMajorStroke(ChartScale.scale(majorStrokeWidth)));
        double yMajorSpacing = yMajorScale.getTickSpacing();
        if (yMajorSpacing > 0) {
            double tick = yMajorScale.getNiceMin();
            double end = yMajorScale.getNiceMax();
            double epsilon = yMajorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMajorSpacing) {
                context.mapToPixel(context.minX(), tick, buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    line.setLine(minX, y, maxX, y);
                    g.draw(line);
                }
            }
        }

        // --- X grid: minor (dashed) then major (dashed stronger) ---
        xMinorScale.setRange(context.minX(), context.maxX());
        xMinorScale.setMaxTicks(minorXTicks);
        g.setStroke(getMinorDashStroke(ChartScale.scale(minorStrokeWidth),
                ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff)));
        g.setColor(minorColor);
        double lastDrawX = Double.NEGATIVE_INFINITY;
        double minPixelSpacing = ChartScale.scale(5);
        double xMinorSpacing = xMinorScale.getTickSpacing();
        if (xMinorSpacing > 0) {
            double tick = xMinorScale.getNiceMin();
            double end = xMinorScale.getNiceMax();
            double epsilon = xMinorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMinorSpacing) {
                context.mapToPixel(tick, context.minY(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    if (Double.isFinite(lastDrawX) && Math.abs(x - lastDrawX) < minPixelSpacing) continue;
                    line.setLine(x, minY, x, maxY);
                    g.draw(line);
                    lastDrawX = x;
                }
            }
        }

        xMajorScale.setRange(context.minX(), context.maxX());
        xMajorScale.setMaxTicks(majorXTicks);
        g.setStroke(getMajorDashStroke(ChartScale.scale(majorStrokeWidth),
                ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff * 0.7f)));
        g.setColor(majorColor);
        lastDrawX = Double.NEGATIVE_INFINITY;
        double xMajorSpacing = xMajorScale.getTickSpacing();
        if (xMajorSpacing > 0) {
            double tick = xMajorScale.getNiceMin();
            double end = xMajorScale.getNiceMax();
            double epsilon = xMajorSpacing * 0.5;
            double minSpacing = ChartScale.scale(10);
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMajorSpacing) {
                context.mapToPixel(tick, context.minY(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    if (Double.isFinite(lastDrawX) && Math.abs(x - lastDrawX) < minSpacing) continue;
                    line.setLine(x, minY, x, maxY);
                    g.draw(line);
                    lastDrawX = x;
                }
            }
        }

        // --- Semantic emphasis: y = 0 line (oscillators, baseline) ---
        if (context.minY() <= 0 && context.maxY() >= 0) {
            g.setColor(zeroLineColor);
            g.setStroke(getZeroStroke(ChartScale.scale(zeroStrokeWidth)));
            context.mapToPixel(context.minX(), 0.0, buf);
            double y = snap(buf[1]);
            if (y >= minY && y <= maxY) {
                line.setLine(minX, y, maxX, y);
                g.draw(line);
            }
        }

        // Frame/border
        g.setColor(frameColor);
        g.setStroke(getFrameStroke(ChartScale.scale(frameStrokeWidth)));
        line.setLine(minX, minY, maxX, minY);
        g.draw(line);
        line.setLine(minX, maxY, maxX, maxY);
        g.draw(line);
        line.setLine(minX, minY, minX, maxY);
        g.draw(line);
        line.setLine(maxX, minY, maxX, maxY);
        g.draw(line);

        // restore hints
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void updateCachedColors(Color gridBase, Color axisColor,
                                    float minorAlpha, float majorAlpha, float frameAlpha, float zeroAlpha) {
        int gridRgb = gridBase != null ? gridBase.getRGB() : 0;
        int axisRgb = axisColor != null ? axisColor.getRGB() : 0;
        if (minorColor == null || gridRgb != cachedGridRgb || minorAlpha != cachedMinorAlpha) {
            minorColor = ColorUtils.withAlpha(gridBase, minorAlpha);
        }
        if (majorColor == null || gridRgb != cachedGridRgb || majorAlpha != cachedMajorAlpha) {
            majorColor = ColorUtils.withAlpha(gridBase, majorAlpha);
        }
        if (frameColor == null || gridRgb != cachedGridRgb || frameAlpha != cachedFrameAlpha) {
            frameColor = ColorUtils.withAlpha(gridBase, frameAlpha);
        }
        if (zeroLineColor == null || axisRgb != cachedAxisRgb || zeroAlpha != cachedZeroAlpha) {
            zeroLineColor = ColorUtils.withAlpha(axisColor, zeroAlpha);
        }
        cachedGridRgb = gridRgb;
        cachedAxisRgb = axisRgb;
        cachedMinorAlpha = minorAlpha;
        cachedMajorAlpha = majorAlpha;
        cachedFrameAlpha = frameAlpha;
        cachedZeroAlpha = zeroAlpha;
    }

    private BasicStroke getMinorStroke(float width) {
        if (minorStroke == null || cachedMinorStrokeWidth != width) {
            minorStroke = new BasicStroke(width);
            cachedMinorStrokeWidth = width;
        }
        return minorStroke;
    }

    private BasicStroke getMajorStroke(float width) {
        if (majorStroke == null || cachedMajorStrokeWidth != width) {
            majorStroke = new BasicStroke(width);
            cachedMajorStrokeWidth = width;
        }
        return majorStroke;
    }

    private BasicStroke getFrameStroke(float width) {
        if (frameStroke == null || cachedFrameStrokeWidth != width) {
            frameStroke = new BasicStroke(width);
            cachedFrameStrokeWidth = width;
        }
        return frameStroke;
    }

    private BasicStroke getZeroStroke(float width) {
        if (zeroStroke == null || cachedZeroStrokeWidth != width) {
            zeroStroke = new BasicStroke(width);
            cachedZeroStrokeWidth = width;
        }
        return zeroStroke;
    }

    private BasicStroke getMinorDashStroke(float width, float dashOn, float dashOff) {
        if (minorDashStroke == null || cachedMinorDashWidth != width
                || cachedMinorDashOn != dashOn || cachedMinorDashOff != dashOff) {
            minorDashPattern[0] = dashOn;
            minorDashPattern[1] = dashOff;
            minorDashStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, minorDashPattern, 0.0f);
            cachedMinorDashWidth = width;
            cachedMinorDashOn = dashOn;
            cachedMinorDashOff = dashOff;
        }
        return minorDashStroke;
    }

    private BasicStroke getMajorDashStroke(float width, float dashOn, float dashOff) {
        if (majorDashStroke == null || cachedMajorDashWidth != width
                || cachedMajorDashOn != dashOn || cachedMajorDashOff != dashOff) {
            majorDashPattern[0] = dashOn;
            majorDashPattern[1] = dashOff;
            majorDashStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, majorDashPattern, 0.0f);
            cachedMajorDashWidth = width;
            cachedMajorDashOn = dashOn;
            cachedMajorDashOff = dashOff;
        }
        return majorDashStroke;
    }

    private static double snap(double value) {
        return Math.floor(value) + 0.5;
    }
}
