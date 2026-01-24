package com.arbergashi.charts.util;

import com.arbergashi.charts.api.BasicChartTheme;
import com.arbergashi.charts.api.ChartTheme;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

/**
 * Smoothly interpolates between two chart themes using cached colors.
 *
 * <p>Interpolation uses {@link ColorRegistry#interpolate(Color, Color, float)} so
 * animation frames are served from the flyweight cache without per-frame allocations.</p>
 */
public final class ThemeTransitionManager {

    private final ChartTheme start;
    private final ChartTheme target;
    private final long durationNanos;
    private final Consumer<ChartTheme> apply;
    private final MutableTheme scratch;
    private final Color[] startSeries;
    private final Color[] targetSeries;
    private final Timer timer;
    private long startTime;

    public static ThemeTransitionManager start(ChartTheme start, ChartTheme target, int durationMs, Consumer<ChartTheme> apply) {
        ThemeTransitionManager mgr = new ThemeTransitionManager(start, target, durationMs, apply);
        mgr.start();
        return mgr;
    }

    public ThemeTransitionManager(ChartTheme start, ChartTheme target, int durationMs, Consumer<ChartTheme> apply) {
        if (start == null || target == null) {
            throw new IllegalArgumentException("start/target theme must not be null");
        }
        if (apply == null) {
            throw new IllegalArgumentException("apply must not be null");
        }
        this.start = start;
        this.target = target;
        this.durationNanos = Math.max(1, durationMs) * 1_000_000L;
        this.apply = apply;

        int seriesCount = Math.max(seriesCount(start), seriesCount(target));
        this.startSeries = new Color[seriesCount];
        this.targetSeries = new Color[seriesCount];
        for (int i = 0; i < seriesCount; i++) {
            startSeries[i] = start.getSeriesColor(i);
            targetSeries[i] = target.getSeriesColor(i);
        }

        this.scratch = new MutableTheme(start, seriesCount);
        this.timer = new Timer(16, e -> tick());
        this.timer.setRepeats(true);
    }

    public void start() {
        startTime = System.nanoTime();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void tick() {
        long now = System.nanoTime();
        float t = (float) ((now - startTime) / (double) durationNanos);
        if (t >= 1f) {
            apply.accept(target);
            stop();
            return;
        }

        scratch.background = ColorRegistry.interpolate(start.getBackground(), target.getBackground(), t);
        scratch.foreground = ColorRegistry.interpolate(start.getForeground(), target.getForeground(), t);
        scratch.gridColor = ColorRegistry.interpolate(start.getGridColor(), target.getGridColor(), t);
        scratch.axisLabelColor = ColorRegistry.interpolate(start.getAxisLabelColor(), target.getAxisLabelColor(), t);
        scratch.accentColor = ColorRegistry.interpolate(start.getAccentColor(), target.getAccentColor(), t);

        for (int i = 0; i < scratch.seriesColors.length; i++) {
            scratch.seriesColors[i] = ColorRegistry.interpolate(startSeries[i], targetSeries[i], t);
        }

        apply.accept(scratch);
    }

    private static int seriesCount(ChartTheme theme) {
        if (theme instanceof BasicChartTheme basic && basic.seriesColors() != null) {
            return basic.seriesColors().length;
        }
        return 0;
    }

    private static final class MutableTheme implements ChartTheme {
        private Color background;
        private Color foreground;
        private Color gridColor;
        private Color axisLabelColor;
        private Color accentColor;
        private final Color[] seriesColors;
        private final Font baseFont;

        private MutableTheme(ChartTheme base, int seriesCount) {
            this.background = base.getBackground();
            this.foreground = base.getForeground();
            this.gridColor = base.getGridColor();
            this.axisLabelColor = base.getAxisLabelColor();
            this.accentColor = base.getAccentColor();
            this.seriesColors = new Color[Math.max(0, seriesCount)];
            for (int i = 0; i < seriesColors.length; i++) {
                seriesColors[i] = base.getSeriesColor(i);
            }
            this.baseFont = base.getBaseFont();
        }

        @Override
        public Color getBackground() {
            return background;
        }

        @Override
        public Color getForeground() {
            return foreground;
        }

        @Override
        public Color getGridColor() {
            return gridColor;
        }

        @Override
        public Color getAxisLabelColor() {
            return axisLabelColor;
        }

        @Override
        public Color getAccentColor() {
            return accentColor;
        }

        @Override
        public Color getSeriesColor(int index) {
            if (seriesColors.length == 0) return accentColor;
            return seriesColors[Math.abs(index) % seriesColors.length];
        }

        @Override
        public Font getBaseFont() {
            return baseFont;
        }
    }
}
