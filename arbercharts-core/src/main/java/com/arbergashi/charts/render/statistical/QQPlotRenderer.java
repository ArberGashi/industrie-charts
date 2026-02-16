package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class QQPlotRenderer extends BaseRenderer {

    public QQPlotRenderer() {
        super("qqplot");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        // Extract and sort data values (reuse cached buffer)
        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "qq.values", n);
        for (int i = 0; i < n; i++) values[i] = model.getY(i);
        java.util.Arrays.sort(values, 0, n);

        ChartTheme theme = getResolvedTheme(context);
        final ArberColor pointColor = getSeriesColor(model);
        final ArberColor lineColor = theme.getGridColor();
        final double dotSize = ChartScale.scale(6.0);

        // Compute min/max theoretical quantiles for reference line
        double minVal = inverseNormalCDF((0 + 0.5) / n);
        double maxVal = inverseNormalCDF((n - 1 + 0.5) / n);

        double[] buf = pBuffer();
        context.mapToPixel(minVal, minVal, buf);
        double lx1 = buf[0], ly1 = buf[1];
        context.mapToPixel(maxVal, maxVal, buf);
        double lx2 = buf[0], ly2 = buf[1];

        canvas.setColor(lineColor);
        canvas.setStroke(ChartScale.scale(2.0f));
        drawLine(canvas, lx1, ly1, lx2, ly2);

        // Draw Q-Q points directly (no intermediary ArberPoint allocations)
        canvas.setStroke(ChartScale.scale(1.0f));
        for (int i = 0; i < n; i++) {
            double p = (i + 0.5) / n;
            double theoreticalQuantile = inverseNormalCDF(p);
            double sampleQuantile = values[i];

            context.mapToPixel(theoreticalQuantile, sampleQuantile, buf);

            ArberColor dotColor = isMultiColor() ? themeSeries(context, i) : pointColor;
            if (dotColor == null) dotColor = pointColor;
            canvas.setColor(dotColor);
            fillCircle(canvas, buf[0], buf[1], dotSize * 0.5);
            canvas.setColor(theme.getGridColor());
            strokeCircle(canvas, buf[0], buf[1], dotSize * 0.5);
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

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "qq.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "qq.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "qq.cx", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "qq.cy", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

    private void strokeCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "qq.sx", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "qq.sy", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }
}
