package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * <h2>ECGRhythmRenderer</h2>
 * <p>
 * Specialized renderer for ECG signals (Electrocardiogram).
 * Optimized for continuous waveforms with medical grid backgrounds.
 * Reads samples in logical time order, including wrapped models. Missing samples
 * and decreasing X coordinates start a separate trace segment.
 * </p>
 *
 * @author Arber Gashi
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

        ensureBufferCapacity(n);

        // ECG is typically a continuous line
        canvas.setStroke(ChartScale.scale(1.5f));
        canvas.setColor(themeForeground(context));

        int count = 0;
        double previousX = Double.NaN;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            boolean missing = !Double.isFinite(x) || !Double.isFinite(y);
            if (missing || x < previousX) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
            }
            previousX = x;
            if (missing) continue;
            context.mapToPixel(x, y, p);
            if (!Double.isFinite(p[0]) || !Double.isFinite(p[1])) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
                continue;
            }
            pathX[count] = (float) p[0];
            pathY[count] = (float) p[1];
            count++;
        }
        if (count > 1) canvas.drawPolyline(pathX, pathY, count);
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
