package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.StatisticalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Optional;

/**
 * Professional, zero-allocation box plot renderer.
 *
 * <p>Headless geometry only: boxes, whiskers, median and outliers.
 * Text and styling are delegated to bridges.</p>
 * <p>{@link StatisticalChartModel} supplies explicit quartiles. For a generic
 * model, component 2 is interpreted as an IQR centered on the Y value.</p>
 *
 * @author Arber Gashi
 * @since 2025-06-01
 */
public final class BoxPlotRenderer extends BaseRenderer {

    public BoxPlotRenderer() {
        super("boxplot");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int count = model.getPointCount();
        for (int i = 0; i < count; i++) {
            double median;
            double q1;
            double q3;
            double min;
            double max;
            if (model instanceof StatisticalChartModel stats) {
                median = stats.getMedian(i);
                q1 = stats.getQ1(i);
                q3 = stats.getQ3(i);
                min = stats.getMin(i);
                max = stats.getMax(i);
            } else {
                median = model.getY(i);
                double halfIqr = Math.max(0.0, model.getValue(i, 2)) / 2.0;
                q1 = median - halfIqr;
                q3 = median + halfIqr;
                min = model.getValue(i, 3);
                max = model.getValue(i, 4);
            }
            if (!Double.isFinite(model.getX(i)) || !Double.isFinite(median)
                    || !Double.isFinite(min) || !Double.isFinite(max)
                    || !Double.isFinite(q1) || !Double.isFinite(q3)) continue;
            minY = Math.min(minY, Math.min(min, Math.min(q1, Math.min(q3, median))));
            maxY = Math.max(maxY, Math.max(max, Math.max(q1, Math.max(q3, median))));
            if (model instanceof BoxPlotOutlierModel outliers) {
                double[] values = outliers.getOutliers(i);
                if (values == null) continue;
                for (double value : values) {
                    if (!Double.isFinite(value)) continue;
                    minY = Math.min(minY, value);
                    maxY = Math.max(maxY, value);
                }
            }
        }
        return minY == Double.POSITIVE_INFINITY ? null : new double[]{minY, maxY};
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ChartTheme theme = getResolvedTheme(context);
        ArberRect bounds = context.getPlotBounds();

        final double barWidth = ChartScale.scale(30.0);
        final double whiskerCap = ChartScale.scale(14.0);
        final float strokeWidth = ChartScale.scale(1.5f);

        canvas.setStroke(strokeWidth);

        for (int i = 0; i < n; i++) {
            final double xVal = model.getX(i);
            final double median;
            final double min;
            final double max;
            final double q1;
            final double q3;
            if (model instanceof StatisticalChartModel stats) {
                median = stats.getMedian(i);
                min = stats.getMin(i);
                max = stats.getMax(i);
                q1 = stats.getQ1(i);
                q3 = stats.getQ3(i);
            } else {
                median = model.getY(i);
                min = model.getValue(i, 3);
                max = model.getValue(i, 4);
                double halfIqr = Math.max(0.0, model.getValue(i, 2)) / 2.0;
                q1 = median - halfIqr;
                q3 = median + halfIqr;
            }
            if (!Double.isFinite(xVal) || !Double.isFinite(median)
                    || !Double.isFinite(min) || !Double.isFinite(max)
                    || !Double.isFinite(q1) || !Double.isFinite(q3)) continue;

            double[] buf = pBuffer();
            context.mapToPixel(xVal, median, buf);
            double x = buf[0];
            double medianY = buf[1];
            if (x < bounds.x() - barWidth || x > bounds.maxX() + barWidth) {
                continue;
            }

            context.mapToPixel(xVal, max, buf);
            double pixMaxY = buf[1];
            context.mapToPixel(xVal, min, buf);
            double pixMinY = buf[1];

            context.mapToPixel(xVal, q3, buf);
            double pixQ3Y = buf[1];
            context.mapToPixel(xVal, q1, buf);
            double pixQ1Y = buf[1];
            if (!Double.isFinite(x) || !Double.isFinite(medianY)
                    || !Double.isFinite(pixMaxY) || !Double.isFinite(pixMinY)
                    || !Double.isFinite(pixQ3Y) || !Double.isFinite(pixQ1Y)) continue;

            ArberColor boxColor = seriesOrBase(model, context, i);
            if (boxColor == null) boxColor = theme.getSeriesColor(0);

            canvas.setColor(boxColor);
            canvas.setStroke(strokeWidth);
            double xLine = x;
            double yMax = pixMaxY;
            double yQ3 = pixQ3Y;
            double yMin = pixMinY;
            double yQ1 = pixQ1Y;

            drawLine(canvas, xLine, yMax, xLine, yQ3);
            drawLine(canvas, xLine, yMin, xLine, yQ1);

            double capHalf = whiskerCap / 2.0;
            drawLine(canvas, xLine - capHalf, yMax, xLine + capHalf, yMax);
            drawLine(canvas, xLine - capHalf, yMin, xLine + capHalf, yMin);

            double boxTopY = Math.min(pixQ3Y, pixQ1Y);
            double boxHeight = Math.abs(pixQ1Y - pixQ3Y);
            if (boxHeight < 1) boxHeight = 1;

            float bx = (float) (x - barWidth / 2.0);
            float by = (float) boxTopY;
            float bw = (float) barWidth;
            float bh = (float) boxHeight;
            canvas.setColor(ColorUtils.applyAlpha(boxColor, 0.60f));
            canvas.fillRect(bx, by, bw, bh);
            canvas.setColor(boxColor);
            canvas.drawRect(bx, by, bw, bh);

            canvas.setColor(themeForeground(context));
            canvas.setStroke(ChartScale.scale(1.8f));
            drawLine(canvas, x - barWidth / 2.0, medianY, x + barWidth / 2.0, medianY);

            if (model instanceof BoxPlotOutlierModel outlierModel) {
                double[] outliers = outlierModel.getOutliers(i);
                if (outliers != null && outliers.length > 0) {
                    double r = ChartScale.scale(2.5f);
                    ArberColor outColor = ColorUtils.applyAlpha(boxColor, 0.90f);
                    canvas.setColor(outColor);
                    for (double outlier : outliers) {
                        if (!Double.isFinite(outlier)) continue;
                        context.mapToPixel(xVal, outlier, buf);
                        if (!Double.isFinite(buf[0]) || !Double.isFinite(buf[1])) continue;
                        float size = (float) (r * 2.0);
                        float ox = (float) (buf[0] - r);
                        float oy = (float) (buf[1] - r);
                        canvas.fillRect(ox, oy, size, size);
                    }
                }
            }
        }
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "boxplot.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "boxplot.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }
}
