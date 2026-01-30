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
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class SweepEraseEKGRenderer extends BaseRenderer {
    private static final int GAP_WIDTH = 20;
    private final double[] sharedCoord = new double[2];
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];
    private final float[] pointX = new float[1];
    private final float[] pointY = new float[1];
    public SweepEraseEKGRenderer() {
        super("sweep_ekg");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, com.arbergashi.charts.model.ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        // 1. Preparation (outside the loop).
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0);
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        int gapEnd = (head + GAP_WIDTH) % capacity;
        boolean wrap = head > gapEnd;
        ensureBufferCapacity(capacity);
        boolean firstPointAfterGap = true;
        int count = 0;
        // 2. Der High-Speed Loop
        for (int i = 0; i < capacity; i++) {
            // Optimized O(1) check.
            if (wrap ? (i >= head || i < gapEnd) : (i >= head && i < gapEnd)) {
                if (count > 1) {
                    canvas.setStroke(2f);
                    canvas.setColor(getWaveColor(model, context));
                    canvas.drawPolyline(pathX, pathY, count);
                }
                count = 0;
                firstPointAfterGap = true;
                continue;
            }
            context.mapToPixel(rawX[i], rawY[i], sharedCoord);
            if (firstPointAfterGap) {
                if (count > 1) {
                    canvas.setStroke(2f);
                    canvas.setColor(getWaveColor(model, context));
                    canvas.drawPolyline(pathX, pathY, count);
                }
                count = 0;
            } else {
                // continue
            }
            pathX[count] = (float) sharedCoord[0];
            pathY[count] = (float) sharedCoord[1];
            count++;
            firstPointAfterGap = false;
        }
        if (count > 1) {
            canvas.setStroke(2f);
            canvas.setColor(getWaveColor(model, context));
            canvas.drawPolyline(pathX, pathY, count);
        }
        drawLeadingPoint(canvas, context, circleModel, head);
    }

    private void drawLeadingPoint(ArberCanvas canvas, PlotContext context, CircularFastMedicalModel model, int head) {
        int lastPoint = (head - 1 + model.getCapacity()) % model.getCapacity();
        context.mapToPixel((double) lastPoint / model.getCapacity(), model.getYRaw(lastPoint, 0), sharedCoord);
        canvas.setColor(themeForeground(context));
        float cx = (float) sharedCoord[0];
        float cy = (float) sharedCoord[1];
        canvas.fillRect(cx - 3f, cy - 3f, 6f, 6f);
    }

    // Optimierte Gap-Logik: O(1) statt O(GAP_WIDTH)
    private boolean isWithinGap(int i, int head, int capacity) {
        int endGap = (head + GAP_WIDTH) % capacity;
        if (head < endGap) {
            return i >= head && i < endGap;
        } else {
            // Wrap-around case (gap extends past array end).
            return i >= head || i < endGap;
        }
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
