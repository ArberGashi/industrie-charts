package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
/**
 * Sweep-erase EKG renderer: clinically authentic oscilloscope-style display with a moving erase bar.
 *
 * @author Arber Gashi
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class SweepEraseEKGRenderer extends BaseRenderer {
    private static final int GAP_WIDTH = 20;
    private final double[] sharedCoord = new double[2];
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];
    public SweepEraseEKGRenderer() {
        super("sweep_ekg");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, com.arbergashi.charts.model.ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        int size = circleModel.getRawSize();
        if (size == 0) return;
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0);
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        ensureBufferCapacity(size);
        canvas.setStroke(2f);
        canvas.setColor(getWaveColor(model, context));
        int count = 0;
        for (int i = 0; i < size; i++) {
            boolean skip = MedicalSweepSupport.isInGap(i, head, capacity, GAP_WIDTH)
                    || !Double.isFinite(rawX[i]) || !Double.isFinite(rawY[i]);
            if (i == head || skip) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
            }
            if (skip) continue;
            context.mapToPixel(rawX[i], rawY[i], sharedCoord);
            if (!Double.isFinite(sharedCoord[0]) || !Double.isFinite(sharedCoord[1])) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
                continue;
            }
            pathX[count] = (float) sharedCoord[0];
            pathY[count] = (float) sharedCoord[1];
            count++;
        }
        if (count > 1) canvas.drawPolyline(pathX, pathY, count);
        drawLeadingPoint(canvas, context, circleModel, head);
    }

    private void drawLeadingPoint(ArberCanvas canvas, PlotContext context, CircularFastMedicalModel model, int head) {
        int lastPoint = (head - 1 + model.getCapacity()) % model.getCapacity();
        double x = model.getXData()[lastPoint];
        double y = model.getYRaw(lastPoint, 0);
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        context.mapToPixel(x, y, sharedCoord);
        if (!Double.isFinite(sharedCoord[0]) || !Double.isFinite(sharedCoord[1])) return;
        canvas.setColor(themeForeground(context));
        float cx = (float) sharedCoord[0];
        float cy = (float) sharedCoord[1];
        canvas.fillRect(cx - 3f, cy - 3f, 6f, 6f);
    }

    @Override
    public String getName() {
        return "SweepEraseEKG";
    }

    private ArberColor getWaveColor(com.arbergashi.charts.model.ChartModel model, PlotContext context) {
        return (model.getColor() != null) ? model.getColor() : themeSeries(context, getLayerIndex());
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
