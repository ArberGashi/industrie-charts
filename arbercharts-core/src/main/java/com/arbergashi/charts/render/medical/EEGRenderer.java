package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * EEG renderer: visualizes multi-channel electroencephalogram (EEG) data.
 * Supports multiple channels with optimized vertical scaling.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
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

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        int n = Math.max(1, channelCount);
        int pointsPerChannel = count / n;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        float[] xs = RendererAllocationCache.getFloatArray(this, "eeg.x", Math.max(2, pointsPerChannel));
        float[] ys = RendererAllocationCache.getFloatArray(this, "eeg.y", Math.max(2, pointsPerChannel));
        for (int ch = 0; ch < n; ch++) {
            int cCount = 0;
            for (int i = 0; i < pointsPerChannel; i++) {
                int idx = ch * pointsPerChannel + i;
                if (idx >= count) break;
                double yOffset = (ch + 0.5) / n * (context.getMaxY() - context.getMinY()) + context.getMinY();
                context.mapToPixel(xData[idx], yData[idx] + yOffset, dest);
                xs[cCount] = (float) dest[0];
                ys[cCount] = (float) dest[1];
                cCount++;
            }
            ArberColor c = ColorRegistry.of(80, 80, Math.min(255, 80 + 20 * ch), 255);
            canvas.setColor(c);
            canvas.setStroke(ChartScale.scale(1.2f));
            if (cCount > 1) {
                canvas.drawPolyline(xs, ys, cCount);
            }
        }
    }
}
