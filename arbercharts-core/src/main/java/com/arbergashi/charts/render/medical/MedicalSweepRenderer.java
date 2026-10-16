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
 * Uses the model's X coordinates and its first Y channel, as defined by ChartModel.
 * <p>
 * Strategy: Zero-Allocation rendering using direct array access.
 *
 * @author Arber Gashi
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

        double[] xRaw = m.getXData();
        double[] yRaw = m.getRawChannelArray(0);

        ensureBufferCapacity(size);
        canvas.setStroke(1.5f);
        ArberColor waveColor = (m.getColor() != null) ? m.getColor() : themeSeries(context, getLayerIndex());
        canvas.setColor(waveColor);

        int count = 0;
        for (int i = 0; i < size; i++) {
            boolean skip = MedicalSweepSupport.isInGap(i, head, capacity, gapSize)
                    || !Double.isFinite(xRaw[i]) || !Double.isFinite(yRaw[i]);
            if (i == head || skip) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
            }
            if (skip) continue;
            context.mapToPixel(xRaw[i], yRaw[i], pixel);
            if (!Double.isFinite(pixel[0]) || !Double.isFinite(pixel[1])) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
                continue;
            }
            pathX[count] = (float) pixel[0];
            pathY[count] = (float) pixel[1];
            count++;
        }
        if (count > 1) canvas.drawPolyline(pathX, pathY, count);
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
