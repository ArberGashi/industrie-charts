package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * Point and Figure Renderer - ArberGashi Engine.
 * A time-agnostic chart style that represents price movements using 'X' (rising)
 * and 'O' (falling) symbols. This implementation is optimized for zero-allocation
 * rendering by reusing geometric objects.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class PointAndFigureRenderer extends BaseRenderer {
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private final float[] polyX = new float[9];
    private final float[] polyY = new float[9];

    public PointAndFigureRenderer() {
        super("pointAndFigure");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        double boxSize = ChartScale.scale(15.0);
        float strokeWidth = (float) ChartScale.scale(2.0);

        canvas.setStroke(strokeWidth);

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor colorUp = theme.getBullishColor();
        final ArberColor colorDown = theme.getBearishColor();

        double[] buf = pBuffer();
        int drawn = 0;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            double x = buf[0];
            double y = buf[1];

            // Use 'weight' as indicator: > 0 for X (bullish), <= 0 for O (bearish)
            if (model.getWeight(i) > 0) {
                canvas.setColor(colorUp);
                drawX(canvas, lineX, lineY, x, y, boxSize * 0.4);
            } else {
                canvas.setColor(colorDown);
                drawO(canvas, polyX, polyY, x, y, boxSize * 0.4);
            }
            if (++drawn > 2000) break; // safety cap
        }
    }

    private void drawX(ArberCanvas canvas, float[] lineX, float[] lineY, double cx, double cy, double r) {
        lineX[0] = (float) (cx - r);
        lineY[0] = (float) (cy - r);
        lineX[1] = (float) (cx + r);
        lineY[1] = (float) (cy + r);
        canvas.drawPolyline(lineX, lineY, 2);
        lineX[0] = (float) (cx + r);
        lineY[0] = (float) (cy - r);
        lineX[1] = (float) (cx - r);
        lineY[1] = (float) (cy + r);
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private void drawO(ArberCanvas canvas, float[] polyX, float[] polyY, double cx, double cy, double r) {
        int segments = 8;
        double step = (Math.PI * 2.0) / segments;
        for (int i = 0; i <= segments; i++) {
            double a = i * step;
            polyX[i] = (float) (cx + Math.cos(a) * r);
            polyY[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(polyX, polyY, segments + 1);
    }
}
