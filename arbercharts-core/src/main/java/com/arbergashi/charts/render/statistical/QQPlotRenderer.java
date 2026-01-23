package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;

/**
 * <h1>QQPlotRenderer - Quantile-Quantile Plot</h1>
 *
 * <p>Professional Q-Q plot renderer for distribution comparison.
 * Compares sample quantiles against theoretical distribution quantiles.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Quantile Points:</b> Scatter plot of observed vs. expected quantiles</li>
 *   <li><b>Reference Line:</b> 45-degree line for perfect normal distribution</li>
 *   <li><b>Distribution Test:</b> Visual test for normality</li>
 *   <li><b>Deviation Highlighting:</b> Points far from line indicate non-normality</li>
 * </ul>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>Points on line = Data follows theoretical distribution</li>
 *   <li>S-curve = Heavy tails (leptokurtic)</li>
 *   <li>Inverted S-curve = Light tails (platykurtic)</li>
 *   <li>Points above line at ends = Right-skewed data</li>
 *   <li>Points below line at ends = Left-skewed data</li>
 * </ul>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Normality testing</li>
 *   <li>Distribution comparison</li>
 *   <li>Regression diagnostics</li>
 *   <li>Quality control</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class QQPlotRenderer extends BaseRenderer {

    public QQPlotRenderer() {
        super("qqplot");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        // Extract and sort data values (reuse cached buffer)
        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "qq.values", n);
        for (int i = 0; i < n; i++) values[i] = model.getY(i);
        java.util.Arrays.sort(values, 0, n);

        ChartTheme theme = resolveTheme(context);
        final Color pointColor = getSeriesColor(model);
        final Color lineColor = theme.getGridColor();
        final double dotSize = ChartScale.scale(6.0);

        // Compute min/max theoretical quantiles for reference line
        double minVal = inverseNormalCDF((0 + 0.5) / n);
        double maxVal = inverseNormalCDF((n - 1 + 0.5) / n);

        double[] buf = pBuffer();
        context.mapToPixel(minVal, minVal, buf);
        double lx1 = buf[0], ly1 = buf[1];
        context.mapToPixel(maxVal, maxVal, buf);
        double lx2 = buf[0], ly2 = buf[1];

        g.setColor(lineColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.0f)));
        g.draw(getLine(lx1, ly1, lx2, ly2));

        // Draw Q-Q points directly (no intermediary Point2D allocations)
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        for (int i = 0; i < n; i++) {
            double p = (i + 0.5) / n;
            double theoreticalQuantile = inverseNormalCDF(p);
            double sampleQuantile = values[i];

            context.mapToPixel(theoreticalQuantile, sampleQuantile, buf);

            Color dotColor = isMultiColor() ? themeSeries(context, i) : pointColor;
            if (dotColor == null) dotColor = pointColor;
            g.setColor(dotColor);
            g.fill(getEllipse(buf[0] - dotSize / 2, buf[1] - dotSize / 2, dotSize, dotSize));
            g.setColor(theme.getGridColor());
            g.draw(getEllipse(buf[0] - dotSize / 2, buf[1] - dotSize / 2, dotSize, dotSize));
        }
    }


    /**
     * Approximation of inverse normal CDF (quantile function) using Beasley-Springer-Moro algorithm.
     */
    private double inverseNormalCDF(double p) {
        if (p <= 0 || p >= 1) {
            return p < 0.5 ? -6 : 6;  // Extreme values
        }

        // Beasley-Springer-Moro approximation
        double[] a = {2.50662823884, -18.61500062529, 41.39119773534, -25.44106049637};
        double[] b = {-8.47351093090, 23.08336743743, -21.06224101826, 3.13082909833};
        double[] c = {0.3374754822726147, 0.9761690190917186, 0.1607979714918209,
                0.0276438810333863, 0.0038405729373609, 0.0003951896511919,
                0.0000321767881768, 0.0000002888167364, 0.0000003960315187};

        double y = p - 0.5;

        if (Math.abs(y) < 0.42) {
            // Central region
            double r = y * y;
            double x = y * (((a[3] * r + a[2]) * r + a[1]) * r + a[0]) /
                    ((((b[3] * r + b[2]) * r + b[1]) * r + b[0]) * r + 1.0);
            return x;
        } else {
            // Tail region
            double r = p < 0.5 ? p : 1.0 - p;
            r = Math.sqrt(-Math.log(r));

            double x = (((c[8] * r + c[7]) * r + c[6]) * r + c[5]) * r + c[4];
            x = ((((x * r + c[3]) * r + c[2]) * r + c[1]) * r + c[0]) / r;

            return y < 0 ? -x : x;
        }
    }
}
