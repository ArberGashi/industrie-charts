package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.ColorUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class ThemeSwitchJmhBenchmark {

    private ArberColor lightBg;
    private ArberColor lightFg;
    private ArberColor lightGrid;
    private ArberColor darkBg;
    private ArberColor darkFg;
    private ArberColor darkGrid;
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

        ArberColor bg = ChartAssets.getColor("Chart.background", lightBg);
        ArberColor fg = ChartAssets.getColor("Chart.foreground", lightFg);
        ArberColor grid = ChartAssets.getColor("Chart.gridColor", lightGrid);

        ColorUtils.applyAlpha(bg, 0.95f);
        ColorUtils.applyAlpha(fg, 0.8f);
        ColorUtils.applyAlpha(grid, 0.5f);

        ColorRegistry.interpolate(bg, fg, 0.5f);
        ChartAssets.getUIColor("Chart.background", bg);
    }

    @Benchmark
    public void themeLookupOnly() {
        ArberColor bg = ChartAssets.getColor("Chart.background", lightBg);
        ArberColor fg = ChartAssets.getColor("Chart.foreground", lightFg);
        ArberColor grid = ChartAssets.getColor("Chart.gridColor", lightGrid);

        ColorUtils.applyAlpha(bg, 0.95f);
        ColorUtils.applyAlpha(fg, 0.8f);
        ColorUtils.applyAlpha(grid, 0.5f);
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
        ArberColor bg = darkMode ? darkBg : lightBg;
        ArberColor fg = darkMode ? darkFg : lightFg;
        ArberColor grid = darkMode ? darkGrid : lightGrid;
        ChartAssets.setProperty("Chart.background", String.format("#%02X%02X%02X",
                bg.red(), bg.green(), bg.blue()));
        ChartAssets.setProperty("Chart.foreground", String.format("#%02X%02X%02X",
                fg.red(), fg.green(), fg.blue()));
        ChartAssets.setProperty("Chart.gridColor", String.format("#%02X%02X%02X",
                grid.red(), grid.green(), grid.blue()));
    }
}
