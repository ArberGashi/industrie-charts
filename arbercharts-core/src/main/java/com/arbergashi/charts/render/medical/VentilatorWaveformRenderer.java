package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.util.function.Function;
/**
 * Ventilator waveform renderer: visualizes pressure, volume, and flow curves.
 * Renders up to three synchronized traces with zero-allocation hot paths.
 * Circular medical models supply their available channels; scalar medical models
 * supply one trace. Missing channels are omitted, and missing samples or a restart
 * of the time axis split a trace.
 *
 * @author Arber Gashi
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
        int channelCount = fastModel instanceof CircularFastMedicalModel medical
                ? Math.min(3, medical.getChannelCount()) : 1;
        ensureBufferCapacity(totalPoints);
        for (int c = 0; c < channelCount; c++) {
            canvas.setColor(colors[c]);
            canvas.setStroke(1.5f);
            int count = 0;
            double previousX = Double.NaN;
            for (int i = 0; i < totalPoints; i++) {
                double x = fastModel.getX(i);
                double yVal = fastModel.getY(i, c);
                boolean missing = !Double.isFinite(x) || !Double.isFinite(yVal);
                if (missing || x < previousX) {
                    if (count > 1) canvas.drawPolyline(pathX[c], pathY[c], count);
                    count = 0;
                }
                previousX = x;
                if (missing) continue;
                context.mapToPixel(x, yVal, sharedPixelCoord);
                if (!Double.isFinite(sharedPixelCoord[0]) || !Double.isFinite(sharedPixelCoord[1])) {
                    if (count > 1) canvas.drawPolyline(pathX[c], pathY[c], count);
                    count = 0;
                    continue;
                }
                pathX[c][count] = (float) sharedPixelCoord[0];
                pathY[c][count] = (float) sharedPixelCoord[1];
                count++;
            }
            if (count > 1) canvas.drawPolyline(pathX[c], pathY[c], count);
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
