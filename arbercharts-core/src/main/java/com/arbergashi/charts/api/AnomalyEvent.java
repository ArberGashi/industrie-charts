package com.arbergashi.charts.api;

/**
 * Anomaly event emitted by predictive and gap renderers.
  * @author Arber Gashi
  * @version 2.0.0
  * @since 2026-01-30
 */
public final class AnomalyEvent {
    private double x;
    private double actual;
    private double predicted;
    private double delta;
    private double sigma;
    private int level;
    private long timestampNanos;

    public AnomalyEvent(double x, double actual, double predicted, double delta, double sigma, int level, long timestampNanos) {
        this.x = x;
        this.actual = actual;
        this.predicted = predicted;
        this.delta = delta;
        this.sigma = sigma;
        this.level = level;
        this.timestampNanos = timestampNanos;
    }

    public double getX() {
        return x;
    }

    public AnomalyEvent setX(double x) {
        this.x = x;
        return this;
    }

    public double getActual() {
        return actual;
    }

    public AnomalyEvent setActual(double actual) {
        this.actual = actual;
        return this;
    }

    public double getPredicted() {
        return predicted;
    }

    public AnomalyEvent setPredicted(double predicted) {
        this.predicted = predicted;
        return this;
    }

    public double getDelta() {
        return delta;
    }

    public AnomalyEvent setDelta(double delta) {
        this.delta = delta;
        return this;
    }

    public double getSigma() {
        return sigma;
    }

    public AnomalyEvent setSigma(double sigma) {
        this.sigma = sigma;
        return this;
    }

    public int getLevel() {
        return level;
    }

    public AnomalyEvent setLevel(int level) {
        this.level = level;
        return this;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public AnomalyEvent setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
        return this;
    }
}
