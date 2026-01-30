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

/**
 * Heikin-Ashi Renderer - modified candlestick visualization for trend readability.
 *
 * <p>Data requirements: OHLC data in ChartPoint (min=low, max=high, weight=open, y=close).</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class HeikinAshiRenderer extends BaseRenderer {

    private final double[] pxHigh = new double[2];
    private final double[] pxLow = new double[2];
    private final double[] pxOpen = new double[2];
    private final double[] pxClose = new double[2];

    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    // Cached computed series
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;

    private transient double[] xValues;
    private transient double[] haOpen;
    private transient double[] haClose;
    private transient double[] haHigh;
    private transient double[] haLow;

    public HeikinAshiRenderer() {
        super("heikinashi");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureCache(model);
        if (cachedPointCount <= 0 || xValues == null) return;

        final ArberRect bounds = context.getPlotBounds();
        final double w = bounds.getWidth();
        final double leftX = bounds.x();
        final double rightX = bounds.x() + bounds.getWidth();
        final double step = (cachedPointCount > 1) ? (w / (double) (cachedPointCount - 1)) : w;

        int start = 0;
        int endExclusive = cachedPointCount;
        if (step > 0.0) {
            start = (int) Math.floor((leftX - bounds.x()) / step) - 2;
            endExclusive = (int) Math.ceil((rightX - bounds.x()) / step) + 2;
            if (start < 0) start = 0;
            if (endExclusive > cachedPointCount) endExclusive = cachedPointCount;
        }
        if (endExclusive - start <= 0) return;

        final double barWidth = (w / (double) cachedPointCount) * 0.75;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor colorBullish = theme.getBullishColor();
        final ArberColor colorBearish = theme.getBearishColor();

        canvas.setStroke((float) ChartScale.scale(1.0f));

        for (int i = start; i < endExclusive; i++) {
            context.mapToPixel(xValues[i], haHigh[i], pxHigh);
            context.mapToPixel(xValues[i], haLow[i], pxLow);
            context.mapToPixel(xValues[i], haOpen[i], pxOpen);
            context.mapToPixel(xValues[i], haClose[i], pxClose);

            final double x = pxOpen[0];

            final boolean bullish = haClose[i] >= haOpen[i];
            final ArberColor candleColor = bullish ? colorBullish : colorBearish;

            canvas.setColor(theme.getForeground());
            drawLine(canvas, pxHigh[0], pxHigh[1], pxLow[0], pxLow[1]);

            final double bodyY = Math.min(pxOpen[1], pxClose[1]);
            final double bodyH = Math.max(Math.abs(pxOpen[1] - pxClose[1]), ChartScale.scale(1.5f));
            final double bx = x - barWidth / 2.0;

            if (bx > rightX || (bx + barWidth) < leftX) continue;

            canvas.setColor(candleColor);
            canvas.fillRect((float) bx, (float) bodyY, (float) barWidth, (float) bodyH);
            canvas.setColor(ColorRegistry.adjustBrightness(candleColor, 0.7f));
            canvas.setStroke((float) ChartScale.scale(0.85f));
            canvas.drawRect((float) bx, (float) bodyY, (float) barWidth, (float) bodyH);
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineX[0] = (float) x1;
        lineX[1] = (float) x2;
        lineY[0] = (float) y1;
        lineY[1] = (float) y2;
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n == 0) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;

        ensureCapacity(n);

        final FinancialChartModel fin = (model instanceof FinancialChartModel) ? (FinancialChartModel) model : null;

        xValues[0] = model.getX(0);

        final double open0 = (fin != null) ? fin.getOpen(0) : model.getWeight(0);
        final double high0 = (fin != null) ? fin.getHigh(0) : model.getMax(0);
        final double low0 = (fin != null) ? fin.getLow(0) : model.getMin(0);
        final double close0 = (fin != null) ? fin.getClose(0) : model.getY(0);

        double prevHaClose = (open0 + high0 + low0 + close0) * 0.25;
        double prevHaOpen = (open0 + close0) * 0.5;

        haOpen[0] = prevHaOpen;
        haClose[0] = prevHaClose;
        haHigh[0] = Math.max(high0, Math.max(prevHaOpen, prevHaClose));
        haLow[0] = Math.min(low0, Math.min(prevHaOpen, prevHaClose));

        for (int i = 1; i < n; i++) {
            xValues[i] = model.getX(i);

            final double open = (fin != null) ? fin.getOpen(i) : model.getWeight(i);
            final double high = (fin != null) ? fin.getHigh(i) : model.getMax(i);
            final double low = (fin != null) ? fin.getLow(i) : model.getMin(i);
            final double close = (fin != null) ? fin.getClose(i) : model.getY(i);

            final double curHaClose = (open + high + low + close) * 0.25;
            final double curHaOpen = 0.5 * (prevHaOpen + prevHaClose);

            final double curHaHigh = Math.max(high, Math.max(curHaOpen, curHaClose));
            final double curHaLow = Math.min(low, Math.min(curHaOpen, curHaClose));

            haOpen[i] = curHaOpen;
            haClose[i] = curHaClose;
            haHigh[i] = curHaHigh;
            haLow[i] = curHaLow;

            prevHaOpen = curHaOpen;
            prevHaClose = curHaClose;
        }
    }

    private void ensureCapacity(int n) {
        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            haOpen = new double[n];
            haClose = new double[n];
            haHigh = new double[n];
            haLow = new double[n];
        }
    }
}
