package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * IBP renderer: visualizes invasive blood pressure (IBP) waveforms with systolic,
 * diastolic, and mean arterial pressure (MAP) emphasis.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class IBPRenderer extends AbstractMedicalSweepRenderer {
    public IBPRenderer() {
        super(ColorRegistry.of(220, 20, 60, 255), 2.5f, 20);
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
