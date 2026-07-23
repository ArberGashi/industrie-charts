package com.arbergashi.charts.api;

@FunctionalInterface/**
 * @since 1.5.0
 * @author Arber Gashi
 */
public interface VitalSignsListener {
    void onVitalSigns(VitalSignsEvent event);
}

