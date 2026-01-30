package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.util.ColorRegistry;
/**
 * Renderer for capnography (CO2 waveform).
 * Uses the shared sweep-erase logic for consistent rendering.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class CapnographyRenderer extends AbstractMedicalSweepRenderer {
    public CapnographyRenderer() {
        // Medical standard yellow/orange.
        // Thickness: 2.0f for visibility.
        // Gap: 20 (standard gap for waveforms).
        super(ColorRegistry.of(255, 140, 0, 255), 2.0f, 20);
    }

    @Override
    public String getName() {
        // Uses the nameTranslator defined in the base class.
        // Key: renderer.capnographyrenderer
        return super.getName();
    }
}
