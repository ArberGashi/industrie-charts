package com.arbergashi.charts.util;

import com.arbergashi.charts.api.types.ArberColor;
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
    public static ArberColor applyAlpha(ArberColor c, float alpha) {
        if (c == null) return null;
        return ColorRegistry.applyAlpha(c, alpha);
    }

    /**
     * Adjust brightness by multiplying RGB channels by factor (>0). Clamps to 0-255.
     */
    public static ArberColor adjustBrightness(ArberColor c, double factor) {
        if (c == null) return null;
        return ColorRegistry.adjustBrightness(c, factor);
    }

    /**
     * Returns BLACK or WHITE depending on which contrasts better with the input color.
     */
    public static ArberColor getContrastColor(ArberColor c) {
        if (c == null) return null;
        double lum = (0.2126 * c.red() + 0.7152 * c.green() + 0.0722 * c.blue()) / 255.0;
        return (lum > 0.5) ? ColorRegistry.ofArgb(0xFFFFFFFF) : ColorRegistry.ofArgb(0xFF000000);
    }

    /**
     * Linearly interpolate between two colors in RGBA space.
     */
    public static ArberColor interpolate(ArberColor a, ArberColor b, float t) {
        if (a == null && b == null) return null;
        return ColorRegistry.interpolate(a, b, t);
    }
}
