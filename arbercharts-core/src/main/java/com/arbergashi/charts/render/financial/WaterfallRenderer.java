package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.util.Optional;
/**
 * Waterfall renderer.
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
 *
 */
public final class WaterfallRenderer extends BaseRenderer {

    private static final float CONNECTOR_WIDTH = ChartScale.scale(1.2f);
    private transient double[] rectX = new double[0];
    private transient double[] rectY = new double[0];
    private transient double[] rectW = new double[0];
    private transient double[] rectH = new double[0];
    private transient int rectCount;
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    public WaterfallRenderer() {
        super("waterfall");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor colorUp = theme.getBullishColor();
        final ArberColor colorDown = theme.getBearishColor();
        final ArberColor colorTotal = theme.getAccentColor();

        ensureRectCapacity(n);
        rectCount = 0;

        final ArberRect bounds = context.getPlotBounds();
        final double barWidth = (bounds.width() / n) * 0.75;

        double runningTotal = 0;
        double lastConnectorX2 = Double.NaN;
        double lastConnectorY = Double.NaN;

        final double[] pix0 = pBuffer();
        final double[] pix1 = pBuffer4();
        for (int i = 0; i < n; i++) {
            final double delta = model.getY(i);
            final double nextTotal = runningTotal + delta;

            context.mapToPixel(model.getX(i), runningTotal, pix0);
            context.mapToPixel(model.getX(i), nextTotal, pix1);

            final double startX = pix0[0];
            final double startY = pix0[1];
            final double endY = pix1[1];

            final double x = startX - barWidth / 2.0;
            final double y = Math.min(startY, endY);
            final double h = Math.max(Math.abs(startY - endY), ChartScale.scale(2.0));

            // connectors (single cached stroke)
            if (!Double.isNaN(lastConnectorX2)) {
                canvas.setColor(theme.getGridColor());
                canvas.setStroke(CONNECTOR_WIDTH);
                lineX[0] = (float) lastConnectorX2;
                lineX[1] = (float) x;
                lineY[0] = (float) lastConnectorY;
                lineY[1] = (float) startY;
                canvas.drawPolyline(lineX, lineY, 2);
            }

            // paint
            ArberColor baseColor = (delta >= 0) ? colorUp : colorDown;
            if (model.getWeight(i) > 0) baseColor = colorTotal;
            canvas.setColor(baseColor);
            canvas.fillRect((float) x, (float) y, (float) barWidth, (float) h);

            // store hit box in reusable buffer
            int idx = rectCount++;
            rectX[idx] = x;
            rectY[idx] = y;
            rectW[idx] = barWidth;
            rectH[idx] = h;

            lastConnectorX2 = x + barWidth;
            lastConnectorY = endY;
            runningTotal = nextTotal;
        }
    }

    public Object getRenderedShape(ChartModel model, PlotContext context) {
        return context.getPlotBounds();
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        final int n = rectCount;
        if (n == 0) return Optional.empty();

        for (int i = 0; i < n; i++) {
            double x = rectX[i];
            double y = rectY[i];
            double w = rectW[i];
            double h = rectH[i];
            double px = pixel.x();
            double py = pixel.y();
            if (px >= x && px <= x + w && py >= y && py <= y + h) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void ensureRectCapacity(int n) {
        if (rectX.length >= n) return;
        int nextSize = Math.max(n, rectX.length * 2);
        double[] nextX = new double[nextSize];
        double[] nextY = new double[nextSize];
        double[] nextW = new double[nextSize];
        double[] nextH = new double[nextSize];
        int copied = rectX.length;
        if (copied > 0) {
            System.arraycopy(rectX, 0, nextX, 0, copied);
            System.arraycopy(rectY, 0, nextY, 0, copied);
            System.arraycopy(rectW, 0, nextW, 0, copied);
            System.arraycopy(rectH, 0, nextH, 0, copied);
        }
        rectX = nextX;
        rectY = nextY;
        rectW = nextW;
        rectH = nextH;
    }
}
