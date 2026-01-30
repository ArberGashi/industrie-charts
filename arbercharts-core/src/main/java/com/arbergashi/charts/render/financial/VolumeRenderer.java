package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.ChartAssets;
/**
 * Professional, zero-allocation volume bar renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class VolumeRenderer extends BaseRenderer {

    private final double[] pxTop = new double[2];
    private final double[] pxBase = new double[2];

    public VolumeRenderer() {
        super("volume");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ArberRect plot = context.getPlotBounds();
        final double leftX = plot.x();
        final double rightX = plot.maxX();

        final double w = plot.width();
        final double barWidth = (w / (double) n) * 0.75;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor bullishColor = theme.getBullishColor();
        final ArberColor bearishColor = theme.getBearishColor();
        final float borderStroke = ChartScale.scale(0.5f);
        final float alpha = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.volume.alpha", 0.35f);
        final float borderAlpha = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.volume.borderAlpha", 0.55f);

        for (int i = 0; i < n; i++) {
            final double xVal = model.getX(i);
            final double volume;
            if (model instanceof FinancialChartModel fin) {
                volume = fin.getVolume(i);
            } else {
                volume = model.getY(i);
            }

            context.mapToPixel(xVal, volume, pxTop);

            final double x = pxTop[0] - barWidth / 2.0;
            if (x + barWidth < leftX || x > rightX) continue;

            context.mapToPixel(xVal, 0, pxBase);

            final boolean bullish;
            if (model instanceof FinancialChartModel fin) {
                bullish = fin.getClose(i) >= fin.getOpen(i);
            } else {
                bullish = model.getValue(i, 2) >= 0; // weight = price change (volume model)
            }
            final ArberColor baseColor = bullish ? bullishColor : bearishColor;
            final ArberColor barColor = ColorRegistry.applyAlpha(baseColor, alpha);

            final double topY = pxTop[1];
            final double baseY = pxBase[1];
            final double barHeight = Math.abs(baseY - topY);
            final double y = Math.min(topY, baseY);

            canvas.setColor(barColor);
            canvas.fillRect((float) x, (float) y, (float) barWidth, (float) barHeight);

            canvas.setColor(ColorRegistry.applyAlpha(ColorRegistry.adjustBrightness(baseColor, 0.7f), borderAlpha));
            canvas.setStroke(borderStroke);
            canvas.drawRect((float) x, (float) y, (float) barWidth, (float) barHeight);
        }
    }
}
