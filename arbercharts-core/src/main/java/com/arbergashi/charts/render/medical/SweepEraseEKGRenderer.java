package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Sweep-erase EKG renderer: clinically authentic oscilloscope-style display with a moving erase bar.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class SweepEraseEKGRenderer extends BaseRenderer {
    private static final int GAP_WIDTH = 20;
    private final Path2D.Double renderPath = new Path2D.Double();
    private final BasicStroke stroke = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final double[] sharedCoord = new double[2];
    public SweepEraseEKGRenderer() {
        super("sweep_ekg");
    }

    @Override
    protected void drawData(Graphics2D g2, com.arbergashi.charts.model.ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        // 1. Preparation (outside the loop).
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(0);
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        int gapEnd = (head + GAP_WIDTH) % capacity;
        boolean wrap = head > gapEnd;
        renderPath.reset();
        boolean firstPointAfterGap = true;
        // 2. Der High-Speed Loop
        for (int i = 0; i < capacity; i++) {
            // Optimized O(1) check.
            if (wrap ? (i >= head || i < gapEnd) : (i >= head && i < gapEnd)) {
                firstPointAfterGap = true;
                continue;
            }
            context.mapToPixel(rawX[i], rawY[i], sharedCoord);
            if (firstPointAfterGap) {
                renderPath.moveTo(sharedCoord[0], sharedCoord[1]);
                firstPointAfterGap = false;
            } else {
                renderPath.lineTo(sharedCoord[0], sharedCoord[1]);
            }
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color waveColor = (model.getColor() != null) ? model.getColor() : themeSeries(context, getLayerIndex());
        g2.setColor(waveColor);
        g2.setStroke(stroke);
        g2.draw(renderPath);
        drawLeadingPoint(g2, context, circleModel, head);
    }

    private void drawLeadingPoint(Graphics2D g, PlotContext context, CircularFastMedicalModel model, int head) {
        int lastPoint = (head - 1 + model.getCapacity()) % model.getCapacity();
        context.mapToPixel((double) lastPoint / model.getCapacity(), model.getYRaw(lastPoint, 0), sharedCoord);
        g.setColor(themeForeground(context));
        g.fillOval((int) sharedCoord[0] - 3, (int) sharedCoord[1] - 3, 6, 6);
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
}
