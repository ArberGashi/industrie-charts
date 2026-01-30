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
/**
 * Chernoff faces renderer: maps multivariate values to facial features. Simplified for demo.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ChernoffFacesRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("chernoff_faces", new RendererDescriptor("chernoff_faces", "renderer.chernoff_faces", "/icons/chernoff.svg"), ChernoffFacesRenderer::new);
    }

    public ChernoffFacesRenderer() {
        super("chernoff_faces");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int total = model.getPointCount();
        if (total == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.x() + bounds.width() * 0.5;
        double cy = bounds.y() + bounds.height() * 0.5;

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
            ArberColor faceColor = seriesOrBase(model, context, i);
            canvas.setColor(faceColor);
            fillCircle(canvas, x, y, size * 0.5, 16);
            canvas.setColor(themeForeground(context));
            // eyes
            fillCircle(canvas, x - size * 0.14, y - size * 0.08, size * 0.04, 10);
            fillCircle(canvas, x + size * 0.14, y - size * 0.08, size * 0.04, 10);
            // mouth as quadratic
            drawQuad(canvas,
                    x - size * 0.15, y + size * 0.02,
                    x, y + size * 0.12,
                    x + size * 0.15, y + size * 0.02,
                    8
            );
            if (++drawn > 50) break; // safety for too many points
        }
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r, int segments) {
        int count = Math.max(6, segments);
        float[] xs = RendererAllocationCache.getFloatArray(this, "chernoff.circle.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "chernoff.circle.y", count);
        double step = (Math.PI * 2.0) / count;
        for (int i = 0; i < count; i++) {
            double a = i * step;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, count);
    }

    private void drawQuad(ArberCanvas canvas, double x1, double y1, double cx, double cy, double x2, double y2, int steps) {
        int count = Math.max(6, steps) + 1;
        float[] xs = RendererAllocationCache.getFloatArray(this, "chernoff.mouth.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "chernoff.mouth.y", count);
        int n = count - 1;
        for (int i = 0; i <= n; i++) {
            double t = (double) i / (double) n;
            double inv = 1.0 - t;
            double x = inv * inv * x1 + 2.0 * inv * t * cx + t * t * x2;
            double y = inv * inv * y1 + 2.0 * inv * t * cy + t * t * y2;
            xs[i] = (float) x;
            ys[i] = (float) y;
        }
        canvas.drawPolyline(xs, ys, count);
    }
}
