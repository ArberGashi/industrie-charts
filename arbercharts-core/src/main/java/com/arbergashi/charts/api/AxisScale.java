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
        AxisConfig config = new AxisConfig();
        config.setMedicalScale(mmPerUnit);
        return config;
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

    public static final class AxisPair {
        private AxisConfig xAxis;
        private AxisConfig yAxis;

        public AxisPair(AxisConfig xAxis, AxisConfig yAxis) {
            if (xAxis == null || yAxis == null) {
                throw new IllegalArgumentException("AxisPair requires non-null xAxis and yAxis");
            }
            this.xAxis = xAxis;
            this.yAxis = yAxis;
        }

        public AxisConfig getXAxis() {
            return xAxis;
        }

        public AxisPair setXAxis(AxisConfig xAxis) {
            if (xAxis == null) {
                throw new IllegalArgumentException("xAxis");
            }
            this.xAxis = xAxis;
        return this;
        }

        public AxisConfig getYAxis() {
            return yAxis;
        }

        public AxisPair setYAxis(AxisConfig yAxis) {
            if (yAxis == null) {
                throw new IllegalArgumentException("yAxis");
            }
            this.yAxis = yAxis;
        return this;
        }
    }
}
