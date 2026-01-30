package com.arbergashi.charts.api;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import java.util.Arrays;

/**
 * Basic implementation of ChartTheme.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class BasicChartTheme implements ChartTheme {
    private ArberColor background;
    private ArberColor foreground;
    private ArberColor gridColor;
    private ArberColor axisLabelColor;
    private ArberColor accentColor;
    private ArberColor[] seriesColors;
    private ArberFont baseFont;

    public BasicChartTheme(ArberColor background,
                           ArberColor foreground,
                           ArberColor gridColor,
                           ArberColor axisLabelColor,
                           ArberColor accentColor,
                           ArberColor[] seriesColors,
                           ArberFont baseFont) {
        this.background = background;
        this.foreground = foreground;
        this.gridColor = gridColor;
        this.axisLabelColor = axisLabelColor;
        this.accentColor = accentColor;
        this.seriesColors = seriesColors != null ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
        this.baseFont = baseFont;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ArberColor getBackground() {
        return background;
    }

    public BasicChartTheme setBackground(ArberColor background) {
        this.background = background;
        return this;
    }

    @Override
    public ArberColor getForeground() {
        return foreground;
    }

    public BasicChartTheme setForeground(ArberColor foreground) {
        this.foreground = foreground;
        return this;
    }

    @Override
    public ArberColor getGridColor() {
        return gridColor;
    }

    public BasicChartTheme setGridColor(ArberColor gridColor) {
        this.gridColor = gridColor;
        return this;
    }

    @Override
    public ArberColor getAxisLabelColor() {
        return axisLabelColor;
    }

    public BasicChartTheme setAxisLabelColor(ArberColor axisLabelColor) {
        this.axisLabelColor = axisLabelColor;
        return this;
    }

    @Override
    public ArberColor getAccentColor() {
        return accentColor;
    }

    public BasicChartTheme setAccentColor(ArberColor accentColor) {
        this.accentColor = accentColor;
        return this;
    }

    @Override
    public ArberColor getSeriesColor(int index) {
        if (seriesColors == null || seriesColors.length == 0) {
            return accentColor;
        }
        return seriesColors[Math.abs(index) % seriesColors.length];
    }

    public ArberColor[] getSeriesColors() {
        return seriesColors != null ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
    }

    public BasicChartTheme setSeriesColors(ArberColor[] seriesColors) {
        this.seriesColors = seriesColors != null ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
        return this;
    }

    @Override
    public ArberFont getBaseFont() {
        return baseFont;
    }

    public BasicChartTheme setBaseFont(ArberFont baseFont) {
        this.baseFont = baseFont;
        return this;
    }

    public static class Builder {
        private ArberColor background = com.arbergashi.charts.util.ColorRegistry.of(30, 30, 30, 255);
        private ArberColor foreground = com.arbergashi.charts.util.ColorRegistry.of(220, 220, 220, 255);
        private ArberColor gridColor = com.arbergashi.charts.util.ColorRegistry.of(60, 60, 60, 255);
        private ArberColor axisLabelColor = com.arbergashi.charts.util.ColorRegistry.of(180, 180, 180, 255);
        private ArberColor accentColor = com.arbergashi.charts.util.ColorRegistry.of(100, 149, 237, 255);
        private ArberColor[] seriesColors = new ArberColor[]{
                com.arbergashi.charts.util.ColorRegistry.of(100, 149, 237, 255),
                com.arbergashi.charts.util.ColorRegistry.of(255, 99, 71, 255),
                com.arbergashi.charts.util.ColorRegistry.of(50, 205, 50, 255)
        };
        private ArberFont baseFont = new ArberFont("SansSerif", ArberFont.PLAIN, 11);

        public Builder setBackground(ArberColor background) {
            this.background = background;
        return this;
        }

        public Builder setForeground(ArberColor foreground) {
            this.foreground = foreground;
        return this;
        }

        public Builder setGridColor(ArberColor gridColor) {
            this.gridColor = gridColor;
        return this;
        }

        public Builder setAxisLabelColor(ArberColor axisLabelColor) {
            this.axisLabelColor = axisLabelColor;
        return this;
        }

        public Builder setAccentColor(ArberColor accentColor) {
            this.accentColor = accentColor;
        return this;
        }

        public Builder setSeriesColors(ArberColor[] seriesColors) {
            this.seriesColors = seriesColors != null ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
        return this;
        }

        public Builder setBaseFont(ArberFont baseFont) {
            this.baseFont = baseFont;
        return this;
        }

        public BasicChartTheme build() {
            return new BasicChartTheme(background, foreground, gridColor, axisLabelColor, accentColor, seriesColors, baseFont);
        }
    }
}
