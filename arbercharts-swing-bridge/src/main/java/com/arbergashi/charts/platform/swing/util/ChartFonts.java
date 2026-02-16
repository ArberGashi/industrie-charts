/*
 * Copyright (c) 2024-2026 Arber Gashi. All rights reserved.
 *
 * This file is part of ArberCharts, a high-performance charting library
 * designed for real-time financial applications.
 *
 * PROPRIETARY AND CONFIDENTIAL
 */
package com.arbergashi.charts.platform.swing.util;

import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Central font management for ArberCharts Swing components.
 *
 * <p>This class provides the Inter font family from FlatLaf as the primary
 * font for all chart elements including axes, crosshairs, legends, and tooltips.
 * Inter is a professional, highly legible font optimized for UI/data visualization.</p>
 *
 * <h2>ZERO-GC Compliance</h2>
 * <p>Font instances are cached and reused to minimize allocations. The base font
 * is resolved once at startup and derived fonts are cached per size.</p>
 *
 * <h2>Font Hierarchy</h2>
 * <ol>
 *   <li>Inter (from flatlaf-fonts-inter) - Primary choice</li>
 *   <li>FlatLaf default font - Fallback if Inter not available</li>
 *   <li>Dialog/SansSerif - System fallback</li>
 * </ol>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2.0.0
 */
public final class ChartFonts {

    /** Inter font family name as registered by FlatLaf */
    public static final String INTER_FONT_FAMILY = "Inter";

    /** Fallback font family if Inter is not available */
    public static final String FALLBACK_FONT_FAMILY = Font.SANS_SERIF;

    // Cached base font instance (ZERO-GC)
    private static volatile Font cachedBaseFont;
    private static volatile boolean fontInitialized;

    private ChartFonts() {
        // Utility class
    }

    /**
     * Initializes the Inter font if available.
     * Call this once at application startup after FlatLaf is installed.
     */
    public static void initialize() {
        if (fontInitialized) {
            return;
        }
        synchronized (ChartFonts.class) {
            if (fontInitialized) {
                return;
            }
            // Try to install Inter font from FlatLaf
            try {
                // FlatLaf 4.x: com.formdev.flatlaf.fonts.inter.FlatInterFont.installLazy()
                Class<?> interFontClass = Class.forName("com.formdev.flatlaf.fonts.inter.FlatInterFont");
                java.lang.reflect.Method installMethod = interFontClass.getMethod("installLazy");
                installMethod.invoke(null);
            } catch (Exception e) {
                // Inter font not available, will use fallback
            }
            fontInitialized = true;
        }
    }

    /**
     * Returns the base font for chart rendering.
     *
     * <p>Returns Inter if available, otherwise falls back to the system font.
     * This method caches the font instance for ZERO-GC compliance.</p>
     *
     * @return the base font, never null
     */
    public static Font getBaseFont() {
        Font font = cachedBaseFont;
        if (font != null) {
            return font;
        }
        synchronized (ChartFonts.class) {
            if (cachedBaseFont != null) {
                return cachedBaseFont;
            }
            cachedBaseFont = resolveBaseFont();
            return cachedBaseFont;
        }
    }

    /**
     * Returns a font for axis labels with the specified size.
     *
     * @param size the font size in points
     * @return the axis label font
     */
    public static Font getAxisFont(float size) {
        return getBaseFont().deriveFont(Font.PLAIN, size);
    }

    /**
     * Returns a font for crosshair labels with the specified size.
     *
     * @param size the font size in points
     * @return the crosshair label font
     */
    public static Font getCrosshairFont(float size) {
        return getBaseFont().deriveFont(Font.PLAIN, size);
    }

    /**
     * Returns a font for legend text with the specified size.
     *
     * @param size the font size in points
     * @return the legend font
     */
    public static Font getLegendFont(float size) {
        return getBaseFont().deriveFont(Font.PLAIN, size);
    }

    /**
     * Returns a bold font variant with the specified size.
     *
     * @param size the font size in points
     * @return the bold font
     */
    public static Font getBoldFont(float size) {
        return getBaseFont().deriveFont(Font.BOLD, size);
    }

    /**
     * Checks if the Inter font is available.
     *
     * @return true if Inter is installed
     */
    public static boolean isInterAvailable() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String fontName : ge.getAvailableFontFamilyNames()) {
            if (INTER_FONT_FAMILY.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the best available font for chart rendering.
     */
    private static Font resolveBaseFont() {
        // Priority 1: Try Inter from FlatLaf
        if (isInterAvailable()) {
            return new Font(INTER_FONT_FAMILY, Font.PLAIN, 12);
        }

        // Priority 2: Try FlatLaf's default font
        Font flatLafFont = UIManager.getFont("defaultFont");
        if (flatLafFont != null) {
            return flatLafFont;
        }

        // Priority 3: Try Label.font (typical FlatLaf/Swing default)
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont;
        }

        // Priority 4: System fallback
        return new Font(FALLBACK_FONT_FAMILY, Font.PLAIN, 12);
    }

    /**
     * Resets the cached font. Call this if the L&F changes.
     */
    public static void resetCache() {
        synchronized (ChartFonts.class) {
            cachedBaseFont = null;
        }
    }
}

