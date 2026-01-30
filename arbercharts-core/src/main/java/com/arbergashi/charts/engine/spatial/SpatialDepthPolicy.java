package com.arbergashi.charts.engine.spatial;

/**
 * Strategy for ordering spatial chunks by depth.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface SpatialDepthPolicy {
    enum Mode {
        LAYERED,
        SORTED_BACK_TO_FRONT,
        SORTED_FRONT_TO_BACK
    }

    Mode getMode();

    default boolean isSorted() {
        return getMode() != Mode.LAYERED;
    }
}
