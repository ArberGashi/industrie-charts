package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartPoint;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.ErrorBarPoint;
import com.arbergashi.charts.model.OHLCBar;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>ModelBuilder - Fluent API for Chart Data</h1>
 *
 * <p>End-user friendly builder for creating chart models with type-safe,
 * self-documenting methods. Follows the builder pattern for maximum clarity.</p>
 *
 * <h2>Usage Examples:</h2>
 *
 * <h3>Simple Line Chart:</h3>
 * <pre>{@code
 * ChartModel model = ModelBuilder.series("Temperature")
 *     .addXY(0, 20.5)
 *     .addXY(1, 21.2)
 *     .addXY(2, 19.8)
 *     .build();
 * }</pre>
 *
 * <h3>Candlestick Chart:</h3>
 * <pre>{@code
 * ChartModel model = ModelBuilder.series("AAPL")
 *     .color(Color.BLUE)
 *     .addOHLC(0, 100, 105, 98, 103)
 *     .addOHLC(1, 103, 108, 102, 106)
 *     .addOHLC(2, 106, 107, 104, 105)
 *     .build();
 * }</pre>
 *
 * <h3>Error Bar Chart:</h3>
 * <pre>{@code
 * ChartModel model = ModelBuilder.series("Measurements")
 *     .addWithError(0, 100, 5)  // symmetric error
 *     .addWithError(1, 105, 98, 110)  // asymmetric error
 *     .build();
 * }</pre>
 *
 * <h3>From Arrays:</h3>
 * <pre>{@code
 * double[] x = {0, 1, 2, 3, 4};
 * double[] y = {10, 15, 13, 17, 20};
 * ChartModel model = ModelBuilder.series("Data")
 *     .addXYArrays(x, y)
 *     .build();
 * }</pre>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-16
 */
public final class ModelBuilder {

    private final DefaultChartModel model;

    private ModelBuilder(String name) {
        this.model = new DefaultChartModel(name);
    }

    /**
     * Start building a new chart model.
     *
     * @param name Series name
     * @return new ModelBuilder instance
     */
    public static ModelBuilder series(String name) {
        return new ModelBuilder(name);
    }

    /**
     * Set the series color.
     *
     * @param color Series color
     * @return this builder
     */
    public ModelBuilder color(Color color) {
        model.setColor(color);
        return this;
    }

    /**
     * Set the series subtitle.
     *
     * @param subtitle Series subtitle
     * @return this builder
     */
    public ModelBuilder subtitle(String subtitle) {
        model.setSubtitle(subtitle);
        return this;
    }

    /**
     * Add simple XY point.
     *
     * @param x X-axis value
     * @param y Y-axis value
     * @return this builder
     */
    public ModelBuilder addXY(double x, double y) {
        model.addXY(x, y);
        return this;
    }

    /**
     * Add XY point with label.
     *
     * @param x X-axis value
     * @param y Y-axis value
     * @param label Point label
     * @return this builder
     */
    public ModelBuilder addXY(double x, double y, String label) {
        model.addXY(x, y, label);
        return this;
    }

    /**
     * Add OHLC bar (for candlestick charts).
     *
     * @param time X-axis value (timestamp or index)
     * @param open Opening price
     * @param high Highest price
     * @param low Lowest price
     * @param close Closing price
     * @return this builder
     */
    public ModelBuilder addOHLC(double time, double open, double high, double low, double close) {
        model.addOHLC(time, open, high, low, close);
        return this;
    }

    /**
     * Add OHLC bar from domain object.
     *
     * @param bar OHLC data bar
     * @return this builder
     */
    public ModelBuilder addOHLC(OHLCBar bar) {
        model.addOHLC(bar);
        return this;
    }

    /**
     * Add point with symmetric error bars.
     *
     * @param x X-axis value
     * @param y Y-axis value (mean)
     * @param error Symmetric error margin (±)
     * @return this builder
     */
    public ModelBuilder addWithError(double x, double y, double error) {
        model.addWithError(x, y, error);
        return this;
    }

    /**
     * Add point with asymmetric error bars.
     *
     * @param x X-axis value
     * @param y Y-axis value (mean)
     * @param errorLow Lower bound
     * @param errorHigh Upper bound
     * @return this builder
     */
    public ModelBuilder addWithError(double x, double y, double errorLow, double errorHigh) {
        model.addWithError(x, y, errorLow, errorHigh);
        return this;
    }

