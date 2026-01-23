package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;

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
 */
public final class ECGRhythmRenderer extends BaseRenderer {

    private final double[] p = new double[2];

    public ECGRhythmRenderer() {
        super("ecg");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Path2D path = getPathCache();
        boolean moved = false;

        // ECG is typically a continuous line
        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(themeForeground(context));

        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p);
            
            if (!moved) {
                path.moveTo(p[0], p[1]);
                moved = true;
            } else {
                path.lineTo(p[0], p[1]);
            }
        }
        g2.draw(path);
    }
}
