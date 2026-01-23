package com.arbergashi.charts.ui.legend;

import java.awt.*;

/**
 * Standard positions for overlay legends.
 *
 * <p>This enum is used by legend overlays to compute their anchor point and apply padding.
 * The default position is typically {@link #TOP_LEFT}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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

    /**
     * Computes a legend rectangle anchored within the given bounds.
     *
     * <p>The returned rectangle is clamped so it always stays fully inside {@code bounds}.</p>
     *
     * @param bounds the available area (typically the component bounds)
     * @param size the preferred legend size
     * @param padding padding from the edges (applied relative to the anchor)
     * @return legend rectangle, clamped to bounds
     */
    public Rectangle place(Rectangle bounds, Dimension size, Insets padding) {
        if (bounds == null) bounds = new Rectangle(0, 0, 0, 0);
        if (size == null) size = new Dimension(0, 0);
        if (padding == null) padding = new Insets(0, 0, 0, 0);

        int w = Math.max(0, Math.min(size.width, bounds.width));
        int h = Math.max(0, Math.min(size.height, bounds.height));

        int x;
        int y;

        // horizontal
        switch (this) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> x = bounds.x + (bounds.width - w) / 2;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> x = bounds.x + bounds.width - w;
            default -> x = bounds.x;
        }

        // vertical
        switch (this) {
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> y = bounds.y + (bounds.height - h) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> y = bounds.y + bounds.height - h;
            default -> y = bounds.y;
        }

        // apply padding relative to the anchor direction
        boolean isLeft = (this == TOP_LEFT || this == MIDDLE_LEFT || this == BOTTOM_LEFT);
        boolean isRight = (this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT);
        boolean isTop = (this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT);
        boolean isBottom = (this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT);

        if (isLeft) x += padding.left;
        if (isRight) x -= padding.right;
        if (!isLeft && !isRight) x += (padding.left - padding.right) / 2;

        if (isTop) y += padding.top;
        if (isBottom) y -= padding.bottom;
        if (!isTop && !isBottom) y += (padding.top - padding.bottom) / 2;

        // clamp
        x = Math.max(bounds.x, Math.min(x, bounds.x + bounds.width - w));
        y = Math.max(bounds.y, Math.min(y, bounds.y + bounds.height - h));

        return new Rectangle(x, y, w, h);
    }
}
