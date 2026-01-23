package com.arbergashi.charts.ui.legend;

import java.awt.*;

/**
 * A single row in the interactive legend.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public record LegendSeriesRow(
        /** Stable series identifier used for toggling/visibility. */
        String id,
        /** Display name shown in the legend. */
        String name,
        /** Swatch color used for the series marker. */
        Color color,
        /** Whether the series is currently visible. */
        boolean visible,
        /** Whether a settings action should be shown. */
        boolean hasSettings,
        /** Optional live values to display next to the name. */
        java.util.Map<String, Object> values
) {
}
