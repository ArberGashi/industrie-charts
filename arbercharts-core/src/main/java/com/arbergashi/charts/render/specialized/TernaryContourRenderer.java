package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Ternary Contour Renderer - extends ternary plot with contour/heatmap characteristics.
 *
 * <p><b>Performance contract</b>:
 * The contour effect is rendered using a fixed number of bands and alpha compositing (no per-band Color allocation).
 * This ensures that even large datasets can be rendered efficiently without excessive memory usage.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class TernaryContourRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("ternary_contour", new RendererDescriptor("ternary_contour", "renderer.ternary_contour", "/icons/ternary_contour.svg"), TernaryContourRenderer::new);
    }

    // reusable transform output to avoid ArberPoint allocations
    private transient final ArberPoint tmp = new ArberPoint();

    public TernaryContourRenderer() {
        super("ternaryContour");
    }

    private static void transformTernaryInto(double a, double b, double c, PlotContext context, ArberPoint out) {
        double sum = a + b + c;
        if (sum == 0) sum = 1.0;
        double na = a / sum;
        double nb = b / sum;

        double side = Math.min(context.getPlotBounds().getWidth(), context.getPlotBounds().getHeight() * 1.15);
        double height = side * Math.sqrt(3) / 2.0;
        double x0 = context.getPlotBounds().getCenterX() - side / 2.0;
        double y0 = context.getPlotBounds().getCenterY() + height / 2.0;

        out.setLocation(x0 + (nb + na / 2.0) * side, y0 - na * height);
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        drawTernaryTriangle(canvas, context);

        ArberColor baseColor = seriesOrBase(model, context, 0);

        // Bound work for huge datasets.
        int n = n0;
        int step = 1;
        if (n > 10_000) step = Math.max(1, n / 8_000);
        if (n > 50_000) step = Math.max(step, n / 10_000);
        if (n > 100_000) step = Math.max(step, n / 12_000);

        // We approximate the contour heat with a single filled circle per point.
        // The previous implementation used multiple bands (multiple fills) which is prohibitively expensive.
        final double maxR = ChartScale.scale(28.0);
        final double minR = ChartScale.scale(6.0);

        // Draw heat layer.
        for (int i = 0; i < n; i += step) {
            double a = model.getX(i);
            double b = model.getY(i);
            double c = model.getWeight(i);
            transformTernaryInto(a, b, c, context, tmp);

            double intensity = (model.getMax(i) > 0) ? model.getMax(i) : 40.0;
            double r = ChartScale.scale(intensity * 0.40);
            if (r < minR) r = minR;
            if (r > maxR) r = maxR;

            // Alpha roughly proportional to radius. Clamp to a small range to avoid overdraw blowups.
            float alpha = (float) (0.06 + (r / maxR) * 0.12);
            alpha = Math.clamp(alpha, 0.06f, 0.20f);

            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            canvas.setColor(ColorUtils.applyAlpha(pointColor, alpha));
            fillCircle(canvas, tmp.x(), tmp.y(), r);
        }

        // Draw the actual points on top (opaque).
        double size = ChartScale.scale(5.0);
        double half = size / 2.0;

        for (int i = 0; i < n; i += step) {
            double a = model.getX(i);
            double b = model.getY(i);
            double c = model.getWeight(i);
            transformTernaryInto(a, b, c, context, tmp);
            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            canvas.setColor(pointColor);
            fillCircle(canvas, tmp.x(), tmp.y(), half);
        }
    }

    private void drawTernaryTriangle(ArberCanvas canvas, PlotContext context) {
        double w = context.getPlotBounds().getWidth();
        double h = context.getPlotBounds().getHeight();
        double side = Math.min(w, h * 1.15);
        double height = side * Math.sqrt(3) / 2.0;

        double x0 = context.getPlotBounds().getCenterX() - side / 2.0;
        double y0 = context.getPlotBounds().getCenterY() + height / 2.0;

        ChartTheme theme = getResolvedTheme(context);
        canvas.setColor(theme.getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));
        float[] xs = RendererAllocationCache.getFloatArray(this, "triX", 4);
        float[] ys = RendererAllocationCache.getFloatArray(this, "triY", 4);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) (x0 + side);
        ys[1] = (float) y0;
        xs[2] = (float) (x0 + side / 2.0);
        ys[2] = (float) (y0 - height);
        xs[3] = xs[0];
        ys[3] = ys[0];
        canvas.drawPolyline(xs, ys, 4);
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "ternary.cx", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ternary.cy", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

}
