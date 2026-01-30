package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data point with error bars or uncertainty range.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ErrorBarPoint implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private double errorLow;
    private double errorHigh;

    public ErrorBarPoint(double x, double y, double errorLow, double errorHigh) {
        this.x = x;
        this.y = y;
        this.errorLow = errorLow;
        this.errorHigh = errorHigh;
        validate();
    }

    public static ErrorBarPoint of(double x, double y, double errorLow, double errorHigh) {
        return new ErrorBarPoint(x, y, errorLow, errorHigh);
    }

    public static ErrorBarPoint symmetric(double x, double y, double error) {
        return new ErrorBarPoint(x, y, y - error, y + error);
    }

    public static ErrorBarPoint ofStdDev(double x, double mean, double stdDev, double sigmas) {
        double margin = stdDev * sigmas;
        return new ErrorBarPoint(x, mean, mean - margin, mean + margin);
    }

    public double getX() {
        return x;
    }

    public ErrorBarPoint setX(double x) {
        double prev = this.x;
        this.x = x;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.x = prev;
            throw ex;
        }
        return this;
    }

    public double getY() {
        return y;
    }

    public ErrorBarPoint setY(double y) {
        double prev = this.y;
        this.y = y;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.y = prev;
            throw ex;
        }
        return this;
    }

    public double getErrorLow() {
        return errorLow;
    }

    public ErrorBarPoint setErrorLow(double errorLow) {
        double prev = this.errorLow;
        this.errorLow = errorLow;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.errorLow = prev;
            throw ex;
        }
        return this;
    }

    public double getErrorHigh() {
        return errorHigh;
    }

    public ErrorBarPoint setErrorHigh(double errorHigh) {
        double prev = this.errorHigh;
        this.errorHigh = errorHigh;
        try {
            validate();
        } catch (RuntimeException ex) {
            this.errorHigh = prev;
            throw ex;
        }
        return this;
    }

    public ChartPoint toChartPoint() {
        return ChartPoint.ofRange(x, y, errorLow, errorHigh);
    }

    public double lowerError() {
        return y - errorLow;
    }

    public double upperError() {
        return errorHigh - y;
    }

    public double range() {
        return errorHigh - errorLow;
    }

    public boolean isSymmetric() {
        return Math.abs(lowerError() - upperError()) < 1e-9;
    }

    private void validate() {
        if (!Double.isFinite(x) || !Double.isFinite(y) ||
                !Double.isFinite(errorLow) || !Double.isFinite(errorHigh)) {
            throw new IllegalArgumentException("All error bar values must be finite numbers");
        }
        if (errorLow > y || errorHigh < y) {
            throw new IllegalArgumentException(
                    String.format("Invalid error bars: y=%.2f must be between errorLow=%.2f and errorHigh=%.2f",
                            y, errorLow, errorHigh)
            );
        }
    }
}
