package com.arbergashi.charts.api;

import com.arbergashi.charts.util.AnimationUtils;
/**
 * Animation doctrine for ArberCharts.
 *
 * <p>Policy: data truth is never sacrificed for motion. Profiles define how much
 * animation is allowed for data transitions vs. UI overlays.</p>
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public enum AnimationProfile {
    /**
     * No animation. Immediate updates for mission-critical displays.
     */
    TACTICAL(false, false, AnimationUtils.EasingType.LINEAR, 0L, 1.0),
    /**
     * Calm, documentable motion.
     */
    ACADEMIC(true, true, AnimationUtils.EasingType.LINEAR, 140L, 0.18),
    /**
     * Premium feel for dashboards and executive views.
     */
    ENTERPRISE(true, true, AnimationUtils.EasingType.EASE_IN_OUT_CUBIC, 200L, 0.12);

    private final boolean animateData;
    private final boolean animateOverlays;
    private final AnimationUtils.EasingType overlayEasing;
    private final long overlayDurationMs;
    private final double dataLerpFactor;

    AnimationProfile(boolean animateData,
                     boolean animateOverlays,
                     AnimationUtils.EasingType overlayEasing,
                     long overlayDurationMs,
                     double dataLerpFactor) {
        this.animateData = animateData;
        this.animateOverlays = animateOverlays;
        this.overlayEasing = overlayEasing;
        this.overlayDurationMs = overlayDurationMs;
        this.dataLerpFactor = dataLerpFactor;
    }

    public boolean animatesData() {
        return animateData;
    }

    public boolean animatesOverlays() {
        return animateOverlays;
    }

    public AnimationUtils.EasingType overlayEasing() {
        return overlayEasing;
    }

    public long overlayDurationMs() {
        return overlayDurationMs;
    }

    /**
     * Smoothing factor for simple per-frame interpolation.
     * Tactical uses 1.0 (immediate), others use eased smoothing.
     */
    public double dataLerpFactor() {
        return dataLerpFactor;
    }

    public static AnimationProfile fromString(String value, AnimationProfile fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return AnimationProfile.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

