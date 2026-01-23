package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Voronoi renderer (optimized): uses a lower-resolution sampling buffer and reuses a backing image.
 * <p>
 * Performance notes:
 * <ul>
 *   <li>No {@code new Color(...)} inside the pixel loop (precomputed palette)</li>
 *   <li>No {@code new Point(...)} or repeated {@code mapToPixel(...)} calls inside tight loops</li>
 *   <li>Sampling runs in buffer space directly</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class VoronoiRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("voronoi", new RendererDescriptor("voronoi", "renderer.voronoi", "/icons/voronoi.svg"), VoronoiRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient BufferedImage buf;
    private transient int[] pixels;
    private transient int bufW = -1, bufH = -1;
    // reusable per-render buffers (avoid allocations)
    private transient double[] sx;
    private transient double[] sy;
    private transient int[] palette;

    public VoronoiRenderer() {
        super("voronoi");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle2D bounds = context.plotBounds();
        int w = Math.min(400, Math.max(120, (int) bounds.getWidth()));
        int h = Math.min(400, Math.max(120, (int) bounds.getHeight()));

        if (buf == null || bufW != w || bufH != h) {
            buf = RendererAllocationCache.getBufferedImage(this, "buf", w, h, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
            bufW = w;
            bufH = h;
        }

        int n = Math.min(count, 200); // limit candidate points
        if (sx == null || sx.length < n) {
            sx = RendererAllocationCache.getDoubleArray(this, "sx", n);
            sy = RendererAllocationCache.getDoubleArray(this, "sy", n);
            palette = RendererAllocationCache.getIntArray(this, "palette", n);
        }

        // Precompute candidate pixel positions + a stable palette.
        Color baseColor = getSeriesColor(model);
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], pBuffer);
            sx[i] = pBuffer[0];
            sy[i] = pBuffer[1];
            Color cellColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (cellColor == null) cellColor = baseColor;
            palette[i] = cellColor.getRGB();
        }

        // Precompute buffer-space mapping (avoid double divides in inner loop).
        double bx = bounds.getX();
        double by = bounds.getY();
        double bw = bounds.getWidth();
        double bh = bounds.getHeight();
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
                pixels[idx] = palette[best];
            }
        }

        g2.drawImage(buf,
                (int) bounds.getX(),
                (int) bounds.getY(),
                (int) bounds.getWidth(),
                (int) bounds.getHeight(),
                null);
    }
}
