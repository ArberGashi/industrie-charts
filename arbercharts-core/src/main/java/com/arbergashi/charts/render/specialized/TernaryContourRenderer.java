package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.CompositePool;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

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
 */
public class TernaryContourRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("ternary_contour", new RendererDescriptor("ternary_contour", "renderer.ternary_contour", "/icons/ternary_contour.svg"), TernaryContourRenderer::new);
    }

    private transient final java.awt.geom.Ellipse2D.Double circle = new java.awt.geom.Ellipse2D.Double();
    // reusable transform output to avoid Point2D allocations
    private transient final Point2D.Double tmp = new Point2D.Double();
    // reusable geometry
    private transient Path2D triangle;
    private transient Stroke triStroke;

    public TernaryContourRenderer() {
        super("ternaryContour");
    }

    private static void transformTernaryInto(double a, double b, double c, PlotContext context, Point2D.Double out) {
        double sum = a + b + c;
        if (sum == 0) sum = 1.0;
        double na = a / sum;
        double nb = b / sum;

        double side = Math.min(context.plotBounds().getWidth(), context.plotBounds().getHeight() * 1.15);
        double height = side * Math.sqrt(3) / 2.0;
        double x0 = context.plotBounds().getCenterX() - side / 2.0;
        double y0 = context.plotBounds().getCenterY() + height / 2.0;

        out.x = x0 + (nb + na / 2.0) * side;
        out.y = y0 - na * height;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        drawTernaryTriangle(g2, context);

        Color baseColor = seriesOrBase(model, context, 0);
        Color prevColor = g2.getColor();
        Composite prevComp = g2.getComposite();

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

            Color pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            g2.setColor(pointColor);
            g2.setComposite(CompositePool.get(alpha));
            circle.x = tmp.x - r;
            circle.y = tmp.y - r;
            circle.width = r * 2;
            circle.height = r * 2;
            g2.fill(circle);
        }

        // Draw the actual points on top (opaque).
        g2.setComposite(prevComp);

        double size = ChartScale.scale(5.0);
        double half = size / 2.0;

        for (int i = 0; i < n; i += step) {
            double a = model.getX(i);
            double b = model.getY(i);
            double c = model.getWeight(i);
            transformTernaryInto(a, b, c, context, tmp);
            Color pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            g2.setColor(pointColor);
            circle.x = tmp.x - half;
            circle.y = tmp.y - half;
            circle.width = size;
            circle.height = size;
            g2.fill(circle);
        }

        g2.setColor(prevColor);
        g2.setComposite(prevComp);
    }

    private void drawTernaryTriangle(Graphics2D g2, PlotContext context) {
        if (triangle == null) triangle = new Path2D.Double(Path2D.WIND_NON_ZERO);

        double w = context.plotBounds().getWidth();
        double h = context.plotBounds().getHeight();
        double side = Math.min(w, h * 1.15);
        double height = side * Math.sqrt(3) / 2.0;

        double x0 = context.plotBounds().getCenterX() - side / 2.0;
        double y0 = context.plotBounds().getCenterY() + height / 2.0;

        triangle.reset();
        triangle.moveTo(x0, y0);
        triangle.lineTo(x0 + side, y0);
        triangle.lineTo(x0 + side / 2.0, y0 - height);
        triangle.closePath();

        if (triStroke == null) {
            triStroke = new BasicStroke((float) ChartScale.scale(1.0));
        }

        Stroke prev = g2.getStroke();
        Color prevColor = g2.getColor();

        ChartTheme theme = resolveTheme(context);

        g2.setColor(theme.getGridColor());
        g2.setStroke(triStroke);
        g2.draw(triangle);

        g2.setColor(prevColor);
        g2.setStroke(prev);
    }

}
