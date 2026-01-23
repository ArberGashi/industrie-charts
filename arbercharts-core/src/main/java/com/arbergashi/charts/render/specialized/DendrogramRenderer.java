package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Dendrogram Renderer - ArberGashi Engine.
 * Visualizes hierarchical cluster structures through a tree diagram.
 * Optimized to reduce allocations and to aggregate when input is dense.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class DendrogramRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("dendrogram", new RendererDescriptor("dendrogram", "renderer.dendrogram", "/icons/dendrogram.svg"), DendrogramRenderer::new);
    }

    public DendrogramRenderer() {
        super("dendrogram");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f)));
        Color baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            g2.setColor(baseColor);
        }

        // decimate when too many points to avoid massive drawing work
        int decimation = 1;
        if (n > 2000) decimation = (int) Math.ceil(n / 1000.0);

        Path2D pth = getPathCache();

        // precompute pixel positions for leaves to reduce repeated mapping
        int pairs = (n + (2 * decimation - 1)) / (2 * decimation);
        double[] xs = RendererAllocationCache.getDoubleArray(this, "xs", pairs * 2);
        double[] ys = RendererAllocationCache.getDoubleArray(this, "ys", pairs * 2);
        int idx = 0;
        double[] buf = pBuffer();
        for (int i = 0; i < n - 1; i += 2 * decimation) {
            int i2 = Math.min(i + decimation, n - 1);
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            xs[idx] = buf[0];
            ys[idx] = buf[1];
            idx++;
            context.mapToPixel(model.getX(i2), model.getY(i2), buf);
            xs[idx] = buf[0];
            ys[idx] = buf[1];
            idx++;
        }

        idx = 0;
        int pairIndex = 0;
        for (int i = 0; i < n - 1; i += 2 * decimation) {
            double pix1x = xs[idx];
            double pix1y = ys[idx];
            idx++;
            double pix2x = xs[idx];
            double pix2y = ys[idx];
            idx++;

            double midY = Math.min(pix1y, pix2y) - ChartScale.scale(30);

            if (isMultiColor()) {
                Color branchColor = themeSeries(context, pairIndex);
                if (branchColor == null) branchColor = baseColor;
                g2.setColor(branchColor);
                g2.draw(getLine(pix1x, pix1y, pix1x, midY));
                g2.draw(getLine(pix1x, midY, pix2x, midY));
                g2.draw(getLine(pix2x, midY, pix2x, pix2y));
                if ((i + 2 * decimation) < n) {
                    double cx = (pix1x + pix2x) / 2.0;
                    double cy = midY - ChartScale.scale(20);
                    g2.draw(getLine((pix1x + pix2x) / 2.0, midY, cx, cy));
                }
                drawLabel(g2, "n" + i, g2.getFont(), g2.getColor(), (float) pix1x, (float) (pix1y + 4));
            } else {
                // build the L-shaped connector for this leaf pair
                pth.moveTo(pix1x, pix1y);
                pth.lineTo(pix1x, midY);
                pth.lineTo(pix2x, midY);
                pth.lineTo(pix2x, pix2y);

                // upward connector as a small vertical line
                if ((i + 2 * decimation) < n) {
                    double cx = (pix1x + pix2x) / 2.0;
                    double cy = midY - ChartScale.scale(20);
                    pth.moveTo((pix1x + pix2x) / 2.0, midY);
                    pth.lineTo(cx, cy);
                }

                drawLabel(g2, "n" + i, g2.getFont(), g2.getColor(), (float) pix1x, (float) (pix1y + 4));
            }
            pairIndex++;
        }

        if (!isMultiColor()) {
            // draw the whole path once
            g2.draw(pth);
        }
    }

    public DendrogramRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
