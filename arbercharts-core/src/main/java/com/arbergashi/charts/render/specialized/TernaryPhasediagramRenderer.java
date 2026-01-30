package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Ternary Phase Diagram Renderer.
 * A triangle plot for mixtures of three components (A, B, C). Uses barycentric coordinates.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class TernaryPhasediagramRenderer extends BaseRenderer {

    // Cached theme resources
    private transient int themeKey;
    private transient ArberColor sepColor;
    private transient ArberColor sepColor30;

    public TernaryPhasediagramRenderer() {
        super("ternary_phase");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ArberRect bounds = context.getPlotBounds();

        ChartTheme theme = getResolvedTheme(context);
        ensureThemeCache(theme);

        double margin = ChartScale.scale(40);
        double xBase = bounds.getX() + margin;
        double yBase = bounds.getY() + bounds.getHeight() - margin;
        double side = Math.min(bounds.getWidth(), bounds.getHeight()) - 2 * margin;
        if (side <= 1) return;

        double topX = xBase + side / 2.0;
        double topY = yBase - side * Math.sqrt(3) / 2.0;
        double leftX = xBase;
        double leftY = yBase;
        double rightX = xBase + side;
        double rightY = yBase;

        canvas.setColor(sepColor);
        drawTriangle(canvas, topX, topY, leftX, leftY, rightX, rightY);
        drawGrid(canvas, topX, topY, leftX, leftY, rightX, rightY);

        // Points
        int count = model.getPointCount();
        if (count > 0) {
            ArberColor baseColor = getSeriesColor(model);
            double r = ChartScale.scale(3.0);
            double d = r * 2.0;
            for (int i = 0; i < count; i++) {
                double a = model.getX(i);
                double b = model.getY(i);
                double c = model.getWeight(i);
                double total = a + b + c;
                if (total <= 0) continue;
                double inv = 1.0 / total;
                a *= inv;
                b *= inv;
                c *= inv;

                double px = Math.fma(a, topX, Math.fma(b, leftX, c * rightX));
                double py = Math.fma(a, topY, Math.fma(b, leftY, c * rightY));

                if (px < bounds.x() - d || px > bounds.maxX() + d || py < bounds.y() - d || py > bounds.maxY() + d)
                    continue;

                ArberColor pointColor = isMultiColor() ? theme.getSeriesColor(i) : baseColor;
                if (pointColor == null) pointColor = baseColor;
                canvas.setColor(pointColor);
                fillCircle(canvas, px, py, r);
            }
        }
    }

    public TernaryPhasediagramRenderer setMultiColor(boolean enabled){
        super.setMultiColor(enabled);
        return this;
        
    }

    private void ensureThemeCache(ChartTheme theme) {
        int k = System.identityHashCode(theme);
        if (k == themeKey && sepColor != null) return;
        themeKey = k;

        sepColor = theme.getGridColor();
        sepColor30 = ColorUtils.applyAlpha(sepColor, 0.3f);
    }

    private void drawGrid(ArberCanvas canvas, double topX, double topY, double leftX, double leftY, double rightX, double rightY) {
        canvas.setStroke(ChartScale.scale(1.0f));
        canvas.setColor(sepColor30);

        int steps = 10;
        for (int i = 1; i < steps; i++) {
            double f = (double) i / steps;

            double x1 = Math.fma(f, (leftX - topX), topX);
            double y1 = Math.fma(f, (leftY - topY), topY);
            double x2 = Math.fma(f, (rightX - topX), topX);
            double y2 = Math.fma(f, (rightY - topY), topY);
            drawLine(canvas, x1, y1, x2, y2);

            double x3 = Math.fma(f, (topX - leftX), leftX);
            double y3 = Math.fma(f, (topY - leftY), leftY);
            double x4 = Math.fma(f, (rightX - leftX), leftX);
            double y4 = Math.fma(f, (rightY - leftY), leftY);
            drawLine(canvas, x3, y3, x4, y4);

            double x5 = Math.fma(f, (topX - rightX), rightX);
            double y5 = Math.fma(f, (topY - rightY), rightY);
            double x6 = Math.fma(f, (leftX - rightX), rightX);
            double y6 = Math.fma(f, (leftY - rightY), rightY);
            drawLine(canvas, x5, y5, x6, y6);
        }
    }

    private void drawTriangle(ArberCanvas canvas, double topX, double topY, double leftX, double leftY, double rightX, double rightY) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "ternary.triX", 4);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ternary.triY", 4);
        xs[0] = (float) topX;
        ys[0] = (float) topY;
        xs[1] = (float) leftX;
        ys[1] = (float) leftY;
        xs[2] = (float) rightX;
        ys[2] = (float) rightY;
        xs[3] = xs[0];
        ys[3] = ys[0];
        canvas.drawPolyline(xs, ys, 4);
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "ternary.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ternary.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "ternary.pcX", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ternary.pcY", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

}
