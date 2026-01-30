package com.arbergashi.charts.api;

import java.util.Arrays;
/**
 * Default reusable prediction buffer.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class DefaultPredictionBuffer implements PredictionBuffer {
    private double[] xs = new double[0];
    private double[] ys = new double[0];
    private double[] confidence = new double[0];
    private int count;
    private double residualStd;
    private double residualScale = 1.0;

    @Override
    public void ensureCapacity(int capacity) {
        if (capacity <= xs.length) return;
        int newCap = nextPowerOfTwo(capacity);
        xs = Arrays.copyOf(xs, newCap);
        ys = Arrays.copyOf(ys, newCap);
        confidence = Arrays.copyOf(confidence, newCap);
    }

    @Override
    public double[] x() {
        return xs;
    }

    @Override
    public double[] y() {
        return ys;
    }

    @Override
    public double[] confidence() {
        return confidence;
    }

    @Override
    public DefaultPredictionBuffer setCount(int count) {
        this.count = Math.max(0, count);
        return this;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public DefaultPredictionBuffer setResidualStd(double std) {
        this.residualStd = (Double.isFinite(std) && std >= 0.0) ? std : 0.0;
        return this;
    }

    @Override
    public DefaultPredictionBuffer setResidualScale(double scale) {
        this.residualScale = (Double.isFinite(scale) && scale > 0.0) ? scale : 1.0;
        return this;
    }

    @Override
    public double residualStd() {
        return residualStd;
    }

    @Override
    public double residualScale() {
        return residualScale;
    }

    private static int nextPowerOfTwo(int v) {
        int n = 1;
        while (n < v && n > 0) n <<= 1;
        return (n > 0) ? n : v;
    }
}
