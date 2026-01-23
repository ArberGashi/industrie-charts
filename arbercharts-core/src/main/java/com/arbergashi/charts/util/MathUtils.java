package com.arbergashi.charts.util;

/**
 * Math utilities.
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class MathUtils {

    private MathUtils() {
    }

    /**
     * Constraints a value to the interval [min, max].
     * (Wrapper for Math.clamp starting from JDK 21, here as fallback/alias).
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Clamps value into [0,1] range (double version).
     */
    public static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    /**
     * Clamps value into [0,1] range (float version).
     */
    public static float clamp01(float v) {
        return (float) clamp(v, 0.0, 1.0);
    }
}