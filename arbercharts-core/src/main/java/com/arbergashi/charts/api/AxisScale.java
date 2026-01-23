package com.arbergashi.charts.api;

/**
 * Convenience helpers for common axis scale presets.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class AxisScale {

    private AxisScale() {
    }

    /**
     * Creates an axis config for a medical-style scale (mm per unit).
     *
     * @param mmPerUnit millimeters per data unit
     * @return configured AxisConfig
     */
    public static AxisConfig medical(double mmPerUnit) {
        return new AxisConfig().medicalScale(mmPerUnit);
    }

    /**
     * Creates X/Y axis configs for medical plots (mm per second, mm per mV).
     *
     * @param mmPerSec millimeters per second
     * @param mmPerMv  millimeters per millivolt
     * @return axis pair with preconfigured scales
     */
    public static AxisPair ofMedical(double mmPerSec, double mmPerMv) {
        return new AxisPair(medical(mmPerSec), medical(mmPerMv));
    }

    public record AxisPair(AxisConfig xAxis, AxisConfig yAxis) {
    }
}
