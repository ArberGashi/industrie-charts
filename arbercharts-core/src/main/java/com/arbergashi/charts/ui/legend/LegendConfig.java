package com.arbergashi.charts.ui.legend;

import java.util.Objects;

/**
 * Legend configuration for an {@code ArberChartPanel}.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public record LegendConfig(
        /** Whether the legend is painted as an overlay or docked outside the plot. */
        LegendPlacement placement,
        /** Anchor position for overlay legends (ignored when docked). */
        LegendPosition overlayPosition,
        /** Docking side for docked legends (ignored when overlay). */
        LegendDockSide dockSide
) {

    /** Default legend configuration (overlay, top-left, right dock fallback). */
    public static final LegendConfig DEFAULT = new LegendConfig(LegendPlacement.OVERLAY, LegendPosition.TOP_LEFT, LegendDockSide.RIGHT);

    /**
     * Creates a legend configuration with null-safe defaults.
     *
     * @param placement overlay vs docked placement
     * @param overlayPosition overlay anchor position
     * @param dockSide docking side for docked legends
     */
    public LegendConfig {
        placement = placement != null ? placement : LegendPlacement.OVERLAY;
        overlayPosition = overlayPosition != null ? overlayPosition : LegendPosition.TOP_LEFT;
        dockSide = dockSide != null ? dockSide : LegendDockSide.RIGHT;
    }

    /**
     * Creates an overlay legend configuration.
     *
     * @param position overlay position (defaults to top-left when null)
     * @return legend configuration in overlay mode
     */
    public static LegendConfig overlay(LegendPosition position) {
        return new LegendConfig(LegendPlacement.OVERLAY, position, LegendDockSide.RIGHT);
    }

    /**
     * Creates a docked legend configuration.
     *
     * @param side docking side (non-null)
     * @return legend configuration in docked mode
     */
    public static LegendConfig docked(LegendDockSide side) {
        Objects.requireNonNull(side, "side");
        return new LegendConfig(LegendPlacement.DOCKED, LegendPosition.TOP_LEFT, side);
    }
}
