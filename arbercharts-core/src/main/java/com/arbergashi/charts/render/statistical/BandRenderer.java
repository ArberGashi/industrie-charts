package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Band/range-area renderer: draws a band between min and max.
 * Uses {@link com.arbergashi.charts.model.ChartPoint#min()} and
 * {@link com.arbergashi.charts.model.ChartPoint#max()}.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public final class BandRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public BandRenderer() {
        super("band");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        int count = model.getPointCount();
        if (count == 0) return null;

        double[] lows = model.getLowData();
        double[] highs = model.getHighData();
        if (lows == null || highs == null || lows.length == 0 || highs.length == 0) return null;

        int limit = Math.min(count, Math.min(lows.length, highs.length));
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < limit; i++) {
            double lo = lows[i];
            double hi = highs[i];
            if (Double.isFinite(lo) && lo < min) min = lo;
            if (Double.isFinite(hi) && hi > max) max = hi;
        }
        if (!(min < max)) return null;
        return new double[]{min, max};
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        Color c = seriesOrBase(model, context, 0);
        float alpha = ChartAssets.getFloat("chart.render.band.alpha", 0.18f);

        double[] xData = model.getXData();
        double[] minData = model.getLowData();
        double[] maxData = model.getHighData();

        Path2D band = getPathCache();
        boolean first = true;
        double[] pBuffer = this.pBuffer;
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], maxData[i], pBuffer);
            if (first) {
                band.moveTo(pBuffer[0], pBuffer[1]);
                first = false;
            } else {
                band.lineTo(pBuffer[0], pBuffer[1]);
            }
        }
        for (int i = count - 1; i >= 0; i--) {
            context.mapToPixel(xData[i], minData[i], pBuffer);
            band.lineTo(pBuffer[0], pBuffer[1]);
        }
        band.closePath();

        g2.setColor(ColorUtils.withAlpha(c, alpha));
        g2.fill(band);

        if (ChartAssets.getBoolean("chart.render.band.outline", false)) {
            Color outline = isMultiColor() ? themeSeries(context, 1) : c;
            if (outline == null) outline = c;
            g2.setStroke(getSeriesStroke());
            g2.setColor(ColorUtils.withAlpha(outline, Math.min(1.0f, alpha * 2f)));
            g2.draw(band);
        }
    }
}
