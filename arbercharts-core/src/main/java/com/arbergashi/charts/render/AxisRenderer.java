package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.NiceScale;
/**
 * Renderer for axes, ticks, and grid lines.
 *
 * <p>Ensures a professional representation of the coordinate system.
 * Separated from any platform-specific view for maintainability and performance optimization.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
class AxisRenderer {

    private static final String KEY_GRID_SHOW_LEGACY = "chart.axis.grid.show";
    private static final String KEY_GRID_SHOW = "Chart.axis.grid.show";

    private final NiceScale niceScale = new NiceScale(0, 1);
    private final double[] mapBuf = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    /**
     * Draws the background grid.
     *
     * @param canvas rendering context
     * @param context plot context with bounds and axis ranges
     */
    public void drawGrid(ArberCanvas canvas, PlotContext context) {
        if (!isGridEnabled()) return;

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberRect b = context.getPlotBounds();

        // Market-grade: separate minor/major grid emphasis.
        // Defaults intentionally subtle; theme supplies base grid token.
        float minorAlpha = ChartAssets.getFloat("Chart.axis.grid.minorAlpha", ChartAssets.getFloat("Chart.axis.grid.alpha", 0.10f));
        float majorAlpha = ChartAssets.getFloat("Chart.axis.grid.majorAlpha", Math.min(0.22f, minorAlpha + 0.10f));

        ArberColor baseGridColor = ChartAssets.getColor("Chart.axis.grid.color", theme.getGridColor());
        if (baseGridColor == null) baseGridColor = theme.getAxisLabelColor();

        float gridStroke = ChartScale.scale(ChartAssets.getFloat("Chart.axis.grid.strokeWidth", 0.9f));

        // Minor grid
        canvas.setStroke(gridStroke);
        canvas.setColor(ColorUtils.applyAlpha(baseGridColor, minorAlpha));
        drawGridLines(canvas, context, b, true);
        drawGridLines(canvas, context, b, false);

        // Major grid (optional): draw fewer ticks for emphasis if enabled.
        if (ChartAssets.getBoolean("Chart.axis.grid.major.enabled", false)) {
            int oldMaxX = ChartAssets.getInt("Chart.axis.grid.maxTicksX", 10);
            int oldMaxY = ChartAssets.getInt("Chart.axis.grid.maxTicksY", 8);
            int majorX = ChartAssets.getInt("Chart.axis.grid.majorTicksX", Math.max(3, oldMaxX / 2));
            int majorY = ChartAssets.getInt("Chart.axis.grid.majorTicksY", Math.max(3, oldMaxY / 2));

            canvas.setColor(ColorUtils.applyAlpha(baseGridColor, majorAlpha));

            // temporarily override tick density for major lines
            drawGridLinesWithMaxTicks(canvas, context, b, true, majorX);
            drawGridLinesWithMaxTicks(canvas, context, b, false, majorY);
        }
    }

    /**
     * Draws axis lines, ticks, and labels.
     *
     * @param canvas rendering context
     * @param context plot context with bounds and axis ranges
     */
    public void drawAxes(ArberCanvas canvas, PlotContext context) {
        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();

        ArberRect b = context.getPlotBounds();

        ArberColor axisColor = ChartAssets.getColor("Chart.axis.label.color", theme.getAxisLabelColor());
        canvas.setColor(axisColor != null ? axisColor : theme.getAxisLabelColor());
        float axisStroke = ChartScale.scale(ChartAssets.getFloat("Chart.axis.strokeWidth", 1.0f));
        canvas.setStroke(axisStroke);

        // Optional frame alpha (Bloomberg-like subtle border)
        float frameAlpha = ChartAssets.getFloat("Chart.axis.frame.alpha", 0.35f);
        ArberColor frameColor = ChartAssets.getColor("Chart.axis.frame.color", axisColor);
        if (frameColor == null) frameColor = theme.getAxisLabelColor();
        canvas.setColor(ColorUtils.applyAlpha(frameColor, frameAlpha));
        canvas.drawRect((float) b.x(), (float) b.y(), (float) b.width(), (float) b.height());

        canvas.setColor(axisColor != null ? axisColor : theme.getAxisLabelColor());
        drawTicks(canvas, context, b, true);
        drawTicks(canvas, context, b, false);
    }

    private void drawGridLinesWithMaxTicks(ArberCanvas canvas, PlotContext ctx, ArberRect b, boolean vertical, int maxTicks) {
        double min = vertical ? ctx.getMinX() : ctx.getMinY();
        double max = vertical ? ctx.getMaxX() : ctx.getMaxY();

        setNiceScale(min, max);
        niceScale.setMaxTicks(Math.max(2, maxTicks));

        double[] p = mapBuf;
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);

            if (vertical) {
                drawLine(canvas, ctx.snapPixel(p[0]), b.y(), ctx.snapPixel(p[0]), b.maxY());
            } else {
                drawLine(canvas, b.x(), ctx.snapPixel(p[1]), b.maxX(), ctx.snapPixel(p[1]));
            }
        }
    }

    private void drawGridLines(ArberCanvas canvas, PlotContext ctx, ArberRect b, boolean vertical) {
        double min = vertical ? ctx.getMinX() : ctx.getMinY();
        double max = vertical ? ctx.getMaxX() : ctx.getMaxY();

        setNiceScale(min, max);
        niceScale.setMaxTicks(Math.max(2, vertical ? ctx.getRequestedTickCountX() : ctx.getRequestedTickCountY()));

        double[] p = mapBuf;
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);

            if (vertical) {
                drawLine(canvas, ctx.snapPixel(p[0]), b.y(), ctx.snapPixel(p[0]), b.maxY());
            } else {
                drawLine(canvas, b.x(), ctx.snapPixel(p[1]), b.maxX(), ctx.snapPixel(p[1]));
            }
        }
    }

    private void drawTicks(ArberCanvas canvas, PlotContext ctx, ArberRect b, boolean vertical) {
        double min = vertical ? ctx.getMinX() : ctx.getMinY();
        double max = vertical ? ctx.getMaxX() : ctx.getMaxY();

        setNiceScale(min, max);
        niceScale.setMaxTicks(Math.max(2, vertical ? ctx.getRequestedTickCountX() : ctx.getRequestedTickCountY()));

        double tickLen = ChartScale.scale(4.0);
        double[] p = mapBuf;
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);

            if (vertical) {
                // X-Axis Ticks (bottom)
                double x = ctx.snapPixel(p[0]);
                drawLine(canvas, x, b.maxY(), x, b.maxY() + tickLen);
            } else {
                // Y-Axis Ticks (left)
                double y = ctx.snapPixel(p[1]);
                drawLine(canvas, b.x(), y, b.x() - tickLen, y);
            }
        }
    }

    private boolean isGridEnabled() {
        boolean modern = ChartAssets.getBoolean(KEY_GRID_SHOW, true);
        boolean legacy = ChartAssets.getBoolean(KEY_GRID_SHOW_LEGACY, true);
        return modern && legacy;
    }

    private void setNiceScale(double min, double max) {
        niceScale.setRange(min, max);
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        lineX[0] = (float) x0;
        lineY[0] = (float) y0;
        lineX[1] = (float) x1;
        lineY[1] = (float) y1;
        canvas.drawPolyline(lineX, lineY, 2);
    }
}
