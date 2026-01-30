package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Locally weighted scatter-plot smoothing (LOESS) renderer.
 *
 * <p>This renderer draws a smoothed trend curve for noisy time series. It uses a lightweight,
 * allocation-free sliding-window weighted average approximation that is stable under high zoom.
 * The implementation is intentionally conservative to keep the hot path zero-allocation.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class LoessRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public LoessRenderer() {
        super("loess");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;
        canvas.setStroke(1.0f);

        // Window size (odd, >= 3)
        int w = Math.max(3, Math.min(101, (int) Math.round(Math.sqrt(count))));
        if ((w & 1) == 0) w++;
        int half = w / 2;

        float[] xs = RendererAllocationCache.getFloatArray(this, "loess.line.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "loess.line.y", count);
        int outCount = 0;

        for (int i = 0; i < count; i++) {
            int a = Math.max(0, i - half);
            int b = Math.min(count - 1, i + half);

            double xi = xData[i];

            // Tri-cubic weights (approx) with stable normalization.
            double sumW = 0.0;
            double sumY = 0.0;
            double x0 = xData[a];
            double x1 = xData[b];
            double span = Math.max(1e-12, x1 - x0);

            for (int j = a; j <= b; j++) {
                double xj = xData[j];
                double t = Math.abs((xj - xi) / span);
                if (t >= 1.0) continue;
                double u = 1.0 - t * t * t;
                double wj = u * u * u;
                sumW += wj;
                sumY = Math.fma(wj, yData[j], sumY);
            }
            double yi = (sumW > 0.0) ? (sumY / sumW) : yData[i];

            context.mapToPixel(xi, yi, pBuffer);
            double px = pBuffer[0];
            double py = pBuffer[1];

            xs[outCount] = (float) px;
            ys[outCount] = (float) py;
            outCount++;
        }

        if (outCount < 2) return;
        if (isMultiColor() && accent != base) {
            canvas.setColor(accent);
            canvas.drawPolyline(xs, ys, outCount);
        }
        canvas.setColor(base);
        canvas.drawPolyline(xs, ys, outCount);
    }
}
