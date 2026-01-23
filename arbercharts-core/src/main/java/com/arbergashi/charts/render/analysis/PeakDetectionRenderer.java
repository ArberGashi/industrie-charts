package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Peak detection overlay renderer.
 *
 * <p>Marks local maxima with a highlight point. A point is a peak if y[i] > y[i-1] and y[i] > y[i+1]
 * and exceeds an optional threshold.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class PeakDetectionRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private final Point2D.Double pCache = new Point2D.Double();

    public PeakDetectionRenderer() {
        super("peakDetection");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        double minProminence = 0.0;
        Color base = seriesOrBase(model, context, 0);
        Color c = ColorUtils.withAlpha(base, 0.95f);

        g2.setColor(c);
        g2.setStroke(getCachedStroke((float) ChartScale.scale(1.5)));

        for (int i = 1; i < count - 1; i++) {
            double y0 = yData[i - 1];
            double y1 = yData[i];
            double y2 = yData[i + 1];

            if (y1 <= y0 || y1 <= y2) continue;
            if (y1 - Math.max(y0, y2) < minProminence) continue;

            if (isMultiColor()) {
                Color peak = themeSeries(context, i);
                if (peak == null) peak = base;
                c = ColorUtils.withAlpha(peak, 0.95f);
                g2.setColor(c);
            }
            context.mapToPixel(xData[i], y1, pBuffer);
            pCache.setLocation(pBuffer[0], pBuffer[1]);
            drawHighlightPoint(g2, pCache, c);
        }
    }
}
