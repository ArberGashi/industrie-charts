package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class VolumeProfileRenderer extends BaseRenderer {
    private static final String KEY_BINS = "Chart.financial.volumeProfile.bins";
    private static final String KEY_WIDTH_RATIO = "Chart.financial.volumeProfile.widthRatio";
    private static final String KEY_ALPHA = "Chart.financial.volumeProfile.alpha";
    private static final String KEY_VOLUME_COMPONENT = "Chart.financial.volumeProfile.volumeComponent";

    private int frameStamp = 1;

    public VolumeProfileRenderer() {
        super("volumeprofile");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final double minY = context.getMinY();
        final double maxY = context.getMaxY();
        final int bins = Math.max(8, ChartAssets.getInt(KEY_BINS, 48));
        final double binSize = (maxY - minY) / bins;

        if (binSize <= 0) return;

        // Advance the frame stamp. If it overflows, clear stamps once.
        frameStamp++;
        if (frameStamp == Integer.MAX_VALUE) {
            frameStamp = 1;
            int[] reset = RendererAllocationCache.getIntArray(this, "vol.profile.stamps", bins);
            java.util.Arrays.fill(reset, 0);
        }

        final double[] volumes = RendererAllocationCache.getDoubleArray(this, "vol.profile", bins);
        final int[] stamps = RendererAllocationCache.getIntArray(this, "vol.profile.stamps", bins);
        final int[] touched = RendererAllocationCache.getIntArray(this, "vol.profile.touched", bins);
        int touchedCount = 0;

        final int volumeComponent = Math.max(0, ChartAssets.getInt(KEY_VOLUME_COMPONENT, 2));

        for (int i = 0; i < n; i++) {
            final double y = model.getY(i);
            if (!Double.isFinite(y)) continue;

            double vol = model.getValue(i, volumeComponent);
            if (!Double.isFinite(vol) || vol <= 0.0) {
                vol = model.getWeight(i);
            }
            if (!Double.isFinite(vol) || vol <= 0.0) continue;

            int bin = (int) ((y - minY) / binSize);
            if (bin >= 0 && bin < bins) {
                if (stamps[bin] != frameStamp) {
                    stamps[bin] = frameStamp;
                    volumes[bin] = vol;
                    touched[touchedCount++] = bin;
                } else {
                    volumes[bin] += vol;
                }
            }
        }

        double maxVol = 0.0;
        for (int i = 0; i < touchedCount; i++) {
            double v = volumes[touched[i]];
            if (v > maxVol) maxVol = v;
        }
        if (maxVol <= 0) return;

        final ArberRect b = context.getPlotBounds();
        final double widthRatio = Math.min(0.6, Math.max(0.05, ChartAssets.getFloat(KEY_WIDTH_RATIO, 0.25f)));
        final double maxWidth = b.width() * widthRatio;
        final double binPixelHeight = b.height() / bins;

        final float alpha = Math.min(0.95f, Math.max(0.05f, ChartAssets.getFloat(KEY_ALPHA, 0.30f)));
        final ArberColor baseColor = themeAccent(context);
        final ArberColor fill = ColorRegistry.applyAlpha(baseColor, alpha);
        canvas.setColor(fill);

        for (int i = 0; i < touchedCount; i++) {
            int bin = touched[i];
            double v = volumes[bin];
            if (v <= 0) continue;

            double width = (v / maxVol) * maxWidth;
            double y = b.maxY() - (bin + 1) * binPixelHeight;

            canvas.fillRect((float) (b.maxX() - width), (float) y,
                    (float) width, (float) Math.max(1.0, binPixelHeight - 1.0));
        }
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }
}
