package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Medical spectrogram renderer: visualizes spectrogram-like data for medical signals.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class SpectrogramMedicalRenderer extends BaseRenderer {

    // Zero-GC: cached resources.
    private final double[] d1 = new double[2];
    private final double[] d2 = new double[2];
    private final BasicStroke stroke = new BasicStroke(1.5f);
    private final Color[] colorPalette = new Color[256];
    private transient int themeKey;

    public SpectrogramMedicalRenderer() {
        super("spectrogram_medical");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        ChartTheme theme = resolveTheme(context);
        ensurePalette(theme);
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0); // frequency/depth
        int capacity = circleModel.getRawCapacity();
        int head = circleModel.getRawHeadIndex();
        int gapEnd = (head + 15) % capacity;
        boolean wrap = head > gapEnd;
        double minY = context.minY();
        g2.setStroke(stroke);
        for (int i = 0; i < capacity; i++) {
            // Sweep-erase gap check.
            if (wrap ? (i >= head || i < gapEnd) : (i >= head && i < gapEnd)) continue;
            // Intensity from channel 1 (0-255).
            double intensityVal = circleModel.getYRaw(i, 1);
            int colorIdx = (int) Math.max(0, Math.min(255, intensityVal));
            context.mapToPixel(rawX[i], minY, d1);
            context.mapToPixel(rawX[i], rawY[i], d2);
            g2.setColor(colorPalette[colorIdx]);
            g2.drawLine((int) d1[0], (int) d1[1], (int) d2[0], (int) d2[1]);
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

        Color low = theme.getSeriesColor(0);
        Color mid = theme.getSeriesColor(1);
        Color high = theme.getSeriesColor(2);
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            Color base = (t < 0.5f)
                    ? ColorUtils.interpolate(low, mid, t * 2f)
                    : ColorUtils.interpolate(mid, high, (t - 0.5f) * 2f);
            colorPalette[i] = ColorUtils.withAlpha(base, 0.78f);
        }
    }
}
