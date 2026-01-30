package com.arbergashi.charts.domain.legend;
/**
 * Standard positions for overlay legends.
 *
 * <p>This enum is used by legend overlays to compute their anchor point and apply padding.
 * The default position is typically {@link #TOP_LEFT}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Platform-independent and headless-certified. No AWT/Swing dependencies.
 *
 */
public enum LegendPosition {
    /** Top-left corner of the chart area. */
    TOP_LEFT,
    /** Top-center of the chart area. */
    TOP_CENTER,
    /** Top-right corner of the chart area. */
    TOP_RIGHT,

    /** Middle-left of the chart area. */
    MIDDLE_LEFT,
    /** Center of the chart area. Use with care; may cover data. */
    CENTER,
    /** Middle-right of the chart area. */
    MIDDLE_RIGHT,

    /** Bottom-left corner of the chart area. */
    BOTTOM_LEFT,
    /** Bottom-center of the chart area. */
    BOTTOM_CENTER,
    /** Bottom-right corner of the chart area. */
    BOTTOM_RIGHT;

    /**
     * Parses common string tokens (used by theme/property-based configuration).
     *
     * @param raw string token such as "top-left" or "BOTTOM_RIGHT"
     * @param fallback fallback when the token is null/blank or cannot be parsed
     * @return parsed position
     */
    public static LegendPosition parse(String raw, LegendPosition fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String norm = raw.trim().toUpperCase().replace('-', '_');
        try {
            return LegendPosition.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public String getName() {
        return name();
    }

    // Placement logic lives in render.legend.LegendLayoutTransformer.
}
