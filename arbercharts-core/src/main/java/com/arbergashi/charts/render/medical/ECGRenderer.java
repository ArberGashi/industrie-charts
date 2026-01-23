package com.arbergashi.charts.render.medical;

import java.awt.*;

/**
 * ECGRenderer: Visualizes ECG curves (Electrocardiogram) with sweep-erase logic.
 * The grid is controlled via the MedicalGridLayer (not in the renderer!).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class ECGRenderer extends AbstractMedicalSweepRenderer {
    /**
     * Creates a specialized ECG renderer.
     * Uses the optimized sweep-erase logic of the base class.
     */
    public ECGRenderer() {
        // Color: A vibrant ECG green (better contrast on DarkLaf than red)
        // Thickness: 2.0f for precise R-peak representation
        // Gap: 20 data points for the classic sweep look
        super(new Color(0, 255, 100), 2.0f, 20);
    }

    @Override
    public String getName() {
        return "ECG";
    }
    /**
     * Note: The grid is now controlled via the MedicalGridLayer.
     * This saves approx. 30% CPU load per channel.
     */
}
