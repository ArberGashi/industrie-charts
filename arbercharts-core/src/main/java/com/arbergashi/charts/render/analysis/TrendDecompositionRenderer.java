package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Trend decomposition renderer.
 *
 * <p>Draws a trend line computed via a long-window moving average.
 * (Seasonal + residuals are outside the scope of this lightweight renderer.)</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class TrendDecompositionRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public TrendDecompositionRenderer() {
        super("trendDecomposition");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 5) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        int window = Math.max(5, Math.min(401, (int) Math.round(Math.sqrt(count) * 4)));
        int half = window / 2;

        Path2D path = getPathCache();
        boolean started = false;

        // O(N) Sliding Window Algorithm
        double sum = 0.0;
        int left = 0;
        int right = -1;

        for (int i = 0; i < count; i++) {
            int targetLeft = Math.max(0, i - half);
            int targetRight = Math.min(count - 1, i + half);

            // Expand right
            while (right < targetRight) {
                right++;
                sum += yData[right];
            }
            // Shrink left
            while (left < targetLeft) {
                sum -= yData[left];
                left++;
            }

            int windowCount = right - left + 1;
            double y = sum / windowCount;

            context.mapToPixel(xData[i], y, pBuffer);
            if (!started) {
                path.moveTo(pBuffer[0], pBuffer[1]);
                started = true;
            } else {
                path.lineTo(pBuffer[0], pBuffer[1]);
            }
        }

        Color base = seriesOrBase(model, context, 0);
        Color accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;
        g2.setStroke(getCachedStroke((float) ChartScale.scale(2.0)));
        if (started && isMultiColor() && accent != base) {
            g2.setColor(ColorUtils.withAlpha(accent, 0.4f));
            g2.draw(path);
        }
        g2.setColor(ColorUtils.withAlpha(base, 0.8f));
        if (started) g2.draw(path);
    }
}
