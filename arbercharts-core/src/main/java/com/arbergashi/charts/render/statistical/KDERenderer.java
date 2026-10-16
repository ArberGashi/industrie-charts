package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Arrays;

/**
 * Gaussian kernel density estimate of the model's finite Y observations.
 * <p>Only the logical sample prefix contributes to the estimate. Automatic
 * bandwidth uses Silverman's rule, with a one-unit fallback for a singleton or
 * constant sample. The domain includes at least three bandwidths beyond each
 * extreme observation, within the finite double range. A regular grid is
 * supplemented by every distinct observation and quarter-bandwidth samples in
 * its kernel neighborhood, so isolated narrow peaks remain visible.</p>
 * <p>Prepared densities are reused while observation contents and bandwidth stay
 * unchanged. Equal observations retain their multiplicity. Sorted neighborhood
 * searches omit only Gaussian terms that already underflow to zero. Rebuilding
 * a changed distribution still has quadratic worst-case cost in the number of
 * distinct observations when their kernels overlap; this renderer does not
 * guarantee constant-time updates for large streaming distributions.</p>
 *
 * @author Arber Gashi
 * @since 2025-06-01
 */
public final class KDERenderer extends BaseRenderer {

    private static final int GRID_POINTS = 201;
    private static final int KERNEL_RADIUS_STEPS = 24;
    private static final double KERNEL_STEP = 0.25;
    // exp(-0.5 * 39^2) is already zero in double precision.
    private static final double ZERO_KERNEL_DISTANCE = 39.0;
    private static final double INVERSE_SQRT_TWO_PI = 1.0 / Math.sqrt(2.0 * Math.PI);
    private double bandwidth = -1.0;
    private double preparedBandwidth = Double.NaN;
    private double resolvedBandwidth;
    private int observationCount = -1;
    private int finiteCount;
    private int distinctCount;
    private int curveCount;
    private boolean curvePrepared;
    private double rangeMin;
    private double rangeMax;
    private double maxDensity;

    public KDERenderer() {
        super("kde");
    }

    @Override
    public double[] getPreferredXRange(ChartModel model) {
        prepareObservations(model);
        return finiteCount == 0 ? null : new double[]{rangeMin, rangeMax};
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        prepareObservations(model);
        if (finiteCount == 0) return null;
        prepareCurve();
        return new double[]{0.0, Math.min(Double.MAX_VALUE, maxDensity * 1.05)};
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        prepareObservations(model);
        if (finiteCount == 0) return;
        prepareCurve();
        int count = mapCurve(context);
        if (count <= 1) return;

        final ArberColor baseColor = seriesOrBase(model, context, 0);
        ArberColor lineColor = isMultiColor() ? themeSeries(context, 1) : baseColor;
        if (lineColor == null) lineColor = baseColor;
        final ArberColor fillColor = ColorUtils.applyAlpha(baseColor, 0.3f);

        float[] xs = RendererAllocationCache.getFloatArray(this, "kde.xs", count + 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "kde.ys", count + 2);
        float[] px = RendererAllocationCache.getFloatArray(this, "kde.px", count);
        float[] py = RendererAllocationCache.getFloatArray(this, "kde.py", count);
        System.arraycopy(px, 0, xs, 0, count);
        System.arraycopy(py, 0, ys, 0, count);
        double baseline = Math.max(context.getMinY(), Math.min(context.getMaxY(), 0.0));
        double[] buf = pBuffer();
        context.mapToPixel(rangeMin, baseline, buf);
        if (Double.isFinite(buf[1])) {
            xs[count] = px[count - 1];
            ys[count] = (float) buf[1];
            xs[count + 1] = px[0];
            ys[count + 1] = (float) buf[1];
            canvas.setColor(fillColor);
            canvas.fillPolygon(xs, ys, count + 2);
        }

        canvas.setColor(lineColor);
        canvas.setStroke(ChartScale.scale(2.5f));
        canvas.drawPolyline(px, py, count);
    }

