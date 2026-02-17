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
        if (model instanceof com.arbergashi.charts.model.CircularFastMedicalModel m && m.getPointCount() > 0) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < m.getPointCount(); i++) {
                double v = m.getY(i, getChannelIndex());
                if (v < min) min = v;
                if (v > max) max = v;
            }
            if (Double.isFinite(min) && Double.isFinite(max)) {
                double span = Math.max(0.2, max - min);
                double pad = span * 0.15;
                return new double[]{min - pad, max + pad};
            }
        }
        return new double[]{60.0, 140.0};
    }

    @Override
    public String getName() {
        return "IBP";
    }
}
