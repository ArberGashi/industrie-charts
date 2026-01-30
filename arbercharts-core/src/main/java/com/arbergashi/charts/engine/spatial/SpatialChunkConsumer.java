package com.arbergashi.charts.engine.spatial;

/**
 * Consumer for spatial buffer chunks.
 *
 * @since 1.7.0
 */
@FunctionalInterface
public interface SpatialChunkConsumer {
    /**
     * Processes a filled spatial buffer.
     *
     * @param buffer     spatial buffer containing input/output coords
     * @param pointCount number of valid points in the buffer
     */
    void accept(SpatialBuffer buffer, int pointCount);
}
