package com.arbergashi.charts.api;
/**
 * Non-linear coordinate transform for specialized renderers (e.g. Smith, Geo).
 *
 * <p>Implementations must be allocation-free and write into the provided arrays.</p>
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface CoordinateTransformer {
    /**
     * Thread-local scratch buffer to avoid per-call allocations in batch helpers.
     */
    ThreadLocal<double[]> SPATIAL_SCRATCH = ThreadLocal.withInitial(() -> new double[2]);

    /**
     * Maps model coordinates to pixel coordinates.
     *
     * @param context base plot context
     * @param x model X
     * @param y model Y
     * @param out output array (length >= 2) for pixel coordinates
     */
    void mapToPixel(PlotContext context, double x, double y, double[] out);

    /**
     * Maps pixel coordinates to model coordinates.
     *
     * @param context base plot context
     * @param pixelX pixel X
     * @param pixelY pixel Y
     * @param out output array (length >= 2) for model coordinates
     */
    void mapToData(PlotContext context, double pixelX, double pixelY, double[] out);

    /**
     * Batch maps model coordinates to pixel coordinates.
     *
     * <p>Input and output are packed as (x1,y1,x2,y2,...).</p>
     *
     * @param context base plot context
     * @param inputCoords packed model coordinates (x1,y1,x2,y2,...)
     * @param outputCoords packed pixel coordinates
     * @param pointCount number of points
     */
    default void mapToPixelBatch(PlotContext context, double[] inputCoords, double[] outputCoords, int pointCount) {
        if (inputCoords == null || outputCoords == null) {
            throw new IllegalArgumentException("input/output buffers required");
        }
        int required = pointCount * 2;
        if (inputCoords.length < required || outputCoords.length < required) {
            throw new IllegalArgumentException("buffers too small for pointCount");
        }
        double[] tmp = SPATIAL_SCRATCH.get();
        int j = 0;
        for (int i = 0; i < pointCount; i++, j += 2) {
            mapToPixel(context, inputCoords[j], inputCoords[j + 1], tmp);
            outputCoords[j] = tmp[0];
            outputCoords[j + 1] = tmp[1];
        }
    }

    /**
     * Batch maps model coordinates into a spatial buffer as (x,y,z) triples.
     *
     * <p>By default, the mapped pixels are written as x/y with z=0.</p>
     *
     * @param context base plot context
     * @param inputCoords packed model coordinates (x1,y1,x2,y2,...)
     * @param buffer spatial buffer to receive xyz triples
     * @param pointCount number of points
     */
    default void mapToSpatialBuffer(PlotContext context,
                                    double[] inputCoords,
                                    com.arbergashi.charts.engine.spatial.SpatialBuffer buffer,
                                    int pointCount) {
        if (inputCoords == null || buffer == null) {
            throw new IllegalArgumentException("input/buffer required");
        }
        int required = pointCount * 2;
        if (inputCoords.length < required) {
            throw new IllegalArgumentException("input buffer too small for pointCount");
        }
        if (buffer.getPointCapacity() < pointCount) {
            throw new IllegalArgumentException("spatial buffer too small for pointCount");
        }
        double[] out = buffer.getInputCoords();
        double[] tmp = SPATIAL_SCRATCH.get();
        int in = 0;
        int outIdx = 0;
        for (int i = 0; i < pointCount; i++, in += 2, outIdx += 3) {
            mapToPixel(context, inputCoords[in], inputCoords[in + 1], tmp);
            out[outIdx] = tmp[0];
            out[outIdx + 1] = tmp[1];
            out[outIdx + 2] = 0.0;
        }
    }
}
