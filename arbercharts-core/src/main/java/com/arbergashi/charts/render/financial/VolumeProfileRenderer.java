package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * <h1>VolumeProfileRenderer</h1>
 * <p>
 * Draws a Volume Profile (Volume at Price) on the side of the chart.
 * Helps identify price levels with high trading activity (Point of Control).
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class VolumeProfileRenderer extends BaseRenderer {

    public VolumeProfileRenderer() {
        super("volumeprofile");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        double minY = context.minY();
        double maxY = context.maxY();
        int bins = 50;
        double binSize = (maxY - minY) / bins;

        if (binSize <= 0) return;

        double[] volumes = RendererAllocationCache.getDoubleArray(this, "vol.profile", bins);
        java.util.Arrays.fill(volumes, 0);

        for (int i = 0; i < n; i++) {
            double y = model.getY(i);
            double vol = model.getValue(i, 2); // We assume component 2 is the volume
            int bin = (int) ((y - minY) / binSize);
            if (bin >= 0 && bin < bins) {
                volumes[bin] += vol;
            }
        }

        double maxVol = 0;
        for (double v : volumes) if (v > maxVol) maxVol = v;
        if (maxVol <= 0) return;

        Rectangle2D b = context.plotBounds();
        double maxWidth = b.getWidth() * 0.25;
        double binPixelHeight = b.getHeight() / bins;

        Color baseColor = themeAccent(context);
        Color fill = ColorUtils.withAlpha(baseColor, 0.3f);
        g2.setColor(fill);

        for (int i = 0; i < bins; i++) {
            double v = volumes[i];
            if (v <= 0) continue;

            double width = (v / maxVol) * maxWidth;
            double y = b.getMaxY() - (i + 1) * binPixelHeight;
            
            // Draw from right to left or left to right
            g2.fill(getRect(b.getX(), y, width, binPixelHeight - 1));
        }
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }
}
