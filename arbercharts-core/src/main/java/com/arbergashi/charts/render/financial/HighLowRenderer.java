package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.Optional;

/**
 * High-low renderer.
 *
 * <p>Performance policy:</p>
 * <ul>
 *   <li>No allocations in the hot drawing loop.</li>
 *   <li>No usage of platform-specific geometry classes.</li>
 *   <li>Hit testing uses a reusable rectangle buffer.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class HighLowRenderer extends BaseRenderer {

    private final double[] pixMid = new double[2];
    private transient double[] hitX = new double[0];
    private transient double[] hitY = new double[0];
    private transient double[] hitW = new double[0];
    private transient double[] hitH = new double[0];
    private transient int hitBoxCount;

    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    public HighLowRenderer() {
        super("highlow");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureHitBoxCapacity(n);
        hitBoxCount = 0;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor bullish = theme.getBullishColor();
        final ArberColor bearish = theme.getBearishColor();
        final ArberColor tickBullish = ColorRegistry.adjustBrightness(bullish, 0.7f);
        final ArberColor tickBearish = ColorRegistry.adjustBrightness(bearish, 0.7f);

        final float mainStrokeWidth = ChartScale.scale(2.0f);
        final float tickStrokeWidth = ChartScale.scale(1.2f);
        final double tickHalfWidth = ChartScale.scale(5.0);
        final double hitWidth = ChartScale.scale(12.0);

        final double[] pixHigh = pBuffer();
        final double[] pixLow = pBuffer4();

        for (int i = 0; i < n; i++) {
            final double xData = model.getX(i);

            context.mapToPixel(xData, model.getMax(i), pixHigh);
            context.mapToPixel(xData, model.getMin(i), pixLow);
            context.mapToPixel(xData, model.getY(i), pixMid);

            final double x = pixHigh[0];

            final double yHigh = pixHigh[1];
            final double yLow = pixLow[1];
            final double open = model.getWeight(i);
            final double close = model.getY(i);
            final boolean up = close >= open;
            final ArberColor mainColor = up ? bullish : bearish;
            final ArberColor tickColor = up ? tickBullish : tickBearish;

            canvas.setStroke(mainStrokeWidth);
            canvas.setColor(mainColor);
            drawLine(canvas, x, yHigh, x, yLow);

            canvas.setStroke(tickStrokeWidth);
            canvas.setColor(tickColor);
            drawLine(canvas, x - tickHalfWidth, yHigh, x + tickHalfWidth, yHigh);
            drawLine(canvas, x - tickHalfWidth, yLow, x + tickHalfWidth, yLow);

            int hb = hitBoxCount++;
            hitX[hb] = x - hitWidth / 2.0;
            hitY[hb] = Math.min(yHigh, yLow);
            hitW[hb] = hitWidth;
            hitH[hb] = Math.max(Math.abs(yLow - yHigh), 1.0);
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineX[0] = (float) x1;
        lineX[1] = (float) x2;
        lineY[0] = (float) y1;
        lineY[1] = (float) y2;
        canvas.drawPolyline(lineX, lineY, 2);
    }

    public Object getRenderedShape(ChartModel model, PlotContext context) {
        return context.getPlotBounds();
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        final int n = hitBoxCount;
        if (n == 0) return Optional.empty();

        for (int i = 0; i < n; i++) {
            double x = hitX[i];
            double y = hitY[i];
            if (pixel.x() >= x && pixel.x() <= x + hitW[i] && pixel.y() >= y && pixel.y() <= y + hitH[i]) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void ensureHitBoxCapacity(int n) {
        if (hitX.length >= n) return;
        hitX = RendererAllocationCache.getDoubleArray(this, "highlow.hit.x", n);
        hitY = RendererAllocationCache.getDoubleArray(this, "highlow.hit.y", n);
        hitW = RendererAllocationCache.getDoubleArray(this, "highlow.hit.w", n);
        hitH = RendererAllocationCache.getDoubleArray(this, "highlow.hit.h", n);
    }
}
