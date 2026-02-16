package com.arbergashi.charts.api;

import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
/**
 * Applies global theme profiles via ChartAssets and returns a ChartTheme.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class ChartThemeProfiles {
    private ChartThemeProfiles() {
    }

    public static ChartTheme getProfileTheme(ChartThemeProfile profile) {
        if (profile == null) {
            return ChartThemes.getDarkTheme();
        }

        switch (profile) {
            case TACTICAL_DARK -> {
                ChartAssets.setProperty("Chart.tooltip.snap", "true");
                ChartAssets.setProperty("Chart.tooltip.decimals", "3");
                ChartAssets.setProperty("Chart.legend.cornerRadius", "0");
                ChartAssets.setProperty("Chart.legend.density", "compact");
                BasicChartTheme.Builder tactical = BasicChartTheme.builder();
                tactical.setBackground(com.arbergashi.charts.util.ColorRegistry.of(5, 5, 5, 255));
                tactical.setForeground(com.arbergashi.charts.util.ColorRegistry.of(230, 230, 230, 255));
                tactical.setGridColor(com.arbergashi.charts.util.ColorRegistry.of(40, 40, 40, 255));
                tactical.setAxisLabelColor(com.arbergashi.charts.util.ColorRegistry.of(200, 200, 200, 255));
                tactical.setAccentColor(com.arbergashi.charts.util.ColorRegistry.of(0, 255, 204, 255));
                tactical.setSeriesColors(new ArberColor[]{
                        com.arbergashi.charts.util.ColorRegistry.of(0, 255, 204, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(255, 180, 0, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(0, 170, 255, 255)
                });
                tactical.setBaseFont(new ArberFont("SansSerif", ArberFont.PLAIN, 11));
                return tactical.build();
            }
            case ACADEMIC_PAPER -> {
                ChartAssets.setProperty("Chart.tooltip.snap", "true");
                ChartAssets.setProperty("Chart.tooltip.decimals", "4");
                ChartAssets.setProperty("Chart.legend.cornerRadius", "0");
                ChartAssets.setProperty("Chart.legend.density", "dense");
                BasicChartTheme.Builder academic = BasicChartTheme.builder();
                academic.setBackground(com.arbergashi.charts.util.ColorRegistry.of(255, 255, 255, 255));
                academic.setForeground(com.arbergashi.charts.util.ColorRegistry.of(0, 0, 0, 255));
                academic.setGridColor(com.arbergashi.charts.util.ColorRegistry.of(180, 180, 180, 255));
                academic.setAxisLabelColor(com.arbergashi.charts.util.ColorRegistry.of(80, 80, 80, 255));
                academic.setAccentColor(com.arbergashi.charts.util.ColorRegistry.of(0, 0, 0, 255));
                academic.setSeriesColors(new ArberColor[]{
                        com.arbergashi.charts.util.ColorRegistry.of(0, 0, 0, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(60, 60, 60, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(120, 120, 120, 255)
                });
                academic.setBaseFont(new ArberFont("Serif", ArberFont.PLAIN, 11));
                return academic.build();
            }
            case MODERN_ENTERPRISE -> {
                ChartAssets.setProperty("Chart.tooltip.snap", "false");
                ChartAssets.setProperty("Chart.tooltip.decimals", "3");
                ChartAssets.setProperty("Chart.legend.cornerRadius", "0");
                ChartAssets.setProperty("Chart.legend.density", "compact");
                BasicChartTheme.Builder modern = BasicChartTheme.builder();
                modern.setBackground(com.arbergashi.charts.util.ColorRegistry.of(245, 247, 250, 255));
                modern.setForeground(com.arbergashi.charts.util.ColorRegistry.of(30, 30, 30, 255));
                modern.setGridColor(com.arbergashi.charts.util.ColorRegistry.of(208, 213, 221, 255));
                modern.setAxisLabelColor(com.arbergashi.charts.util.ColorRegistry.of(90, 90, 90, 255));
                modern.setAccentColor(com.arbergashi.charts.util.ColorRegistry.of(37, 99, 235, 255));
                modern.setSeriesColors(new ArberColor[]{
                        com.arbergashi.charts.util.ColorRegistry.of(37, 99, 235, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(16, 185, 129, 255),
                        com.arbergashi.charts.util.ColorRegistry.of(239, 68, 68, 255)
                });
                modern.setBaseFont(new ArberFont("SansSerif", ArberFont.PLAIN, 11));
                return modern.build();
            }
        }

        return ChartThemes.getDarkTheme();
    }
}
