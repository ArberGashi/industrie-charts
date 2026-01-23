package com.arbergashi.charts.render.medical;

/**
 * EOG renderer: visualizes electrooculogram (EOG) signals, typically rectangular or sawtooth.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class EOGRenderer extends AbstractMedicalSweepRenderer {
    public EOGRenderer() {
        super(new java.awt.Color(0, 100, 180), 1.5f, 20); // Typical EOG color, moderate stroke, standard gap.
    }

    @Override
    public String getName() {
        return "EOG";
    }
}
