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
 * Ultrasound M-mode renderer: visualizes M-mode ultrasound as a scrolling heatmap.
 * Requires a circular medical model with depth in channel 0 and intensity (0-255)
 * in channel 1. Models without both channels produce no output.
 *
 * @author Arber Gashi
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class UltrasoundMModeRenderer extends BaseRenderer {

    // Cached grayscale palette (0-255).
    private final ArberColor[] grayPalette = new ArberColor[256];
    private final double[] sharedDest1 = new double[2];
    private final double[] sharedDest2 = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private transient int themeKey;
    public UltrasoundMModeRenderer() {
        super("ultrasound_mmode");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel fastModel) || fastModel.getChannelCount() < 2) return;
        ChartTheme theme = getResolvedTheme(context);
        ensurePalette(theme);
        int size = fastModel.getPointCount();
        if (size == 0) return;
        double minY = context.getMinY();
        canvas.setStroke(1.0f);
        for (int i = 0; i < size; i++) {
            double xVal = fastModel.getX(i);
            double yVal = fastModel.getY(i, 0); // Y: tissue depth
            double intensityVal = fastModel.getY(i, 1);
            if (!Double.isFinite(xVal) || !Double.isFinite(yVal) || !Double.isFinite(intensityVal)) continue;
            context.mapToPixel(xVal, minY, sharedDest1);
            context.mapToPixel(xVal, yVal, sharedDest2);
            if (!Double.isFinite(sharedDest1[0]) || !Double.isFinite(sharedDest1[1])
                    || !Double.isFinite(sharedDest2[0]) || !Double.isFinite(sharedDest2[1])) continue;
            // Intensity from the model (e.g., channel 1 as gray value).
            int intensity = (int) Math.max(0, Math.min(255, intensityVal));
            canvas.setColor(grayPalette[intensity]);
            lineX[0] = (float) sharedDest1[0];
            lineY[0] = (float) sharedDest1[1];
            lineX[1] = (float) sharedDest2[0];
            lineY[1] = (float) sharedDest2[1];
            canvas.drawPolyline(lineX, lineY, 2);
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

        ArberColor low = theme.getBackground();
        ArberColor high = theme.getForeground();
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            ArberColor base = ColorRegistry.interpolate(low, high, t);
            grayPalette[i] = ColorRegistry.applyAlpha(base, 0.78f);
        }
    }
}
