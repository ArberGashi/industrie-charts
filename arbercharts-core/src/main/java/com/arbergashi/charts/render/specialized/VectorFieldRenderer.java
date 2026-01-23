package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;

/**
 * Vector Field Renderer - ArberGashi Engine.
 * Visualizes flow or vector fields using arrow glyphs. Optimized for low allocations.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class VectorFieldRenderer extends BaseRenderer {

    static {
        // Registration removed: use com.arbergashi.charts.render.analysis.VectorFieldRenderer
        // RendererRegistry.register("vectorfield", new RendererDescriptor("vectorfield", "renderer.vectorfield", "/icons/vectorfield.svg"), VectorFieldRenderer::new);
    }

    public VectorFieldRenderer() {
        super("vectorfield");
        throw new UnsupportedOperationException("Specialized VectorFieldRenderer is removed. Use com.arbergashi.charts.render.analysis.VectorFieldRenderer instead.");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        g2.setColor(getSeriesColor(model));
        g2.setStroke(getCachedStroke(ChartScale.scale(1.2f)));

        double arrowLength = ChartScale.scale(15.0);
        double headSize = ChartScale.scale(5.0);

        double[] buf = pBuffer();
        for (int i = 0; i < count; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            double cx = buf[0];
            double cy = buf[1];

            // Use 'weight' as angle in radians if present; otherwise approximate from y
            double angle = Double.isFinite(model.getWeight(i)) ? model.getWeight(i) : model.getY(i);
            double c = Math.cos(angle);
            double s = Math.sin(angle);

            double x2 = Math.fma(c, arrowLength, cx);
            double y2 = Math.fma(s, arrowLength, cy);

            // shaft
            g2.draw(getLine(cx, cy, x2, y2));

            // arrow heads (two lines)
            double angle1 = angle + Math.PI * 0.75;
            double angle2 = angle - Math.PI * 0.75;
            double xh1 = Math.fma(Math.cos(angle1), headSize, x2);
            double yh1 = Math.fma(Math.sin(angle1), headSize, y2);
            double xh2 = Math.fma(Math.cos(angle2), headSize, x2);
            double yh2 = Math.fma(Math.sin(angle2), headSize, y2);

            g2.draw(getLine(x2, y2, xh1, yh1));
            g2.draw(getLine(x2, y2, xh2, yh2));
        }
    }
}
