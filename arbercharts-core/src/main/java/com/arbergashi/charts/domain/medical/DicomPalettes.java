package com.arbergashi.charts.domain.medical;
/**
 * DICOM Standard Color Palettes for Medical Imaging.
  * Platform-independent and headless-certified. No AWT/Swing dependencies.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class DicomPalettes {

    /**
     * Classic Grayscale (Monochrome2)
     */
    public static int[] grayscale(int steps) {
        int[] colors = new int[steps];
        for (int i = 0; i < steps; i++) {
            int v = (int) (255 * (i / (double) (steps - 1)));
            colors[i] = argb(255, v, v, v);
        }
        return colors;
    }

    /**
     * Hot Iron Palette (often used for PET/SPECT)
     */
    public static int[] hotIron(int steps) {
        int[] colors = new int[steps];
        for (int i = 0; i < steps; i++) {
            double t = i / (double) (steps - 1);
            int r = (int) Math.min(255, 255 * t * 3);
            int g = (int) Math.min(255, Math.max(0, 255 * (t - 0.33) * 3));
            int b = (int) Math.min(255, Math.max(0, 255 * (t - 0.66) * 3));
            colors[i] = argb(255, r, g, b);
        }
        return colors;
    }

    /**
     * PET Palette (Blue-White-Yellow-Red)
     */
    public static int[] pet(int steps) {
        // Simplified PET color ramp
        int[] colors = new int[steps];
        for (int i = 0; i < steps; i++) {
            double t = i / (double) (steps - 1);
            if (t < 0.25) {
                colors[i] = argb(255, 0, 0, (int)(t * 4 * 255));
            } else if (t < 0.5) {
                colors[i] = argb(255, 0, (int)((t-0.25) * 4 * 255), 255);
            } else if (t < 0.75) {
                colors[i] = argb(255, (int)((t-0.5) * 4 * 255), 255, (int)((0.75-t) * 4 * 255));
            } else {
                colors[i] = argb(255, 255, (int)((1.0-t) * 4 * 255), 0);
            }
        }
        return colors;
    }

    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
    }

    private DicomPalettes() {}
}
