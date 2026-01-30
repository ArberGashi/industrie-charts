package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

import java.util.Optional;

/**
 * Enterprise ErrorBar Renderer - headless.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public class ErrorBarRenderer extends BaseRenderer {

    private final double[] selectionBuf = new double[2];
    private final double[] pixMidBuf = new double[2];
    private final double[] pixMaxBuf = new double[2];

    public ErrorBarRenderer() {
        super("errorbar");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count0 = model.getPointCount();
        if (count0 == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] maxData = model.getHighData();
        double[] minData = model.getLowData();

        int count = count0;
        count = Math.min(count, xData.length);
        count = Math.min(count, yData.length);
        count = Math.min(count, maxData.length);
        count = Math.min(count, minData.length);
        if (count == 0) return;

        float strokeWidth = ChartScale.scale(1.2f);
        canvas.setStroke(strokeWidth);

        double capWidth = ChartScale.scale(8.0);

        final double[] pixMax = this.pixMaxBuf;
        final double[] pixMin = this.selectionBuf;

        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double yMaxD = maxData[i];
            double yMinD = minData[i];
            if (!Double.isFinite(x) || !Double.isFinite(yMaxD) || !Double.isFinite(yMinD)) continue;

            context.mapToPixel(x, yMaxD, pixMax);
            context.mapToPixel(x, yMinD, pixMin);

            if (!Double.isFinite(pixMax[0]) || !Double.isFinite(pixMax[1]) || !Double.isFinite(pixMin[0]) || !Double.isFinite(pixMin[1])) continue;

            double px = pixMax[0];
            double yMax = pixMax[1];
            double yMin = pixMin[1];

            ArberColor errorColor = seriesOrBase(model, context, i);
            if (errorColor == null) errorColor = themeAccent(context);
            canvas.setColor(errorColor);

            double halfCap = capWidth / 2.0;
            drawLine(canvas, px, yMax, px, yMin);
            drawLine(canvas, px - halfCap, yMax, px + halfCap, yMax);
            drawLine(canvas, px - halfCap, yMin, px + halfCap, yMin);
        }
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "err.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "err.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int count0 = model.getPointCount();
        double threshold = ChartScale.scale(10.0);

        final double[] pixMid = this.pixMidBuf;
        final double[] pixMax = this.pixMaxBuf;
        final double[] pixMin = this.selectionBuf;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] maxData = model.getHighData();
        double[] minData = model.getLowData();

        int count = count0;
        count = Math.min(count, xData.length);
        count = Math.min(count, yData.length);
        count = Math.min(count, maxData.length);
        count = Math.min(count, minData.length);
        if (count == 0) return Optional.empty();

        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, pixMid);
            if (!Double.isFinite(pixMid[0]) || !Double.isFinite(pixMid[1])) continue;

            if (Math.abs(pixel.x() - pixMid[0]) < threshold) {
                double yMaxD = maxData[i];
                double yMinD = minData[i];
                if (!Double.isFinite(yMaxD) || !Double.isFinite(yMinD)) continue;

                context.mapToPixel(x, yMaxD, pixMax);
                context.mapToPixel(x, yMinD, pixMin);

                if (!Double.isFinite(pixMax[1]) || !Double.isFinite(pixMin[1])) continue;

                double minY = Math.min(pixMax[1], pixMin[1]) - threshold;
                double maxY = Math.max(pixMax[1], pixMin[1]) + threshold;

                if (pixel.y() >= minY && pixel.y() <= maxY) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }
}
