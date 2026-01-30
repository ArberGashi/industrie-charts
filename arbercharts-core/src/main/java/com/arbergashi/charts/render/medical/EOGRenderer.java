package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * EOG renderer: visualizes electrooculogram (EOG) signals, typically rectangular or sawtooth.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class EOGRenderer extends AbstractMedicalSweepRenderer {
    public EOGRenderer() {
        super(ColorRegistry.of(0, 100, 180, 255), 1.5f, 20); // Typical EOG color, moderate stroke, standard gap.
    }

    @Override
    public String getName() {
        return "EOG";
    }
}
