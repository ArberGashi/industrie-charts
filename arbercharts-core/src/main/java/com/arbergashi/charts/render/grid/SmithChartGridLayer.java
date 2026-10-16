package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

/**
 * Smith chart grid layer.
 *
 * <p>Renders the Smith chart grid (unit circle, resistance circles and reactance arcs)
 * directly in chart space. This grid is optimized for high-frequency rendering and
 * uses batch-friendly line segments for spatial pipelines.</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class SmithChartGridLayer extends DefaultGridLayer {
    /**
     * Returns the common unit-circle radius used by the grid and Smith transform.
     * @param bounds plot bounds in canvas units
     * @return non-negative radius in canvas units
     */
    public static double radius(ArberRect bounds) {
        return Math.max(0.0, Math.min(bounds.width(), bounds.height())) * 0.46;
    }
    private static final String KEY_MINOR_ALPHA = "Chart.smithGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.smithGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.smithGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.smithGrid.majorStrokeWidth";

    private static final double[] RESISTANCE = new double[]{0.2, 0.5, 1.0, 2.0, 5.0};
    private static final double[] REACTANCE = new double[]{0.2, 0.5, 1.0, 2.0, 5.0};

    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        if (context == null) return;
        ArberRect bounds = context.getPlotBounds();
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();

        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double radius = radius(bounds);

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();
        ArberColor axisColor = gridBase;

        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(axisColor, majorAlpha);

        // Outer unit circle
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStroke));
        drawCircle(canvas, cx, cy, radius, 140);

        // Resistance circles (minor)
        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStroke));
        for (double r : RESISTANCE) {
            double ccx = r / (1.0 + r);
            double rr = 1.0 / (1.0 + r);
            drawCircle(canvas, cx, cy, radius, ccx, 0.0, rr, 120);
        }

        // Reactance arcs (minor)
        for (double x : REACTANCE) {
            drawReactanceArc(canvas, cx, cy, radius, x, 140);
            drawReactanceArc(canvas, cx, cy, radius, -x, 140);
        }

        // Real axis
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStroke));
        drawLine(canvas, cx - radius, cy, cx + radius, cy);
    }

    private void drawCircle(ArberCanvas canvas, double cx, double cy, double radius, int segments) {
        drawCircle(canvas, cx, cy, radius, 0.0, 0.0, 1.0, segments);
    }

    private void drawCircle(ArberCanvas canvas, double cx, double cy, double radius,
                            double nCx, double nCy, double nR, int segments) {
        double prevX = 0;
        double prevY = 0;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * i / segments;
            double nx = nCx + nR * Math.cos(t);
            double ny = nCy + nR * Math.sin(t);
            double px = cx + nx * radius;
            double py = cy - ny * radius;
            if (i > 0) {
                drawLine(canvas, prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }
    }

    private void drawReactanceArc(ArberCanvas canvas, double cx, double cy, double radius, double x, int segments) {
        double ccx = 1.0;
        double ccy = 1.0 / x;
        double rr = 1.0 / Math.abs(x);
        double prevX = 0;
        double prevY = 0;
        boolean havePrev = false;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * i / segments;
            double nx = ccx + rr * Math.cos(t);
            double ny = ccy + rr * Math.sin(t);
            if ((nx * nx + ny * ny) > 1.0001) {
                havePrev = false;
                continue;
            }
            double px = cx + nx * radius;
            double py = cy - ny * radius;
            if (havePrev) {
                drawLine(canvas, prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
            havePrev = true;
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
