package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.tools.RendererAllocationCache;
import java.util.Optional;
/**
 * <h1>Modern Donut Renderer</h1>
 * <p>
 * Draws a professional, interactive donut chart with a customizable center content area,
 * adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Center Content:</b> API to display text (e.g., total, title) in the center hole.</li>
 *     <li><b>Smart Labels:</b> Inherits intelligent external labeling from {@link PieRenderer}.</li>
 *     <li><b>Hover Effect:</b> Inherits segment highlighting from {@link PieRenderer}.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class DonutRenderer extends PieRenderer {

    private static final double INNER_FACTOR = 0.55;

    private double lastCx, lastCy, lastOuterR;
    private String centerText;
    private String centerSubText;
    
    public DonutRenderer() {
        super("donut");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        super.drawData(canvas, model, context);

        ArberRect plot = context.getPlotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        double cx = plot.centerX();
        double cy = plot.centerY();

        lastCx = cx;
        lastCy = cy;
        lastOuterR = diameter * 0.5;

        double innerDiameter = diameter * INNER_FACTOR;
        if (innerDiameter <= 0) return;

        ChartTheme t = getResolvedTheme(context);
        ArberColor bg = t.getBackground();
        canvas.setColor(bg);
        drawCircleFill(canvas, cx, cy, innerDiameter / 2.0);
    }


    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (lastOuterR <= 0) return Optional.empty();

        double dx = pixel.x() - lastCx;
        double dy = pixel.y() - lastCy;
        double d2 = dx * dx + dy * dy;

        double innerR = lastOuterR * INNER_FACTOR;
        if (d2 < innerR * innerR) {
            setHoverIndex(-1);
            return Optional.empty();
        }

        return super.getPointAt(pixel, model, context);
    }

    // --- Public API for Center Content ---

    public DonutRenderer setCenterText(String text){
        this.centerText = text;
        return this;
        
    }

    public DonutRenderer setCenterSubText(String text){
        this.centerSubText = text;
        return this;
        
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 48;
        float[] xs = RendererAllocationCache.getFloatArray(this, "donut.inner.x", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "donut.inner.y", segments);
        for (int i = 0; i < segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }
}
