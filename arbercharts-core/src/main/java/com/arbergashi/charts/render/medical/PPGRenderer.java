package com.arbergashi.charts.render.medical;

import java.awt.*;

/**
 * PPG renderer: visualizes photoplethysmogram (PPG) curves, suitable for pulse oximetry.
 * Uses smooth curves optimized for SpO2-style signals.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class PPGRenderer extends AbstractMedicalSweepRenderer {
    public PPGRenderer() {
        // Medical green, 2.0f stroke width, gap 25 (PPG typically updates slower).
        super(new Color(0, 120, 0), 2.0f, 25);
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
