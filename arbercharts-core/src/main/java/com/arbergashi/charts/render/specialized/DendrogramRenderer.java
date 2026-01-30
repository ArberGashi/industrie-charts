package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

/**
 * Dendrogram Renderer - ArberGashi Engine.
 * Visualizes hierarchical cluster structures through a tree diagram.
 * Optimized to reduce allocations and to aggregate when input is dense.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class DendrogramRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("dendrogram", new RendererDescriptor("dendrogram", "renderer.dendrogram", "/icons/dendrogram.svg"), DendrogramRenderer::new);
    }

    public DendrogramRenderer() {
        super("dendrogram");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        canvas.setStroke(ChartScale.scale(1.5f));
        ArberColor baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            canvas.setColor(baseColor);
        }

        int decimation = 1;
        if (n > 2000) decimation = (int) Math.ceil(n / 1000.0);

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

        float[] lineX = RendererAllocationCache.getFloatArray(this, "den.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "den.line.y", 2);

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
                ArberColor branchColor = themeSeries(context, pairIndex);
                if (branchColor == null) branchColor = baseColor;
                canvas.setColor(branchColor);
            }

            // vertical from leaf 1
            lineX[0] = (float) pix1x;
            lineY[0] = (float) pix1y;
            lineX[1] = (float) pix1x;
            lineY[1] = (float) midY;
            canvas.drawPolyline(lineX, lineY, 2);

            // horizontal connector
            lineX[0] = (float) pix1x;
            lineY[0] = (float) midY;
            lineX[1] = (float) pix2x;
            lineY[1] = (float) midY;
            canvas.drawPolyline(lineX, lineY, 2);

            // vertical to leaf 2
            lineX[0] = (float) pix2x;
            lineY[0] = (float) midY;
            lineX[1] = (float) pix2x;
            lineY[1] = (float) pix2y;
            canvas.drawPolyline(lineX, lineY, 2);

            // upward connector
            if ((i + 2 * decimation) < n) {
                double cx = (pix1x + pix2x) / 2.0;
                double cy = midY - ChartScale.scale(20);
                lineX[0] = (float) cx;
                lineY[0] = (float) midY;
                lineX[1] = (float) cx;
                lineY[1] = (float) cy;
                canvas.drawPolyline(lineX, lineY, 2);
            }

            pairIndex++;
        }
    }

    public DendrogramRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
