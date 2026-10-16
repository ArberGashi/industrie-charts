package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;

/**
 * Draws a truncated periodic Fourier approximation of uniformly spaced samples.
 *
 * <p>The mean and the first eight harmonics are fitted from model Y values.
 * {@code chart.render.fourier.harmonics} changes the harmonic limit (1 through 64).
 * The period is the sample count multiplied by the X sampling interval. Invalid
 * or unevenly spaced samples produce no curve rather than an invented spectrum.
 * Reusable coefficient and geometry arrays grow only when needed.</p>
 *
 * @author Arber Gashi
 * @since 2026-01-01
 */
public class FourierOverlayRenderer extends BaseRenderer {
    private final double[] pixel = new double[2];

    public FourierOverlayRenderer() {
        super("fourierOverlay");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        double[] x = RendererAllocationCache.getDoubleArray(this, "fourier.input.x", count);
        double[] y = RendererAllocationCache.getDoubleArray(this, "fourier.input.y", count);
        for (int i = 0; i < count; i++) {
            x[i] = model.getX(i);
            y[i] = model.getY(i);
        }
        double step = FourierTransform.samplingInterval(x, y, count);
        if (!Double.isFinite(step)) return;
        int harmonics = Math.min(count / 2,
                Math.clamp(ChartAssets.getInt("chart.render.fourier.harmonics", 8), 1, 64));
        double[] cosine = RendererAllocationCache.getDoubleArray(this, "fourier.cosine", harmonics + 1);
        double[] sine = RendererAllocationCache.getDoubleArray(this, "fourier.sine", harmonics + 1);
        for (int k = 0; k <= harmonics; k++) {
            double re = 0.0;
            double im = 0.0;
            for (int i = 0; i < count; i++) {
                double angle = 2.0 * Math.PI * k * i / count;
                re += y[i] * Math.cos(angle);
                im += y[i] * Math.sin(angle);
            }
            double factor = k == 0 || 2 * k == count ? 1.0 / count : 2.0 / count;
            cosine[k] = re * factor;
            sine[k] = im * factor;
        }
        ArberColor color = seriesOrBase(model, context, isMultiColor() ? 1 : 0);
        canvas.setColor(color);
        canvas.setStroke(ChartScale.scale(1.5f));
        int samples = Math.max(200, harmonics * 24 + 1);
        float[] xs = RendererAllocationCache.getFloatArray(this, "fourier.x", samples);
        float[] ys = RendererAllocationCache.getFloatArray(this, "fourier.y", samples);
        int used = 0;
        for (int i = 0; i < samples; i++) {
            double position = (count - 1.0) * i / (samples - 1.0);
            double value = cosine[0];
            for (int k = 1; k <= harmonics; k++) {
                double angle = 2.0 * Math.PI * k * position / count;
                value += cosine[k] * Math.cos(angle) + sine[k] * Math.sin(angle);
            }
            context.mapToPixel(x[0] + position * step, value, pixel);
            if (!Float.isFinite((float) pixel[0]) || !Float.isFinite((float) pixel[1])) {
                if (used > 1) canvas.drawPolyline(xs, ys, used);
                used = 0;
                continue;
            }
            xs[used] = (float) pixel[0];
            ys[used++] = (float) pixel[1];
        }
        if (used > 1) canvas.drawPolyline(xs, ys, used);
    }
}
