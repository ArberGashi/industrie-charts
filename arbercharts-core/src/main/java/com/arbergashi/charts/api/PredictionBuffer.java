package com.arbergashi.charts.api;
/**
 * Allocation-conscious buffer for predicted points.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface PredictionBuffer {

    /**
     * Ensures capacity for the given number of points.
     */
    void ensureCapacity(int capacity);

    /**
     * Returns the reusable X buffer.
     */
    double[] x();

    /**
     * Returns the reusable Y buffer.
     */
    double[] y();

    /**
     * Returns the reusable confidence buffer in range [0..1].
     */
    double[] confidence();

    /**
     * Sets the logical point count written into the buffers.
     */
    PredictionBuffer setCount(int count);

    /**
     * Returns the logical point count.
     */
    int count();

    /**
     * Residual standard deviation used to score anomalies.
     */
    PredictionBuffer setResidualStd(double std);

    /**
     * Residual scaling factor used by the predictor.
     */
    PredictionBuffer setResidualScale(double scale);

    /**
     * Returns the residual standard deviation.
     */
    double residualStd();

    /**
     * Returns the residual scaling factor.
     */
    double residualScale();
}
