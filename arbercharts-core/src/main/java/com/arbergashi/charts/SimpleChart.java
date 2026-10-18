package com.arbergashi.charts;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;

import java.util.function.Supplier;

/**
 * Fluent convenience API for line-chart data and display.
 *
 * <p>Create an instance through {@link Charts#lineChart()} or
 * {@link Charts#lineChart(int)}. Manual data insertion and display through the
 * Swing bridge are implemented. Streaming and file-export helpers always throw
 * {@link UnsupportedOperationException}.
 *
 * <p>The bundled display provider uses the title as the window title. It does
 * not apply the subtitle, axis-label or theme values stored by this facade.
 * Configure {@code ArberChartPanel} from {@code arbercharts-swing-bridge}
 * directly for those presentation features. For PNG output, use
 * {@code ServerRenderService} from {@code arbercharts-server-bridge} with an
 * application-owned model. The
 * <a href="https://github.com/ArberGashi/industrie-charts/blob/main/docs/QUICK_START.md">quick start</a>
 * contains working examples of both approaches.
 *
 * <h2>Line Chart Example</h2>
 * <pre>{@code
 * Charts.lineChart()
 *     .title("Sales 2026")
 *     .addData(1, 10, 2, 20, 3, 15)
 *     .show();
 * }</pre>
 *
 * @since 2.0.0
 * @see Charts
 */
public class SimpleChart {

    private ChartModel model;
    private ChartRenderer renderer;
    private String title;
    private String subtitle;
    private String xLabel;
    private String yLabel;
    private String theme = "dark";
    private boolean streamingEnabled = false;
    private int updateInterval = 100; // ms
    private Supplier<Double> dataSource;

    /**
     * Package-private constructor. Use {@link Charts} factory methods.
     */
    SimpleChart() {
    }

    /**
     * Sets the title passed to the display provider.
     *
     * <p>The bundled Swing provider uses this value as the window title.
     *
     * @param title the title text
     * @return this chart for chaining
     */
    public SimpleChart title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Stores a subtitle value without changing the displayed chart.
     *
     * <p>The bundled display provider does not apply this value. Configure the
     * presentation directly in the chart host.
     *
     * @param subtitle the subtitle text
     * @return this chart for chaining
     */
    public SimpleChart subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Stores an X-axis label without changing the displayed chart.
     *
     * <p>The bundled display provider does not apply this value. Configure the
     * axis directly in the chart host.
     *
     * @param label the X-axis label
     * @return this chart for chaining
     */
    public SimpleChart xLabel(String label) {
        this.xLabel = label;
        return this;
    }

    /**
     * Stores a Y-axis label without changing the displayed chart.
     *
     * <p>The bundled display provider does not apply this value. Configure the
     * axis directly in the chart host.
     *
     * @param label the Y-axis label
     * @return this chart for chaining
     */
    public SimpleChart yLabel(String label) {
        this.yLabel = label;
        return this;
    }

    /**
     * Stores a theme name without changing the displayed chart.
     *
     * <p>The bundled display provider does not apply or validate this value.
     * Configure {@code ArberChartPanel.setTheme(ChartTheme)} directly in the
     * Swing host with a theme from {@link com.arbergashi.charts.api.ChartThemes}.
     *
     * @param theme the theme name
     * @return this chart for chaining
     */
    public SimpleChart theme(String theme) {
        this.theme = theme;
        return this;
    }

