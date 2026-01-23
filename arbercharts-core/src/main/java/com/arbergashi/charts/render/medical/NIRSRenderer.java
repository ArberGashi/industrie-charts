package com.arbergashi.charts.render.medical;

import java.awt.*;

/**
 * NIRS renderer: visualizes near-infrared spectroscopy (NIRS) trends.
 * Slow, smoothed curves suitable for long time spans.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class NIRSRenderer extends AbstractMedicalSweepRenderer {
    public NIRSRenderer() {
        // Blue/violet, 2.0f stroke width, gap 10 (trend data).
        super(new Color(100, 100, 200), 2.0f, 10);
    }

    @Override
    public double[] getPreferredYRange(com.arbergashi.charts.model.ChartModel model) {
        return new double[]{55.0, 80.0};
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
