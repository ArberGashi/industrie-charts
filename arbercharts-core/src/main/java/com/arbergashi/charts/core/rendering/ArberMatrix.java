package com.arbergashi.charts.core.rendering;

/**
 * Minimal 2D transform abstraction for headless rendering.
 * @author Arber Gashi
 * @since 2026-01-30
 */
public interface ArberMatrix {
    double scaleX();
    double scaleY();
    double translateX();
    double translateY();
}
