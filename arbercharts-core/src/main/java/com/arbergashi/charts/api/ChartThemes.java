package com.arbergashi.charts.api;

import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.util.ColorRegistry;
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
            ColorRegistry.of(30, 30, 30, 255),     // background
            ColorRegistry.of(220, 220, 220, 255),  // foreground
            ColorRegistry.of(85, 85, 85, 255),     // grid
            ColorRegistry.of(170, 170, 170, 255),  // axis label
            ColorRegistry.ofArgb(0xFF3B82F6),      // accent (blue)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF3B82F6), ColorRegistry.ofArgb(0xFF22C55E), ColorRegistry.ofArgb(0xFFF59E0B),
                    ColorRegistry.ofArgb(0xFFA855F7), ColorRegistry.ofArgb(0xFF06B6D4), ColorRegistry.ofArgb(0xFFEF4444)
            },
            null
    );

    private static final ChartTheme DEFAULT_LIGHT = new BasicChartTheme(
            ColorRegistry.of(250, 250, 250, 255),  // background
            ColorRegistry.of(20, 20, 20, 255),     // foreground
            ColorRegistry.of(200, 200, 200, 255),  // grid
            ColorRegistry.of(80, 80, 80, 255),     // axis label
            ColorRegistry.ofArgb(0xFF2563EB),      // accent
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF2563EB), ColorRegistry.ofArgb(0xFF16A34A), ColorRegistry.ofArgb(0xFFD97706),
                    ColorRegistry.ofArgb(0xFF7C3AED), ColorRegistry.ofArgb(0xFF0891B2), ColorRegistry.ofArgb(0xFFDC2626)
            },
            null
    );

    private static boolean hasAssetOverrides() {
        return com.arbergashi.charts.util.ChartAssets.getString("Chart.background", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.foreground", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.grid.color", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.axisLabelColor", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.accentColor", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.accent.blue", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series1", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series2", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series3", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series4", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series5", null) != null;
    }

    private static ChartTheme buildFromAssets(ChartTheme fallback) {
        if (!hasAssetOverrides()) {
            return fallback;
        }
        com.arbergashi.charts.util.ChartAssets.clearCache();
        ArberColor background = com.arbergashi.charts.util.ChartAssets.getColor("Chart.background", fallback.getBackground());
        ArberColor foreground = com.arbergashi.charts.util.ChartAssets.getColor("Chart.foreground", fallback.getForeground());
        ArberColor grid = com.arbergashi.charts.util.ChartAssets.getColor("Chart.grid.color", fallback.getGridColor());
        ArberColor axis = com.arbergashi.charts.util.ChartAssets.getColor("Chart.axisLabelColor", fallback.getAxisLabelColor());
        ArberColor accent = com.arbergashi.charts.util.ChartAssets.getColor("Chart.accentColor",
                com.arbergashi.charts.util.ChartAssets.getColor("Chart.accent.blue", fallback.getAccentColor()));
        ArberColor[] series = new ArberColor[]{
                com.arbergashi.charts.util.ChartAssets.getColor("series1", fallback.getSeriesColor(0)),
                com.arbergashi.charts.util.ChartAssets.getColor("series2", fallback.getSeriesColor(1)),
                com.arbergashi.charts.util.ChartAssets.getColor("series3", fallback.getSeriesColor(2)),
                com.arbergashi.charts.util.ChartAssets.getColor("series4", fallback.getSeriesColor(3)),
                com.arbergashi.charts.util.ChartAssets.getColor("series5", fallback.getSeriesColor(4))
        };
        return new BasicChartTheme(background, foreground, grid, axis, accent, series, null);
    }

    /**
     * The default dark theme used as a stable fallback.
     */
    public static ChartTheme getDarkTheme() {
        return buildFromAssets(DEFAULT_DARK);
    }

    /**
     * The default light theme used as a stable fallback.
     */
    public static ChartTheme getLightTheme() {
        return buildFromAssets(DEFAULT_LIGHT);
    }
}
