package com.arbergashi.charts.ui.legend;

/**
 * Callbacks for interactive legend actions.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public interface LegendActionListener {

    /** Toggle visibility for a series/layer. */
    void toggleSeries(String seriesId);

    /** Open settings/configuration UI for a series/layer. */
    void openSeriesSettings(String seriesId);

    /**
     * Solo the given series/layer (make it the only visible one).
     *
     * <p>This is typically triggered via a modifier click (e.g., Alt+Click) to match pro charting tools.</p>
     *
     * @param seriesId series/layer id
     */
    default void soloSeries(String seriesId) {
        // Optional. Implementers may ignore if not supported.
    }
}
