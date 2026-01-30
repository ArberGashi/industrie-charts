package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * PPG renderer: visualizes photoplethysmogram (PPG) curves, suitable for pulse oximetry.
 * Uses smooth curves optimized for SpO2-style signals.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class PPGRenderer extends AbstractMedicalSweepRenderer {
    public PPGRenderer() {
        // Medical green, 2.0f stroke width, gap 25 (PPG typically updates slower).
        super(ColorRegistry.of(0, 120, 0, 255), 2.0f, 25);
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
