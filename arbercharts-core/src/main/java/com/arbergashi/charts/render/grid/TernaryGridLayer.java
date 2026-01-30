package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;


/**
 * Ternary grid layer.
 *
 * <p>Renders a barycentric triangle with interior grid lines (a+b+c=1).
 * Suitable for ternary plots and compositional charts.</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class TernaryGridLayer extends DefaultGridLayer {
    private static final String KEY_MINOR_ALPHA = "Chart.ternaryGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.ternaryGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.ternaryGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.ternaryGrid.majorStrokeWidth";
    private static final String KEY_DIVISIONS = "Chart.ternaryGrid.divisions";
    private static final String KEY_MAJOR_EVERY = "Chart.ternaryGrid.majorEvery";

    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        if (context == null) return;
        ArberRect bounds = context.getPlotBounds();
        Triangle tri = computeTriangle(bounds);

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();

        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));
        int divisions = Math.max(4, ChartAssets.getInt(KEY_DIVISIONS, 10));
        int majorEvery = Math.max(2, ChartAssets.getInt(KEY_MAJOR_EVERY, 5));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        // Outer triangle
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStroke));
        drawTriangle(canvas, tri);

        for (int i = 1; i < divisions; i++) {
            double t = (double) i / divisions;
            boolean isMajor = (i % majorEvery == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));

            // Lines parallel to BC (constant A)
            Point p1 = lerp(tri.a, tri.b, t);
            Point p2 = lerp(tri.a, tri.c, t);
            drawLine(canvas, p1.x, p1.y, p2.x, p2.y);

            // Lines parallel to AC (constant B)
            p1 = lerp(tri.b, tri.a, t);
            p2 = lerp(tri.b, tri.c, t);
            drawLine(canvas, p1.x, p1.y, p2.x, p2.y);

            // Lines parallel to AB (constant C)
            p1 = lerp(tri.c, tri.a, t);
            p2 = lerp(tri.c, tri.b, t);
            drawLine(canvas, p1.x, p1.y, p2.x, p2.y);
        }
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        GridBatchConfig effective = (config != null) ? config : getGridBatchConfig();
        builder.setZMin(effective.getZMin())
                .setClippingMode(effective.getClippingMode());

        ArberRect bounds = context.getPlotBounds();
        Triangle tri = computeTriangle(bounds);

        int divisions = Math.max(4, ChartAssets.getInt(KEY_DIVISIONS, 10));
        int majorEvery = Math.max(2, ChartAssets.getInt(KEY_MAJOR_EVERY, 5));

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        // Outer triangle
        builder.setStyleKey(SpatialStyleDescriptor.pack(majorColor.argb(), ChartScale.scale(majorStroke), 0, 0));
        builder.setLineSegment(tri.a.x, tri.a.y, 1.0, tri.b.x, tri.b.y, 1.0);
        builder.setLineSegment(tri.b.x, tri.b.y, 1.0, tri.c.x, tri.c.y, 1.0);
        builder.setLineSegment(tri.c.x, tri.c.y, 1.0, tri.a.x, tri.a.y, 1.0);

        for (int i = 1; i < divisions; i++) {
            double t = (double) i / divisions;
            boolean isMajor = (i % majorEvery == 0);
            ArberColor color = isMajor ? majorColor : minorColor;
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
            // Lines parallel to BC (constant A)
            Point p1 = lerp(tri.a, tri.b, t);
            Point p2 = lerp(tri.a, tri.c, t);
            builder.setLineSegment(p1.x, p1.y, 1.0, p2.x, p2.y, 1.0);

            // Lines parallel to AC (constant B)
            p1 = lerp(tri.b, tri.a, t);
            p2 = lerp(tri.b, tri.c, t);
            builder.setLineSegment(p1.x, p1.y, 1.0, p2.x, p2.y, 1.0);

            // Lines parallel to AB (constant C)
            p1 = lerp(tri.c, tri.a, t);
            p2 = lerp(tri.c, tri.b, t);
            builder.setLineSegment(p1.x, p1.y, 1.0, p2.x, p2.y, 1.0);
        }
    }

    private static Triangle computeTriangle(ArberRect bounds) {
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();
        double w = bounds.width();
        double h = bounds.height();

        double padding = Math.min(w, h) * 0.06;
        double baseY = maxY - padding;
        double apexY = minY + padding;
        double centerX = (minX + maxX) * 0.5;

        double usableH = baseY - apexY;
        double side = Math.min(w - 2 * padding, usableH * 2.0 / Math.sqrt(3.0));
        double halfSide = side * 0.5;

        Point a = new Point(centerX, apexY);
        Point b = new Point(centerX - halfSide, baseY);
        Point c = new Point(centerX + halfSide, baseY);
        return new Triangle(a, b, c);
    }

    private void drawTriangle(ArberCanvas canvas, Triangle tri) {
        drawLine(canvas, tri.a.x, tri.a.y, tri.b.x, tri.b.y);
        drawLine(canvas, tri.b.x, tri.b.y, tri.c.x, tri.c.y);
        drawLine(canvas, tri.c.x, tri.c.y, tri.a.x, tri.a.y);
    }

    private static Point lerp(Point a, Point b, double t) {
        return new Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    private static final class Triangle {
        private final Point a;
        private final Point b;
        private final Point c;

        private Triangle(Point a, Point b, Point c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private static final class Point {
        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
