package com.arbergashi.charts.api;

import java.awt.*;
import java.util.Arrays;

/**
 * Basic implementation of ChartTheme using immutable value object pattern.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-16
 */
public record BasicChartTheme(
        Color background,
        Color foreground,
        Color gridColor,
        Color axisLabelColor,
        Color accentColor,
        Color[] seriesColors,
        Font baseFont
) implements ChartTheme {

    public BasicChartTheme {
        // Defensively copy to preserve immutability (arrays are mutable).
        seriesColors = (seriesColors != null) ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
    }

    public Color getBackground() {
        return background;
    }

    public Color getForeground() {
        return foreground;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public Color getAxisLabelColor() {
        return axisLabelColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public Color getSeriesColor(int index) {
        if (seriesColors == null || seriesColors.length == 0) {
            return accentColor;
        }
        return seriesColors[Math.abs(index) % seriesColors.length];
    }

    public Font getBaseFont() {
        return baseFont;
    }

    /**
     * Creates a new builder for constructing a BasicChartTheme.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for BasicChartTheme.
     */
    public static class Builder {
        private Color background = new Color(30, 30, 30);
        private Color foreground = new Color(220, 220, 220);
        private Color gridColor = new Color(60, 60, 60);
        private Color axisLabelColor = new Color(180, 180, 180);
        private Color accentColor = new Color(100, 149, 237);
        private Color[] seriesColors = new Color[]{
                new Color(100, 149, 237),
                new Color(255, 99, 71),
                new Color(50, 205, 50)
        };
        private Font baseFont = new Font("SansSerif", Font.PLAIN, 11);

        /**
         * Sets the chart background color.
         *
         * @param background background color
         * @return this builder
         */
        public Builder background(Color background) {
            this.background = background;
            return this;
        }

        /**
         * Sets the foreground color used for labels and strokes.
         *
         * @param foreground foreground color
         * @return this builder
         */
        public Builder foreground(Color foreground) {
            this.foreground = foreground;
            return this;
        }

        /**
         * Sets the grid line color.
         *
         * @param gridColor grid color
         * @return this builder
         */
        public Builder gridColor(Color gridColor) {
            this.gridColor = gridColor;
            return this;
        }

        /**
         * Sets the axis label color.
         *
         * @param axisLabelColor axis label color
         * @return this builder
         */
        public Builder axisLabelColor(Color axisLabelColor) {
            this.axisLabelColor = axisLabelColor;
            return this;
        }

        /**
         * Sets the accent color used for highlights and primary series.
         *
         * @param accentColor accent color
         * @return this builder
         */
        public Builder accentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        /**
         * Sets the series palette used by index-based renderers.
         *
         * @param seriesColors series colors (copied defensively)
         * @return this builder
         */
        public Builder seriesColors(Color[] seriesColors) {
            // Defensive copy to avoid external mutation of the builder state.
            this.seriesColors = (seriesColors != null) ? Arrays.copyOf(seriesColors, seriesColors.length) : null;
            return this;
        }

        /**
         * Sets the base font for chart text.
         *
         * @param baseFont base font
         * @return this builder
         */
        public Builder baseFont(Font baseFont) {
            this.baseFont = baseFont;
            return this;
        }

        /**
         * Builds an immutable {@link BasicChartTheme} instance.
         *
         * @return new theme instance
         */
        public BasicChartTheme build() {
            return new BasicChartTheme(
                    background,
                    foreground,
                    gridColor,
                    axisLabelColor,
                    accentColor,
                    seriesColors,
                    baseFont
            );
        }
    }
}
