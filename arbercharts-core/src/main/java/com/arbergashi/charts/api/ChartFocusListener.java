package com.arbergashi.charts.api;

/**
 * Listener for chart focus updates.
 *
 * <p>Focus updates are emitted whenever the crosshair (or touch long-press) changes the
 * active data position. Implementations should be fast; handlers may run on the EDT.</p>
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2.0.0
 */
@FunctionalInterface/**
 * @since 1.5.0
 */
public interface ChartFocusListener {

    /**
     * Called when chart focus changes.
     *
     * @param focus immutable focus state (never {@code null})
     */
    void onFocusChanged(ChartFocus focus);
}
