package com.arbergashi.charts;

import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;

/**
 * Factory for line charts using the {@link SimpleChart} convenience API.
 *
 * <p>Line-chart creation, manual data insertion and display are implemented.
 * The other convenience factories are reserved and always throw
 * {@link UnsupportedOperationException}. Their renderer classes can be used
 * directly with an appropriate model and a Swing or server host.
 *
 * <p>Displaying a chart requires {@code arbercharts-swing-bridge} on the classpath
 * and a graphical desktop. See the
 * <a href="https://github.com/ArberGashi/industrie-charts/blob/main/docs/QUICK_START.md">quick start</a>
 * for working panel and server examples.
 *
 * <h2>Line Chart Example</h2>
 * <pre>{@code
 * // Create chart with sensible defaults
 * var chart = Charts.lineChart();
 * chart.addData(1, 10, 2, 20, 3, 15);
 * chart.show();
 * }</pre>
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
     * <p>Uses a 1000-point {@link CircularChartModel} and a {@link LineRenderer}.
     * The display host controls presentation and interaction.
     *
     * <h4>Example</h4>
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
     * Creates a line chart and records the streaming-enabled flag.
     *
     * <p>This method does not start a producer or schedule updates.
     * {@link SimpleChart#startStreaming(java.util.function.Supplier)} is not
     * implemented and always throws {@link UnsupportedOperationException}.
     * Supply data explicitly through {@link SimpleChart#addPoint(double, double)}
     * or manage a {@link CircularChartModel} directly in the host application.
     *
     * @param capacity number of data points to keep in memory
     * @return a line chart with its streaming flag set, without automatic updates
     */
    public static SimpleChart streamingLineChart(int capacity) {
        return lineChart(capacity)
            .enableStreaming();
    }

    /**
     * Reserved scatter-plot convenience factory; not implemented.
     *
     * <p>Use {@link com.arbergashi.charts.render.standard.ScatterRenderer}
     * directly with a chart model and a Swing or server host.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public static SimpleChart scatterPlot() {
        throw new UnsupportedOperationException(
            "The scatter-plot convenience factory is not implemented. " +
            "Use ScatterRenderer with a chart model in a Swing or server host."
        );
    }

    /**
     * Reserved bar-chart convenience factory; not implemented.
     *
     * <p>Use {@link com.arbergashi.charts.render.standard.BarRenderer}
     * directly with a chart model and a Swing or server host.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public static SimpleChart barChart() {
        throw new UnsupportedOperationException(
            "The bar-chart convenience factory is not implemented. " +
            "Use BarRenderer with a chart model in a Swing or server host."
        );
    }

    /**
     * Reserved candlestick convenience factory; not implemented.
     *
     * <p>Use {@link com.arbergashi.charts.model.DefaultFinancialChartModel}
     * with {@link com.arbergashi.charts.render.financial.CandlestickRenderer}
     * in a Swing or server host. The
     * <a href="https://github.com/ArberGashi/industrie-charts/blob/main/arbercharts-spring-boot-demo/README.md">Spring Boot workbench</a>
     * includes a runnable example with synthetic OHLC data.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public static SimpleChart candlestickChart() {
        throw new UnsupportedOperationException(
            "The candlestick convenience factory is not implemented. " +
            "Use CandlestickRenderer with DefaultFinancialChartModel in a Swing or server host."
        );
    }

    /**
     * Reserved ECG/EKG convenience factory; not implemented.
     *
     * <p>For a sweep display, use
     * {@link com.arbergashi.charts.model.CircularFastMedicalModel} with
     * {@link com.arbergashi.charts.render.medical.MedicalSweepRenderer} in a
     * chart host. The
     * <a href="https://github.com/ArberGashi/industrie-charts/blob/main/arbercharts-demo/docs/WORKBENCH.md">Swing workbench</a>
     * demonstrates the available medical renderers and their model setup.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public static SimpleChart ecgChart() {
        throw new UnsupportedOperationException(
            "The ECG convenience factory is not implemented. " +
            "Use MedicalSweepRenderer with CircularFastMedicalModel in a chart host."
        );
    }
}

