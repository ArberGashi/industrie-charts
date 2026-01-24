package com.arbergashi.charts.util;

import java.awt.*;

/**
 * Color utilities.
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class ColorUtils {

    private ColorUtils() {
    }

    /**
     * Creates a new color based on c, but with a new alpha value.
     *
     * <p>Note: Creates a new object. Should be cached in render loops
     * if the color does not change.</p>
     *
     * @param c     Base color
     * @param alpha Alpha (0.0 - 1.0)
     * @return New Color instance
     */
    public static Color withAlpha(Color c, float alpha) {
        return ColorRegistry.withAlpha(c, alpha);
    }

    /**
     * Adjust brightness by multiplying RGB channels by factor (>0). Clamps to 0-255.
     */
    public static Color adjustBrightness(Color c, double factor) {
        return ColorRegistry.adjustBrightness(c, factor);
    }

    /**
     * Returns BLACK or WHITE depending on which contrasts better with the input color.
     */
    public static Color getContrastColor(Color c) {
        // Perceived luminance (Rec. 709)
        double lum = (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
        return (lum > 0.5) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Linearly interpolate between two colors in RGBA space.
     */
    public static Color interpolate(Color a, Color b, float t) {
        return ColorRegistry.interpolate(a, b, t);
    }
}
