package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * Peak detection overlay renderer.
 *
 * <p>Marks local maxima with a highlight point. A point is a peak if y[i] > y[i-1] and y[i] > y[i+1]
 * and exceeds an optional threshold.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PeakDetectionRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private final ArberPoint pCache = new ArberPoint();

    public PeakDetectionRenderer() {
        super("peakDetection");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        double minProminence = 0.0;
        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor c = base;

        canvas.setColor(c);
        canvas.setStroke((float) ChartScale.scale(1.5));

        for (int i = 1; i < count - 1; i++) {
            double y0 = yData[i - 1];
            double y1 = yData[i];
            double y2 = yData[i + 1];

            if (y1 <= y0 || y1 <= y2) continue;
            if (y1 - Math.max(y0, y2) < minProminence) continue;

            if (isMultiColor()) {
                ArberColor peak = themeSeries(context, i);
                if (peak == null) peak = base;
                c = peak;
                canvas.setColor(c);
            }
            context.mapToPixel(xData[i], y1, pBuffer);
            pCache.setLocation(pBuffer[0], pBuffer[1]);
            float r = (float) ChartScale.scale(3.0);
            canvas.fillRect((float) pCache.getX() - r, (float) pCache.getY() - r, r * 2, r * 2);
        }
    }
}
