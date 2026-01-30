package com.arbergashi.charts.model;
/**
 * Provenance flags for per-point audit metadata.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class ProvenanceFlags {
    private ProvenanceFlags() {
    }

    public static final byte ORIGINAL = 0;
    public static final byte SMOOTHED = 1;
    public static final byte MANUAL = 2;
    public static final byte SYNTHETIC = 3;

    public static String label(byte flag) {
        return switch (flag) {
            case SMOOTHED -> "Smoothed";
            case MANUAL -> "Manual";
            case SYNTHETIC -> "Synthetic";
            default -> "Original";
        };
    }
}