    /**
     * Adds data points to the chart.
     *
     * <p>Accepts alternating x,y values for convenience:
     * <pre>{@code
     * chart.addData(1, 10, 2, 20, 3, 30);
     * // Equivalent to:
     * chart.addPoint(1, 10);
     * chart.addPoint(2, 20);
     * chart.addPoint(3, 30);
     * }</pre>
     *
     * @param values alternating x,y values
     * @return this chart for chaining
     * @throws IllegalArgumentException if odd number of values
     */
    public SimpleChart addData(double... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Must provide pairs of (x,y) values. Got " + values.length + " values."
            );
        }

        if (model == null) {
            throw new IllegalStateException(
                "Model not initialized. Use Charts factory methods."
            );
        }

        // Add all points to the model
        for (int i = 0; i < values.length; i += 2) {
            double x = values[i];
            double y = values[i + 1];
            // Use CircularChartModel.setPoint() method
            if (model instanceof com.arbergashi.charts.model.CircularChartModel circularModel) {
                circularModel.setPoint(x, y, 1.0, null);
            }
        }

        return this;
    }

    /**
     * Adds a single data point.
     *
     * @param x the x coordinate
     * @param y the y value
     * @return this chart for chaining
     */
    public SimpleChart addPoint(double x, double y) {
        if (model == null) {
            throw new IllegalStateException(
                "Model not initialized. Use Charts factory methods."
            );
        }

        if (model instanceof com.arbergashi.charts.model.CircularChartModel circularModel) {
            circularModel.setPoint(x, y, 1.0, null);
        }

        return this;
    }

    /**
     * Records the streaming-enabled flag without starting data production.
     *
     * <p>Automatic streaming is not implemented. Calling
     * {@link #startStreaming(Supplier)} always throws
     * {@link UnsupportedOperationException}; this flag does not schedule updates.
     *
     * @return this chart for chaining
     */
    public SimpleChart enableStreaming() {
        this.streamingEnabled = true;
        return this;
    }

    /**
     * Stores the requested streaming interval in milliseconds.
     *
     * <p>Automatic streaming is not implemented, so this value does not schedule
     * updates or change the display refresh rate.
     *
     * @param intervalMs update interval (default: 100ms = 10 Hz)
     * @return this chart for chaining
     */
    public SimpleChart updateInterval(int intervalMs) {
        this.updateInterval = intervalMs;
        return this;
    }

    /**
     * Reserved supplier-based streaming operation; not implemented.
     *
     * <p>No data producer is started. Supply points explicitly with
     * {@link #addPoint(double, double)} or manage a chart model and its update
     * lifecycle directly in the host application.
     *
     * @param dataSource supplier that provides new Y values
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public SimpleChart startStreaming(Supplier<Double> dataSource) {
        this.dataSource = dataSource;
        this.streamingEnabled = true;

        throw new UnsupportedOperationException(
            "Automatic streaming through SimpleChart is not implemented. " +
            "Supply points with addPoint() or addData(), or manage model updates in the host application."
        );
    }

    /**
     * Displays the chart in a window.
     *
     * <p>Requires arbercharts-swing-bridge on classpath. Uses ServiceLoader
     * to find the appropriate chart display implementation.
     *
     * @return this chart for chaining
     * @throws IllegalStateException if swing-bridge is not on classpath
     */
    public SimpleChart show() {
        if (model == null || renderer == null) {
            throw new IllegalStateException(
                "Chart not properly initialized. Use Charts factory methods."
            );
        }

        // Use ServiceLoader to find display implementation
        java.util.ServiceLoader<ChartDisplayProvider> loader =
            java.util.ServiceLoader.load(ChartDisplayProvider.class);

        java.util.Optional<ChartDisplayProvider> provider = loader.findFirst();

        if (provider.isPresent()) {
            provider.get().showChart(this, title, model, renderer);
        } else {
            throw new IllegalStateException(
                "No ChartDisplayProvider found. Add arbercharts-swing-bridge to classpath. " +
                "Maven: <dependency><groupId>com.arbergashi</groupId>" +
                "<artifactId>arbercharts-swing-bridge</artifactId></dependency>"
            );
        }
        return this;
    }


    /**
     * Reserved PNG-export convenience operation; not implemented.
     *
     * <p>No file is written. Render an application-owned model with
     * {@code ServerRenderService.renderToPng} from
     * {@code arbercharts-server-bridge}, or export a Swing panel through
     * {@code ChartExportService.exportPng} from {@code arbercharts-swing-bridge}.
     *
     * @param filename requested output filename; not used
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public SimpleChart exportToPNG(String filename) {
        throw new UnsupportedOperationException(
            "PNG export through SimpleChart is not implemented. " +
            "Use ServerRenderService.renderToPng() or ChartExportService.exportPng() with a Swing panel."
        );
    }

    /**
     * Reserved SVG-export convenience operation; not implemented.
     *
     * <p>No file is written. Swing applications can use
     * {@code ChartExportService.exportSvg} from {@code arbercharts-swing-bridge}
     * with the optional {@code org.jfree:jfreesvg} backend on the classpath.
     *
     * @param filename requested output filename; not used
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public SimpleChart exportToSVG(String filename) {
        throw new UnsupportedOperationException(
            "SVG export through SimpleChart is not implemented. " +
            "Use ChartExportService.exportSvg() with a Swing panel and the optional JFreeSVG backend."
        );
    }

    // Package-private setters for Charts factory

    SimpleChart withModel(ChartModel model) {
        this.model = model;
        return this;
    }

    SimpleChart withRenderer(ChartRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    SimpleChart withTheme(String theme) {
        this.theme = theme;
        return this;
    }
}
