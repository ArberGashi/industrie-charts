package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * <h1>OHLCBar - Type-Safe Financial Data Point</h1>
 *
 * <p>Immutable record representing Open-High-Low-Close financial data.
 * Designed for end-user convenience with clear, self-documenting fields.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create OHLC bar for candlestick charts
 * OHLCBar bar = OHLCBar.of(0, 100.0, 105.0, 98.0, 103.0);
 *
 * // Add to chart model
 * DefaultChartModel model = new DefaultChartModel("AAPL");
 * model.addOHLC(bar);
 *
 * // Or create with timestamp
 * long timestamp = System.currentTimeMillis();
 * OHLCBar timestamped = OHLCBar.of(timestamp, 100, 105, 98, 103);
 * }</pre>
 *
 * <h2>Field Mapping:</h2>
 * <ul>
 *   <li><b>time:</b> X-axis value (timestamp, index, or ordinal)</li>
 *   <li><b>open:</b> Opening price</li>
 *   <li><b>high:</b> Highest price in period</li>
 *   <li><b>low:</b> Lowest price in period</li>
 *   <li><b>close:</b> Closing price</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-16
 */
public record OHLCBar(
        double time,
        double open,
        double high,
        double low,
        double close
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Validates OHLC data integrity.
     */
    public OHLCBar {
        if (!Double.isFinite(time) || !Double.isFinite(open) ||
            !Double.isFinite(high) || !Double.isFinite(low) || !Double.isFinite(close)) {
            throw new IllegalArgumentException("All OHLC values must be finite numbers");
        }

        if (high < Math.max(open, close) || low > Math.min(open, close)) {
            throw new IllegalArgumentException(
                String.format("Invalid OHLC data: high=%.2f, low=%.2f, open=%.2f, close=%.2f",
                    high, low, open, close)
            );
        }
    }

    /**
     * Factory method for creating OHLC bars.
     *
     * @param time X-axis value (timestamp or index)
     * @param open Opening price
     * @param high Highest price
     * @param low Lowest price
     * @param close Closing price
     * @return new OHLCBar instance
     */
    public static OHLCBar of(double time, double open, double high, double low, double close) {
        return new OHLCBar(time, open, high, low, close);
    }

    /**
     * Converts to ChartPoint for internal rendering.
     * Mapping: x→time, y→close, weight→open, min→low, max→high
     */
    public ChartPoint toChartPoint() {
        return ChartPoint.ofOHLC(time, open, high, low, close);
    }

    /**
     * Returns true if this is a bullish (green) candle.
     */
    public boolean isBullish() {
        return close >= open;
    }

    /**
     * Returns true if this is a bearish (red) candle.
     */
    public boolean isBearish() {
        return close < open;
    }

    /**
     * Returns the body size (absolute difference between open and close).
     */
    public double bodySize() {
        return Math.abs(close - open);
    }

    /**
     * Returns the upper wick size (high minus max of open/close).
     */
    public double upperWick() {
        return high - Math.max(open, close);
    }

    /**
     * Returns the lower wick size (min of open/close minus low).
     */
    public double lowerWick() {
        return Math.min(open, close) - low;
    }

    /**
     * Returns the total range (high - low).
     */
    public double range() {
        return high - low;
    }
}
