package com.arbergashi.charts.platform.swing.util;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;

/**
 * Swing DPI helper.
 */
public final class PhysicalScaleResolver {
    private static final double MM_PER_INCH = 25.4;

    private PhysicalScaleResolver() {
    }

    public static double pixelsPerMillimeter(Graphics2D g2) {
        double dpi = 96.0;
        try {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (Exception ignored) {
        }

        double scale = 1.0;
        if (g2 != null) {
            GraphicsConfiguration cfg = g2.getDeviceConfiguration();
            if (cfg != null && cfg.getDefaultTransform() != null) {
                scale = cfg.getDefaultTransform().getScaleX();
            }
        }
        return (dpi * scale) / MM_PER_INCH;
    }

    /**
     * Returns millimeter density in Swing user coordinates. Device transforms and
     * export scaling are already applied by Graphics2D and must not be applied twice.
     */
    public static double logicalPixelsPerMillimeter(Graphics2D g2) {
        double deviceScale = 1.0;
        if (g2 != null && g2.getDeviceConfiguration() != null) {
            deviceScale = g2.getDeviceConfiguration().getDefaultTransform().getScaleX();
        }
        return pixelsPerMillimeter(g2) / Math.max(deviceScale, 1e-9);
    }
}
