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
 * Ternary grid layer.
 *
 * <p>Renders a barycentric triangle with interior grid lines (a+b+c=1).
 * Suitable for ternary plots and compositional charts.</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
 * @author Arber Gashi
 */
public final class TernaryGridLayer extends DefaultGridLayer {
    private static final String KEY_MINOR_ALPHA = "Chart.ternaryGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.ternaryGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.ternaryGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.ternaryGrid.majorStrokeWidth";
    private static final String KEY_DIVISIONS = "Chart.ternaryGrid.divisions";
    private static final String KEY_MAJOR_EVERY = "Chart.ternaryGrid.majorEvery";

    private final TernaryGeometry triangle = new TernaryGeometry();
    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        if (context == null) return;
        ArberRect bounds = context.getPlotBounds();
        triangle.setBounds(bounds);

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();

        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));
        int divisions = Math.clamp(ChartAssets.getInt(KEY_DIVISIONS, 10), 4, 100);
        int majorEvery = Math.max(2, ChartAssets.getInt(KEY_MAJOR_EVERY, 5));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        // Outer triangle
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStroke));
        drawLine(canvas, triangle.ax(), triangle.ay(), triangle.bx(), triangle.by());
        drawLine(canvas, triangle.bx(), triangle.by(), triangle.cx(), triangle.cy());
        drawLine(canvas, triangle.cx(), triangle.cy(), triangle.ax(), triangle.ay());

        for (int i = 1; i < divisions; i++) {
            double t = (double) i / divisions;
            boolean isMajor = (i % majorEvery == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));

            drawBand(canvas, triangle.ax(), triangle.ay(), triangle.bx(), triangle.by(), triangle.cx(), triangle.cy(), t);
            drawBand(canvas, triangle.bx(), triangle.by(), triangle.ax(), triangle.ay(), triangle.cx(), triangle.cy(), t);
            drawBand(canvas, triangle.cx(), triangle.cy(), triangle.ax(), triangle.ay(), triangle.bx(), triangle.by(), t);
        }
    }

    private void drawBand(ArberCanvas canvas, double ax, double ay, double bx, double by, double cx, double cy, double t) {
        drawLine(canvas, Math.fma(bx - ax, t, ax), Math.fma(by - ay, t, ay),
                Math.fma(cx - ax, t, ax), Math.fma(cy - ay, t, ay));
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
