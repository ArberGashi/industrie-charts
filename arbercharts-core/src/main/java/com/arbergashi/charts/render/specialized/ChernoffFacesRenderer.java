package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Chernoff faces renderer: maps multivariate values to facial features. Simplified for demo.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChernoffFacesRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("chernoff_faces", new RendererDescriptor("chernoff_faces", "renderer.chernoff_faces", "/icons/chernoff.svg"), ChernoffFacesRenderer::new);
    }

    public ChernoffFacesRenderer() {
        super("chernoff_faces");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int total = model.getPointCount();
        if (total == 0) return;

        ChartTheme theme = resolveTheme(context);
        Rectangle2D bounds = context.plotBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();

        int count = model.getPointCount();
        if (count == 0) return;
        double[] buf = pBuffer();
        int drawn = 0;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), buf);
            double x = buf[0];
            double y = buf[1];
            // JDK 25: Use Math.clamp() for face size bounds
            double size = Math.clamp(model.getY(i), 8, 40);
            // face
            Color faceColor = seriesOrBase(model, context, i);
            g2.setColor(faceColor);
            g2.fill(getEllipse(x - size / 2, y - size / 2, size, size));
            g2.setColor(theme.getForeground());
            // eyes
            g2.fill(getEllipse(x - size * 0.18, y - size * 0.12, size * 0.08, size * 0.08));
            g2.fill(getEllipse(x + size * 0.1, y - size * 0.12, size * 0.08, size * 0.08));
            // mouth as quadratic
            Path2D mouth = getPathCache();
            mouth.moveTo(x - size * 0.15, y + size * 0.02);
            mouth.quadTo(x, y + size * 0.12, x + size * 0.15, y + size * 0.02);
            g2.draw(mouth);
            if (++drawn > 50) break; // safety for too many points
        }
    }
}
