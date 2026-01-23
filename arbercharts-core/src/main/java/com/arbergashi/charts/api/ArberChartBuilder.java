package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.ui.grid.GridLayer;
import com.arbergashi.charts.ui.legend.LegendConfig;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/**
 * Fluent builder for quick chart panel configuration.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ArberChartBuilder {

    private final ArberChartPanel panel;
    private final AxisConfig xAxis = new AxisConfig();
    private final AxisConfig yAxis = new AxisConfig();
    private final ChartRenderHints hints = new ChartRenderHints();
    private boolean xAxisConfigured;
    private boolean yAxisConfigured;
    private boolean hintsConfigured;
    private ChartTheme theme;

    private ArberChartBuilder(ArberChartPanel panel) {
        this.panel = panel;
    }

    public static ArberChartBuilder create() {
        return new ArberChartBuilder(new ArberChartPanel(null, null));
    }

    public static ArberChartBuilder of(ChartModel model, ChartRenderer renderer) {
        return new ArberChartBuilder(new ArberChartPanel(model, renderer));
    }

    public ArberChartBuilder withTitle(String title) {
        panel.setName(title);
        panel.putClientProperty("chart.title", title);
        return this;
    }

    public ArberChartBuilder withTheme(ChartTheme theme) {
        this.theme = theme;
        return this;
    }

    public ArberChartBuilder withDarkMode() {
        return withTheme(ChartThemes.defaultDark());
    }

    public ArberChartBuilder withGridLayer(GridLayer gridLayer) {
        panel.withGridLayer(gridLayer);
        return this;
    }

    public ArberChartBuilder withTooltips(boolean enabled) {
        panel.withTooltips(enabled);
        return this;
    }

    public ArberChartBuilder withLegend(boolean visible) {
        panel.withLegend(visible);
        return this;
    }

    public ArberChartBuilder withLegendConfig(LegendConfig config) {
        panel.withLegendConfig(config);
        return this;
    }

    public ArberChartBuilder withLocale(Locale locale) {
        if (locale != null) {
            panel.setLocale(locale);
        }
        return this;
    }

    public ArberChartBuilder addLayer(ChartModel model, ChartRenderer renderer) {
        panel.addLayer(model, renderer);
        return this;
    }

    public ArberChartBuilder addLineSeries(String name, double[] x, double[] y) {
        DefaultChartModel model = new DefaultChartModel(name);
        if (x != null && y != null) {
            model.addXYArrays(x, y);
        }
        panel.addLayer(model, new LineRenderer());
        return this;
    }

    public <T> ArberChartBuilder addLineSeries(String name, List<T> data, ToDoubleFunction<T> xFn, ToDoubleFunction<T> yFn) {
        DefaultChartModel model = new DefaultChartModel(name);
        if (data != null && xFn != null && yFn != null) {
            for (T item : data) {
                model.addXY(xFn.applyAsDouble(item), yFn.applyAsDouble(item));
            }
        }
        panel.addLayer(model, new LineRenderer());
        return this;
    }

    public ArberChartBuilder xAxis(Consumer<AxisConfig> config) {
        if (config != null) {
            config.accept(xAxis);
            xAxisConfigured = true;
        }
        return this;
    }

    public ArberChartBuilder yAxis(Consumer<AxisConfig> config) {
        if (config != null) {
            config.accept(yAxis);
            yAxisConfigured = true;
        }
        return this;
    }

    public ArberChartBuilder hints(Consumer<ChartRenderHints> config) {
        if (config != null) {
            config.accept(hints);
            hintsConfigured = true;
        }
        return this;
    }

    public ArberChartPanel build() {
        if (panel.getLayerCount() == 0) {
            throw new IllegalStateException("No chart layers configured");
        }
        if (xAxisConfigured) {
            panel.withXAxisConfig(xAxis);
        }
        if (yAxisConfigured) {
            panel.withYAxisConfig(yAxis);
        }
        if (hintsConfigured) {
            panel.setRenderHints(hints);
        }
        if (theme != null) {
            panel.withTheme(theme);
        }
        return panel;
    }
}
