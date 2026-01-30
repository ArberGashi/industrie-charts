package com.arbergashi.charts.engine.spatial;

/**
 * Predefined depth policies for spatial rendering.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialDepthPolicies {
    private static final SpatialDepthPolicy LAYERED = () -> SpatialDepthPolicy.Mode.LAYERED;
    private static final SpatialDepthPolicy SORTED_BACK_TO_FRONT = () -> SpatialDepthPolicy.Mode.SORTED_BACK_TO_FRONT;
    private static final SpatialDepthPolicy SORTED_FRONT_TO_BACK = () -> SpatialDepthPolicy.Mode.SORTED_FRONT_TO_BACK;

    private SpatialDepthPolicies() {
    }

    public static SpatialDepthPolicy getLayered() {
        return LAYERED;
    }

    public static SpatialDepthPolicy getSortedBackToFront() {
        return SORTED_BACK_TO_FRONT;
    }

    public static SpatialDepthPolicy getSortedFrontToBack() {
        return SORTED_FRONT_TO_BACK;
    }
}
