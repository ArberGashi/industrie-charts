package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Type-safe OHLC data point for financial charts.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class OHLCBar implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private double time;
    private double open;
    private double high;
    private double low;
    private double close;

    public OHLCBar(double time, double open, double high, double low, double close) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        validate();
    }

    public static OHLCBar of(double time, double open, double high, double low, double close) {
        return new OHLCBar(time, open, high, low, close);
    }

    public double getTime() {
        return time;
    }

    public OHLCBar setTime(double time) {
        double prev = this.time;
        this.time = time;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.time = prev;
            throw ex;
        }
        return this;
    }

    public double getOpen() {
        return open;
    }

    public OHLCBar setOpen(double open) {
        double prev = this.open;
        this.open = open;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.open = prev;
            throw ex;
        }
        return this;
    }

    public double getHigh() {
        return high;
    }

    public OHLCBar setHigh(double high) {
        double prev = this.high;
        this.high = high;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.high = prev;
            throw ex;
        }
        return this;
    }

    public double getLow() {
        return low;
    }

    public OHLCBar setLow(double low) {
        double prev = this.low;
        this.low = low;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.low = prev;
            throw ex;
        }
        return this;
    }

    public double getClose() {
        return close;
    }

    public OHLCBar setClose(double close) {
        double prev = this.close;
        this.close = close;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.close = prev;
            throw ex;
        }
        return this;
    }

    public ChartPoint toChartPoint() {
        return ChartPoint.ofOHLC(time, open, high, low, close);
    }

    public boolean isBullish() {
        return close >= open;
    }

    public boolean isBearish() {
        return close < open;
    }

    public double bodySize() {
        return Math.abs(close - open);
    }

    public double upperWick() {
        return high - Math.max(open, close);
    }

    public double lowerWick() {
        return Math.min(open, close) - low;
    }

    public double range() {
        return high - low;
    }

    private void validate() {
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
}
