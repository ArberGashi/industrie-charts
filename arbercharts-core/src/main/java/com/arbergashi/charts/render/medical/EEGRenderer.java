package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * EEG renderer: visualizes multi-channel electroencephalogram (EEG) data.
 * Supports multiple channels with optimized vertical scaling.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class EEGRenderer extends BaseRenderer {
    private final double[] dest = new double[2];
    private int channelCount = 8;

    public EEGRenderer() {
        super("eeg");
    }

    public EEGRenderer(int channelCount) {
        super("eeg");
        this.channelCount = channelCount;
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        int n = Math.max(1, channelCount);
        int pointsPerChannel = count / n;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        for (int ch = 0; ch < n; ch++) {
            Path2D path = (Path2D) getPathCache();
            path.reset();
            for (int i = 0; i < pointsPerChannel; i++) {
                int idx = ch * pointsPerChannel + i;
                if (idx >= count) break;
                double yOffset = (ch + 0.5) / n * (context.maxY() - context.minY()) + context.minY();
                context.mapToPixel(xData[idx], yData[idx] + yOffset, dest);
                if (i == 0) path.moveTo(dest[0], dest[1]);
                else path.lineTo(dest[0], dest[1]);
            }
            Color old = g.getColor();
            g.setColor(com.arbergashi.charts.tools.RendererAllocationCache.getColor(this, "eeg.color." + ch, 80, 80, Math.min(255, 80 + 20 * ch)));
            g.setStroke(getCachedStroke(ChartScale.scale(1.2f)));
            g.draw(path);
            g.setColor(old);
        }
    }
}
