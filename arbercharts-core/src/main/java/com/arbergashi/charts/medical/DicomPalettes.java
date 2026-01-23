package com.arbergashi.charts.medical;

import java.awt.Color;

/**
 * DICOM Standard Color Palettes for Medical Imaging.
 */
public final class DicomPalettes {

    /**
     * Classic Grayscale (Monochrome2)
     */
    public static Color[] grayscale(int steps) {
        Color[] colors = new Color[steps];
        for (int i = 0; i < steps; i++) {
            int v = (int) (255 * (i / (double) (steps - 1)));
            colors[i] = new Color(v, v, v);
        }
        return colors;
    }

    /**
     * Hot Iron Palette (often used for PET/SPECT)
     */
    public static Color[] hotIron(int steps) {
        Color[] colors = new Color[steps];
        for (int i = 0; i < steps; i++) {
            double t = i / (double) (steps - 1);
            int r = (int) Math.min(255, 255 * t * 3);
            int g = (int) Math.min(255, Math.max(0, 255 * (t - 0.33) * 3));
            int b = (int) Math.min(255, Math.max(0, 255 * (t - 0.66) * 3));
            colors[i] = new Color(r, g, b);
        }
        return colors;
    }

    /**
     * PET Palette (Blue-White-Yellow-Red)
     */
    public static Color[] pet(int steps) {
        // Simplified PET color ramp
        Color[] colors = new Color[steps];
        for (int i = 0; i < steps; i++) {
            double t = i / (double) (steps - 1);
            if (t < 0.25) {
                colors[i] = new Color(0, 0, (int)(t * 4 * 255));
            } else if (t < 0.5) {
                colors[i] = new Color(0, (int)((t-0.25) * 4 * 255), 255);
            } else if (t < 0.75) {
                colors[i] = new Color((int)((t-0.5) * 4 * 255), 255, (int)((0.75-t) * 4 * 255));
            } else {
                colors[i] = new Color(255, (int)((1.0-t) * 4 * 255), 0);
            }
        }
        return colors;
    }

    private DicomPalettes() {}
}
