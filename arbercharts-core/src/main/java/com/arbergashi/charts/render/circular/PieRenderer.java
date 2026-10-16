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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
/**
 * <h2>Modern Pie Renderer</h2>
 * <p>
 * Draws grouped category sectors with measured external labels and hover effects.
 * Drawing buffers are reused; aggregation and label formatting may allocate.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Smart Labels:</b> Automatically places labels outside with leader lines to avoid clutter.</li>
 *     <li><b>Rich Labels:</b> Displays category name and percentage value.</li>
 *     <li><b>Hover Effect:</b> Segments brighten on mouse hover for better interactivity.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @since 2025-06-01
 *
 */
public class PieRenderer extends BaseRenderer {

    private static final double LABEL_OFFSET = 1.15;
    private static final double LABEL_THRESHOLD_ANGLE = 8.0;

    private final LinkedHashMap<String, Double> perLabel = new LinkedHashMap<>(64);
    private final LinkedHashMap<String, Integer> firstIndexPerLabel = new LinkedHashMap<>(64);

    private double[] hitStartDeg = new double[0];
    private double[] hitExtentDeg = new double[0];
    private int[] hitModelIndices = new int[0];
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
        hitN = 0;
        hitOuterR = 0;
        perLabel.clear();
        firstIndexPerLabel.clear();
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect plot = context.getPlotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        double total = aggregateData(model, perLabel);
        if (perLabel.isEmpty() || !(total > 0.0) || !Double.isFinite(total)) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        hitCx = plot.getCenterX();
        hitCy = plot.getCenterY();
        hitOuterR = diameter * 0.5;

        renderTheme = getResolvedTheme(context);
        drawSegments(canvas, total, diameter);
        drawLabels(canvas, plot, total);
    }

    private void drawLabels(ArberCanvas canvas, ArberRect plot, double total) {
        canvas.setFont(renderTheme.getBaseFont());
        canvas.setColor(renderTheme.getForeground());
        canvas.setStroke(1.0f);
        double lineHeight = canvas.getTextLineHeight();
        double ascent = canvas.getTextAscent();
        double minY = plot.y() + ascent + 2;
        double maxY = plot.maxY() - (lineHeight - ascent) - 2;
        double previousRight = minY - lineHeight;
        double previousLeft = maxY + lineHeight;
        int i = 0;
        for (Map.Entry<String, Double> entry : perLabel.entrySet()) {
            double extent = hitExtentDeg[i];
            double angle = Math.toRadians(hitStartDeg[i++] - extent * 0.5);
            if (extent < LABEL_THRESHOLD_ANGLE) continue;
            boolean right = Math.cos(angle) >= 0;
            double anchorX = hitCx + (right ? 1 : -1) * hitOuterR * LABEL_OFFSET;
            double available = right ? plot.maxX() - anchorX - 4 : anchorX - plot.x() - 4;
            if (available < canvas.measureTextWidth("…")) continue;
            String label = entry.getKey() + " " + Math.round(entry.getValue() / total * 1000.0) / 10.0 + "%";
            label = fitLabel(canvas, label, available);
            double baseline = hitCy - Math.sin(angle) * hitOuterR * LABEL_OFFSET + ascent * 0.35;
            baseline = Math.max(minY, Math.min(maxY, baseline));
            if (right) {
                baseline = Math.max(baseline, previousRight + lineHeight);
                if (baseline > maxY) continue;
                previousRight = baseline;
            } else {
                baseline = Math.min(baseline, previousLeft - lineHeight);
                if (baseline < minY) continue;
                previousLeft = baseline;
            }
            double width = canvas.measureTextWidth(label);
            double x = right ? anchorX : anchorX - width;
            float leaderY = (float) (baseline - ascent * 0.35);
            canvas.drawLine((float) (hitCx + Math.cos(angle) * hitOuterR),
                    (float) (hitCy - Math.sin(angle) * hitOuterR),
                    (float) (anchorX + (right ? -3 : 3)), leaderY);
            canvas.drawText((float) x, (float) baseline, label);
        }
    }

    protected static String fitLabel(ArberCanvas canvas, String text, double width) {
        if (canvas.measureTextWidth(text) <= width) return text;
        int end = text.length();
        while (end > 0) {
            end = text.offsetByCodePoints(end, -1);
            String shortened = text.substring(0, end) + "…";
            if (canvas.measureTextWidth(shortened) <= width) return shortened;
        }
        return "";
    }

    private void drawSegments(ArberCanvas canvas, double total, double diameter) {
        ensureHitCapacity(perLabel.size());
        hitN = 0;
        double startAngle = 90.0;
        int idx = 0;

        for (Map.Entry<String, Double> entry : perLabel.entrySet()) {
            double value = entry.getValue();
            if (value <= 0.0) continue;
            double angle = 360.0 * (value / total);
            if (angle <= 0.0) continue;

            ArberColor color = getSegmentColor(idx);
            if (idx == hoverIndex) color = com.arbergashi.charts.util.ColorRegistry.adjustBrightness(color, 1.1);
            canvas.setColor(color);
            fillArcSegment(canvas, hitCx, hitCy, hitOuterR, startAngle, angle);

            hitStartDeg[hitN] = startAngle;
            hitExtentDeg[hitN] = angle;
            hitModelIndices[hitN] = firstIndexPerLabel.get(entry.getKey());
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
            firstIndexPerLabel.putIfAbsent(label, i);
            total += w;
        }
        return total;
    }

    /**
     * Returns the first contributing model point in the category under the pointer.
     * Grouped categories retain their own internal hover index.
     *
     * @param pixel pointer position in canvas coordinates
     * @param model model used for the current frame
     * @param context current plot context
     * @return an original model index, or an empty result outside the chart
     */
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
            double extent = hitExtentDeg[i];
            double distance = ((hitStartDeg[i] - a) % 360.0 + 360.0) % 360.0;
            boolean hit = distance <= extent;
            if (hit) {
                setHoverIndex(i);
                return Optional.of(hitModelIndices[i]);
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
            hitModelIndices = new int[newCap];
        }
    }

    // Package-private for tests in the same package.
    ArberColor getSegmentColor(int idx) {
        ChartTheme theme = (renderTheme != null) ? renderTheme : getResolvedTheme(null);
        // Theme instances may change their palettes between frames.
        return theme.getSeriesColor(idx);
    }

    @Override
    public void clearHover() {
        super.clearHover();
        hoverIndex = -1;
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
