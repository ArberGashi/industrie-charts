package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Vector Field renderer.
 * Draws a vector field (e.g. gravity/electromagnetism) as a grid of arrows.
 *
 * <p><b>Performance</b>: the draw path is allocation-free (no Point2D/Path objects per arrow).
 * The grid resolution can be adjusted for performance vs. detail.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class VectorFieldRenderer extends BaseRenderer {

    private final VectorFieldFunction vectorField;
    // Allocation-free mapping buffers.
    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];
    private final double[] vecBuffer = new double[2];
    // Reusable arrow head path.
    private final Path2D head = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
    private int gridResolution = 20;

    public VectorFieldRenderer(VectorFieldFunction vectorField) {
        super("vectorfield");
        this.vectorField = vectorField;
    }

    public void setGridResolution(int resolution) {
        this.gridResolution = Math.max(5, resolution);
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        Rectangle clip = g2.getClipBounds();

        double minX = context.minX();
        double maxX = context.maxX();
        double minY = context.minY();
        double maxY = context.maxY();

        double stepX = (maxX - minX) / gridResolution;
        double stepY = (maxY - minY) / gridResolution;

        Color base = seriesOrBase(model, context, 0);
        g2.setColor(base);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.0f)));

        // Arrow geometry (pixels)
        double arrowSize = ChartScale.scale(5.0);

        for (int i = 0; i <= gridResolution; i++) {
            double x = minX + i * stepX;
            for (int j = 0; j <= gridResolution; j++) {
                double y = minY + j * stepY;

                if (!vectorField.compute(x, y, vecBuffer)) continue;
                double dx = vecBuffer[0];
                double dy = vecBuffer[1];

                // Normalize direction (fast reject for near-zero vectors)
                double mag2 = dx * dx + dy * dy;
                if (mag2 < 1e-12) continue;
                double invMag = 1.0 / Math.sqrt(mag2);
                double ux = dx * invMag;
                double uy = dy * invMag;

                // Arrow length in data units (scaled to grid cell size)
                double targetX = x + ux * (context.rangeX() / gridResolution) * 0.8;
                double targetY = y + uy * (context.rangeY() / gridResolution) * 0.8;

                context.mapToPixel(x, y, p0);
                context.mapToPixel(targetX, targetY, p1);

                double sx = p0[0], sy = p0[1];
                double ex = p1[0], ey = p1[1];

                if (clip != null) {
                    // Cheap clip test: if both ends are outside on the same side, skip.
                    if ((sx < clip.x && ex < clip.x) || (sx > clip.x + clip.width && ex > clip.x + clip.width)
                            || (sy < clip.y && ey < clip.y) || (sy > clip.y + clip.height && ey > clip.y + clip.height)) {
                        continue;
                    }
                }

                if (isMultiColor()) {
                    int idx = i * (gridResolution + 1) + j;
                    Color cell = themeSeries(context, idx);
                    if (cell == null) cell = base;
                    g2.setColor(cell);
                }

                // Main line (int cast is OK for polyline speed)
                g2.drawLine((int) sx, (int) sy, (int) ex, (int) ey);

                // Arrow head without trig: build from normalized screen direction
                double sdx = ex - sx;
                double sdy = ey - sy;
                double sm2 = sdx * sdx + sdy * sdy;
                if (sm2 < 1e-6) continue;
                double sinv = 1.0 / Math.sqrt(sm2);
                double nx = sdx * sinv;
                double ny = sdy * sinv;

                // perpendicular
                double px = -ny;
                double py = nx;

                // Two wing points: end - n*arrowSize +/- p*(arrowSize*0.6)
                double backX = ex - nx * arrowSize;
                double backY = ey - ny * arrowSize;
                double wing = arrowSize * 0.60;

                double x1 = backX + px * wing;
                double y1 = backY + py * wing;
                double x2 = backX - px * wing;
                double y2 = backY - py * wing;

                head.reset();
                head.moveTo(ex, ey);
                head.lineTo(x1, y1);
                head.lineTo(x2, y2);
                head.closePath();
                g2.fill(head);
            }
        }
    }

    /**
     * Primitive interface for vector fields to avoid boxing/allocations.
     */
    public interface VectorFieldFunction {
        /**
         * Computes the vector at position (x, y).
         *
         * @param x   X coordinate in data space.
         * @param y   Y coordinate in data space.
         * @param out Array of length 2 with the result [dx, dy].
         * @return true if a vector exists, otherwise false.
         */
        boolean compute(double x, double y, double[] out);
    }
}
