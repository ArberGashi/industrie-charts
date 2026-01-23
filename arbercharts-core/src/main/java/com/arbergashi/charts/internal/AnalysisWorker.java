package com.arbergashi.charts.internal;

/**
 * Internal worker for performing analytical calculations on primitive arrays.
 * This class is designed to be allocation-free in its core methods.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-01-01
 */
public final class AnalysisWorker {

    private AnalysisWorker() {
    }

    /**
     * Calculates Bollinger Bands from an array of price data.
     *
     * @param values       The input price data (e.g., close prices).
     * @param period       The moving average period (e.g., 20).
     * @param stdDevFactor The number of standard deviations for the bands (e.g., 2.0).
     * @param smaOut       Output array for the Simple Moving Average.
     * @param upperOut     Output array for the upper band.
     * @param lowerOut     Output array for the lower band.
     */
    public static void calculateBollingerBands(double[] values, int period, double stdDevFactor,
                                               double[] smaOut, double[] upperOut, double[] lowerOut) {
        final int n = values.length;
        if (n < period) return;

        double sum = 0;
        double sumSq = 0;

        // Initial window
        for (int i = 0; i < period; i++) {
            sum += values[i];
        }

        for (int i = period - 1; i < n; i++) {
            if (i >= period) {
                double oldVal = values[i - period];
                sum -= oldVal;
                sum += values[i];
            }

            double mean = sum / period;
            smaOut[i] = mean;

            // Calculate standard deviation for the current window
            sumSq = 0;
            for (int j = 0; j < period; j++) {
                double dev = values[i - j] - mean;
                sumSq += dev * dev;
            }
            double stdDev = Math.sqrt(sumSq / period);

            upperOut[i] = mean + stdDev * stdDevFactor;
            lowerOut[i] = mean - stdDev * stdDevFactor;
        }

        // Fill initial NaNs
        for (int i = 0; i < period - 1; i++) {
            smaOut[i] = Double.NaN;
            upperOut[i] = Double.NaN;
            lowerOut[i] = Double.NaN;
        }
    }
}
