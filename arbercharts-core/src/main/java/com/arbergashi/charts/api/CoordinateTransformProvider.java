package com.arbergashi.charts.api;
/**
 * Optional hook for renderers that require a non-linear coordinate transform.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface CoordinateTransformProvider {
    /**
     * Provides a coordinate transformer for the renderer.
     *
     * @return transformer instance, or {@code null} for linear mapping
     */
    CoordinateTransformer getCoordinateTransformer();
}
