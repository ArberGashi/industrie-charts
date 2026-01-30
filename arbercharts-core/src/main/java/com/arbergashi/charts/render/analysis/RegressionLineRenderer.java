package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
/**
 * Linear regression line renderer.
 *
 * <p>Computes a least-squares fit {@code y = a + b*x} and draws the resulting line across the visible plot bounds.</p>
 *
 * <p><b>Performance:</b> O(n) per render.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class RegressionLineRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public RegressionLineRenderer() {
        super("regressionLine");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // Compute linear regression.
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double y = yData[i];
            sx += x;
            sy += y;
            sxx = Math.fma(x, x, sxx);
            sxy = Math.fma(x, y, sxy);
        }

        double denom = count * sxx - sx * sx;
        if (Math.abs(denom) < 1e-12) return;

        double b = (count * sxy - sx * sy) / denom;
        double a = (sy - b * sx) / count;

        double xMin = context.getMinX();
        double xMax = context.getMaxX();
        double y1 = a + b * xMin;
        double y2 = a + b * xMax;

        context.mapToPixel(xMin, y1, pBuffer);
        double px1 = pBuffer[0], py1 = pBuffer[1];
        context.mapToPixel(xMax, y2, pBuffer);
        double px2 = pBuffer[0], py2 = pBuffer[1];

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;

        float w = ChartAssets.getFloat("chart.analysis.lineWidth", 2.0f);
        if (isMultiColor() && accent != base) {
            canvas.setColor(accent);
            canvas.setStroke(ChartScale.scale(w + 0.8f));
            float[] xs = { (float) px1, (float) px2 };
            float[] ys = { (float) py1, (float) py2 };
            canvas.drawPolyline(xs, ys, 2);
        }

        canvas.setColor(base);
        canvas.setStroke(ChartScale.scale(w));
        float[] xs = { (float) px1, (float) px2 };
        float[] ys = { (float) py1, (float) py2 };
        canvas.drawPolyline(xs, ys, 2);
    }
}
