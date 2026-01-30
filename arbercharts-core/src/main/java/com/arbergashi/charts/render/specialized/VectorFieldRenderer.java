package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

/**
 * Vector Field Renderer - ArberGashi Engine.
 * Visualizes flow or vector fields using arrow glyphs. Optimized for low allocations.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class VectorFieldRenderer extends BaseRenderer {

    static {
        // Registration removed: use com.arbergashi.charts.render.analysis.VectorFieldRenderer
    }

    public VectorFieldRenderer() {
        super("vectorfield");
        throw new UnsupportedOperationException("Specialized VectorFieldRenderer is removed. Use com.arbergashi.charts.render.analysis.VectorFieldRenderer instead.");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        canvas.setColor(getSeriesColor(model));
        canvas.setStroke(ChartScale.scale(1.2f));

        double arrowLength = ChartScale.scale(15.0);
        double headSize = ChartScale.scale(5.0);

        double[] buf = pBuffer();
        float[] lineX = RendererAllocationCache.getFloatArray(this, "vec.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "vec.line.y", 2);

        for (int i = 0; i < count; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            double cx = buf[0];
            double cy = buf[1];

            double angle = Double.isFinite(model.getWeight(i)) ? model.getWeight(i) : model.getY(i);
            double c = Math.cos(angle);
            double s = Math.sin(angle);

            double x2 = Math.fma(c, arrowLength, cx);
            double y2 = Math.fma(s, arrowLength, cy);

            lineX[0] = (float) cx;
            lineY[0] = (float) cy;
            lineX[1] = (float) x2;
            lineY[1] = (float) y2;
            canvas.drawPolyline(lineX, lineY, 2);

            double angle1 = angle + Math.PI * 0.75;
            double angle2 = angle - Math.PI * 0.75;
            double xh1 = Math.fma(Math.cos(angle1), headSize, x2);
            double yh1 = Math.fma(Math.sin(angle1), headSize, y2);
            double xh2 = Math.fma(Math.cos(angle2), headSize, x2);
            double yh2 = Math.fma(Math.sin(angle2), headSize, y2);

            lineX[0] = (float) x2;
            lineY[0] = (float) y2;
            lineX[1] = (float) xh1;
            lineY[1] = (float) yh1;
            canvas.drawPolyline(lineX, lineY, 2);

            lineX[0] = (float) x2;
            lineY[0] = (float) y2;
            lineX[1] = (float) xh2;
            lineY[1] = (float) yh2;
            canvas.drawPolyline(lineX, lineY, 2);
        }
    }
}
