package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * Kagi chart renderer for JDK 25.
 * Time-independent visualization of price movements.
 * Switches line thickness (Yin/Yang) on trend reversals.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class KagiRenderer extends BaseRenderer {
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    public KagiRenderer() {
        super("kagi");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 2) return;

        ArberColor baseColor = getSeriesColor(model);
        float thinStroke = ChartScale.scale(1.0f);
        float thickStroke = ChartScale.scale(3.0f);

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

            canvas.setStroke(isThick ? thickStroke : thinStroke);
            canvas.setColor(y2 >= y1 ? baseColor : themeBearish(context));

            // Vertical line
            lineX[0] = (float) pix1x;
            lineY[0] = (float) pix1y;
            lineX[1] = (float) pix1x;
            lineY[1] = (float) pix2y;
            canvas.drawPolyline(lineX, lineY, 2);
            // Horizontal connection
            lineX[0] = (float) pix1x;
            lineY[0] = (float) pix2y;
            lineX[1] = (float) pix2x;
            lineY[1] = (float) pix2y;
            canvas.drawPolyline(lineX, lineY, 2);

            lastY = y1;
        }
    }
}
