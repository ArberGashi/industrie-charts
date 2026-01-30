package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.util.function.Function;
/**
 * Ventilator waveform renderer: visualizes pressure, volume, and flow curves.
 * Renders three synchronized traces with zero-allocation hot paths.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class VentilatorWaveformRenderer extends BaseRenderer {
    /**
     * Name translation function. Can be set by end users.
     * Default returns the key unchanged (no translation).
     */
    private static Function<String, String> nameTranslator = key -> key;
    private final ArberColor[] colors = new ArberColor[3];
    private final float[][] pathX = {new float[0], new float[0], new float[0]};
    private final float[][] pathY = {new float[0], new float[0], new float[0]};
    // Cached coordinate arrays as members to avoid allocations in the render loop.
    private final double[] sharedPixelCoord = new double[2];
    private transient int themeKey;

    public VentilatorWaveformRenderer() {
        super("ventilator_waveform");
    }

    /**
     * Allows end users to set a custom name translator.
     * Example: {@code VentilatorWaveformRenderer.setNameTranslator(key -> MyI18n.get(key));}
     */
    public static void setNameTranslator(Function<String, String> translator) {
        nameTranslator = translator != null ? translator : key -> key;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = getResolvedTheme(context);
        ensureColors(theme);
        int totalPoints = fastModel.getPointCount();
        if (totalPoints < 2) return;
        ensureBufferCapacity(totalPoints);
        for (int i = 0; i < totalPoints; i++) {
            double x = fastModel.getX(i);
            for (int c = 0; c < 3; c++) {
                double yVal = fastModel.getY(i, c);
                context.mapToPixel(x, yVal, sharedPixelCoord);
                double xPix = sharedPixelCoord[0];
                double yPix = sharedPixelCoord[1];
                pathX[c][i] = (float) xPix;
                pathY[c][i] = (float) yPix;
            }
        }
        for (int c = 0; c < 3; c++) {
            canvas.setColor(colors[c]);
            canvas.setStroke(1.5f);
            canvas.drawPolyline(pathX[c], pathY[c], totalPoints);
        }
    }

    @Override
    public String getName() {
        String key = "renderer." + getClass().getSimpleName().toLowerCase();
        return nameTranslator.apply(key);
    }

    private void ensureColors(ChartTheme theme) {
        int key = System.identityHashCode(theme);
        if (key == themeKey && colors[0] != null) return;
        themeKey = key;
        for (int i = 0; i < colors.length; i++) {
            colors[i] = theme.getSeriesColor(i);
        }
    }

    private void ensureBufferCapacity(int capacity) {
        for (int i = 0; i < 3; i++) {
            if (pathX[i].length >= capacity) continue;
            int next = 1;
            while (next < capacity && next > 0) next <<= 1;
            if (next <= 0) next = capacity;
            pathX[i] = new float[next];
            pathY[i] = new float[next];
        }
    }
}
