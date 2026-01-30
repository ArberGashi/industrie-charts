package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

/**
 * RangeRenderer - Range Area Chart
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class RangeRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public RangeRenderer() {
        super("range");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        final ArberColor baseColor = getSeriesColor(model);
        final ArberColor rangeColor = ColorUtils.applyAlpha(baseColor, 0.3f);

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] minData = model.getLowData();
        double[] maxData = model.getHighData();

        float[] xMax = RendererAllocationCache.getFloatArray(this, "range.x.max", count);
        float[] yMax = RendererAllocationCache.getFloatArray(this, "range.y.max", count);
        float[] xMin = RendererAllocationCache.getFloatArray(this, "range.x.min", count);
        float[] yMin = RendererAllocationCache.getFloatArray(this, "range.y.min", count);
        float[] xMid = RendererAllocationCache.getFloatArray(this, "range.x.mid", count);
        float[] yMid = RendererAllocationCache.getFloatArray(this, "range.y.mid", count);

        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], maxData[i], p0);
            xMax[i] = (float) p0[0];
            yMax[i] = (float) p0[1];
            context.mapToPixel(xData[i], minData[i], p0);
            xMin[i] = (float) p0[0];
            yMin[i] = (float) p0[1];
            context.mapToPixel(xData[i], yData[i], p0);
            xMid[i] = (float) p0[0];
            yMid[i] = (float) p0[1];
        }

        float[] polyX = RendererAllocationCache.getFloatArray(this, "range.poly.x", count * 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "range.poly.y", count * 2);
        int p = 0;
        for (int i = 0; i < count; i++) {
            polyX[p] = xMax[i];
            polyY[p] = yMax[i];
            p++;
        }
        for (int i = count - 1; i >= 0; i--) {
            polyX[p] = xMin[i];
            polyY[p] = yMin[i];
            p++;
        }

        canvas.setColor(rangeColor);
        canvas.fillPolygon(polyX, polyY, p);

        canvas.setColor(ColorUtils.applyAlpha(baseColor, 0.6f));
        canvas.setStroke(ChartScale.scale(1.0f));
        canvas.drawPolyline(xMax, yMax, count);
        canvas.drawPolyline(xMin, yMin, count);

        canvas.setColor(baseColor);
        canvas.setStroke(ChartScale.scale(2.0f));
        canvas.drawPolyline(xMid, yMid, count);
    }
}
