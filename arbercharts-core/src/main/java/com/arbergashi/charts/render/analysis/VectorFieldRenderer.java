package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * Vector Field renderer.
 * Draws a vector field (e.g. gravity/electromagnetism) as a grid of arrows.
 *
 * <p><b>Performance</b>: the draw path is allocation-free (no ArberPoint/Path objects per arrow).
 * The grid resolution can be adjusted for performance vs. detail.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class VectorFieldRenderer extends BaseRenderer {

    private final VectorFieldFunction vectorField;
    // Allocation-free mapping buffers.
    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];
    private final double[] vecBuffer = new double[2];
    private int gridResolution = 20;

    public VectorFieldRenderer(VectorFieldFunction vectorField) {
        super("vectorfield");
        this.vectorField = vectorField;
    }

    public VectorFieldRenderer setGridResolution(int resolution) {
        this.gridResolution = Math.max(5, resolution);
        return this;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double minY = context.getMinY();
        double maxY = context.getMaxY();

        double stepX = (maxX - minX) / gridResolution;
        double stepY = (maxY - minY) / gridResolution;

        ArberColor base = seriesOrBase(model, context, 0);
        canvas.setColor(base);
        canvas.setStroke(ChartScale.scale(1.0f));

        // Arrow geometry (pixels)
        double arrowSize = ChartScale.scale(5.0);
        float[] lineX = RendererAllocationCache.getFloatArray(this, "vec.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "vec.line.y", 2);
        float[] headX = RendererAllocationCache.getFloatArray(this, "vec.head.x", 3);
        float[] headY = RendererAllocationCache.getFloatArray(this, "vec.head.y", 3);

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

                if (isMultiColor()) {
                    int idx = i * (gridResolution + 1) + j;
                    ArberColor cell = themeSeries(context, idx);
                    if (cell == null) cell = base;
                    canvas.setColor(cell);
                }

                // Main line (int cast is OK for polyline speed)
                lineX[0] = (float) sx;
                lineY[0] = (float) sy;
                lineX[1] = (float) ex;
                lineY[1] = (float) ey;
                canvas.drawPolyline(lineX, lineY, 2);

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

                headX[0] = (float) ex;
                headY[0] = (float) ey;
                headX[1] = (float) x1;
                headY[1] = (float) y1;
                headX[2] = (float) x2;
                headY[2] = (float) y2;
                canvas.fillPolygon(headX, headY, 3);
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
