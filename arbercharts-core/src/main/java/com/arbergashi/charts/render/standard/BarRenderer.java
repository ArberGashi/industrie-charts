package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;

/**
 * Professional, zero-allocation, high-precision bar chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class BarRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BarRenderer() {
        super("bar");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n == 0) return;

        ArberColor seriesColor = getSeriesColor(model);
        ArberRect bounds = context.getPlotBounds();
        double barWidth = Math.max(2.0, bounds.width() / Math.max(1, n));

        context.mapToPixel(0, 0.0, p0);
        double baselineY = MathUtils.clamp(p0[1], bounds.minY(), bounds.maxY());

        for (int i = 0; i < n; i++) {
            final double x = xData[i];
            final double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;

            double bx = p0[0] - barWidth / 2;
            if (bx + barWidth < bounds.minX() || bx > bounds.maxX()) {
                continue;
            }

            double by = Math.min(p0[1], baselineY);
            double height = Math.abs(p0[1] - baselineY);
            if (height < 1.0) height = 1.0;

            canvas.setColor(seriesColor);
            canvas.fillRect((float) bx, (float) by, (float) barWidth, (float) height);

            if (ChartAssets.getBoolean("Chart.bar.outline", false)) {
                canvas.setStroke(ChartScale.scale(1.0f));
                canvas.setColor(ColorRegistry.adjustBrightness(seriesColor, 0.8));
                float[] lineX = { (float) bx, (float) (bx + barWidth), (float) (bx + barWidth), (float) bx, (float) bx };
                float[] lineY = { (float) by, (float) by, (float) (by + height), (float) (by + height), (float) by };
                canvas.drawPolyline(lineX, lineY, 5);
            }
        }
    }
}