    private void prepareObservations(ChartModel model) {
        int count = model.getPointCount();
        double[] snapshot = RendererAllocationCache.getDoubleArray(this, "kde.snapshot", count);
        boolean changed = observationCount != count
                || Double.doubleToLongBits(preparedBandwidth) != Double.doubleToLongBits(bandwidth);
        for (int i = 0; i < count; i++) {
            double value = model.getY(i);
            changed |= Double.doubleToLongBits(snapshot[i]) != Double.doubleToLongBits(value);
            snapshot[i] = value;
        }
        if (!changed) return;

        observationCount = count;
        preparedBandwidth = bandwidth;
        curvePrepared = false;
        curveCount = 0;
        double[] values = RendererAllocationCache.getDoubleArray(this, "kde.values", count);
        finiteCount = 0;
        for (int i = 0; i < count; i++) {
            if (Double.isFinite(snapshot[i])) values[finiteCount++] = snapshot[i];
        }
        if (finiteCount == 0) return;
        Arrays.sort(values, 0, finiteCount);
        resolvedBandwidth = resolveBandwidth(values, finiteCount);
        double padding = domainPadding(values, finiteCount, resolvedBandwidth);
        rangeMin = Math.max(-Double.MAX_VALUE, values[0] - padding);
        rangeMax = Math.min(Double.MAX_VALUE, values[finiteCount - 1] + padding);

        double[] centers = RendererAllocationCache.getDoubleArray(this, "kde.centers", finiteCount);
        int[] weights = RendererAllocationCache.getIntArray(this, "kde.weights", finiteCount);
        distinctCount = 0;
        for (int i = 0; i < finiteCount; i++) {
            if (distinctCount == 0 || values[i] != centers[distinctCount - 1]) {
                centers[distinctCount] = values[i];
                weights[distinctCount++] = 1;
            } else {
                weights[distinctCount - 1]++;
            }
        }
    }

    private void prepareCurve() {
        if (curvePrepared) return;
        double[] centers = RendererAllocationCache.getDoubleArray(this, "kde.centers", distinctCount);
        int[] weights = RendererAllocationCache.getIntArray(this, "kde.weights", distinctCount);
        curveCount = buildSamplePositions(centers, distinctCount, resolvedBandwidth);
        double[] positions = RendererAllocationCache.getDoubleArray(this, "kde.positions", curveCount);
        double[] densities = RendererAllocationCache.getDoubleArray(this, "kde.densities", curveCount);
        maxDensity = 0.0;
        int first = 0;
        int end = 0;
        for (int i = 0; i < curveCount; i++) {
            double x = positions[i];
            while (first < distinctCount
                    && standardizedDistance(x, centers[first], resolvedBandwidth) >= ZERO_KERNEL_DISTANCE) first++;
            end = Math.max(first, end);
            while (end < distinctCount
                    && standardizedDistance(centers[end], x, resolvedBandwidth) < ZERO_KERNEL_DISTANCE) end++;
            double density = densityAt(x, centers, weights, first, end, resolvedBandwidth);
            densities[i] = density;
            maxDensity = Math.max(maxDensity, density);
        }
        if (!(maxDensity > 0.0)) maxDensity = 1.0;
        curvePrepared = true;
    }

    private int mapCurve(PlotContext context) {
        double[] positions = RendererAllocationCache.getDoubleArray(this, "kde.positions", curveCount);
        double[] densities = RendererAllocationCache.getDoubleArray(this, "kde.densities", curveCount);
        double[] buf = pBuffer();
        float[] px = RendererAllocationCache.getFloatArray(this, "kde.px", curveCount);
        float[] py = RendererAllocationCache.getFloatArray(this, "kde.py", curveCount);
        for (int i = 0; i < curveCount; i++) {
            context.mapToPixel(positions[i], densities[i], buf);
            if (!Double.isFinite(buf[0]) || !Double.isFinite(buf[1])) return 0;
            px[i] = (float) buf[0];
            py[i] = (float) buf[1];
            if (!Float.isFinite(px[i]) || !Float.isFinite(py[i])) return 0;
        }
        return curveCount;
    }

