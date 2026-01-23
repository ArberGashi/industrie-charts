package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Rug plot renderer: short ticks on the X-axis to show distributions.
 * Very cheap to render and useful as additional context.
 *
 * @author Arber Gashi
 * @version 1.0.1
 * @since 2026-01-01
 */
public final class RugPlotRenderer extends BaseRenderer {

    public RugPlotRenderer() {
        super("rug");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        double len = ChartScale.scale(ChartAssets.getFloat("chart.render.rug.length", 8.0f));
        float w = ChartAssets.getFloat("chart.render.rug.width", 1.5f);

        ChartTheme theme = resolveTheme(context);
        Color c = ColorUtils.withAlpha(theme.getAxisLabelColor(), 0.75f);
        g2.setStroke(getCachedStroke(ChartScale.scale(w), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        // Always draw at the bottom of the visible plot area, regardless of data range.
        double y0 = context.plotBounds().getY() + context.plotBounds().getHeight();

        final Rectangle viewBounds = g2.getClipBounds() != null ? g2.getClipBounds() : context.plotBounds().getBounds();

        double[] xs = model.getXData();
        double[] buf = pBuffer();
        for (int i = 0; i < n; i++) {
            // We only care about the x-value for a rug plot.
            context.mapToPixel(xs[i], 0, buf);
            double x = buf[0];

            // Simple clipping
            if (x < viewBounds.getMinX() || x > viewBounds.getMaxX()) {
                continue;
            }

            Color tickColor = isMultiColor() ? themeSeries(context, i) : c;
            if (tickColor == null) tickColor = c;
            g2.setColor(tickColor);
            g2.draw(getLine(x, y0, x, y0 - len));
        }
    }

}
