package com.arbergashi.charts.api;

import java.awt.*;

/**
 * Test-only theme definitions.
 *
 * <p>Rationale: {@link ChartThemes} intentionally exposes only stable framework defaults. Some
 * unit tests need additional specialty themes (e.g., high-contrast) without making them part
 * of the public API surface.</p>
 */
final class TestThemes {

    private TestThemes() {
        // utility
    }

    static ChartTheme highContrast() {
        return BasicChartTheme.builder()
                .background(Color.BLACK)
                .foreground(Color.WHITE)
                .gridColor(new Color(255, 255, 255, 80))
                .axisLabelColor(Color.WHITE)
                .accentColor(Color.YELLOW)
                .seriesColors(new Color[]{
                        Color.YELLOW,
                        new Color(0, 255, 255),
                        new Color(255, 0, 255),
                        new Color(0, 255, 0),
                        new Color(255, 128, 0)
                })
                .baseFont(new Font("SansSerif", Font.BOLD, 12))
                .build();
    }

    static ChartTheme tailwindEmerald() {
        return BasicChartTheme.builder()
                .background(new Color(255, 255, 255))
                .foreground(new Color(17, 24, 39))
                .gridColor(new Color(226, 232, 240))
                .axisLabelColor(new Color(55, 65, 81))
                .accentColor(new Color(16, 185, 129))
                .seriesColors(new Color[]{
                        new Color(16, 185, 129),
                        new Color(59, 130, 246),
                        new Color(234, 88, 12),
                        new Color(168, 85, 247),
                        new Color(239, 68, 68),
                        new Color(14, 116, 144)
                })
                .baseFont(new Font("SansSerif", Font.PLAIN, 11))
                .build();
    }

    static ChartTheme tailwindSlateDark() {
        return BasicChartTheme.builder()
                .background(new Color(15, 23, 42))
                .foreground(new Color(226, 232, 240))
                .gridColor(new Color(51, 65, 85))
                .axisLabelColor(new Color(203, 213, 225))
                .accentColor(new Color(56, 189, 248))
                .seriesColors(new Color[]{
                        new Color(56, 189, 248),
                        new Color(16, 185, 129),
                        new Color(249, 115, 22),
                        new Color(244, 63, 94),
                        new Color(167, 139, 250),
                        new Color(250, 204, 21)
                })
                .baseFont(new Font("SansSerif", Font.PLAIN, 11))
                .build();
    }
}
