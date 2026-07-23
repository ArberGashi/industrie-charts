package com.arbergashi.charts.engine.spatial;

/**
 * Runtime guard for the incubating Vector API used by the spatial engine.
 *
 * <p>The {@code jdk.incubator.vector} module is optional at runtime. This class
 * probes availability exactly once without referencing any incubator types
 * itself; when the module is not resolved (no {@code --add-modules
 * jdk.incubator.vector} flag), the spatial engine transparently falls back to
 * scalar code paths and {@link #getLaneCount()} reports {@code 1}.</p>
 *
 * <p>SIMD can be disabled explicitly for benchmarking or diagnostics via the
 * system property {@code arbercharts.simd.disabled=true}.</p>
 *
 * @since 1.7.0
 * @author Arber Gashi
 */
public final class VectorIntrinsics {
    private static final boolean AVAILABLE;
    private static final int LANE_COUNT;

    static {
        boolean available = false;
        int lanes = 1;
        if (!Boolean.getBoolean("arbercharts.simd.disabled")) {
            try {
                lanes = SpatialSimdKernels.preferredLaneCount();
                available = true;
            } catch (Throwable t) {
                available = false;
                lanes = 1;
            }
        }
        AVAILABLE = available;
        LANE_COUNT = lanes;
    }

    private VectorIntrinsics() {
    }

    /**
     * Returns whether the Vector API is available and SIMD paths may be taken.
     *
     * @return true if {@code jdk.incubator.vector} is resolved and SIMD is enabled
     * @since 2.0.1
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Returns the preferred species when the Vector API is available.
     *
     * @return preferred double species
     * @throws IllegalStateException if the Vector API is not available at runtime
     */
    public static jdk.incubator.vector.VectorSpecies<Double> getPreferredSpecies() {
        if (!AVAILABLE) {
            throw new IllegalStateException(
                    "Vector API not available (run with --add-modules jdk.incubator.vector)");
        }
        return SpatialSimdKernels.preferredSpecies();
    }

    /**
     * Returns the preferred SIMD lane count, or {@code 1} when the Vector API
     * is unavailable and scalar fallbacks are in effect.
     *
     * @return lane count (&gt;= 1)
     */
    public static int getLaneCount() {
        return LANE_COUNT;
    }
}
