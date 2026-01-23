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

    @Override
    public void renderGrid(Graphics2D g, PlotContext context) {
        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();
        Rectangle bounds = context.plotBounds().getBounds();

        // Pixel-aware density to avoid overcrowding
        int w = Math.max(1, bounds.width);
        int h = Math.max(1, bounds.height);

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

        Color minor = ColorUtils.withAlpha(base, minorAlpha);
        Color major = ColorUtils.withAlpha(base, majorAlpha);
        Color zeroLine = ColorUtils.withAlpha(theme.getAxisLabelColor(), zeroAlpha);

        // --- Y grid: minor then major ---
        NiceScale yMinorScale = new NiceScale(context.minY(), context.maxY());
        yMinorScale.setMaxTicks(minorYTicks);
        g.setColor(minor);
        g.setStroke(new BasicStroke(ChartScale.scale(minorStrokeWidth)));
        for (double tick : yMinorScale.getTicks()) {
            context.mapToPixel(context.minX(), tick, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        NiceScale yMajorScale = new NiceScale(context.minY(), context.maxY());
        yMajorScale.setMaxTicks(majorYTicks);
        g.setColor(major);
        g.setStroke(new BasicStroke(ChartScale.scale(majorStrokeWidth)));
        for (double tick : yMajorScale.getTicks()) {
            context.mapToPixel(context.minX(), tick, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        // --- X grid: minor (dashed) then major (dashed stronger) ---
        NiceScale xMinorScale = new NiceScale(context.minX(), context.maxX());
        xMinorScale.setMaxTicks(minorXTicks);
        Stroke minorDash = new BasicStroke(
                ChartScale.scale(minorStrokeWidth), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff)}, 0.0f
        );
        g.setStroke(minorDash);
        g.setColor(minor);
        double lastDrawX = Double.NEGATIVE_INFINITY;
        double minPixelSpacing = ChartScale.scale(5);
        for (double tick : xMinorScale.getTicks()) {
            context.mapToPixel(tick, context.minY(), buf);
            double x = buf[0];
            if (x >= bounds.getMinX() && x <= bounds.getMaxX()) {
                if (Double.isFinite(lastDrawX) && Math.abs(x - lastDrawX) < minPixelSpacing) continue;
                line.setLine(x, bounds.getMinY(), x, bounds.getMaxY());
                g.draw(line);
                lastDrawX = x;
            }
        }

        NiceScale xMajorScale = new NiceScale(context.minX(), context.maxX());
        xMajorScale.setMaxTicks(majorXTicks);
        Stroke majorDash = new BasicStroke(
                ChartScale.scale(majorStrokeWidth), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff * 0.7f)}, 0.0f
        );
        g.setStroke(majorDash);
        g.setColor(major);
        lastDrawX = Double.NEGATIVE_INFINITY;
        for (double tick : xMajorScale.getTicks()) {
            context.mapToPixel(tick, context.minY(), buf);
            double x = buf[0];
            if (x >= bounds.getMinX() && x <= bounds.getMaxX()) {
                if (Double.isFinite(lastDrawX) && Math.abs(x - lastDrawX) < ChartScale.scale(10)) continue;
                line.setLine(x, bounds.getMinY(), x, bounds.getMaxY());
                g.draw(line);
                lastDrawX = x;
            }
        }

        // --- Semantic emphasis: y = 0 line (oscillators, baseline) ---
        if (context.minY() <= 0 && context.maxY() >= 0) {
            g.setColor(zeroLine);
            g.setStroke(new BasicStroke(ChartScale.scale(zeroStrokeWidth)));
            context.mapToPixel(context.minX(), 0.0, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        // Frame/border
        g.setColor(ColorUtils.withAlpha(base, frameAlpha));
        g.setStroke(new BasicStroke(ChartScale.scale(frameStrokeWidth)));
        g.draw(context.plotBounds());

        // restore hints
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }
}
