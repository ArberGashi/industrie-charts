package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * High-Performance Renderer for CircularFastMedicalModel.
 * Implements the classic "Sweep-Erase" effect used in patient monitors.
 * <p>
 * Strategy: Zero-Allocation rendering using direct array access.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class MedicalSweepRenderer extends BaseRenderer {

    private final int gapSize = 10; // Number of points to leave as gap (erase)
    private final Path2D.Double pathCache = new Path2D.Double(); // Path cache for zero-allocation
    private final BasicStroke stroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final double[] pixel = new double[2]; // allocation-free pixel buffer
    public MedicalSweepRenderer() {
        super("medical_sweep");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel m)) {
            return; // Only accepts CircularFastMedicalModel
        }
        int size = m.getRawSize();
        int capacity = m.getRawCapacity();
        int head = m.getRawHeadIndex();
        if (size == 0) return;

        double[] xRaw = m.getRawChannelArray(0);
        double[] yRaw = m.getRawChannelArray(1); // Channel 1 is standard for medical Y

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(stroke);
        Color waveColor = (m.getColor() != null) ? m.getColor() : themeSeries(context, getLayerIndex());
        g2.setColor(waveColor);

        pathCache.reset();
        boolean firstPoint = true;
        for (int i = 0; i < capacity; i++) {
            if (isInsideGap(i, head, capacity)) continue;
            context.mapToPixel(xRaw[i], yRaw[i], pixel);
            double px = pixel[0];
            double py = pixel[1];
            if (firstPoint || isBreakPoint(i, head, capacity)) {
                pathCache.moveTo(px, py);
                firstPoint = false;
            } else {
                pathCache.lineTo(px, py);
            }
        }
        g2.draw(pathCache);
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
}
