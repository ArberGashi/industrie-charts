package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.core.geometry.ArberRect;

/** Shared plot geometry for polar data and their external grid. */
public final class PolarGeometry {
    private PolarGeometry() {
    }

    /**
     * Returns the common outer radius, leaving room for labels around the plot.
     * @param bounds plot bounds in canvas units
     * @return non-negative radius in canvas units
     */
    public static double radius(ArberRect bounds) {
        return Math.max(0.0, Math.min(bounds.width(), bounds.height())) * 0.425;
    }
}
