package com.arbergashi.charts.api;

/**
 * Listener for predictive anomaly events.
 */
@FunctionalInterface/**
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface AnomalyListener {
    void onAnomaly(AnomalyEvent event);
}

