package com.arbergashi.charts.internal;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Central design system for ArberGashi Charts.
 * Abstracts access to UI keys and provides intelligent fallbacks.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-12-30
 */
public final class ChartStyle {

    /* Constants for UI keys - matching FlatDarkLaf.properties / FlatLightLaf.properties */
    public static final String KEY_GRID = "Chart.gridColor";
    public static final String KEY_ACCENT = "Chart.accentColor";
    public static final String KEY_BG = "Chart.background";
    public static final String KEY_FG = "Chart.foreground";
    public static final String KEY_TOOLTIP_BG = "Chart.tooltip.background";
    public static final String KEY_TOOLTIP_FG = "Chart.tooltip.foreground";

    private ChartStyle() {
        /* Utility class - no constructor needed */
    }

    /**
     * Returns a subtle gray with transparency as default when the property is missing.
     */
    public static Color getGridColor() {
        return getSafeColor(KEY_GRID, com.arbergashi.charts.util.ColorRegistry.of(128, 128, 128, 80));
    }

    /**
     * Falls back to the system/default accent color when the property is missing.
     */
    public static Color getAccentColor() {
        return getSafeColor(KEY_ACCENT, UIManager.getColor("Component.accentColor"));
    }

    public static Color getBackgroundColor() {
        return getSafeColor(KEY_BG, UIManager.getColor("Panel.background"));
    }

    public static Color getForegroundColor() {
        return getSafeColor(KEY_FG, UIManager.getColor("Panel.foreground"));
    }

    /**
     * Core logic for robustness. Now public so panels can safely handle specialized color logic (e.g. tooltips).
     */
    public static Color getSafeColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        if (color != null) return color;

        /* If UIManager provides nothing, use fallback; if fallback is null, return magenta as an explicit error signal */
        return Objects.requireNonNullElse(fallback, Color.MAGENTA);
    }
}
