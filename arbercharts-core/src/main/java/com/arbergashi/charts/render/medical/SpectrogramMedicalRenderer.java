package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Medical spectrogram renderer: visualizes spectrogram-like data for medical signals.
 * Channel 0 supplies frequency/depth and channel 1 supplies intensity (0-255).
 * Models with fewer than two channels do not produce a spectrogram.
 *
 * @author Arber Gashi
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class SpectrogramMedicalRenderer extends BaseRenderer {

    // Zero-GC: cached resources.
    private final double[] d1 = new double[2];
    private final double[] d2 = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private final ArberColor[] colorPalette = new ArberColor[256];
    private transient int themeKey;

    public SpectrogramMedicalRenderer() {
        super("spectrogram_medical");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        int size = circleModel.getRawSize();
        if (size == 0 || circleModel.getChannelCount() < 2) return;
        ChartTheme theme = getResolvedTheme(context);
        ensurePalette(theme);
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0); // frequency/depth
        int capacity = circleModel.getRawCapacity();
        int head = circleModel.getRawHeadIndex();
        double minY = context.getMinY();
        canvas.setStroke(1.5f);
        for (int i = 0; i < size; i++) {
            if (MedicalSweepSupport.isInGap(i, head, capacity, 15)
                    || !Double.isFinite(rawX[i]) || !Double.isFinite(rawY[i])) continue;
            // Intensity from channel 1 (0-255).
            double intensityVal = circleModel.getYRaw(i, 1);
            if (!Double.isFinite(intensityVal)) continue;
            int colorIdx = (int) Math.max(0, Math.min(255, intensityVal));
            context.mapToPixel(rawX[i], minY, d1);
            context.mapToPixel(rawX[i], rawY[i], d2);
            if (!Double.isFinite(d1[0]) || !Double.isFinite(d1[1])
                    || !Double.isFinite(d2[0]) || !Double.isFinite(d2[1])) continue;
            canvas.setColor(colorPalette[colorIdx]);
            lineX[0] = (float) d1[0];
            lineY[0] = (float) d1[1];
            lineX[1] = (float) d2[0];
            lineY[1] = (float) d2[1];
            canvas.drawPolyline(lineX, lineY, 2);
        }
    }

    @Override
    public String getName() {
        return "SpectrogramMedical";
    }

    private void ensurePalette(ChartTheme theme) {
        int key = System.identityHashCode(theme);
        if (key == themeKey && colorPalette[0] != null) return;
        themeKey = key;

        ArberColor low = theme.getSeriesColor(0);
        ArberColor mid = theme.getSeriesColor(1);
        ArberColor high = theme.getSeriesColor(2);
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            ArberColor base = (t < 0.5f)
                    ? ColorRegistry.interpolate(low, mid, t * 2f)
                    : ColorRegistry.interpolate(mid, high, (t - 0.5f) * 2f);
            colorPalette[i] = ColorRegistry.applyAlpha(base, 0.78f);
        }
    }
}
