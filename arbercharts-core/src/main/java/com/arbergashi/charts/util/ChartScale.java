package com.arbergashi.charts.util;

import java.awt.GraphicsConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * application startup (or call {@link #autoDetect(GraphicsConfiguration)} once from the primary window).
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
     * Auto-detects the scale factor from the given {@link GraphicsConfiguration}.
     *
     * <p><b>Framework policy:</b> This method initializes the scale factor only once.
     * The first call with a non-null {@code GraphicsConfiguration} wins. Subsequent calls are ignored
     * to avoid global scale thrash when windows move across monitors.</p>
     */
    public static void autoDetect(GraphicsConfiguration gc) {
        if (gc == null) {
            // Only initialize to 1.0 if nothing has been set yet.
            if (autoDetected.compareAndSet(false, true)) {
                scaleFactor = 1.0f;
            }
            return;
        }

        if (!autoDetected.compareAndSet(false, true)) {
            return;
        }

        double s = gc.getDefaultTransform().getScaleX();
        if (!Double.isFinite(s) || s <= 0.0) {
            scaleFactor = 1.0f;
        } else {
            scaleFactor = (float) s;
        }
    }

    /**
     * Returns the currently active global scale factor.
     */
    public static float getScaleFactor() {
        return scaleFactor;
    }

    public static float scale(float value) {
        return value * scaleFactor;
    }

    public static double scale(double value) {
        return value * scaleFactor;
    }

    /**
     * Scales font sizes.
     * Fonts often require slightly different scaling than geometric shapes.
     */
    public static float font(float size) {
        return size * scaleFactor;
    }

    /**
     * Scales a target font size in a UI-toolkit-aware way.
     * <p>
     * <b>Why:</b> In modern Swing applications (e.g. FlatLaf), {@link javax.swing.UIManager} fonts are already
     * HiDPI-aware and may already include the platform scale factor. If we then apply {@link #font(float)}
     * again, text becomes double-scaled.
     * </p>
     * <p>
     * <b>Policy:</b> If the provided {@code baseFont} looks like a UI-managed font (i.e. its size already
     * tracks the UI scale), we treat {@code sizeInDp} as the final point size. Otherwise we apply the
     * framework scale factor.
     * </p>
     *
     * @param baseFont  the font used as baseline; may be {@code null}
     * @param sizeInDp  desired size in design units (dp)
     * @return a size value suitable for {@link java.awt.Font#deriveFont(float)}
     */
    public static float uiFontSize(java.awt.Font baseFont, float sizeInDp) {
        if (baseFont == null) {
            return font(sizeInDp);
        }

        // Heuristic: UI-managed fonts are typically already scaled on HiDPI setups.
        // If the UI font size is already "large" relative to the requested dp size
        // (e.g. 13pt UI font on a 2x scale system), do not scale again.
        float uiSize = baseFont.getSize2D();
        if (scaleFactor > 1.01f && uiSize >= sizeInDp * 1.15f) {
            return sizeInDp;
        }

        return font(sizeInDp);
    }
}