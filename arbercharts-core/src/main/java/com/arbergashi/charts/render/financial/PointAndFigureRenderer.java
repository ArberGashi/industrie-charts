package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;

/**
 * Point and Figure Renderer - ArberGashi Engine.
 * A time-agnostic chart style that represents price movements using 'X' (rising)
 * and 'O' (falling) symbols. This implementation is optimized for zero-allocation
 * rendering by reusing geometric objects.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class PointAndFigureRenderer extends BaseRenderer {

    public PointAndFigureRenderer() {
        super("pointAndFigure");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        double boxSize = ChartScale.scale(15.0);
        float strokeWidth = (float) ChartScale.scale(2.0);

        g2.setStroke(getCachedStroke(strokeWidth));

        final ChartTheme theme = resolveTheme(context);
        final Color colorUp = theme.getBullishColor();
        final Color colorDown = theme.getBearishColor();

        double[] buf = pBuffer();
        int drawn = 0;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            double x = buf[0];
            double y = buf[1];

            // Use 'weight' as indicator: > 0 for X (bullish), <= 0 for O (bearish)
            if (model.getWeight(i) > 0) {
                g2.setColor(colorUp);
                drawX(g2, x, y, boxSize * 0.4);
            } else {
                g2.setColor(colorDown);
                drawO(g2, x, y, boxSize * 0.4);
            }
            if (++drawn > 2000) break; // safety cap
        }
    }

    private void drawX(Graphics2D g2, double cx, double cy, double r) {
        g2.draw(getLine(cx - r, cy - r, cx + r, cy + r));
        g2.draw(getLine(cx + r, cy - r, cx - r, cy + r));
    }

    private void drawO(Graphics2D g2, double cx, double cy, double r) {
        g2.draw(getEllipse(cx - r, cy - r, r * 2, r * 2));
    }
}
