package com.arbergashi.charts.util;
/**
 * Preset profiles for physical (mm-based) chart scaling.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PhysicalScaleProfiles {

    /**
     * ISO EKG standard: 25 mm/s and 10 mm/unit (mV).
     */
    public static final Profile ISO_EKG_STANDARD = new Profile("ISO_EKG_STANDARD", 25.0, 10.0);

    private PhysicalScaleProfiles() {
    }

    /**
     * Applies the given profile to the global ChartAssets properties.
     */
    public static void apply(Profile profile) {
        if (profile == null) return;
        ChartAssets.setProperty("Chart.scale.physical.enabled", "true");
        ChartAssets.setProperty("Chart.scale.physical.ratio", Double.toString(profile.getXMmPerUnit()));
        ChartAssets.setProperty("Chart.scale.physical.y.enabled", "true");
        ChartAssets.setProperty("Chart.scale.physical.y.ratio", Double.toString(profile.getYMmPerUnit()));
    }

    /**
     * Simple immutable profile holder.
     */
    public static final class Profile {
        private String name;
        private double xMmPerUnit;
        private double yMmPerUnit;

        public Profile(String name, double xMmPerUnit, double yMmPerUnit) {
            this.name = name;
            this.xMmPerUnit = xMmPerUnit;
            this.yMmPerUnit = yMmPerUnit;
        }

        public String getName() {
            return name;
        }

        public Profile setName(String name) {
            this.name = name;
        return this;
        }

        public double getXMmPerUnit() {
            return xMmPerUnit;
        }

        public Profile setXMmPerUnit(double xMmPerUnit) {
            this.xMmPerUnit = xMmPerUnit;
        return this;
        }

        public double getYMmPerUnit() {
            return yMmPerUnit;
        }

        public Profile setYMmPerUnit(double yMmPerUnit) {
            this.yMmPerUnit = yMmPerUnit;
        return this;
        }
    }
}