    /**
     * Add point with error bars from domain object.
     *
     * @param point Error bar data point
     * @return this builder
     */
    public ModelBuilder addWithError(ErrorBarPoint point) {
        model.addWithError(point);
        return this;
    }

    /**
     * Add multiple chart points in bulk.
     *
     * @param points List of chart points
     * @return this builder
     */
    public ModelBuilder addAll(List<ChartPoint> points) {
        model.addAll(points);
        return this;
    }

    /**
     * Add XY data from parallel arrays.
     *
     * @param x Array of X values
     * @param y Array of Y values
     * @return this builder
     */
    public ModelBuilder addXYArrays(double[] x, double[] y) {
        model.addXYArrays(x, y);
        return this;
    }

    /**
     * Build and return the chart model.
     *
     * @return configured DefaultChartModel
     */
    public DefaultChartModel build() {
        return model;
    }

    /**
     * Batch builder for financial data with fluent OHLC entry.
     */
    public static final class FinancialDataBuilder {
        private final String name;
        private final List<OHLCBar> bars = new ArrayList<>();

        private FinancialDataBuilder(String name) {
            this.name = name;
        }

        /**
         * Start building financial (OHLC) data.
         *
         * @param name Series name
         * @return new FinancialDataBuilder
         */
        public static FinancialDataBuilder create(String name) {
            return new FinancialDataBuilder(name);
        }

        /**
         * Add OHLC bar.
         *
         * @param time X-axis value (timestamp or index)
         * @param open Opening price
         * @param high Highest price
         * @param low Lowest price
         * @param close Closing price
         * @return this builder
         */
        public FinancialDataBuilder bar(double time, double open, double high, double low, double close) {
            bars.add(OHLCBar.of(time, open, high, low, close));
            return this;
        }

        /**
         * Add OHLC bar from domain object.
         *
         * @param bar OHLC data bar
         * @return this builder
         */
        public FinancialDataBuilder bar(OHLCBar bar) {
            bars.add(bar);
            return this;
        }

        /**
         * Build and return the chart model.
         *
         * @return configured DefaultChartModel with OHLC data
         */
        public DefaultChartModel build() {
            DefaultChartModel model = new DefaultChartModel(name);
            for (OHLCBar bar : bars) {
                model.addOHLC(bar);
            }
            return model;
        }
    }

    /**
     * Batch builder for statistical data with error bars.
     */
    public static final class StatisticalDataBuilder {
        private final String name;
        private final List<ErrorBarPoint> points = new ArrayList<>();

        private StatisticalDataBuilder(String name) {
            this.name = name;
        }

        /**
         * Start building statistical data with error bars.
         *
         * @param name Series name
         * @return new StatisticalDataBuilder
         */
        public static StatisticalDataBuilder create(String name) {
            return new StatisticalDataBuilder(name);
        }

        /**
         * Add point with symmetric error bars.
         *
         * @param x X-axis value
         * @param y Y-axis value (mean)
         * @param error Symmetric error margin (±)
         * @return this builder
         */
        public StatisticalDataBuilder point(double x, double y, double error) {
            points.add(ErrorBarPoint.symmetric(x, y, error));
            return this;
        }

        /**
         * Add point with asymmetric error bars.
         *
         * @param x X-axis value
         * @param y Y-axis value (mean)
         * @param errorLow Lower bound
         * @param errorHigh Upper bound
         * @return this builder
         */
        public StatisticalDataBuilder point(double x, double y, double errorLow, double errorHigh) {
            points.add(ErrorBarPoint.of(x, y, errorLow, errorHigh));
            return this;
        }

        /**
         * Add point from domain object.
         *
         * @param point Error bar data point
         * @return this builder
         */
        public StatisticalDataBuilder point(ErrorBarPoint point) {
            points.add(point);
            return this;
        }

        /**
         * Build and return the chart model.
         *
         * @return configured DefaultChartModel with error bar data
         */
        public DefaultChartModel build() {
            DefaultChartModel model = new DefaultChartModel(name);
            for (ErrorBarPoint point : points) {
                model.addWithError(point);
            }
            return model;
        }
    }
}
