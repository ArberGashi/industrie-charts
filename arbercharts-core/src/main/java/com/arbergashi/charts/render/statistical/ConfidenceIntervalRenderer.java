package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Professional, zero-allocation confidence interval renderer (headless).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public class ConfidenceIntervalRenderer extends BaseRenderer {

    // Buffer arrays for pixel mapping
    private final double[] topBuf = new double[2];
    private final double[] bottomBuf = new double[2];
    private final double[] meanBuf = new double[2];

    public ConfidenceIntervalRenderer() {
        super("confidence");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        final ArberColor meanColor = seriesOrBase(model, context, 0);
        ArberColor boundsColor = isMultiColor() ? themeSeries(context, 1) : meanColor;
        if (boundsColor == null) boundsColor = meanColor;

        float[] xs = RendererAllocationCache.getFloatArray(this, "confidence.xs", n * 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "confidence.ys", n * 2);
        float[] lineX = RendererAllocationCache.getFloatArray(this, "confidence.lineX", n);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "confidence.lineY", n);

        int count = 0;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 4), topBuf); // max
            xs[count] = (float) topBuf[0];
            ys[count] = (float) topBuf[1];
            count++;
        }
        for (int i = n - 1; i >= 0; i--) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 3), bottomBuf); // min
            xs[count] = (float) bottomBuf[0];
            ys[count] = (float) bottomBuf[1];
            count++;
        }

        canvas.setColor(ColorUtils.applyAlpha(boundsColor, 0.25f));
        canvas.fillPolygon(xs, ys, count);

        // upper and lower outlines
        canvas.setColor(ColorUtils.applyAlpha(boundsColor, 0.4f));
        canvas.setStroke(ChartScale.scale(1.0f));
        int lineCount = 0;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 4), topBuf);
            lineX[lineCount] = (float) topBuf[0];
            lineY[lineCount] = (float) topBuf[1];
            lineCount++;
        }
        canvas.drawPolyline(lineX, lineY, lineCount);

        lineCount = 0;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 3), bottomBuf);
            lineX[lineCount] = (float) bottomBuf[0];
            lineY[lineCount] = (float) bottomBuf[1];
            lineCount++;
        }
        canvas.drawPolyline(lineX, lineY, lineCount);

        // mean line
        lineCount = 0;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getY(i), meanBuf);
            lineX[lineCount] = (float) meanBuf[0];
            lineY[lineCount] = (float) meanBuf[1];
            lineCount++;
        }
        canvas.setStroke(getSeriesStrokeWidth());
        canvas.setColor(meanColor);
        canvas.drawPolyline(lineX, lineY, lineCount);
    }
}
