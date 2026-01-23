package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
 */
public final class PolarRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private PointShape pointShape = PointShape.CIRCLE;
    private double pointSize = 8.0;

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

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        drawPolarGrid(g, context);
        drawPoints(g, model, context);
    }

    private void drawPolarGrid(Graphics2D g, PlotContext context) {
        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.85;

        g.setColor(resolveTheme(context).getGridColor());
        g.setStroke(new BasicStroke(ChartScale.scale(1.0f)));

        // Concentric circles
        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            g.draw(getEllipse(cx - r, cy - r, r * 2, r * 2));
        }

        // Radial lines
        int radialLines = 8;
        for (int i = 0; i < radialLines; i++) {
            double angleRad = Math.toRadians(i * (360.0 / radialLines));
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            g.draw(getLine(cx, cy, x, y));
        }
    }

    private void drawPoints(Graphics2D g, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        double cx = context.plotBounds().getCenterX();
        double cy = context.plotBounds().getCenterY();
        double maxRadius = Math.min(context.plotBounds().getWidth(), context.plotBounds().getHeight()) / 2.0 * 0.85;

        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            double weight = model.getWeight(i);

            double angleRad = Math.toRadians(angleDeg - 90); // Start from top
            double radius = maxRadius * (radiusValue / context.maxY());

            double x = cx + Math.cos(angleRad) * radius;
            double y = cy + Math.sin(angleRad) * radius;

            // Color can be based on series or mapped to weight
            Color color = (weight > 0) ? mapWeightToColor(weight, context) : getSeriesColor(model);
            if (i == hoverIndex) {
                color = color.brighter();
            }
            g.setColor(color);

            double size = ChartScale.scale(pointSize);
            if (i == hoverIndex) {
                size *= 1.5;
            }

            drawShape(g, x, y, size, pointShape);
        }
    }

    private void drawShape(Graphics2D g, double x, double y, double size, PointShape shape) {
        switch (shape) {
            case SQUARE:
                g.fill(new Rectangle2D.Double(x - size / 2, y - size / 2, size, size));
                break;
            case TRIANGLE:
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(x, y - size / 2);
                triangle.lineTo(x - size / 2, y + size / 2);
                triangle.lineTo(x + size / 2, y + size / 2);
                triangle.closePath();
                g.fill(triangle);
                break;
            case CIRCLE:
            default:
                g.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
                break;
        }
    }

    private Color mapWeightToColor(double weight, PlotContext context) {
        // Linear interpolation between theme palette colors.
        float t = (float) MathUtils.clamp(weight, 0, 1);
        Color c0 = themeSeries(context, 0);
        Color c1 = themeSeries(context, 1);
        return ColorUtils.interpolate(c0, c1, t);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        double cx = context.plotBounds().getCenterX();
        double cy = context.plotBounds().getCenterY();
        double maxRadius = Math.min(context.plotBounds().getWidth(), context.plotBounds().getHeight()) / 2.0 * 0.85;
        double thresholdSq = Math.pow(ChartScale.scale(pointSize), 2);

        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            double angleRad = Math.toRadians(angleDeg - 90);
            double radius = maxRadius * (radiusValue / context.maxY());

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


    // --- Public API ---

    public PolarRenderer setPointSize(double size) {
        this.pointSize = size;
        return this;
    }

    public PolarRenderer setPointShape(PointShape shape) {
        this.pointShape = shape;
        return this;
    }
}
