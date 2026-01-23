package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * <h1>RangeRenderer - Range Area Chart</h1>
 *
 * <p>Professional range renderer for displaying data with min/max boundaries.
 * Visualizes uncertainty, confidence intervals, or value ranges.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Range Area:</b> Filled area between min and max values</li>
 *   <li><b>Center Line:</b> Optional median/mean line</li>
 *   <li><b>Transparency:</b> Semi-transparent fill for overlapping ranges</li>
 *   <li><b>Smooth Edges:</b> Anti-aliased boundaries</li>
 * </ul>
 *
 * <h2>Data Mapping:</h2>
 * <pre>
 * ChartPoint fields:
 *   x       → X coordinate
 *   y       → Center value (mean/median)
 *   min     → Lower boundary
 *   max     → Upper boundary
 * </pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Temperature ranges (daily min/max)</li>
 *   <li>Stock price ranges (high/low/close)</li>
 *   <li>Forecast confidence intervals</li>
 *   <li>Sensor data with measurement uncertainty</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class RangeRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    /**
     * Reused paths to avoid allocations during rendering.
     */
    private final Path2D.Double upperPath = new Path2D.Double();
    private final Path2D.Double lowerPath = new Path2D.Double();
    private final Path2D.Double centerPath = new Path2D.Double();

    public RangeRenderer() {
        super("range");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        final Color baseColor = getSeriesColor(model);
        final Color rangeColor = ColorUtils.withAlpha(baseColor, 0.3f);

        final Rectangle viewBounds = g.getClipBounds() != null ? g.getClipBounds() : context.plotBounds().getBounds();

        // 1) Range fill path (upper forward + lower reverse)
        Path2D rangePath = getPathCache();
        boolean started = false;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] minData = model.getLowData();
        double[] maxData = model.getHighData();

        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], maxData[i], p0);
            if (!started) {
                rangePath.moveTo(p0[0], p0[1]);
                started = true;
            } else {
                rangePath.lineTo(p0[0], p0[1]);
            }
        }

        for (int i = count - 1; i >= 0; i--) {
            context.mapToPixel(xData[i], minData[i], p0);
            rangePath.lineTo(p0[0], p0[1]);
        }

        rangePath.closePath();

        // Only draw if it intersects the clip (cheap reject)
        if (rangePath.getBounds2D().intersects(viewBounds)) {
            g.setColor(rangeColor);
            g.fill(rangePath);
        }

        // 2) Upper boundary (reused)
        upperPath.reset();
        started = false;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], maxData[i], p0);
            if (!started) {
                upperPath.moveTo(p0[0], p0[1]);
                started = true;
            } else {
                upperPath.lineTo(p0[0], p0[1]);
            }
        }

        // 3) Lower boundary (reused)
        lowerPath.reset();
        started = false;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], minData[i], p0);
            if (!started) {
                lowerPath.moveTo(p0[0], p0[1]);
                started = true;
            } else {
                lowerPath.lineTo(p0[0], p0[1]);
            }
        }

        g.setColor(ColorUtils.withAlpha(baseColor, 0.6f));
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        if (upperPath.getBounds2D().intersects(viewBounds)) g.draw(upperPath);
        if (lowerPath.getBounds2D().intersects(viewBounds)) g.draw(lowerPath);

        // 4) Center line (reused)
        centerPath.reset();
        started = false;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            if (!started) {
                centerPath.moveTo(p0[0], p0[1]);
                started = true;
            } else {
                centerPath.lineTo(p0[0], p0[1]);
            }
        }

        g.setColor(baseColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.0f)));
        if (centerPath.getBounds2D().intersects(viewBounds)) g.draw(centerPath);
    }
}