    private int buildSamplePositions(double[] values, int n, double h) {
        int capacity = Math.addExact(GRID_POINTS, Math.multiplyExact(2 * KERNEL_RADIUS_STEPS + 1, n));
        double[] positions = RendererAllocationCache.getDoubleArray(this, "kde.positions", capacity);
        int count = 0;
        for (int i = 0; i < GRID_POINTS; i++) {
            double fraction = (double) i / (GRID_POINTS - 1);
            positions[count++] = rangeMin * (1.0 - fraction) + rangeMax * fraction;
        }
        for (int i = 0; i < n; i++) {
            for (int step = -KERNEL_RADIUS_STEPS; step <= KERNEL_RADIUS_STEPS; step++) {
                double x = Math.fma(step * KERNEL_STEP, h, values[i]);
                if (Double.isFinite(x) && x >= rangeMin && x <= rangeMax) positions[count++] = x;
            }
        }
        Arrays.sort(positions, 0, count);
        int unique = 0;
        for (int i = 0; i < count; i++) {
            if (unique == 0 || positions[i] > positions[unique - 1]) positions[unique++] = positions[i];
        }
        return unique;
    }

    private double densityAt(double x, double[] values, int[] weights, int first, int end, double h) {
        double sum = 0.0;
        double correction = 0.0;
        for (int i = first; i < end; i++) {
            double u = standardizedDistance(x, values[i], h);
            double term = weights[i] * Math.exp(-0.5 * u * u) - correction;
            double next = sum + term;
            correction = (next - sum) - term;
            sum = next;
        }
        // Divide separately so a finite n and h cannot overflow their product.
        return Math.min(Double.MAX_VALUE, (sum / finiteCount) * INVERSE_SQRT_TWO_PI / h);
    }

    private static double standardizedDistance(double x, double center, double h) {
        double difference = x - center;
        return Double.isInfinite(difference) ? x / h - center / h : difference / h;
    }

    private double domainPadding(double[] values, int n, double h) {
        double relativePadding = values[n - 1] * 0.1 - values[0] * 0.1;
        double representablePadding = 4.0 * Math.ulp(Math.max(Math.abs(values[0]), Math.abs(values[n - 1])));
        return Math.max(representablePadding, Math.max(relativePadding, 3.0 * h));
    }

    private double resolveBandwidth(double[] values, int n) {
        if (Double.isFinite(bandwidth) && bandwidth > 0.0) return bandwidth;
        if (n < 2) return 1.0;
        double scale = Math.max(Math.abs(values[0]), Math.abs(values[n - 1]));
        if (scale == 0.0) return 1.0;

        double sum = 0.0;
        for (int i = 0; i < n; i++) sum += values[i] / scale;
        double mean = sum / n;
        double sumSqDiff = 0.0;
        for (int i = 0; i < n; i++) {
            double difference = values[i] / scale - mean;
            sumSqDiff += difference * difference;
        }
        double stdDev = Math.sqrt(sumSqDiff / n);
        double q1 = values[(int) (n * 0.25)] / scale;
        double q3 = values[(int) (n * 0.75)] / scale;
        double iqr = (q3 - q1) / 1.34;
        double spread = iqr > 0.0 ? Math.min(stdDev, iqr) : stdDev;
        double calculated = (0.9 * spread * Math.pow(n, -0.2)) * scale;
        return Double.isFinite(calculated) && calculated > 0.0 ? calculated : 1.0;
    }

    /**
     * Sets a finite positive kernel bandwidth; other values select automatic bandwidth.
     * @param bandwidth kernel width in observation units, or a value selecting automatic bandwidth
     * @return this renderer
     */
    public KDERenderer setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
        return this;
    }
}
