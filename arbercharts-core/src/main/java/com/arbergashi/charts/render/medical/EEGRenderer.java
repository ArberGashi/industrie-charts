package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.SignalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * EEG renderer: visualizes multi-channel electroencephalogram (EEG) data.
 * Draws simultaneous channels over the same logical time range in separate vertical bands.
 * Circular medical and signal models expose their channel count; other models are treated
 * as a single channel. The configured count limits the number of displayed channels.
 *
 * @author Arber Gashi
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
        int available = model instanceof CircularFastMedicalModel medical ? medical.getChannelCount()
                : model instanceof SignalChartModel signal ? signal.getChannelCount() : 1;
        int n = Math.min(Math.max(1, channelCount), available);
        if (n == 0) return;
        float[] xs = RendererAllocationCache.getFloatArray(this, "eeg.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "eeg.y", count);
        double centerY = context.getMinY() * 0.5 + context.getMaxY() * 0.5;
        double bandHeight = (context.getMaxY() - context.getMinY()) / n;
        ChartTheme theme = getResolvedTheme(context);
        for (int ch = 0; ch < n; ch++) {
            int cCount = 0;
            double previousX = Double.NaN;
            double yOffset = context.getMinY() + (ch + 0.5) * bandHeight;
            canvas.setColor(theme.getSeriesColor(ch));
            canvas.setStroke(ChartScale.scale(1.2f));
            for (int i = 0; i < count; i++) {
                double x = model.getX(i);
                double value = model instanceof SignalChartModel signal
                        ? signal.getValue(i, ch) : model.getY(i, ch);
                boolean missing = !Double.isFinite(x) || !Double.isFinite(value);
                if (missing || x < previousX) {
                    if (cCount > 1) canvas.drawPolyline(xs, ys, cCount);
                    cCount = 0;
                }
                previousX = x;
                if (missing) continue;
                context.mapToPixel(x, yOffset + (value - centerY) / n, dest);
                if (!Double.isFinite(dest[0]) || !Double.isFinite(dest[1])) {
                    if (cCount > 1) canvas.drawPolyline(xs, ys, cCount);
                    cCount = 0;
                    continue;
                }
                xs[cCount] = (float) dest[0];
                ys[cCount] = (float) dest[1];
                cCount++;
            }
            if (cCount > 1) canvas.drawPolyline(xs, ys, cCount);
        }
    }
}
