package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Ultrasound M-mode renderer: visualizes M-mode ultrasound as a scrolling heatmap.
 *
 * @author Arber Gashi
 * @version 1.0.0
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
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = getResolvedTheme(context);
        ensurePalette(theme);
        int size = fastModel.getPointCount();
        if (size < 2) return;
        double minY = context.getMinY();
        canvas.setStroke(1.0f);
        for (int i = 0; i < size; i++) {
            double xVal = fastModel.getX(i);
            double yVal = fastModel.getY(i, 0); // Y: tissue depth
            context.mapToPixel(xVal, minY, sharedDest1);
            context.mapToPixel(xVal, yVal, sharedDest2);
            // Intensity from the model (e.g., channel 1 as gray value).
            int intensity = (int) Math.max(0, Math.min(255, fastModel.getY(i, 1)));
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
