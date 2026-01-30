package com.arbergashi.charts.core.rendering;

/**
 * Convenience constructors for {@link ArberMatrix}.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ArberMatrices {
    private ArberMatrices() {}

    public static ArberMatrix identity() {
        return new ArberMatrix() {
            @Override public double scaleX() { return 1.0; }
            @Override public double scaleY() { return 1.0; }
            @Override public double translateX() { return 0.0; }
            @Override public double translateY() { return 0.0; }
        };
    }
}
