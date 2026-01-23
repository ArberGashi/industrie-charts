package com.arbergashi.charts.api;

import java.awt.*;

/**
 * Central access point for the framework's built-in theme defaults.
 *
 * <p><b>Framework contract:</b> Core rendering code must never depend on demo resources
 * or application-specific Look&amp;Feel state. Instead it should request a theme from the
 * active {@link PlotContext} and only fall back to the stable defaults exposed here.</p>
 *
 * <p>This class intentionally provides only a small set of stable defaults.
 * Applications are expected to supply their own {@link ChartTheme} (e.g. derived from
 * FlatLaf/UI defaults) via the chart panel / builder APIs.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartThemes {

    private ChartThemes() {
        // utility class
    }

    // Stable fallback themes (no external resource / demo dependencies).
    private static final ChartTheme DEFAULT_DARK = new BasicChartTheme(
            new Color(30, 30, 30),     // background
            new Color(220, 220, 220),  // foreground
            new Color(85, 85, 85),     // grid
            new Color(170, 170, 170),  // axis label
            new Color(0x3B82F6),       // accent (blue)
            new Color[]{
                    new Color(0x3B82F6), new Color(0x22C55E), new Color(0xF59E0B),
                    new Color(0xA855F7), new Color(0x06B6D4), new Color(0xEF4444)
            },
            null
    );

    private static final ChartTheme DEFAULT_LIGHT = new BasicChartTheme(
            new Color(250, 250, 250),  // background
            new Color(20, 20, 20),     // foreground
            new Color(200, 200, 200),  // grid
            new Color(80, 80, 80),     // axis label
            new Color(0x2563EB),       // accent
            new Color[]{
                    new Color(0x2563EB), new Color(0x16A34A), new Color(0xD97706),
                    new Color(0x7C3AED), new Color(0x0891B2), new Color(0xDC2626)
            },
            null
    );

    /**
     * The default dark theme used as a stable fallback.
     */
    public static ChartTheme defaultDark() {
        return DEFAULT_DARK;
    }

    /**
     * The default light theme used as a stable fallback.
     */
    public static ChartTheme defaultLight() {
        return DEFAULT_LIGHT;
    }
}
