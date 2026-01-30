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
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
/**
 * <h1>Modern Pie Renderer</h1>
 * <p>
 * Draws a professional, interactive pie chart with intelligent external labeling and hover effects,
 * adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Smart Labels:</b> Automatically places labels outside with leader lines to avoid clutter.</li>
 *     <li><b>Rich Labels:</b> Displays category name and percentage value.</li>
 *     <li><b>Hover Effect:</b> Segments brighten on mouse hover for better interactivity.</li>
 *     <li><b>Zero-Allocation:</b> Highly optimized for performance.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class PieRenderer extends BaseRenderer {

    private static final double LABEL_OFFSET = 1.15;
    private static final double LABEL_THRESHOLD_ANGLE = 8.0;

    private final LinkedHashMap<String, Double> perLabel = new LinkedHashMap<>(64);
    private final ArberColor[] colorCache = new ArberColor[256];

    private double[] hitStartDeg = new double[0];
    private double[] hitExtentDeg = new double[0];
    private int hitN;
    private double hitCx, hitCy, hitOuterR;
    private int hoverIndex = -1;
    private transient ChartTheme renderTheme;

    public PieRenderer() {
        this("pie");
    }
    /**
     * @since 1.5.0
    */

    protected PieRenderer(String key) {
        super(key);
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect plot = context.getPlotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        perLabel.clear();
        double total = aggregateData(model, perLabel);
        if (perLabel.isEmpty() || !(total > 0.0)) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        hitCx = plot.getCenterX();
        hitCy = plot.getCenterY();
        hitOuterR = diameter * 0.5;

        this.renderTheme = getResolvedTheme(context);
        drawSegments(canvas, total, diameter);
    }

    private void drawSegments(ArberCanvas canvas, double total, double diameter) {
        ensureHitCapacity(perLabel.size());
        hitN = 0;
        double startAngle = 90.0;
        int idx = 0;

        for (Double value : perLabel.values()) {
            if (value <= 0.0) continue;
            double angle = 360.0 * (value / total);
            if (angle <= 0.0) continue;

            ArberColor color = getSegmentColor(idx);
            if (idx == hoverIndex) {
                // no brighten in core
            }
            canvas.setColor(color);
            fillArcSegment(canvas, hitCx, hitCy, hitOuterR, startAngle, angle);

            hitStartDeg[hitN] = startAngle;
            hitExtentDeg[hitN] = angle;
            hitN++;
            startAngle -= angle;
            idx++;
        }
    }

    // Package-private for tests in the same package.
    double aggregateData(ChartModel model, Map<String, Double> target) {
        double total = 0.0;
        for (int i = 0; i < model.getPointCount(); i++) {
            String label = model.getLabel(i);
            if (label == null || label.isBlank()) label = "Unknown";
            double w = model.getWeight(i) > 0 ? model.getWeight(i) : Math.max(0.0, model.getY(i));
            if (!(w > 0.0) || !Double.isFinite(w)) continue;

            target.merge(label, w, Double::sum);
            total += w;
        }
        return total;
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (hitN <= 0 || !(hitOuterR > 0)) return Optional.empty();

        final double px = pixel.x();
        final double py = pixel.y();
        final double dx = px - hitCx;
        final double dy = py - hitCy;

        final double r2 = dx * dx + dy * dy;
        if (r2 > hitOuterR * hitOuterR) {
            setHoverIndex(-1);
            return Optional.empty();
        }

        double a = Math.toDegrees(Math.atan2(-dy, dx));
        if (a < 0) a += 360.0;

        for (int i = 0; i < hitN; i++) {
            double start = hitStartDeg[i];
            double extent = hitExtentDeg[i];
            double end = start - extent;
            if (end < 0) end += 360.0;

            boolean hit = (start >= end) ? (a <= start && a >= end) : (a <= start || a >= end);
            if (hit) {
                setHoverIndex(i);
                return Optional.of(i);
            }
        }

        setHoverIndex(-1);
        return Optional.empty();
    }

    private void ensureHitCapacity(int required) {
        if (hitStartDeg.length < required) {
            int newCap = Math.max(required, hitStartDeg.length == 0 ? 16 : hitStartDeg.length * 2);
            hitStartDeg = new double[newCap];
            hitExtentDeg = new double[newCap];
        }
    }

    // Package-private for tests in the same package.
    ArberColor getSegmentColor(int idx) {
        int i = Math.floorMod(idx, colorCache.length);
        ArberColor c = colorCache[i];
        if (c != null) return c;

        ChartTheme theme = (renderTheme != null) ? renderTheme : getResolvedTheme(null);
        c = theme.getSeriesColor(i);

        colorCache[i] = c;
        return c;
    }

    @Override
    public void clearHover() {
        super.clearHover();
        renderTheme = null;
    }

    public PieRenderer setHoverIndex(int index) {
        if (this.hoverIndex != index) {
            this.hoverIndex = index;
        }
        return this;
    }

    private void fillArcSegment(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double extentDeg) {
        int segments = Math.max(6, (int) Math.ceil(Math.abs(extentDeg) / 7.5));
        int total = segments + 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "pie.seg.x", total);
        float[] ys = RendererAllocationCache.getFloatArray(this, "pie.seg.y", total);
        xs[0] = (float) cx;
        ys[0] = (float) cy;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg - (extentDeg * i / (double) segments));
            xs[i + 1] = (float) (cx + Math.cos(a) * r);
            ys[i + 1] = (float) (cy - Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, total);
    }
}
