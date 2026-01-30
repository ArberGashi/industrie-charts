package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Wind Rose renderer: aggregates wind direction (angle) into radial bins and draws stacked rings.
 * This implementation focuses on reuse of arrays and minimal allocations.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class WindRoseRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("wind_rose", new RendererDescriptor("wind_rose", "renderer.wind_rose", "/icons/wind_rose.svg"), WindRoseRenderer::new);
    }

    private transient int[] bins; // counts per direction bin
    private transient double[] speeds; // aggregated speeds per bin

    public WindRoseRenderer() {
        super("wind_rose");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        var bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double radius = Math.min(bounds.width(), bounds.height()) * 0.4;

        int dirs = 16; // 16 direction bins (22.5° each)
        if (bins == null || bins.length < dirs) {
            bins = RendererAllocationCache.getIntArray(this, "bins", dirs);
            speeds = RendererAllocationCache.getDoubleArray(this, "speeds", dirs);
        }
        for (int i = 0; i < dirs; i++) {
            bins[i] = 0;
            speeds[i] = 0.0;
        }

        // Interpret ChartPoint: x=directionDegrees, y=speed
        for (int i = 0; i < count; i++) {
            double deg = xData[i];
            int bin = (int) Math.floor((deg % 360 + 360) % 360 / (360.0 / dirs));
            bins[bin]++;
            speeds[bin] += yData[i];
        }

        int max = 1;
        for (int v : bins) if (v > max) max = v;

        canvas.setStroke(getSeriesStrokeWidth());

        int segs = 24;
        float[] xs = RendererAllocationCache.getFloatArray(this, "wind.arc.x", segs + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "wind.arc.y", segs + 1);
        float[] polyX = RendererAllocationCache.getFloatArray(this, "wind.poly.x", segs * 2 + 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "wind.poly.y", segs * 2 + 2);

        for (int i = 0; i < dirs; i++) {
            double startDeg = i * (360.0 / dirs);
            double extent = 360.0 / dirs;
            double r = radius * (bins[i] / (double) max);

            if (r <= 0.0) continue;

            // outer arc
            double startRad = Math.toRadians(-startDeg - extent);
            double endRad = Math.toRadians(-startDeg);
            for (int s = 0; s <= segs; s++) {
                double t = startRad + (endRad - startRad) * (s / (double) segs);
                xs[s] = (float) (cx + Math.cos(t) * r);
                ys[s] = (float) (cy + Math.sin(t) * r);
            }
            int idx = 0;
            for (int s = 0; s <= segs; s++) {
                polyX[idx] = xs[s];
                polyY[idx] = ys[s];
                idx++;
            }
            // inner arc at center
            polyX[idx] = (float) cx;
            polyY[idx] = (float) cy;
            idx++;

            ArberColor directionColor = seriesOrBase(model, context, i);
            canvas.setColor(directionColor);
            canvas.fillPolygon(polyX, polyY, idx);
        }
    }
}
