package com.arbergashi.charts.api;

@FunctionalInterface/**
 * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface VitalSignsListener {
    void onVitalSigns(VitalSignsEvent event);
}

