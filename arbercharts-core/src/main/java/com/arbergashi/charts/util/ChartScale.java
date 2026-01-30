package com.arbergashi.charts.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
/**
 * Utility for DPI scaling.
 * Ensures that lines and fonts look correct on high-DPI (e.g., 4K / Retina) monitors.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartScale {

    private static volatile float scaleFactor = 1.0f;
    private static final ThreadLocal<Float> renderScaleOverride = new ThreadLocal<>();

    /**
     * Framework policy: auto-detection initializes the scale factor once.
     *
     * <p>Rationale: {@link GraphicsConfiguration} can change when windows move across monitors.
     * If we updated the global scale factor on every event, components in the same JVM could
     * suddenly change size. For a framework, deterministic behavior is more important than
     * perfectly matching per-monitor DPI in a global scaling model.</p>
     */
    private static final AtomicBoolean autoDetected = new AtomicBoolean(false);

    private ChartScale() {
    }

    /**
     * Sets the global scale factor used by the chart framework.
     *
     * <p><b>Framework note:</b> This is a process-wide setting. Prefer setting it once during
     * application startup (or call {@link #autoDetect(float)} once from the primary window).
     * This method always overrides any previously auto-detected value.</p>
     */
    public static void setScaleFactor(float factor) {
        if (!Float.isFinite(factor) || factor <= 0f) {
            scaleFactor = 1.0f;
        } else {
            scaleFactor = factor;
        }

        // Explicit override should prevent later auto-detection from changing the value.
        autoDetected.set(true);
    }

    /**
     * Auto-detects the scale factor from a provided scale value.
     *
     * <p><b>Framework policy:</b> This method initializes the scale factor only once.
     * The first call with a non-null {@code GraphicsConfiguration} wins. Subsequent calls are ignored
     * to avoid global scale thrash when windows move across monitors.</p>
     */
    public static void autoDetect(float detectedScale) {
        if (!Float.isFinite(detectedScale) || detectedScale <= 0f) {
            // Only initialize to 1.0 if nothing has been set yet.
            if (autoDetected.compareAndSet(false, true)) {
                scaleFactor = 1.0f;
            }
            return;
        }

        if (!autoDetected.compareAndSet(false, true)) {
            return;
        }

        scaleFactor = detectedScale;
    }

    /**
     * Returns the currently active scale factor for the calling thread.
     */
    public static float getScaleFactor() {
        return effectiveScale();
    }

    /**
     * Returns true if a thread-local scale override is currently active.
     */
    public static boolean hasScaleOverride() {
        return renderScaleOverride.get() != null;
    }

    public static float scale(float value) {
        return value * effectiveScale();
    }

    public static double scale(double value) {
        return value * effectiveScale();
    }

    /**
     * Scales font sizes.
     * Fonts often require slightly different scaling than geometric shapes.
     */
    public static float font(float size) {
        return size * effectiveScale();
    }

    /**
     * Updates the global scale factor from a provided scale value.
     *
     * @param detectedScale detected scale factor
     * @return the applied scale factor
     */
    public static float setScaleFromDetected(float detectedScale) {
        float detected = (Float.isFinite(detectedScale) && detectedScale > 0f) ? detectedScale : 1.0f;
        if (Math.abs(detected - scaleFactor) > 0.001f) {
            scaleFactor = detected;
        }
        autoDetected.set(true);
        return detected;
    }

    /**
     * Runs the given block with a thread-local scale override.
     */
    public static void applyScale(float scale, Runnable block) {
        renderScaleOverride.set(scale);
        try {
            block.run();
        } finally {
            renderScaleOverride.remove();
        }
    }

    /**
     * Runs the given block with a thread-local scale override and returns a value.
     */
    public static <T> T applyScale(float scale, Supplier<T> block) {
        renderScaleOverride.set(scale);
        try {
            return block.get();
        } finally {
            renderScaleOverride.remove();
        }
    }


    private static float effectiveScale() {
        Float override = renderScaleOverride.get();
        return override != null ? override : scaleFactor;
    }

}
