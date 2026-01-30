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
 *
 * @author Arber Gashi
 * @version 1.0.0
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
        ChartTheme theme = getResolvedTheme(context);
        ensurePalette(theme);
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0); // frequency/depth
        int capacity = circleModel.getRawCapacity();
        int head = circleModel.getRawHeadIndex();
        int gapEnd = (head + 15) % capacity;
        boolean wrap = head > gapEnd;
        double minY = context.getMinY();
        canvas.setStroke(1.5f);
        for (int i = 0; i < capacity; i++) {
            // Sweep-erase gap check.
            if (wrap ? (i >= head || i < gapEnd) : (i >= head && i < gapEnd)) continue;
            // Intensity from channel 1 (0-255).
            double intensityVal = circleModel.getYRaw(i, 1);
            int colorIdx = (int) Math.max(0, Math.min(255, intensityVal));
            context.mapToPixel(rawX[i], minY, d1);
            context.mapToPixel(rawX[i], rawY[i], d2);
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
