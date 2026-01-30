package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * <h1>ECGRhythmRenderer</h1>
 * <p>
 * Specialized renderer for ECG signals (Electrocardiogram).
 * Optimized for continuous waveforms with medical grid backgrounds.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 *
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ECGRhythmRenderer extends BaseRenderer {

    private final double[] p = new double[2];
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];

    public ECGRhythmRenderer() {
        super("ecg");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ensureBufferCapacity(n);

        // ECG is typically a continuous line
        canvas.setStroke(ChartScale.scale(1.5f));
        canvas.setColor(themeForeground(context));

        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p);
            pathX[i] = (float) p[0];
            pathY[i] = (float) p[1];
        }
        canvas.drawPolyline(pathX, pathY, n);
    }

    private void ensureBufferCapacity(int capacity) {
        if (pathX.length >= capacity) return;
        int next = 1;
        while (next < capacity && next > 0) next <<= 1;
        if (next <= 0) next = capacity;
        pathX = new float[next];
        pathY = new float[next];
    }
}
