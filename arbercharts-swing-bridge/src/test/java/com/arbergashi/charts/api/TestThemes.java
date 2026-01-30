package com.arbergashi.charts.api;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.util.ColorRegistry;

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
                .setBackground(ColorRegistry.of(0, 0, 0, 255))
                .setForeground(ColorRegistry.of(255, 255, 255, 255))
                .setGridColor(ColorRegistry.of(255, 255, 255, 80))
                .setAxisLabelColor(ColorRegistry.of(255, 255, 255, 255))
                .setAccentColor(ColorRegistry.of(255, 255, 0, 255))
                .setSeriesColors(new ArberColor[]{
                        ColorRegistry.of(255, 255, 0, 255),
                        ColorRegistry.of(0, 255, 255, 255),
                        ColorRegistry.of(255, 0, 255, 255),
                        ColorRegistry.of(0, 255, 0, 255),
                        ColorRegistry.of(255, 128, 0, 255)
                })
                .setBaseFont(new ArberFont("SansSerif", ArberFont.BOLD, 12))
                .build();
    }

    static ChartTheme tailwindEmerald() {
        return BasicChartTheme.builder()
                .setBackground(ColorRegistry.of(255, 255, 255, 255))
                .setForeground(ColorRegistry.of(17, 24, 39, 255))
                .setGridColor(ColorRegistry.of(226, 232, 240, 255))
                .setAxisLabelColor(ColorRegistry.of(55, 65, 81, 255))
                .setAccentColor(ColorRegistry.of(16, 185, 129, 255))
                .setSeriesColors(new ArberColor[]{
                        ColorRegistry.of(16, 185, 129, 255),
                        ColorRegistry.of(59, 130, 246, 255),
                        ColorRegistry.of(234, 88, 12, 255),
                        ColorRegistry.of(168, 85, 247, 255),
                        ColorRegistry.of(239, 68, 68, 255),
                        ColorRegistry.of(14, 116, 144, 255)
                })
                .setBaseFont(new ArberFont("SansSerif", ArberFont.PLAIN, 11))
                .build();
    }

    static ChartTheme tailwindSlateDark() {
        return BasicChartTheme.builder()
                .setBackground(ColorRegistry.of(15, 23, 42, 255))
                .setForeground(ColorRegistry.of(226, 232, 240, 255))
                .setGridColor(ColorRegistry.of(51, 65, 85, 255))
                .setAxisLabelColor(ColorRegistry.of(203, 213, 225, 255))
                .setAccentColor(ColorRegistry.of(56, 189, 248, 255))
                .setSeriesColors(new ArberColor[]{
                        ColorRegistry.of(56, 189, 248, 255),
                        ColorRegistry.of(16, 185, 129, 255),
                        ColorRegistry.of(249, 115, 22, 255),
                        ColorRegistry.of(244, 63, 94, 255),
                        ColorRegistry.of(167, 139, 250, 255),
                        ColorRegistry.of(250, 204, 21, 255)
                })
                .setBaseFont(new ArberFont("SansSerif", ArberFont.PLAIN, 11))
                .build();
    }
}
