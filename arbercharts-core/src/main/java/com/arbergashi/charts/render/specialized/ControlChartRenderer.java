package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

/**
 * ControlChartRenderer (Shewhart Chart / SPC).
 * Headless implementation using ArberCanvas primitives only.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ControlChartRenderer extends BaseRenderer {

    static {
        RendererRegistry.register(
                "control_chart",
                new RendererDescriptor("control_chart", "renderer.control_chart", "/icons/control_chart.svg"),
                ControlChartRenderer::new
        );
    }

    // Cached stats (avoid O(n) scan on repeated paints).
    private transient int statsKey;
    private transient double statsMean;
    private transient double statsUcl;
    private transient double statsLcl;
    private transient boolean statsValid;

    public ControlChartRenderer() {
        super("control_chart");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        ensureStats(yData, n);

        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.width() <= 1 || bounds.height() <= 1) return;

        // Map control lines.
        double[] buf = pBuffer();
        context.mapToPixel(0, statsMean, buf);
        double yMean = buf[1];
        context.mapToPixel(0, statsUcl, buf);
        double yUcl = buf[1];
        context.mapToPixel(0, statsLcl, buf);
        double yLcl = buf[1];

        double xStart = bounds.x();
        double xEnd = bounds.x() + bounds.width();

        ArberColor limitColor = themeBearish(context);
        ArberColor meanColor = themeAccent(context);
        ArberColor lineColor = themeSeries(context, getLayerIndex());
        if (lineColor == null) lineColor = themeForeground(context);

        canvas.setStroke((float) ChartScale.scale(1.5f));
        canvas.setColor(limitColor);
        drawLine(canvas, xStart, yUcl, xEnd, yUcl);
        drawLine(canvas, xStart, yLcl, xEnd, yLcl);

        canvas.setColor(meanColor);
        canvas.setStroke((float) ChartScale.scale(2.0f));
        drawLine(canvas, xStart, yMean, xEnd, yMean);

        // Map series to polyline.
        int step = 1;
        if (n > 20_000) step = Math.max(1, n / 8_000);
        if (n > 100_000) step = Math.max(step, n / 12_000);
        int pts = (n + step - 1) / step + 1;
        float[] xs = RendererAllocationCache.getFloatArray(this, "control.xs", pts);
        float[] ys = RendererAllocationCache.getFloatArray(this, "control.ys", pts);
        int idx = 0;
        for (int i = 0; i < n; i += step) {
            context.mapToPixel(xData[i], yData[i], buf);
            xs[idx] = (float) buf[0];
            ys[idx] = (float) buf[1];
            idx++;
        }
        if (n > 1 && ((n - 1) % step) != 0) {
            context.mapToPixel(xData[n - 1], yData[n - 1], buf);
            xs[idx] = (float) buf[0];
            ys[idx] = (float) buf[1];
            idx++;
        }
        canvas.setColor(lineColor);
        canvas.setStroke((float) ChartScale.scale(1.2f));
        canvas.drawPolyline(xs, ys, idx);

        // Draw outliers as small rects.
        float dot = (float) ChartScale.scale(5.0f);
        float half = dot * 0.5f;
        canvas.setColor(limitColor);
        for (int i = 0; i < n; i++) {
            double v = yData[i];
            if (v <= statsUcl && v >= statsLcl) continue;
            context.mapToPixel(xData[i], v, buf);
            canvas.fillRect((float) buf[0] - half, (float) buf[1] - half, dot, dot);
        }
    }

    private void ensureStats(double[] yData, int n) {
        int key = System.identityHashCode(yData) * 31 + n;
        if (statsValid && key == statsKey) return;

        double mean = 0.0;
        double m2 = 0.0;
        for (int i = 0; i < n; i++) {
            double y = yData[i];
            double delta = y - mean;
            mean += delta / (i + 1);
            double delta2 = y - mean;
            m2 += delta * delta2;
        }
        double variance = (n > 1) ? (m2 / n) : 0.0;
        double stdDev = Math.sqrt(Math.max(0.0, variance));
        statsMean = mean;
        statsUcl = mean + 3.0 * stdDev;
        statsLcl = mean - 3.0 * stdDev;
        statsKey = key;
        statsValid = true;
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "control.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "control.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }
}
