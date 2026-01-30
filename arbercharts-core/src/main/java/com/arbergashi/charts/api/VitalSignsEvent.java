package com.arbergashi.charts.api;

/**
 * Vital signs event (e.g., BPM threshold breach).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class VitalSignsEvent {
    private double bpm;
    private double lowThreshold;
    private double highThreshold;
    private VitalSignsLevel level;
    private long timestampNanos;

    public VitalSignsEvent(double bpm, double lowThreshold, double highThreshold, VitalSignsLevel level, long timestampNanos) {
        this.bpm = bpm;
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
        this.level = level;
        this.timestampNanos = timestampNanos;
    }

    public double getBpm() {
        return bpm;
    }

    public VitalSignsEvent setBpm(double bpm) {
        this.bpm = bpm;
        return this;
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public VitalSignsEvent setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
        return this;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public VitalSignsEvent setHighThreshold(double highThreshold) {
        this.highThreshold = highThreshold;
        return this;
    }

    public VitalSignsLevel getLevel() {
        return level;
    }

    public VitalSignsEvent setLevel(VitalSignsLevel level) {
        this.level = level;
        return this;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public VitalSignsEvent setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
        return this;
    }

    public boolean isAlarm() {
        return level != VitalSignsLevel.NORMAL;
    }
}
