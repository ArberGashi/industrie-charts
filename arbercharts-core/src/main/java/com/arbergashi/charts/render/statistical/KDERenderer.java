package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Arrays;
/**
 * Professional, zero-allocation Kernel Density Estimate (KDE) renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class KDERenderer extends BaseRenderer {

    private double bandwidth = -1.0;  // -1 = automatic (Silverman's rule)

    public KDERenderer() {
        super("kde");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        final int n = model.getPointCount();
        if (n == 0) return null;

        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "kde.range.values", n);
        for (int i = 0; i < n; i++) {
            values[i] = model.getY(i);
        }
        Arrays.sort(values, 0, n);

        double h = bandwidth > 0 ? bandwidth : getCalculatedSilvermanBandwidth(values);
        if (!(h > 0)) h = 1.0;
        double min = values[0];
        double max = values[n - 1];
        double range = max - min;
        double xMin = min - range * 0.1;
        double xMax = max + range * 0.1;

        int numPoints = 200;
        double maxDensity = 0.0;
        for (int i = 0; i < numPoints; i++) {
            double x = xMin + (xMax - xMin) * i / (numPoints - 1);
            double density = 0.0;
            for (int j = 0; j < n; j++) {
                density += gaussianKernel((x - values[j]) / h);
            }
            density /= (n * h);
            if (density > maxDensity) maxDensity = density;
        }

        if (!(maxDensity > 0.0)) {
            maxDensity = 1.0;
        }
        return new double[]{0.0, maxDensity * 1.05};
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        // 1. Extract data into a primitive array (reuse cached buffer)
        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "kde.values", n);
        for (int i = 0; i < n; i++) {
            values[i] = model.getY(i);
        }
        java.util.Arrays.sort(values, 0, n);

        // 2. Calculate KDE
        int count = buildKDEPoints(values, context);
        if (count <= 1) return;

        final ArberColor baseColor = seriesOrBase(model, context, 0);
        ArberColor lineColor = isMultiColor() ? themeSeries(context, 1) : baseColor;
        if (lineColor == null) lineColor = baseColor;
        final ArberColor fillColor = ColorUtils.applyAlpha(baseColor, 0.3f);

        // 3. Fill area under curve
        ArberRect bounds = context.getPlotBounds();
        float[] xs = RendererAllocationCache.getFloatArray(this, "kde.xs", count + 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "kde.ys", count + 2);
        float[] px = RendererAllocationCache.getFloatArray(this, "kde.px", count);
        float[] py = RendererAllocationCache.getFloatArray(this, "kde.py", count);
        System.arraycopy(px, 0, xs, 0, count);
        System.arraycopy(py, 0, ys, 0, count);
        xs[count] = (float) bounds.maxX();
        ys[count] = (float) bounds.maxY();
        xs[count + 1] = (float) bounds.x();
        ys[count + 1] = (float) bounds.maxY();

        canvas.setColor(fillColor);
        canvas.fillPolygon(xs, ys, count + 2);

        // 4. Draw KDE curve
        canvas.setColor(lineColor);
        canvas.setStroke(ChartScale.scale(2.5f));
        canvas.drawPolyline(px, py, count);
    }

    private int buildKDEPoints(double[] values, PlotContext context) {
        final int n = values.length;
        if (n == 0) return 0;

        double h = bandwidth > 0 ? bandwidth : getCalculatedSilvermanBandwidth(values);
        double min = values[0];
        double max = values[n - 1];
        double range = max - min;

        double xMin = min - range * 0.1;
        double xMax = max + range * 0.1;

        int numPoints = 200;
        double[] buf = pBuffer();
        float[] px = RendererAllocationCache.getFloatArray(this, "kde.px", numPoints);
        float[] py = RendererAllocationCache.getFloatArray(this, "kde.py", numPoints);
        int count = 0;

        for (int i = 0; i < numPoints; i++) {
            double x = xMin + (xMax - xMin) * i / (numPoints - 1);
            double density = 0;
            for (int j = 0; j < n; j++) {
                density += gaussianKernel((x - values[j]) / h);
            }
            density /= (n * h);

            context.mapToPixel(x, density, buf);
            px[count] = (float) buf[0];
            py[count] = (float) buf[1];
            count++;
        }
        return count;
    }

    private double gaussianKernel(double u) {
        return Math.exp(-0.5 * u * u) / Math.sqrt(2 * Math.PI);
    }

    private double getCalculatedSilvermanBandwidth(double[] values) {
        final int n = values.length;
        if (n < 2) return 1.0;

        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / n;

        double sumSqDiff = 0;
        for (double v : values) sumSqDiff += (v - mean) * (v - mean);
        double stdDev = Math.sqrt(sumSqDiff / n);

        double q1 = values[(int) (n * 0.25)];
        double q3 = values[(int) (n * 0.75)];
        double iqr = (q3 - q1) / 1.34;

        return 0.9 * Math.min(stdDev, iqr) * Math.pow(n, -0.2);
    }

    public KDERenderer setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
        return this;
    }
}
