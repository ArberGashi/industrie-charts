package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;

/**
 * Kagi chart renderer for JDK 25.
 * Time-independent visualization of price movements.
 * Switches line thickness (Yin/Yang) on trend reversals.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class KagiRenderer extends BaseRenderer {

    public KagiRenderer() {
        super("kagi");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 2) return;

        Color baseColor = getSeriesColor(model);
        Stroke thinStroke = getCachedStroke(ChartScale.scale(1.0f));
        Stroke thickStroke = getCachedStroke(ChartScale.scale(3.0f));

        boolean isThick = false;
        double lastY = model.getY(0);

        double[] buf1 = pBuffer();
        double[] buf2 = pBuffer4(); // reuse larger buffer if needed

        for (int i = 0; i < n - 1; i++) {
            double x1 = model.getX(i);
            double y1 = model.getY(i);
            double x2 = model.getX(i + 1);
            double y2 = model.getY(i + 1);

            context.mapToPixel(x1, y1, buf1);
            double pix1x = buf1[0], pix1y = buf1[1];
            context.mapToPixel(x2, y2, buf2);
            double pix2x = buf2[0], pix2y = buf2[1];

            // Trend logic
            if (y2 > lastY && !isThick) isThick = true;
            else if (y2 < lastY && isThick) isThick = false;

            g2.setStroke(isThick ? thickStroke : thinStroke);
            g2.setColor(y2 >= y1 ? baseColor : themeBearish(context));

            // Vertical line
            g2.draw(getLine(pix1x, pix1y, pix1x, pix2y));
            // Horizontal connection
            g2.draw(getLine(pix1x, pix2y, pix2x, pix2y));

            lastY = y1;
        }
    }
}
