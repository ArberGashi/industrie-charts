package com.arbergashi.charts.engine.spatial;

/**
 * Exposes spatial peak metadata for accessibility or logging purposes.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface SpatialPeakProvider {
    /**
     * Returns the latest spatial peak metadata snapshot.
     *
     * @return peak metadata container
     */
    SpatialPeakMetadata getSpatialPeakMetadata();
}
