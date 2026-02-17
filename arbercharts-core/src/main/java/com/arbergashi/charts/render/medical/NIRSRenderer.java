package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * NIRS renderer: visualizes near-infrared spectroscopy (NIRS) trends.
 * Slow, smoothed curves suitable for long time spans.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class NIRSRenderer extends AbstractMedicalSweepRenderer {
    public NIRSRenderer() {
        // Blue/violet, 2.0f stroke width, gap 10 (trend data).
        super(ColorRegistry.of(100, 100, 200, 255), 2.0f, 10);
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
                double span = Math.max(0.15, max - min);
                double pad = span * 0.2;
                return new double[]{min - pad, max + pad};
            }
        }
        return new double[]{55.0, 80.0};
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
