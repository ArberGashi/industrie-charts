package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;

/**
 * Lollipop chart: a lightweight, highly readable alternative to a bar chart.
 * Draws a stick from the baseline to the value and a circular head at the end.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class LollipopRenderer extends BaseRenderer {

    public LollipopRenderer() {
        super("lollipop");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Color c = resolveTheme(context).getSeriesColor(0);

        double[] buf = pBuffer();
        context.mapToPixel(0, 0.0, buf);
        double zeroY = buf[1];
        // JDK 25: Use Math.clamp() for baseline bounds
        double baselineY = MathUtils.clamp(zeroY, context.plotBounds().getY(), context.plotBounds().getMaxY());

        float stickWidth = ChartAssets.getFloat("chart.render.lollipop.stickWidth", 1.5f);
        double radius = ChartScale.scale(ChartAssets.getFloat("chart.render.lollipop.radius", 4.5f));

        ChartTheme theme = resolveTheme(context);

        g2.setStroke(getCachedStroke(ChartScale.scale(stickWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(theme.getGridColor());

        // Optional: subtle baseline
        if (ChartAssets.getBoolean("chart.render.lollipop.showBaseline", true)) {
            g2.draw(getLine(context.plotBounds().getX(), baselineY, context.plotBounds().getMaxX(), baselineY));
        }

        for (int i = 0; i < count; i++) {
            Color pointColor = isMultiColor() ? themeSeries(context, i) : c;
            if (pointColor == null) pointColor = c;
            g2.setColor(pointColor);
            context.mapToPixel(xData[i], yData[i], buf);
            double px = buf[0];
            double py = buf[1];
            g2.draw(getLine(px, baselineY, px, py));
            g2.fill(getEllipse(px - radius, py - radius, radius * 2, radius * 2));
        }
    }

    public LollipopRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
