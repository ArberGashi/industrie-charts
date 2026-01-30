package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

/**
 * ColumnRenderer - Vertical Column Chart
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class ColumnRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public ColumnRenderer() {
        super("column");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        final ArberColor baseColor = getSeriesColor(model);
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberRect bounds = context.getPlotBounds();
        final double columnWidth = Math.max(ChartScale.scale(2.0), (bounds.width() / (double) count) * 0.7);

        context.mapToPixel(0, 0.0, p0);
        final double baselineY = MathUtils.clamp(p0[1], bounds.minY(), bounds.maxY());

        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], p0);

            final double x = p0[0] - columnWidth / 2.0;
            final double columnHeight = Math.abs(p0[1] - baselineY);
            if (columnHeight < 1.0) continue;

            final double y = Math.min(p0[1], baselineY);

            if (x + columnWidth < bounds.minX() || x > bounds.maxX()) {
                continue;
            }

            canvas.setColor(baseColor);
            canvas.fillRect((float) x, (float) y, (float) columnWidth, (float) columnHeight);
            canvas.setStroke(ChartScale.scale(1.0f));
            canvas.setColor(baseColor);
            float[] lineX = { (float) x, (float) (x + columnWidth), (float) (x + columnWidth), (float) x, (float) x };
            float[] lineY = { (float) y, (float) y, (float) (y + columnHeight), (float) (y + columnHeight), (float) y };
            canvas.drawPolyline(lineX, lineY, 5);
        }
    }
}
