package com.arbergashi.charts;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;

import java.util.function.Supplier;

/**
 * Simplified chart API with fluent interface.
 *
 * <p><strong>Design Philosophy:</strong> Developer should be able to create
 * production-ready charts in 2-5 lines of code.
 *
 * <h2>Feature Highlights</h2>
 * <ul>
 *   <li><strong>Fluent API:</strong> Method chaining for readability</li>
 *   <li><strong>Sensible Defaults:</strong> Works without configuration</li>
 *   <li><strong>Type-Safe:</strong> Compile-time validation</li>
 *   <li><strong>Zero-GC:</strong> Inherited from core framework</li>
 * </ul>
 *
 * <h2>Quick Examples</h2>
 *
 * <h3>Minimal Line Chart</h3>
 * <pre>{@code
 * Charts.lineChart()
 *     .addData(1, 10, 2, 20, 3, 15)
 *     .show();
 * }</pre>
 *
 * <h3>Styled Chart</h3>
 * <pre>{@code
 * Charts.lineChart()
 *     .title("Sales 2026")
 *     .subtitle("Q1 Performance")
 *     .xLabel("Month")
 *     .yLabel("Revenue (CHF)")
 *     .theme("obsidian")
 *     .addData(1, 100, 2, 150, 3, 200)
 *     .show();
 * }</pre>
 *
 * <h3>Real-Time Streaming</h3>
 * <pre>{@code
 * Charts.streamingLineChart(1000)
 *     .title("CPU Usage")
 *     .updateInterval(100)
 *     .startStreaming(() -> getCpuUsage())
 *     .show();
 * }</pre>
 *
 * <h3>Export to PNG</h3>
 * <pre>{@code
 * Charts.lineChart()
 *     .addData(data)
 *     .exportToPNG("chart.png");
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
     * Sets the chart title.
     *
     * @param title the title text
     * @return this chart for chaining
     */
    public SimpleChart title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the chart subtitle.
     *
     * @param subtitle the subtitle text
     * @return this chart for chaining
     */
    public SimpleChart subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Sets the X-axis label.
     *
     * @param label the X-axis label
     * @return this chart for chaining
     */
    public SimpleChart xLabel(String label) {
        this.xLabel = label;
        return this;
    }

    /**
     * Sets the Y-axis label.
     *
     * @param label the Y-axis label
     * @return this chart for chaining
     */
    public SimpleChart yLabel(String label) {
        this.yLabel = label;
        return this;
    }

    /**
     * Sets the visual theme.
     *
     * <p>Available themes:
     * <ul>
     *   <li><strong>dark</strong> - Dark background (default)</li>
     *   <li><strong>light</strong> - Light background</li>
     *   <li><strong>obsidian</strong> - Professional dark theme</li>
     * </ul>
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
     * Enables real-time streaming mode.
     *
     * @return this chart for chaining
     */
    public SimpleChart enableStreaming() {
        this.streamingEnabled = true;
        return this;
    }

    /**
     * Sets the streaming update interval in milliseconds.
     *
     * @param intervalMs update interval (default: 100ms = 10 Hz)
     * @return this chart for chaining
     */
    public SimpleChart updateInterval(int intervalMs) {
        this.updateInterval = intervalMs;
        return this;
    }

    /**
     * Starts streaming data from a supplier.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * chart.startStreaming(() -> sensor.readTemperature());
     * }</pre>
     *
     * @param dataSource supplier that provides new Y values
     * @return this chart for chaining
     * @throws UnsupportedOperationException streaming not yet implemented
     */
    public SimpleChart startStreaming(Supplier<Double> dataSource) {
        this.dataSource = dataSource;
        this.streamingEnabled = true;

        throw new UnsupportedOperationException(
            "Streaming is not available in ArberCharts 2.0.0. " +
            "Use addData() with manual updates."
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
     * Exports the chart to PNG file.
     *
     * @param filename the output filename
     * @return this chart for chaining
     * @throws UnsupportedOperationException export not yet implemented
     */
    public SimpleChart exportToPNG(String filename) {
        throw new UnsupportedOperationException(
            "PNG export is not available in ArberCharts 2.0.0. " +
            "Use interactive display via show()."
        );
    }

    /**
     * Exports the chart to SVG file.
     *
     * @param filename the output filename
     * @return this chart for chaining
     * @throws UnsupportedOperationException export not yet implemented
     */
    public SimpleChart exportToSVG(String filename) {
        throw new UnsupportedOperationException(
            "SVG export is not available in ArberCharts 2.0.0. " +
            "Use interactive display via show()."
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
