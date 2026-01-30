package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;
import java.util.Optional;
/**
 * <h1>Modern Polar Area Renderer</h1>
 * <p>
 * Draws a professional, interactive polar area chart (also known as a rose chart).
 * It's similar to a pie chart, but sectors have equal angles and differ in radius.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Concentric Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Data Points:</b> Renders data as distinct points on the polar grid.</li>
 *     <li><b>Hover Effect:</b> Points are highlighted on mouse hover.</li>
 *     <li><b>Color Mapping:</b> Point color can be mapped to a third dimension (weight).</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PolarRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private PointShape pointShape = PointShape.CIRCLE;
    private double pointSize = 8.0;
    private static final int CIRCLE_SEGMENTS = 48;

    /**
     * Supported marker shapes for polar points.
     */
    public enum PointShape {
        /** Circular marker. */
        CIRCLE,
        /** Square marker. */
        SQUARE,
        /** Triangular marker. */
        TRIANGLE
    }

    public PolarRenderer() {
        super("polar");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        drawPolarGrid(canvas, context);
        drawPoints(canvas, model, context);
    }

    private void drawPolarGrid(ArberCanvas canvas, PlotContext context) {
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        canvas.setColor(getResolvedTheme(context).getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));

        // Concentric circles
        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            drawCirclePolyline(canvas, cx, cy, r);
        }

        // Radial lines
        int radialLines = 8;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polar.grid.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polar.grid.line.y", 2);
        for (int i = 0; i < radialLines; i++) {
            double angleRad = Math.toRadians(i * (360.0 / radialLines));
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            xs[0] = (float) cx;
            ys[0] = (float) cy;
            xs[1] = (float) x;
            ys[1] = (float) y;
            canvas.drawPolyline(xs, ys, 2);
        }
    }

    private void drawPoints(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            double weight = model.getWeight(i);

            double angleRad = Math.toRadians(angleDeg - 90); // Start from top
            double radius = maxRadius * (radiusValue / context.getMaxY());

            double x = cx + Math.cos(angleRad) * radius;
            double y = cy + Math.sin(angleRad) * radius;

            // Color can be based on series or mapped to weight
            ArberColor color = (weight > 0) ? mapWeightToColor(weight, context, model) : getSeriesColor(model);
            if (i == hoverIndex) {
                // no brighten in core; reuse same color
            }
            canvas.setColor(color);

            double size = ChartScale.scale(pointSize);
            if (i == hoverIndex) {
                size *= 1.5;
            }

            drawShape(canvas, x, y, size, pointShape);
        }
    }

    private void drawShape(ArberCanvas canvas, double x, double y, double size, PointShape shape) {
        switch (shape) {
            case SQUARE:
                canvas.fillRect((float) (x - size / 2), (float) (y - size / 2), (float) size, (float) size);
                break;
            case TRIANGLE:
                float[] tx = RendererAllocationCache.getFloatArray(this, "polar.tri.x", 3);
                float[] ty = RendererAllocationCache.getFloatArray(this, "polar.tri.y", 3);
                tx[0] = (float) x;
                ty[0] = (float) (y - size / 2);
                tx[1] = (float) (x - size / 2);
                ty[1] = (float) (y + size / 2);
                tx[2] = (float) (x + size / 2);
                ty[2] = (float) (y + size / 2);
                canvas.fillPolygon(tx, ty, 3);
                break;
            case CIRCLE:
            default:
                drawCircleFill(canvas, x, y, size / 2.0);
                break;
        }
    }

    private ArberColor mapWeightToColor(double weight, PlotContext context, ChartModel model) {
        // Linear interpolation between theme palette colors.
        float t = (float) MathUtils.clamp(weight, 0, 1);
        ArberColor c0 = themeSeries(context, 0);
        ArberColor c1 = themeSeries(context, 1);
        if (c0 == null) c0 = getSeriesColor(model);
        if (c1 == null) c1 = c0;
        return interpolate(c0, c1, t);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;
        double thresholdSq = Math.pow(ChartScale.scale(pointSize), 2);

        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            double angleRad = Math.toRadians(angleDeg - 90);
            double radius = maxRadius * (radiusValue / context.getMaxY());

            double x = cx + Math.cos(angleRad) * radius;
            double y = cy + Math.sin(angleRad) * radius;

            if (pixel.distanceSq(x, y) < thresholdSq) {
                hoverIndex = i;
                return Optional.of(i);
            }
        }

        hoverIndex = -1;
        return Optional.empty();
    }

    private void drawCirclePolyline(ArberCanvas canvas, double cx, double cy, double r) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "polar.circle.x", CIRCLE_SEGMENTS + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polar.circle.y", CIRCLE_SEGMENTS + 1);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (i * 2.0 * Math.PI) / CIRCLE_SEGMENTS;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, CIRCLE_SEGMENTS + 1);
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "polar.circle.fill.x", CIRCLE_SEGMENTS);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polar.circle.fill.y", CIRCLE_SEGMENTS);
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a = (i * 2.0 * Math.PI) / CIRCLE_SEGMENTS;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, CIRCLE_SEGMENTS);
    }

    private ArberColor interpolate(ArberColor a, ArberColor b, float t) {
        int ac = a.argb();
        int bc = b.argb();
        int ar = (ac >> 16) & 0xFF;
        int ag = (ac >> 8) & 0xFF;
        int ab = ac & 0xFF;
        int aa = (ac >>> 24) & 0xFF;
        int br = (bc >> 16) & 0xFF;
        int bg = (bc >> 8) & 0xFF;
        int bb = bc & 0xFF;
        int ba = (bc >>> 24) & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        int ra = (int) (aa + (ba - aa) * t);
        return ArberColor.of(rr, rg, rb, ra);
    }


    // --- Public API ---

    public PolarRenderer setPointSize(double size){
        this.pointSize = size;
        return this;
        
    }

    public PolarRenderer setPointShape(PointShape shape){
        this.pointShape = shape;
        return this;
        
    }
}
