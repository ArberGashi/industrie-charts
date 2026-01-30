package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Band/range-area renderer: draws a band between min and max.
 * Uses {@link com.arbergashi.charts.model.ChartPoint#getMin()} and
 * {@link com.arbergashi.charts.model.ChartPoint#getMax()}.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class BandRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public BandRenderer() {
        super("band");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        int count = model.getPointCount();
        if (count == 0) return null;

        double[] lows = model.getLowData();
        double[] highs = model.getHighData();
        if (lows == null || highs == null || lows.length == 0 || highs.length == 0) return null;

        int limit = Math.min(count, Math.min(lows.length, highs.length));
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < limit; i++) {
            double lo = lows[i];
            double hi = highs[i];
            if (Double.isFinite(lo) && lo < min) min = lo;
            if (Double.isFinite(hi) && hi > max) max = hi;
        }
        if (!(min < max)) return null;
        return new double[]{min, max};
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        ArberColor c = seriesOrBase(model, context, 0);
        float alpha = ChartAssets.getFloat("chart.render.band.alpha", 0.18f);

        double[] xData = model.getXData();
        double[] minData = model.getLowData();
        double[] maxData = model.getHighData();

        float[] xs = RendererAllocationCache.getFloatArray(this, "band.xs", count * 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "band.ys", count * 2);
        double[] pBuffer = this.pBuffer;
        int idx = 0;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], maxData[i], pBuffer);
            xs[idx] = (float) pBuffer[0];
            ys[idx] = (float) pBuffer[1];
            idx++;
        }
        for (int i = count - 1; i >= 0; i--) {
            context.mapToPixel(xData[i], minData[i], pBuffer);
            xs[idx] = (float) pBuffer[0];
            ys[idx] = (float) pBuffer[1];
            idx++;
        }

        canvas.setColor(ColorUtils.applyAlpha(c, alpha));
        canvas.fillPolygon(xs, ys, idx);

        if (ChartAssets.getBoolean("chart.render.band.outline", false)) {
            ArberColor outline = isMultiColor() ? themeSeries(context, 1) : c;
            if (outline == null) outline = c;
            canvas.setStroke(getSeriesStrokeWidth());
            canvas.setColor(ColorUtils.applyAlpha(outline, Math.min(1.0f, alpha * 2f)));
            canvas.drawPolyline(xs, ys, idx);
        }
    }
}
