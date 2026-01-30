package com.arbergashi.charts.core.geometry;

/**
 * Pure data anchor used for text or label placement in headless core logic.
 *
 * <p>Rendering backends decide how to translate these anchors into pixel offsets
 * based on font metrics and platform-specific alignment rules.</p>
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public enum TextAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    MIDDLE_LEFT,
    CENTER,
    MIDDLE_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    BASELINE_LEFT,
    BASELINE_CENTER,
    BASELINE_RIGHT
}
