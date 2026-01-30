package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
/**
 * High-Performance Renderer for CircularFastMedicalModel.
 * Implements the classic "Sweep-Erase" effect used in patient monitors.
 * <p>
 * Strategy: Zero-Allocation rendering using direct array access.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class MedicalSweepRenderer extends BaseRenderer {

    private final int gapSize = 10; // Number of points to leave as gap (erase)
    private final double[] pixel = new double[2]; // allocation-free pixel buffer
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];
    public MedicalSweepRenderer() {
        super("medical_sweep");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel m)) {
            return; // Only accepts CircularFastMedicalModel
        }
        int size = m.getRawSize();
        int capacity = m.getRawCapacity();
        int head = m.getRawHeadIndex();
        if (size == 0) return;

        double[] xRaw = m.getRawChannelArray(0);
        double[] yRaw = m.getRawChannelArray(1); // Channel 1 is standard for medical Y

        ensureBufferCapacity(capacity);
        canvas.setStroke(1.5f);
        ArberColor waveColor = (m.getColor() != null) ? m.getColor() : themeSeries(context, getLayerIndex());
        canvas.setColor(waveColor);

        boolean firstPoint = true;
        int count = 0;
        for (int i = 0; i < capacity; i++) {
            if (isInsideGap(i, head, capacity)) {
                if (count > 1) {
                    canvas.drawPolyline(pathX, pathY, count);
                }
                count = 0;
                firstPoint = true;
                continue;
            }
            context.mapToPixel(xRaw[i], yRaw[i], pixel);
            double px = pixel[0];
            double py = pixel[1];
            if (firstPoint || isBreakPoint(i, head, capacity)) {
                if (count > 1) {
                    canvas.drawPolyline(pathX, pathY, count);
                }
                count = 0;
            } else {
                // continue
            }
            pathX[count] = (float) px;
            pathY[count] = (float) py;
            count++;
            firstPoint = false;
        }
        if (count > 1) {
            canvas.drawPolyline(pathX, pathY, count);
        }
    }

    private boolean isInsideGap(int i, int head, int capacity) {
        for (int g = 0; g < gapSize; g++) {
            if (i == (head + g) % capacity) return true;
        }
        return false;
    }

    private boolean isBreakPoint(int i, int head, int capacity) {
        // Needed to restart path after the gap
        return i == (head + gapSize) % capacity;
    }

    @Override
    public String getName() {
        return "MedicalSweepRenderer";
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
