package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Professional, zero-allocation Kernel Density Estimate (KDE) renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
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

        double h = bandwidth > 0 ? bandwidth : calculateSilvermanBandwidth(values);
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

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        // 1. Extract data into a primitive array (reuse cached buffer)
        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "kde.values", n);
        for (int i = 0; i < n; i++) {
            values[i] = model.getY(i);
        }
        java.util.Arrays.sort(values, 0, n);

        // 2. Calculate KDE
        Path2D kdePath = buildKDEPath(values, context);
        if (kdePath == null) return;

        final Color baseColor = seriesOrBase(model, context, 0);
        Color lineColor = isMultiColor() ? themeSeries(context, 1) : baseColor;
        if (lineColor == null) lineColor = baseColor;
        final Color fillColor = ColorUtils.withAlpha(baseColor, 0.3f);

        // 3. Fill area under curve
        Path2D fillPath = com.arbergashi.charts.tools.RendererAllocationCache.getPath(this, "kde.fillPath");
        fillPath.append(kdePath, false);
        Rectangle2D bounds = context.plotBounds();
        fillPath.lineTo(bounds.getMaxX(), bounds.getMaxY());
        fillPath.lineTo(bounds.getMinX(), bounds.getMaxY());
        fillPath.closePath();

        g.setColor(fillColor);
        g.fill(fillPath);

        // 4. Draw KDE curve
        g.setColor(lineColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.5f)));
        g.draw(kdePath);
    }

    private Path2D.Double buildKDEPath(double[] values, PlotContext context) {
        final int n = values.length;
        if (n == 0) return null;

        double h = bandwidth > 0 ? bandwidth : calculateSilvermanBandwidth(values);
        double min = values[0];
        double max = values[n - 1];
        double range = max - min;

        double xMin = min - range * 0.1;
        double xMax = max + range * 0.1;

        int numPoints = 200;
        Path2D.Double path = getPathCache();
        path.reset();
        boolean moved = false;
        double[] buf = pBuffer();

        for (int i = 0; i < numPoints; i++) {
            double x = xMin + (xMax - xMin) * i / (numPoints - 1);
            double density = 0;
            for (int j = 0; j < n; j++) {
                density += gaussianKernel((x - values[j]) / h);
            }
            density /= (n * h);

            context.mapToPixel(x, density, buf);
            if (!moved) {
                path.moveTo(buf[0], buf[1]);
                moved = true;
            } else {
                path.lineTo(buf[0], buf[1]);
            }
        }
        return path;
    }

    private double gaussianKernel(double u) {
        return Math.exp(-0.5 * u * u) / Math.sqrt(2 * Math.PI);
    }

    private double calculateSilvermanBandwidth(double[] values) {
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

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }
}
