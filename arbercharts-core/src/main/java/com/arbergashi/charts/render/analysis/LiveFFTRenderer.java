package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import java.util.Objects;

/**
 * Displays the one-sided amplitude spectrum of a uniformly sampled time signal.
 *
 * <p>X values specify sample times; frequencies use inverse X units. The transform
 * uses a rectangular window and zero-pads to the next power of two. Amplitudes
 * are normalized by the original sample count, with doubled positive-frequency
 * bins except DC and Nyquist. Invalid or nonuniform samples produce no spectrum.
 * Hosts should use the preferred X and Y ranges when autoscaling derived data.</p>
 *
 * <p>Call {@link #setSpectrumInput(boolean)} to display already computed frequency
 * and nonnegative amplitude pairs. Geometry always uses the supplied plot
 * context, so zoom, inversion and axis transforms apply consistently.</p>
 *
 * @author Arber Gashi
 * @since 2026-01-01
 */
public class LiveFFTRenderer extends BaseRenderer {
    private final double[] pixel = new double[2];
    private final double[] baselinePixel = new double[2];
    private boolean asBars = true;
    private boolean spectrumInput;
    private double[] frequencies = new double[0];
    private double[] amplitudes = new double[0];

    public LiveFFTRenderer() {
        super("fft");
    }

