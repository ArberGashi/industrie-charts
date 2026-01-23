package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.AnalysisWorker;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation Bollinger Bands renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class BollingerBandsRenderer extends BaseRenderer {

    private final Path2D.Double rangePath = new Path2D.Double();
    private final double[] p0 = new double[2];

    private long lastModelStamp = -1;
    private int cachedPointCount;
    private double[] yValues;
    private double[] smaValues;
    private double[] upperValues;
    private double[] lowerValues;
    private double[] xValues;

    public BollingerBandsRenderer() {
        super("bollinger");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        ensureCache(model);
        if (cachedPointCount < 2) return;

        final Rectangle2D viewBounds = g2.getClipBounds() != null ? g2.getClipBounds() : context.plotBounds();
        final double viewMinX = viewBounds.getMinX();
        final double viewMaxX = viewBounds.getMaxX();

        Color base = getSeriesColor(model);

        drawFillChannel(g2, context, base, viewMinX, viewMaxX);

        g2.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        g2.setColor(ColorUtils.withAlpha(base, 0.4f));
        drawPath(g2, context, upperValues, viewMinX, viewMaxX);
        drawPath(g2, context, lowerValues, viewMinX, viewMaxX);

        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(base);
        drawPath(g2, context, smaValues, viewMinX, viewMaxX);
    }

    private void drawFillChannel(Graphics2D g2, PlotContext context, Color base, double viewMinX, double viewMaxX) {
        rangePath.reset();
        boolean moved = false;
        for (int i = 0; i < cachedPointCount; i++) {
            if (Double.isNaN(upperValues[i])) continue;
            context.mapToPixel(xValues[i], upperValues[i], p0);
            if (p0[0] < viewMinX) continue;
            if (p0[0] > viewMaxX) break;
            if (!moved) {
                rangePath.moveTo(p0[0], p0[1]);
                moved = true;
            } else {
                rangePath.lineTo(p0[0], p0[1]);
            }
        }
        if (!moved) return;

        for (int i = cachedPointCount - 1; i >= 0; i--) {
            if (Double.isNaN(lowerValues[i])) continue;
            context.mapToPixel(xValues[i], lowerValues[i], p0);
            if (p0[0] < viewMinX) break;
            if (p0[0] > viewMaxX) continue;
            rangePath.lineTo(p0[0], p0[1]);
        }
        rangePath.closePath();

        float fillAlpha = ChartAssets.getFloat("chart.financial.bollinger.fillAlpha", 0.10f);
        g2.setColor(ColorUtils.withAlpha(base, fillAlpha));
        g2.fill(rangePath);
    }

    private void drawPath(Graphics2D g2, PlotContext context, double[] values, double viewMinX, double viewMaxX) {
        Path2D path = getPathCache();
        boolean moved = false;
        for (int i = 0; i < cachedPointCount; i++) {
            if (Double.isNaN(values[i])) continue;
            context.mapToPixel(xValues[i], values[i], p0);
            if (p0[0] < viewMinX) continue;
            if (p0[0] > viewMaxX) break;
            if (!moved) {
                path.moveTo(p0[0], p0[1]);
                moved = true;
            } else {
                path.lineTo(p0[0], p0[1]);
            }
        }
        if (moved) g2.draw(path);
    }

    private void ensureCache(ChartModel model) {
        final long stamp = model.getUpdateStamp();
        if (stamp == lastModelStamp && cachedPointCount == model.getPointCount()) {
            return;
        }
        lastModelStamp = stamp;
        cachedPointCount = model.getPointCount();
        final int n = cachedPointCount;

        int period = ChartAssets.getInt("chart.financial.bollinger.period", 20);
        if (n < period) {
            cachedPointCount = 0;
            return;
        }

        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            yValues = new double[n];
            smaValues = new double[n];
            upperValues = new double[n];
            lowerValues = new double[n];
        }

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
            yValues[i] = model.getY(i);
        }

        double stdDevFactor = ChartAssets.getFloat("chart.financial.bollinger.stddev", 2.0f);
        AnalysisWorker.calculateBollingerBands(yValues, period, stdDevFactor, smaValues, upperValues, lowerValues);
    }
}
