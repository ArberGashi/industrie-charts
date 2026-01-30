package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * EMG renderer: visualizes high-frequency EMG signals (electromyogram).
 * Shows peaks and noise, optimized for high data density.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class EMGRenderer extends AbstractMedicalSweepRenderer {
    public EMGRenderer() {
        // Medical violet, 1.2f stroke width, gap 12 (EMG is very fast).
        super(ColorRegistry.of(120, 0, 120, 255), 1.2f, 12);
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
