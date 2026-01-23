package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * <h1>ErrorBarPoint - Type-Safe Statistical Data Point</h1>
 *
 * <p>Immutable record representing a data point with error bars or uncertainty range.
 * Designed for scientific data, statistical analysis, and uncertainty visualization.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create point with symmetric error bars
 * ErrorBarPoint point = ErrorBarPoint.symmetric(1.0, 100.0, 5.0);
 *
 * // Create point with asymmetric error bars
 * ErrorBarPoint asymmetric = ErrorBarPoint.of(2.0, 100.0, 95.0, 108.0);
 *
 * // Add to chart model
 * DefaultChartModel model = new DefaultChartModel("Temperature");
 * model.addWithError(point);
 * }</pre>
 *
 * <h2>Field Mapping:</h2>
 * <ul>
 *   <li><b>x:</b> X-axis value (time, category, independent variable)</li>
 *   <li><b>y:</b> Y-axis value (measurement, mean, central value)</li>
 *   <li><b>errorLow:</b> Lower bound of uncertainty range</li>
 *   <li><b>errorHigh:</b> Upper bound of uncertainty range</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-16
 */
public record ErrorBarPoint(
        double x,
        double y,
        double errorLow,
        double errorHigh
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Validates error bar data integrity.
     */
    public ErrorBarPoint {
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

    /**
     * Factory method for creating error bar points with asymmetric errors.
     *
     * @param x X-axis value
     * @param y Y-axis value (central measurement)
     * @param errorLow Lower bound of uncertainty
     * @param errorHigh Upper bound of uncertainty
     * @return new ErrorBarPoint instance
     */
    public static ErrorBarPoint of(double x, double y, double errorLow, double errorHigh) {
        return new ErrorBarPoint(x, y, errorLow, errorHigh);
    }

    /**
     * Factory method for creating error bar points with symmetric errors.
     *
     * @param x X-axis value
     * @param y Y-axis value (central measurement)
     * @param error Symmetric error margin (±)
     * @return new ErrorBarPoint instance
     */
    public static ErrorBarPoint symmetric(double x, double y, double error) {
        return new ErrorBarPoint(x, y, y - error, y + error);
    }

    /**
     * Factory method for creating error bar points with standard deviation.
     *
     * @param x X-axis value
     * @param mean Mean value
     * @param stdDev Standard deviation
     * @param sigmas Number of standard deviations (e.g., 1.96 for 95% CI)
     * @return new ErrorBarPoint instance
     */
    public static ErrorBarPoint withStdDev(double x, double mean, double stdDev, double sigmas) {
        double margin = stdDev * sigmas;
        return new ErrorBarPoint(x, mean, mean - margin, mean + margin);
    }

    /**
     * Converts to ChartPoint for internal rendering.
     * Mapping: x→x, y→y, min→errorLow, max→errorHigh
     */
    public ChartPoint toChartPoint() {
        return ChartPoint.ofRange(x, y, errorLow, errorHigh);
    }

    /**
     * Returns the lower error margin (y - errorLow).
     */
    public double lowerError() {
        return y - errorLow;
    }

    /**
     * Returns the upper error margin (errorHigh - y).
     */
    public double upperError() {
        return errorHigh - y;
    }

    /**
     * Returns the total uncertainty range (errorHigh - errorLow).
     */
    public double range() {
        return errorHigh - errorLow;
    }

    /**
     * Returns true if error bars are symmetric.
     */
    public boolean isSymmetric() {
        return Math.abs(lowerError() - upperError()) < 1e-9;
    }
}
