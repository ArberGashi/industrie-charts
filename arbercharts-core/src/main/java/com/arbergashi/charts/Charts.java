package com.arbergashi.charts;

import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;

/**
 * Simple factory for creating charts with minimal code.
 *
 * <p><strong>Philosophy:</strong> ArberCharts should be simple to use for common cases
 * while remaining powerful for advanced scenarios.
 *
 * <h2>Quick Start Examples</h2>
 *
 * <h3>Line Chart (3 lines of code)</h3>
 * <pre>{@code
 * // Create chart with sensible defaults
 * var chart = Charts.lineChart();
 * chart.addData(1, 10, 2, 20, 3, 15);
 * chart.show();
 * }</pre>
 *
 * <h3>Real-Time Streaming (4 lines)</h3>
 * <pre>{@code
 * var chart = Charts.streamingLineChart(1000);
 * chart.startStreaming(() -> Math.random() * 100);
 * chart.show();
 * }</pre>
 *
 * <h3>Financial Candlestick (2 lines)</h3>
 * <pre>{@code
 * var chart = Charts.candlestickChart();
 * chart.loadFromCSV("prices.csv");
 * }</pre>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Minimal Code:</strong> Common charts in 2-5 lines</li>
 *   <li><strong>Sensible Defaults:</strong> Works out-of-box, customizable later</li>
 *   <li><strong>Fluent API:</strong> Method chaining for readability</li>
 *   <li><strong>Zero Configuration:</strong> No XML, no properties files</li>
 * </ul>
 *
 * @since 2.0.0
 * @see SimpleChart
 */
public final class Charts {

    private Charts() {
        // Utility class
    }

    /**
     * Creates a simple line chart with default settings.
     *
     * <p><strong>What you get:</strong>
     * <ul>
     *   <li>1000-point circular buffer (auto-scrolling)</li>
     *   <li>Green line on dark background</li>
     *   <li>Auto-scaling Y-axis</li>
     *   <li>Anti-aliasing enabled</li>
     * </ul>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Charts.lineChart()
     *     .title("Temperature")
     *     .addData(1, 20.5, 2, 21.0, 3, 20.8)
     *     .show();
     * }</pre>
     *
     * @return a SimpleChart configured as line chart
     */
    public static SimpleChart lineChart() {
        return new SimpleChart()
            .withModel(new CircularChartModel(1000))
            .withRenderer(new LineRenderer())
            .withTheme("dark");
    }

    /**
     * Creates a line chart with specific capacity.
     *
     * @param capacity number of data points to keep in memory
     * @return a SimpleChart configured as line chart
     */
    public static SimpleChart lineChart(int capacity) {
        return new SimpleChart()
            .withModel(new CircularChartModel(capacity))
            .withRenderer(new LineRenderer())
            .withTheme("dark");
    }

    /**
     * Creates a real-time streaming line chart.
     *
     * <p>Perfect for live dashboards, monitoring, IoT data.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * // CPU monitoring
     * Charts.streamingLineChart(500)
     *     .title("CPU Usage")
     *     .updateInterval(100)  // 10 Hz
     *     .startStreaming(() -> getCpuUsage())
     *     .show();
     * }</pre>
     *
     * @param capacity number of points visible in window
     * @return a SimpleChart with streaming support
     */
    public static SimpleChart streamingLineChart(int capacity) {
        return lineChart(capacity)
            .enableStreaming();
    }

    /**
     * Creates a scatter plot chart.
     *
     * @return a SimpleChart configured as scatter plot
     * @throws UnsupportedOperationException scatter plots coming in v2.1
     */
    public static SimpleChart scatterPlot() {
        throw new UnsupportedOperationException(
            "Scatter plots will be added in v2.1.0. " +
            "Current version v2.0.0 supports line charts only. " +
            "Use lineChart() for now."
        );
    }

    /**
     * Creates a bar chart.
     *
     * @return a SimpleChart configured as bar chart
     * @throws UnsupportedOperationException bar charts coming in v2.1
     */
    public static SimpleChart barChart() {
        throw new UnsupportedOperationException(
            "Bar charts will be added in v2.1.0. " +
            "Current version v2.0.0 supports line charts only. " +
            "Use lineChart() for now."
        );
    }

    /**
     * Creates a financial candlestick chart.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Charts.candlestickChart()
     *     .title("BTC/USD")
     *     .loadFromCSV("bitcoin-prices.csv")
     *     .show();
     * }</pre>
     *
     * @return a SimpleChart configured as candlestick chart
     * @throws UnsupportedOperationException candlestick charts coming in v2.2
     */
    public static SimpleChart candlestickChart() {
        throw new UnsupportedOperationException(
            "Candlestick charts will be added in v2.2.0. " +
            "This is a complex renderer requiring OHLC data model. " +
            "Use lineChart() for now."
        );
    }

    /**
     * Creates an ECG/EKG medical chart.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Charts.ecgChart()
     *     .sweepMode(true)  // Oscilloscope-style
     *     .gridEnabled(true)
     *     .startStreaming(() -> getHeartRateSignal())
     *     .show();
     * }</pre>
     *
     * @return a SimpleChart configured for medical ECG
     * @throws UnsupportedOperationException ECG charts coming in v2.3
     */
    public static SimpleChart ecgChart() {
        throw new UnsupportedOperationException(
            "ECG charts will be added in v2.3.0. " +
            "This requires sweep-erase rendering and medical grid. " +
            "Use streamingLineChart() for now."
        );
    }
}

