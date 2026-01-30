package com.arbergashi.charts.util;
/**
 * High-performance animation and interpolation utilities for ArberCharts.
 *
 * <p>Design goals:
 * <ul>
 *   <li>EDT-friendly: pure functions, no global mutable state.</li>
 *   <li>Stable timing: use {@link System#nanoTime()} for progress computation.</li>
 *   <li>Small API surface: only what is required by the framework.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class AnimationUtils {

    private AnimationUtils() {
        throw new AssertionError("Utility class");
    }

    // ========================================
    // INTERPOLATION
    // ========================================

    /**
     * Linear interpolation (lerp).
     *
     * @param start    start value
     * @param end      end value
     * @param fraction interpolation factor in [0..1]
     */
    public static double lerp(double start, double end, double fraction) {
        double t = MathUtils.clamp01(fraction);
        return Math.fma(end - start, t, start);
    }

    /**
     * Float lerp.
     */
    public static float lerp(float start, float end, float fraction) {
        float t = MathUtils.clamp01(fraction);
        return Math.fma(end - start, t, start);
    }

    /**
     * Int lerp (rounded).
     */
    public static int lerp(int start, int end, double fraction) {
        return (int) Math.round(lerp(start, (double) end, fraction));
    }

    // ========================================
    // EASING
    // ========================================

    /**
     * Cubic ease-in-out.
     */
    public static double easeInOut(double fraction) {
        double t = MathUtils.clamp01(fraction);
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * Quadratic ease-in-out.
     */
    public static double easeInOutQuad(double fraction) {
        double t = MathUtils.clamp01(fraction);
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    /**
     * Exponential ease-in-out.
     */
    public static double easeInOutExpo(double fraction) {
        double t = MathUtils.clamp01(fraction);
        if (t == 0.0) return 0.0;
        if (t == 1.0) return 1.0;
        return t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
    }

    /**
     * Sine ease-in-out.
     */
    public static double easeInOutSine(double fraction) {
        double t = MathUtils.clamp01(fraction);
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    /**
     * Monotonic timestamp suitable for animations.
     */
    public static long nowNanos() {
        return System.nanoTime();
    }

    // ========================================
    // TIME / PROGRESS
    // ========================================

    /**
     * Progress based on {@link System#nanoTime()}.
     */
    public static double getProgressNanos(long startNanos, long durationMillis) {
        if (durationMillis <= 0L) return 1.0;

        long durationNanos = durationMillis * 1_000_000L;
        if (durationNanos <= 0L) return 1.0; // overflow guard

        long elapsed = System.nanoTime() - startNanos;
        return MathUtils.clamp01((double) elapsed / (double) durationNanos);
    }

    /**
     * Returns true once progress reaches 1.0.
     */
    public static boolean isFinishedNanos(long startNanos, long durationMillis) {
        return getProgressNanos(startNanos, durationMillis) >= 1.0;
    }

    /**
     * Convenience: cubic eased progress.
     */
    public static double getEasedProgressNanos(long startNanos, long durationMillis) {
        return easeInOut(getProgressNanos(startNanos, durationMillis));
    }

    /**
     * Convenience: eased progress with selectable easing.
     */
    public static double getEasedProgressNanos(long startNanos, long durationMillis, EasingType easingType) {
        double progress = getProgressNanos(startNanos, durationMillis);
        return (easingType != null ? easingType : EasingType.EASE_IN_OUT_CUBIC).apply(progress);
    }

    /**
     * Easing function selector.
     */
    public enum EasingType {
        /**
         * Linear.
         */
        LINEAR(t -> t),
        /**
         * Cubic ease-in-out (default).
         */
        EASE_IN_OUT_CUBIC(AnimationUtils::easeInOut),
        /**
         * Quadratic ease-in-out.
         */
        EASE_IN_OUT_QUAD(AnimationUtils::easeInOutQuad),
        /**
         * Exponential ease-in-out.
         */
        EASE_IN_OUT_EXPO(AnimationUtils::easeInOutExpo),
        /**
         * Sine ease-in-out.
         */
        EASE_IN_OUT_SINE(AnimationUtils::easeInOutSine);

        private final java.util.function.DoubleUnaryOperator fn;

        EasingType(java.util.function.DoubleUnaryOperator fn) {
            this.fn = fn;
        }

        public double apply(double fraction) {
            return fn.applyAsDouble(fraction);
        }
    }

}
