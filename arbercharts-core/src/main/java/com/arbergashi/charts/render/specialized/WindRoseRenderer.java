package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Wind Rose renderer: aggregates wind direction (angle) into radial bins and draws stacked rings.
 * This implementation focuses on reuse of arrays and minimal allocations.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle2D bounds = context.plotBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double radius = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.4;

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

        Stroke prevStroke = g2.getStroke();
        Color prevColor = g2.getColor();
        g2.setStroke(getSeriesStroke());

        // draw each direction as an arc sector with radius proportional to count
        for (int i = 0; i < dirs; i++) {
            double startDeg = i * (360.0 / dirs);
            double extent = 360.0 / dirs;
            double r = radius * (bins[i] / (double) max);
            // draw filled arc
            Shape arc = getArc(cx - r, cy - r, r * 2, r * 2, -startDeg - extent, extent, Arc2D.PIE);

            // Each direction gets a distinct color from the theme palette
            Color directionColor = seriesOrBase(model, context, i);
            g2.setColor(directionColor);
            g2.fill(arc);
        }

        // restore
        g2.setColor(prevColor);
        g2.setStroke(prevStroke);
    }
}
