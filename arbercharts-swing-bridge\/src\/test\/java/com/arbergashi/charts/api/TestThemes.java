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
                .setBackground(Color.BLACK)
                .setForeground(Color.WHITE)
                .setGridColor(new Color(255, 255, 255, 80))
                .setAxisLabelColor(Color.WHITE)
                .setAccentColor(Color.YELLOW)
                .setSeriesColors(new Color[]{
                        Color.YELLOW,
                        new Color(0, 255, 255),
                        new Color(255, 0, 255),
                        new Color(0, 255, 0),
                        new Color(255, 128, 0)
                })
                .setBaseFont(new Font("SansSerif", Font.BOLD, 12))
                .build();
    }

    static ChartTheme tailwindEmerald() {
        return BasicChartTheme.builder()
                .setBackground(new Color(255, 255, 255))
                .setForeground(new Color(17, 24, 39))
                .setGridColor(new Color(226, 232, 240))
                .setAxisLabelColor(new Color(55, 65, 81))
                .setAccentColor(new Color(16, 185, 129))
                .setSeriesColors(new Color[]{
                        new Color(16, 185, 129),
                        new Color(59, 130, 246),
                        new Color(234, 88, 12),
                        new Color(168, 85, 247),
                        new Color(239, 68, 68),
                        new Color(14, 116, 144)
                })
                .setBaseFont(new Font("SansSerif", Font.PLAIN, 11))
                .build();
    }

    static ChartTheme tailwindSlateDark() {
        return BasicChartTheme.builder()
                .setBackground(new Color(15, 23, 42))
                .setForeground(new Color(226, 232, 240))
                .setGridColor(new Color(51, 65, 85))
                .setAxisLabelColor(new Color(203, 213, 225))
                .setAccentColor(new Color(56, 189, 248))
                .setSeriesColors(new Color[]{
                        new Color(56, 189, 248),
                        new Color(16, 185, 129),
                        new Color(249, 115, 22),
                        new Color(244, 63, 94),
                        new Color(167, 139, 250),
                        new Color(250, 204, 21)
                })
                .setBaseFont(new Font("SansSerif", Font.PLAIN, 11))
                .build();
    }
}
