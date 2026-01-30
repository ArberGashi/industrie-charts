package com.arbergashi.charts.domain.legend;

import java.util.Objects;

/**
 * Legend configuration for an {@code ArberChartPanel}.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class LegendConfig {
    public static final LegendConfig DEFAULT = new LegendConfig(
            LegendPlacement.OVERLAY,
            LegendPosition.TOP_LEFT,
            LegendDockSide.RIGHT
    );

    private LegendPlacement placement;
    private LegendPosition overlayPosition;
    private LegendDockSide dockSide;

    public LegendConfig(LegendPlacement placement, LegendPosition overlayPosition, LegendDockSide dockSide) {
        this.placement = placement != null ? placement : LegendPlacement.OVERLAY;
        this.overlayPosition = overlayPosition != null ? overlayPosition : LegendPosition.TOP_LEFT;
        this.dockSide = dockSide != null ? dockSide : LegendDockSide.RIGHT;
    }

    public static LegendConfig overlay(LegendPosition position) {
        return new LegendConfig(LegendPlacement.OVERLAY, position, LegendDockSide.RIGHT);
    }

    public static LegendConfig docked(LegendDockSide side) {
        Objects.requireNonNull(side, "side");
        return new LegendConfig(LegendPlacement.DOCKED, LegendPosition.TOP_LEFT, side);
    }

    public LegendPlacement getPlacement() {
        return placement;
    }

    public LegendConfig setPlacement(LegendPlacement placement) {
        this.placement = placement != null ? placement : LegendPlacement.OVERLAY;
        return this;
    }

    public LegendPosition getOverlayPosition() {
        return overlayPosition;
    }

    public LegendConfig setOverlayPosition(LegendPosition overlayPosition) {
        this.overlayPosition = overlayPosition != null ? overlayPosition : LegendPosition.TOP_LEFT;
        return this;
    }

    public LegendDockSide getDockSide() {
        return dockSide;
    }

    public LegendConfig setDockSide(LegendDockSide dockSide) {
        this.dockSide = dockSide != null ? dockSide : LegendDockSide.RIGHT;
        return this;
    }
}
