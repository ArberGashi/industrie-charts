package com.arbergashi.charts.ui;

import com.arbergashi.charts.internal.ChartStyle;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.ColorUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import javax.swing.UIManager;
import java.awt.Color;

@State(Scope.Thread)
public class ThemeSwitchJmhBenchmark {

    private Color lightBg;
    private Color lightFg;
    private Color lightGrid;
    private Color darkBg;
    private Color darkFg;
    private Color darkGrid;
    private boolean dark;

    @Setup(Level.Trial)
    public void setup() {
        lightBg = ColorRegistry.of(250, 250, 250, 255);
        lightFg = ColorRegistry.of(20, 20, 20, 255);
        lightGrid = ColorRegistry.of(200, 200, 200, 255);

        darkBg = ColorRegistry.of(30, 30, 30, 255);
        darkFg = ColorRegistry.of(220, 220, 220, 255);
        darkGrid = ColorRegistry.of(85, 85, 85, 255);

        applyTheme(false);
    }

    @Benchmark
    public void themeSwitchAndLookup() {
        dark = !dark;
        applyTheme(dark);

        Color bg = ChartStyle.getBackgroundColor();
        Color fg = ChartStyle.getForegroundColor();
        Color grid = ChartStyle.getGridColor();

        ColorUtils.withAlpha(bg, 0.95f);
        ColorUtils.withAlpha(fg, 0.8f);
        ColorUtils.withAlpha(grid, 0.5f);

        ColorRegistry.interpolate(bg, fg, 0.5f);
        ChartAssets.getUIColor("Chart.background", bg);
    }

    @Benchmark
    public void themeLookupOnly() {
        Color bg = ChartStyle.getBackgroundColor();
        Color fg = ChartStyle.getForegroundColor();
        Color grid = ChartStyle.getGridColor();

        ColorUtils.withAlpha(bg, 0.95f);
        ColorUtils.withAlpha(fg, 0.8f);
        ColorUtils.withAlpha(grid, 0.5f);
        ColorRegistry.interpolate(bg, fg, 0.5f);
        ChartAssets.getUIColor("Chart.background", bg);
    }

    @Benchmark
    public void themeTransitionInterpolate() {
        ColorRegistry.interpolate(lightBg, darkBg, 0.5f);
        ColorRegistry.interpolate(lightFg, darkFg, 0.5f);
        ColorRegistry.interpolate(lightGrid, darkGrid, 0.5f);
    }

    private void applyTheme(boolean darkMode) {
        if (darkMode) {
            UIManager.put("Chart.background", darkBg);
            UIManager.put("Chart.foreground", darkFg);
            UIManager.put("Chart.gridColor", darkGrid);
        } else {
            UIManager.put("Chart.background", lightBg);
            UIManager.put("Chart.foreground", lightFg);
            UIManager.put("Chart.gridColor", lightGrid);
        }
    }
}
