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
 * Grid layer tuned for financial charts: clear horizontal price lines,
 * subtle vertical time lines and an optional faint separator for volume areas.
 *
 * <p>All styling is theme-driven via UIManager properties for Bloomberg-grade quality.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class FinancialGridLayer implements GridLayer {

    // Theme property keys
    private static final String KEY_MINOR_ALPHA = "Chart.financialGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.financialGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.financialGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.financialGrid.majorStrokeWidth";
    private static final String KEY_MINOR_DASH_ON = "Chart.financialGrid.minorDash.on";
    private static final String KEY_MINOR_DASH_OFF = "Chart.financialGrid.minorDash.off";
    private static final String KEY_FRAME_ALPHA = "Chart.financialGrid.frameAlpha";
    private static final String KEY_ZERO_ALPHA = "Chart.financialGrid.zeroLineAlpha";
    private static final String KEY_VOLUME_SEP_ALPHA = "Chart.financialGrid.volumeSeparatorAlpha";

    private final Line2D.Double line = new Line2D.Double();
    private final double[] buf = new double[2];
    private final NiceScale yMinorScale = new NiceScale(0, 1);
    private final NiceScale yMajorScale = new NiceScale(0, 1);
    private final NiceScale xMinorScale = new NiceScale(0, 1);
    private final NiceScale xMajorScale = new NiceScale(0, 1);

    // Cached strokes
    private float cachedMinorStrokeWidth = Float.NaN;
    private float cachedMajorStrokeWidth = Float.NaN;
    private float cachedFrameStrokeWidth = Float.NaN;
    private float cachedZeroStrokeWidth = Float.NaN;
    private float cachedVolumeStrokeWidth = Float.NaN;
    private BasicStroke minorStroke;
    private BasicStroke majorStroke;
    private BasicStroke frameStroke;
    private BasicStroke zeroStroke;
    private BasicStroke volumeStroke;

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

    private float cachedDayDashWidth = Float.NaN;
    private float cachedDayDashOn = Float.NaN;
    private float cachedDayDashOff = Float.NaN;
    private final float[] dayDashPattern = new float[]{1f, 1f};
    private BasicStroke dayDashStroke;

    // Cached colors
    private int cachedGridRgb = 0;
    private int cachedAxisRgb = 0;
    private float cachedMinorAlpha = Float.NaN;
    private float cachedMajorAlpha = Float.NaN;
    private float cachedFrameAlpha = Float.NaN;
    private float cachedZeroAlpha = Float.NaN;
    private float cachedVolumeAlpha = Float.NaN;
    private Color minorColor;
    private Color majorColor;
    private Color frameColor;
    private Color emphasisColor;
    private Color volumeColor;

    @Override
    public void renderGrid(Graphics2D g, PlotContext context) {
        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();
        Rectangle2D bounds = context.plotBounds();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        // Crisp lines
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldStroke = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = Math.max(1, (int) Math.ceil(bounds.getWidth()));
        int h = Math.max(1, (int) Math.ceil(bounds.getHeight()));

        Color base = theme.getGridColor();

        // Theme-driven alpha and stroke values
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.20f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        float minorDashOn = ChartAssets.getUIFloat(KEY_MINOR_DASH_ON, 2.0f);
        float minorDashOff = ChartAssets.getUIFloat(KEY_MINOR_DASH_OFF, 5.0f);
        float frameAlpha = ChartAssets.getUIFloat(KEY_FRAME_ALPHA, 0.50f);
        float zeroAlpha = ChartAssets.getUIFloat(KEY_ZERO_ALPHA, 0.40f);
        float volumeSepAlpha = ChartAssets.getUIFloat(KEY_VOLUME_SEP_ALPHA, 0.28f);

        updateCachedColors(base, theme.getAxisLabelColor(), minorAlpha, majorAlpha, frameAlpha, zeroAlpha, volumeSepAlpha);

        // Major/minor tick density
        int majorYTicks = Math.max(4, Math.min(10, h / 70));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));

        int majorXTicks = Math.max(4, Math.min(12, w / 90));
        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));

        // --- Horizontal price lines: minor then major ---
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

        // --- Vertical time lines: minor dashed then major dashed stronger ---
        xMinorScale.setRange(context.minX(), context.maxX());
        xMinorScale.setMaxTicks(minorXTicks);
        g.setStroke(getMinorDashStroke(ChartScale.scale(minorStrokeWidth),
                ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff)));
        g.setColor(minorColor);
        double lastX = Double.NEGATIVE_INFINITY;
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
                    if (Double.isFinite(lastX) && Math.abs(x - lastX) < minPixelSpacing) continue;
                    line.setLine(x, minY, x, maxY);
                    g.draw(line);
                    lastX = x;
                }
            }
        }

        xMajorScale.setRange(context.minX(), context.maxX());
        xMajorScale.setMaxTicks(majorXTicks);
        g.setStroke(getMajorDashStroke(ChartScale.scale(majorStrokeWidth),
                ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff * 0.7f)));
        g.setColor(majorColor);
        lastX = Double.NEGATIVE_INFINITY;
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
                    if (Double.isFinite(lastX) && Math.abs(x - lastX) < minSpacing) continue;
                    line.setLine(x, minY, x, maxY);
                    g.draw(line);
                    lastX = x;
                }
            }
        }

        // --- Semantic emphasis: y = 0 (for returns/oscillators) ---
        if (context.minY() <= 0 && context.maxY() >= 0) {
            g.setColor(emphasisColor);
            g.setStroke(getZeroStroke(ChartScale.scale(1.05f)));
            context.mapToPixel(context.minX(), 0.0, buf);
            double y = snap(buf[1]);
            if (y >= minY && y <= maxY) {
                line.setLine(minX, y, maxX, y);
                g.draw(line);
            }
        }

        // Volume separator (theme-derived, subtle)
        if (h > 120) {
            g.setStroke(getVolumeStroke(ChartScale.scale(1.0f)));
            g.setColor(volumeColor);
            double ySep = snap(minY + (h * 0.72));
            line.setLine(minX, ySep, maxX, ySep);
            g.draw(line);
        }

        // Optional: emphasize session/day boundaries when X looks like epoch timestamps.
        drawDayBoundariesIfTimeAxis(g, context);

        // Frame/border
        g.setColor(frameColor);
        g.setStroke(getFrameStroke(ChartScale.scale(1.0f)));
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

    private void drawDayBoundariesIfTimeAxis(Graphics2D g, PlotContext context) {
        double minX = context.minX();
        double maxX = context.maxX();
        if (!(Double.isFinite(minX) && Double.isFinite(maxX)) || maxX <= minX) return;

        // Heuristic: treat as epoch milliseconds or seconds if values are large.
        double absMax = Math.max(Math.abs(minX), Math.abs(maxX));
        final boolean millis = absMax >= 1.0e11;
        final boolean seconds = absMax >= 1.0e9 && absMax < 1.0e11;
        if (!(millis || seconds)) return;

        Rectangle2D b = context.plotBounds();
        if (b.getWidth() < 220) return;

        final long minMs = millis ? (long) minX : (long) (minX * 1000.0);
        final long maxMs = millis ? (long) maxX : (long) (maxX * 1000.0);
        if (maxMs <= minMs) return;

        // Guard: if range is too large, skip to avoid overdraw
        final long dayMs = 86_400_000L;
        long startDayEpoch = Math.floorDiv(minMs, dayMs);
        long endDayEpoch = Math.floorDiv(maxMs, dayMs);
        long days = endDayEpoch - startDayEpoch;
        if (days > 120) return;

        Stroke oldStroke = g.getStroke();
        Color oldColor = g.getColor();
        g.setColor(emphasisColor);
        g.setStroke(getDayDashStroke(ChartScale.scale(1.0f),
                ChartScale.scale(4.0f), ChartScale.scale(4.0f)));

        double lastDrawX = Double.NEGATIVE_INFINITY;
        double minPixelSpacing = ChartScale.scale(30);

        // Draw each day start inside (minX, maxX]
        for (long day = startDayEpoch + 1; day <= endDayEpoch; day++) {
            long dayStartMs = day * dayMs;
            double xData = millis ? (double) dayStartMs : (dayStartMs / 1000.0);

            context.mapToPixel(xData, context.minY(), buf);
            double x = snap(buf[0]);
            if (x >= b.getMinX() && x <= b.getMaxX()) {
                if (!Double.isFinite(lastDrawX) || Math.abs(x - lastDrawX) >= minPixelSpacing) {
                    line.setLine(x, b.getMinY(), x, b.getMaxY());
                    g.draw(line);
                    lastDrawX = x;
                }
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    private void updateCachedColors(Color gridBase, Color axisColor,
                                    float minorAlpha, float majorAlpha, float frameAlpha,
                                    float zeroAlpha, float volumeAlpha) {
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
        if (emphasisColor == null || axisRgb != cachedAxisRgb || zeroAlpha != cachedZeroAlpha) {
            emphasisColor = ColorUtils.withAlpha(axisColor, zeroAlpha);
        }
        if (volumeColor == null || gridRgb != cachedGridRgb || volumeAlpha != cachedVolumeAlpha) {
            volumeColor = ColorUtils.withAlpha(gridBase, volumeAlpha);
        }
        cachedGridRgb = gridRgb;
        cachedAxisRgb = axisRgb;
        cachedMinorAlpha = minorAlpha;
        cachedMajorAlpha = majorAlpha;
        cachedFrameAlpha = frameAlpha;
        cachedZeroAlpha = zeroAlpha;
        cachedVolumeAlpha = volumeAlpha;
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

    private BasicStroke getVolumeStroke(float width) {
        if (volumeStroke == null || cachedVolumeStrokeWidth != width) {
            volumeStroke = new BasicStroke(width);
            cachedVolumeStrokeWidth = width;
        }
        return volumeStroke;
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

    private BasicStroke getDayDashStroke(float width, float dashOn, float dashOff) {
        if (dayDashStroke == null || cachedDayDashWidth != width
                || cachedDayDashOn != dashOn || cachedDayDashOff != dashOff) {
            dayDashPattern[0] = dashOn;
            dayDashPattern[1] = dashOff;
            dayDashStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, dayDashPattern, 0.0f);
            cachedDayDashWidth = width;
            cachedDayDashOn = dashOn;
            cachedDayDashOff = dashOff;
        }
        return dayDashStroke;
    }

    private static double snap(double value) {
        return Math.floor(value) + 0.5;
    }
}
