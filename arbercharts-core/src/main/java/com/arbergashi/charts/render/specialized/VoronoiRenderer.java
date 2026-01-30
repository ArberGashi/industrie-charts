package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Voronoi renderer (optimized): uses a lower-resolution sampling buffer and renders as rectangles.
 * <p>
 * Performance notes:
 * <ul>
 *   <li>No per-point allocations in the hot loop</li>
 *   <li>Sampling runs in buffer space directly</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class VoronoiRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("voronoi", new RendererDescriptor("voronoi", "renderer.voronoi", "/icons/voronoi.svg"), VoronoiRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient double[] sx;
    private transient double[] sy;
    private transient ArberColor[] palette;
    private transient int[] cells;

    public VoronoiRenderer() {
        super("voronoi");
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

        ArberRect bounds = context.getPlotBounds();
        int w = Math.min(200, Math.max(120, (int) bounds.width()));
        int h = Math.min(200, Math.max(120, (int) bounds.height()));

        int total = w * h;
        if (cells == null || cells.length < total) {
            cells = RendererAllocationCache.getIntArray(this, "cells", total);
        }

        int n = Math.min(count, 200); // limit candidate points
        if (sx == null || sx.length < n) {
            sx = RendererAllocationCache.getDoubleArray(this, "sx", n);
            sy = RendererAllocationCache.getDoubleArray(this, "sy", n);
        }
        if (palette == null || palette.length < n) {
            palette = (ArberColor[]) RendererAllocationCache.getArray(this, "voronoi.palette", ArberColor.class, n);
        }

        // Precompute candidate pixel positions + a stable palette.
        ArberColor baseColor = getSeriesColor(model);
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], pBuffer);
            sx[i] = pBuffer[0];
            sy[i] = pBuffer[1];
            ArberColor cellColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (cellColor == null) cellColor = baseColor;
            palette[i] = cellColor;
        }

        // Precompute buffer-space mapping (avoid double divides in inner loop).
        double bx = bounds.x();
        double by = bounds.y();
        double bw = bounds.width();
        double bh = bounds.height();
        double stepX = bw / (double) w;
        double stepY = bh / (double) h;

        // sample grid (tight hot loop)
        int idx = 0;
        for (int iy = 0; iy < h; iy++) {
            double py = by + (iy + 0.5) * stepY;
            for (int ix = 0; ix < w; ix++, idx++) {
                double px = bx + (ix + 0.5) * stepX;
                int best = 0;
                double bestD = Double.POSITIVE_INFINITY;
                for (int i = 0; i < n; i++) {
                    double dx = sx[i] - px;
                    double dy = sy[i] - py;
                    double d = dx * dx + dy * dy;
                    if (d < bestD) {
                        bestD = d;
                        best = i;
                    }
                }
                cells[idx] = best;
            }
        }

        // draw cells
        idx = 0;
        for (int iy = 0; iy < h; iy++) {
            double y0 = by + iy * stepY;
            for (int ix = 0; ix < w; ix++, idx++) {
                ArberColor c = palette[cells[idx]];
                if (c == null) continue;
                canvas.setColor(c);
                float x0 = (float) (bx + ix * stepX);
                canvas.fillRect(x0, (float) y0, (float) stepX + 0.5f, (float) stepY + 0.5f);
            }
        }
    }
}
