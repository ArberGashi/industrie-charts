package com.arbergashi.charts.util;

import java.awt.*;

/**
 * Core rendering engine utilities.
 * Ensures consistent graphics quality settings across screen rendering and exports.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class ChartEngine {

    private ChartEngine() {
    }

    /**
     * Configures Graphics2D with high-quality rendering hints.
     *
     * @param g2              The graphics context.
     * @param textLcdContrast If true, uses LCD contrast for text (better for screens, worse for transparent exports).
     */
    public static void prepareGraphics(Graphics2D g2, boolean textLcdContrast) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (textLcdContrast) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    /**
     * Placeholder for platform-specific default registrations (e.g., themes, icons).
     * No-op in core module.
     */
    public static void registerDefaultsIfNeeded() {
        // No-op for core; placeholder for platform-specific bootstrap (themes, icon registration)
    }
}