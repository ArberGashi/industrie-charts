package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Ultrasound M-mode renderer: visualizes M-mode ultrasound as a scrolling heatmap.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class UltrasoundMModeRenderer extends BaseRenderer {

    // Cached objects to avoid allocations during rendering.
    private final BasicStroke stroke = new BasicStroke(1.0f);
    // Cached grayscale palette (0-255).
    private final Color[] grayPalette = new Color[256];
    private final double[] sharedDest1 = new double[2];
    private final double[] sharedDest2 = new double[2];
    private transient int themeKey;
    public UltrasoundMModeRenderer() {
        super("ultrasound_mmode");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = resolveTheme(context);
        ensurePalette(theme);
        int size = fastModel.getPointCount();
        if (size < 2) return;
        double minY = context.minY();
        g2.setStroke(stroke);
        for (int i = 0; i < size; i++) {
            double xVal = fastModel.getX(i);
            double yVal = fastModel.getY(i, 0); // Y: tissue depth
            context.mapToPixel(xVal, minY, sharedDest1);
            context.mapToPixel(xVal, yVal, sharedDest2);
            // Intensity from the model (e.g., channel 1 as gray value).
            int intensity = (int) Math.max(0, Math.min(255, fastModel.getY(i, 1)));
            g2.setColor(grayPalette[intensity]);
            g2.drawLine((int) sharedDest1[0], (int) sharedDest1[1], (int) sharedDest2[0], (int) sharedDest2[1]);
        }
    }

    @Override
    public String getName() {
        return "UltrasoundMMode";
    }

    private void ensurePalette(ChartTheme theme) {
        int key = System.identityHashCode(theme);
        if (key == themeKey && grayPalette[0] != null) return;
        themeKey = key;

        Color low = theme.getBackground();
        Color high = theme.getForeground();
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            Color base = ColorUtils.interpolate(low, high, t);
            grayPalette[i] = ColorUtils.withAlpha(base, 0.78f);
        }
    }
}