    /**
     * Computes a new one-sided amplitude array without modifying the input.
     * The result contains {@code N / 2 + 1} bins, where N is the next power of two;
     * an empty input returns an empty array. Bin k represents k/N cycles per sample.
     *
     * @param timeSignal finite, uniformly spaced samples
     * @return normalized nonnegative amplitudes, including DC and Nyquist
     * @throws NullPointerException if the input is null
     * @throws IllegalArgumentException if a sample is nonfinite
     */
    public static double[] getComputedFFT(double[] timeSignal) {
        Objects.requireNonNull(timeSignal, "timeSignal");
        if (timeSignal.length == 0) return new double[0];
        for (double value : timeSignal) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Samples must be finite");
        }
        int length = FourierTransform.paddedLength(timeSignal.length);
        double[] real = new double[length];
        double[] imaginary = new double[length];
        FourierTransform.compute(timeSignal, timeSignal.length, real, imaginary, length);
        double[] result = new double[length / 2 + 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = FourierTransform.amplitude(real, imaginary, i, length, timeSignal.length);
        }
        return result;
    }

    /**
     * Selects filled bars or a connected spectrum line.
     * @param asBars true for bars
     * @return this renderer
     */
    public LiveFFTRenderer setAsBars(boolean asBars) {
        this.asBars = asBars;
        return this;
    }

    /**
     * Selects whether the model already contains frequency/amplitude pairs.
     * @param spectrumInput true for precomputed spectrum data; false for time samples
     * @return this renderer
     */
    public LiveFFTRenderer setSpectrumInput(boolean spectrumInput) {
        this.spectrumInput = spectrumInput;
        return this;
    }

    @Override
    public double[] getPreferredXRange(ChartModel model) {
        int count = prepareSpectrum(model);
        if (count == 0) return null;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            if (!validBin(i)) continue;
            min = Math.min(min, frequencies[i]);
            max = Math.max(max, frequencies[i]);
        }
        return Double.isFinite(min) ? new double[]{min, max > min ? max : min + 1.0} : null;
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        int count = prepareSpectrum(model);
        double max = 0.0;
        for (int i = 0; i < count; i++) {
            if (validBin(i)) max = Math.max(max, amplitudes[i]);
        }
        return count == 0 ? null : new double[]{0.0, max > 0.0 ? max * 1.05 : 1.0};
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = prepareSpectrum(model);
        if (count == 0) return;
        ArberColor base = seriesOrBase(model, context, 0);
        canvas.setColor(base);
        canvas.setStroke(ChartScale.scale(1.5f));
        float[] xs = RendererAllocationCache.getFloatArray(this, "fft.line.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "fft.line.y", count);
        int used = 0;
        double baseline = context.getMinY() > 0.0 ? context.getMinY() : 0.0;
        ArberRect bounds = context.getPlotBounds();
        for (int i = 0; i < count; i++) {
            if (!validBin(i)) {
                if (!asBars && used > 1) canvas.drawPolyline(xs, ys, used);
                used = 0;
                continue;
            }
            context.mapToPixel(frequencies[i], amplitudes[i], pixel);
            if (!Float.isFinite((float) pixel[0]) || !Float.isFinite((float) pixel[1])) {
                if (!asBars && used > 1) canvas.drawPolyline(xs, ys, used);
                used = 0;
                continue;
            }
            if (asBars) {
                double spacing = count == 1 ? 1.0 : i == 0
                        ? Math.abs(frequencies[1] - frequencies[0])
                        : Math.abs(frequencies[i] - frequencies[i - 1]);
                if (!Double.isFinite(spacing) || spacing <= 0.0) continue;
                double center = pixel[0];
                double top = pixel[1];
                context.mapToPixel(frequencies[i] + spacing * 0.4, baseline, baselinePixel);
                double halfWidth = Math.abs(baselinePixel[0] - center);
                double left = Math.max(bounds.minX(), center - halfWidth);
                double right = Math.min(bounds.maxX(), center + halfWidth);
                double y0 = Math.max(bounds.minY(), Math.min(top, baselinePixel[1]));
                double y1 = Math.min(bounds.maxY(), Math.max(top, baselinePixel[1]));
                if (isMultiColor()) canvas.setColor(seriesOrBase(model, context, i));
                if (right > left && y1 > y0) canvas.fillRect((float) left, (float) y0,
                        (float) (right - left), (float) (y1 - y0));
            } else {
                xs[used] = (float) pixel[0];
                ys[used++] = (float) pixel[1];
                if (isMultiColor() && used == 2) {
                    canvas.setColor(seriesOrBase(model, context, i));
                    canvas.drawPolyline(xs, ys, 2);
                    xs[0] = xs[1];
                    ys[0] = ys[1];
                    used = 1;
                }
            }
        }
        if (!asBars && used > 1) canvas.drawPolyline(xs, ys, used);
    }

    private boolean validBin(int i) {
        return Double.isFinite(frequencies[i]) && Double.isFinite(amplitudes[i]) && amplitudes[i] >= 0.0;
    }

    private int prepareSpectrum(ChartModel model) {
        if (model == null) return 0;
        int count = model.getPointCount();
        if (count <= 0) return 0;
        double[] x = RendererAllocationCache.getDoubleArray(this, "fft.input.x", count);
        double[] y = RendererAllocationCache.getDoubleArray(this, "fft.input.y", count);
        for (int i = 0; i < count; i++) {
            x[i] = model.getX(i);
            y[i] = model.getY(i);
        }
        if (spectrumInput) {
            frequencies = x;
            amplitudes = y;
            return count;
        }
        double step = FourierTransform.samplingInterval(x, y, count);
        if (!Double.isFinite(step)) return 0;
        int length = FourierTransform.paddedLength(count);
        double[] real = RendererAllocationCache.getDoubleArray(this, "fft.real", length);
        double[] imaginary = RendererAllocationCache.getDoubleArray(this, "fft.imaginary", length);
        FourierTransform.compute(y, count, real, imaginary, length);
        int bins = length / 2 + 1;
        frequencies = RendererAllocationCache.getDoubleArray(this, "fft.frequencies", bins);
        amplitudes = RendererAllocationCache.getDoubleArray(this, "fft.amplitudes", bins);
        for (int i = 0; i < bins; i++) {
            frequencies[i] = (i / (double) length) / step;
            amplitudes[i] = FourierTransform.amplitude(real, imaginary, i, length, count);
        }
        return bins;
    }
}
