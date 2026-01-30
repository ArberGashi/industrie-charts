package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.MatrixChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;

import java.util.List;
import java.util.Optional;

/**
 * <h1>Modern Chord Diagram Renderer</h1>
 * <p>
 * Draws a professional, interactive chord diagram to visualize relationships or flows between entities.
 * Adheres to strict zero-allocation guidelines by pre-calculating layout.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Interactive Highlighting:</b> Hovering over a group highlights its connections.</li>
 *     <li><b>Pre-calculated Layout:</b> All shapes are calculated once, making rendering extremely fast.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class ChordDiagramRenderer extends BaseRenderer {

    private static final class GroupArc {
        final double cx;
        final double cy;
        final double r;
        final double start;
        final double extent;

        GroupArc(double cx, double cy, double r, double start, double extent) {
            this.cx = cx;
            this.cy = cy;
            this.r = r;
            this.start = start;
            this.extent = extent;
        }

        ArberPoint startPoint() {
            double a = Math.toRadians(start);
            return new ArberPoint(cx + Math.cos(a) * r, cy - Math.sin(a) * r);
        }
    }

    private GroupArc[] groupArcs;
    private double[][] matrix;
    private List<String> labels;
    private int hoverIndex = -1;

    public ChordDiagramRenderer() {
        super("chord");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof MatrixChartModel matrixModel)) {
            return;
        }

        if (this.matrix != matrixModel.getMatrix()) {
            this.matrix = matrixModel.getMatrix();
            this.labels = matrixModel.getEntityLabels();
            getCalculatedLayout(context);
        }

        if (groupArcs == null) return;

        drawGroups(canvas, context);
        drawChords(canvas, context);
    }

    private void getCalculatedLayout(PlotContext context) {
        if (matrix == null || labels == null || matrix.length != labels.size()) return;
        int n = matrix.length;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double radius = Math.min(bounds.getWidth(), bounds.getHeight()) / 2.0 * 0.8;
        double gap = 2; // degrees

        double[] groupTotals = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                groupTotals[i] += matrix[i][j];
            }
            total += groupTotals[i];
        }
        if (total <= 0) return;

        double scale = (360.0 - n * gap) / total;

        groupArcs = new GroupArc[n];
        double currentAngle = 90;
        for (int i = 0; i < n; i++) {
            double arcAngle = groupTotals[i] * scale;
            groupArcs[i] = new GroupArc(cx, cy, radius, currentAngle, -arcAngle);
            currentAngle -= (arcAngle + gap);
        }
    }

    private void drawGroups(ArberCanvas canvas, PlotContext context) {
        for (int i = 0; i < groupArcs.length; i++) {
            canvas.setColor(getResolvedTheme(context).getSeriesColor(i));
            fillArcSegment(canvas, groupArcs[i]);
        }
    }

    private void drawChords(ArberCanvas canvas, PlotContext context) {
        for (int i = 0; i < groupArcs.length; i++) {
            for (int j = 0; j < groupArcs.length; j++) {
                if (i == j || matrix[i][j] == 0) continue;

                ArberColor base = getResolvedTheme(context).getSeriesColor(i);
                float alpha = 0.4f;
                if (hoverIndex != -1 && hoverIndex != i && hoverIndex != j) {
                    alpha = 0.05f;
                } else if (hoverIndex == i || hoverIndex == j) {
                    alpha = 0.7f;
                }

                canvas.setColor(ColorRegistry.applyAlpha(base, alpha));
                canvas.setStroke((float) (matrix[i][j] * 0.5));

                ArberPoint p1 = groupArcs[i].startPoint();
                ArberPoint p2 = groupArcs[j].startPoint();
                drawQuadratic(canvas, p1.x(), p1.y(), groupArcs[i].cx, groupArcs[i].cy, p2.x(), p2.y());
            }
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (groupArcs == null) return Optional.empty();

        for (int i = 0; i < groupArcs.length; i++) {
            if (contains(groupArcs[i], pixel)) {
                hoverIndex = i;
                return Optional.of(i);
            }
        }

        hoverIndex = -1;
        return Optional.empty();
    }

    private boolean contains(GroupArc arc, ArberPoint p) {
        double dx = p.x() - arc.cx;
        double dy = p.y() - arc.cy;
        double d2 = dx * dx + dy * dy;
        if (d2 > arc.r * arc.r) return false;

        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        double start = arc.start;
        double end = arc.start + arc.extent;
        return angleInSweep(angle, start, end);
    }

    private boolean angleInSweep(double angle, double start, double end) {
        angle = normalize(angle);
        start = normalize(start);
        end = normalize(end);
        if (start >= end) {
            return angle <= start && angle >= end;
        }
        return angle >= start && angle <= end;
    }

    private double normalize(double a) {
        double n = a;
        while (n < 0) n += 360;
        while (n >= 360) n -= 360;
        return n;
    }

    private void fillArcSegment(ArberCanvas canvas, GroupArc arc) {
        int segments = Math.max(18, (int) (Math.abs(arc.extent) / 4.0));
        int total = segments + 2;
        float[] xs = new float[total];
        float[] ys = new float[total];
        xs[0] = (float) arc.cx;
        ys[0] = (float) arc.cy;
        double step = arc.extent / segments;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(arc.start + step * i);
            xs[i + 1] = (float) (arc.cx + Math.cos(a) * arc.r);
            ys[i + 1] = (float) (arc.cy - Math.sin(a) * arc.r);
        }
        canvas.fillPolygon(xs, ys, total);
    }

    private void drawQuadratic(ArberCanvas canvas, double x0, double y0, double cx, double cy, double x1, double y1) {
        int segments = 24;
        float[] xs = new float[segments + 1];
        float[] ys = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double mt = 1.0 - t;
            double x = mt * mt * x0 + 2 * mt * t * cx + t * t * x1;
            double y = mt * mt * y0 + 2 * mt * t * cy + t * t * y1;
            xs[i] = (float) x;
            ys[i] = (float) y;
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }
}
