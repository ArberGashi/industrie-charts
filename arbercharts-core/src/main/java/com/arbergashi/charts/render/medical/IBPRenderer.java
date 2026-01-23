package com.arbergashi.charts.render.medical;

import java.awt.*;

/**
 * IBP renderer: visualizes invasive blood pressure (IBP) waveforms with systolic,
 * diastolic, and mean arterial pressure (MAP) emphasis.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class IBPRenderer extends AbstractMedicalSweepRenderer {
    public IBPRenderer() {
        super(new Color(220, 20, 60), 2.5f, 20);
    }

    @Override
    public double[] getPreferredYRange(com.arbergashi.charts.model.ChartModel model) {
        return new double[]{60.0, 140.0};
    }

    @Override
    public String getName() {
        return "IBP";
    }
}
