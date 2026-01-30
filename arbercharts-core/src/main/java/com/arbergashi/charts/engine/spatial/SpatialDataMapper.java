package com.arbergashi.charts.engine.spatial;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;

/**
 * Zero-allocation mapper for streaming model data into spatial buffers.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialDataMapper {
    /**
     * Z-value selection strategy for 2D series.
     */
    public enum ZMode {
        CONSTANT,
        FROM_Y,
        FROM_WEIGHT,
        FROM_INDEX,
        CUSTOM
    }

    private ZMode zMode = ZMode.CONSTANT;
    private double zValue;
    private ZComponentProvider zComponentProvider;
    private VectorizedSpatialOptimizer optimizer;

    public ZMode getZMode() {
        return zMode;
    }

    public SpatialDataMapper setZMode(ZMode zMode) {
        this.zMode = (zMode != null) ? zMode : ZMode.CONSTANT;
        return this;
    }

    public double getZValue() {
        return zValue;
    }

    public SpatialDataMapper setZValue(double zValue) {
        this.zValue = zValue;
        return this;
    }

    public ZComponentProvider getZComponentProvider() {
        return zComponentProvider;
    }

    public SpatialDataMapper setZComponentProvider(ZComponentProvider zComponentProvider) {
        this.zComponentProvider = zComponentProvider;
        return this;
    }

    public VectorizedSpatialOptimizer getOptimizer() {
        return optimizer;
    }

    public SpatialDataMapper setOptimizer(VectorizedSpatialOptimizer optimizer) {
        this.optimizer = optimizer;
        return this;
    }

    /**
     * Calculates the logical point count that can be mapped from the model.
     */
    public int getCalculatedPointCount(ChartModel model) {
        if (model == null) return 0;
        int count = model.getPointCount();
        if (count <= 0) return 0;
        double[] xs = model.getXData();
        double[] ys = model.getYData();
        if (xs == null || ys == null) return 0;
        return Math.min(count, Math.min(xs.length, ys.length));
    }

    /**
     * Calculates the number of chunks required to map the model into the buffer capacity.
     */
    public int getCalculatedChunkCount(ChartModel model, SpatialBuffer buffer) {
        if (buffer == null) return 0;
        int total = getCalculatedPointCount(model);
        if (total <= 0) return 0;
        int cap = Math.max(1, buffer.getPointCapacity());
        return (total + cap - 1) / cap;
    }

    /**
     * Maps a range of model points into the spatial buffer input coordinates.
     *
     * <p>Output is packed as xyz triples.</p>
     *
     * @param model       source model
     * @param startIndex  first point index
     * @param pointCount  number of points to map
     * @param buffer      destination spatial buffer
     * @return number of points written
     */
    public int mapToSpatialBuffer(ChartModel model, int startIndex, int pointCount, SpatialBuffer buffer) {
        if (model == null || buffer == null) return 0;
        if (startIndex < 0 || pointCount <= 0) return 0;

        double[] xs = model.getXData();
        double[] ys = model.getYData();
        if (xs == null || ys == null) return 0;

        int available = getCalculatedPointCount(model);
        if (available <= 0 || startIndex >= available) return 0;

        int capacity = buffer.getPointCapacity();
        int max = Math.min(pointCount, Math.min(capacity, available - startIndex));

        double[] out = buffer.getInputCoords();
        double[] weights = (zMode == ZMode.FROM_WEIGHT || zMode == ZMode.CUSTOM) ? model.getWeightData() : null;

        int in = startIndex;
        int outIdx = 0;
        for (int i = 0; i < max; i++, in++, outIdx += 3) {
            out[outIdx] = xs[in];
            out[outIdx + 1] = ys[in];
            out[outIdx + 2] = getCalculatedZValue(in, xs[in], ys[in], weights);
        }
        return max;
    }

    /**
     * Maps a range of model points into the spatial buffer using a coordinate transformer.
     *
     * <p>Output is packed as xyz triples.</p>
     *
     * @param model       source model
     * @param startIndex  first point index
     * @param pointCount  number of points to map
     * @param buffer      destination spatial buffer
     * @param transformer coordinate transformer (may be null)
     * @param context     plot context for the transformer (may be null)
     * @return number of points written
     */
    public int mapToSpatialBuffer(ChartModel model,
                                  int startIndex,
                                  int pointCount,
                                  SpatialBuffer buffer,
                                  CoordinateTransformer transformer,
                                  PlotContext context) {
        if (transformer == null || context == null) {
            return mapToSpatialBuffer(model, startIndex, pointCount, buffer);
        }
        if (model == null || buffer == null) return 0;
        if (startIndex < 0 || pointCount <= 0) return 0;

        double[] xs = model.getXData();
        double[] ys = model.getYData();
        if (xs == null || ys == null) return 0;

        int available = getCalculatedPointCount(model);
        if (available <= 0 || startIndex >= available) return 0;

        int capacity = buffer.getPointCapacity();
        int max = Math.min(pointCount, Math.min(capacity, available - startIndex));

        double[] out = buffer.getInputCoords();
        double[] weights = (zMode == ZMode.FROM_WEIGHT || zMode == ZMode.CUSTOM) ? model.getWeightData() : null;
        double[] tmp = CoordinateTransformer.SPATIAL_SCRATCH.get();

        int in = startIndex;
        int outIdx = 0;
        for (int i = 0; i < max; i++, in++, outIdx += 3) {
            transformer.mapToPixel(context, xs[in], ys[in], tmp);
            out[outIdx] = tmp[0];
            out[outIdx + 1] = tmp[1];
            out[outIdx + 2] = getCalculatedZValue(in, xs[in], ys[in], weights);
        }
        return max;
    }
    /**
     * Streams the entire model into the spatial buffer in fixed-size chunks.
     *
     * @param model    source model
     * @param buffer   destination buffer
     * @param consumer consumer invoked for each filled chunk
     */
    public void mapAll(ChartModel model, SpatialBuffer buffer, SpatialChunkConsumer consumer) {
        if (model == null || buffer == null || consumer == null) return;
        int total = getCalculatedPointCount(model);
        if (total <= 0) return;
        int capacity = Math.max(1, buffer.getPointCapacity());
        if (optimizer != null) {
            optimizer.reset();
        }
        for (int offset = 0; offset < total; offset += capacity) {
            int remaining = total - offset;
            int count = Math.min(capacity, remaining);
            int written = mapToSpatialBuffer(model, offset, count, buffer);
            if (written > 0) {
                consumer.accept(buffer, written);
            }
        }
    }

    /**
     * Streams the entire model into the spatial buffer using a coordinate transformer.
     *
     * @param model       source model
     * @param buffer      destination buffer
     * @param transformer coordinate transformer (may be null)
     * @param context     plot context for the transformer (may be null)
     * @param consumer    consumer invoked for each filled chunk
     */
    public void mapAll(ChartModel model,
                       SpatialBuffer buffer,
                       CoordinateTransformer transformer,
                       PlotContext context,
                       SpatialChunkConsumer consumer) {
        if (model == null || buffer == null || consumer == null) return;
        int total = getCalculatedPointCount(model);
        if (total <= 0) return;
        int capacity = Math.max(1, buffer.getPointCapacity());
        if (optimizer != null) {
            optimizer.reset();
        }
        for (int offset = 0; offset < total; offset += capacity) {
            int remaining = total - offset;
            int count = Math.min(capacity, remaining);
            int written = mapToSpatialBuffer(model, offset, count, buffer, transformer, context);
            if (written > 0) {
                consumer.accept(buffer, written);
            }
        }
    }

    /**
     * Streams the model through an optional transform and projector chain.
     *
     * <p>Flow: model -> buffer input -> matrix transform (optional) -> projector (optional) -> buffer output.</p>
     *
     * @param model      source model
     * @param buffer     spatial buffer
     * @param transform  optional transform (may be null)
     * @param projector  optional projector (may be null)
     * @param consumer   consumer invoked after each processed chunk
     */
    public void mapAllProjected(ChartModel model,
                                SpatialBuffer buffer,
                                Matrix4x4 transform,
                                SpatialProjector projector,
                                SpatialChunkConsumer consumer) {
        if (model == null || buffer == null || consumer == null) return;
        int total = getCalculatedPointCount(model);
        if (total <= 0) return;
        int capacity = Math.max(1, buffer.getPointCapacity());
        for (int offset = 0; offset < total; offset += capacity) {
            int remaining = total - offset;
            int count = Math.min(capacity, remaining);
            int written = mapToSpatialBuffer(model, offset, count, buffer);
            if (written <= 0) continue;
            if (optimizer != null) {
                written = optimizer.applyBoundsFilter(buffer, written, null, false);
                if (written <= 0) continue;
            }

            double[] in = buffer.getInputCoords();
            double[] out = buffer.getOutputCoords();
            if (transform != null) {
                transform.getCalculatedTransformBatch(in, out, written, buffer.getScratch());
            } else {
                System.arraycopy(in, 0, out, 0, written * 3);
            }

            if (projector != null) {
                projector.getCalculatedProjectionBatch(out, in, written);
                consumer.accept(buffer, written);
            } else {
                consumer.accept(buffer, written);
            }
        }
    }

    /**
     * Streams the model through an optional transform and projector chain using a coordinate transformer.
     *
     * <p>Flow: model -> buffer input -> coordinate transform (optional) -> matrix transform (optional)
     * -> projector (optional) -> buffer output.</p>
     *
     * @param model       source model
     * @param buffer      spatial buffer
     * @param transform   optional transform (may be null)
     * @param projector   optional projector (may be null)
     * @param transformer coordinate transformer (may be null)
     * @param context     plot context for the transformer (may be null)
     * @param consumer    consumer invoked after each processed chunk
     */
    public void mapAllProjected(ChartModel model,
                                SpatialBuffer buffer,
                                Matrix4x4 transform,
                                SpatialProjector projector,
                                CoordinateTransformer transformer,
                                PlotContext context,
                                SpatialChunkConsumer consumer) {
        if (model == null || buffer == null || consumer == null) return;
        int total = getCalculatedPointCount(model);
        if (total <= 0) return;
        int capacity = Math.max(1, buffer.getPointCapacity());
        for (int offset = 0; offset < total; offset += capacity) {
            int remaining = total - offset;
            int count = Math.min(capacity, remaining);
            int written = mapToSpatialBuffer(model, offset, count, buffer, transformer, context);
            if (written <= 0) continue;
            if (optimizer != null && context != null) {
                written = optimizer.applyBoundsFilter(buffer, written, context, transformer != null);
                if (written <= 0) continue;
            }

            double[] in = buffer.getInputCoords();
            double[] out = buffer.getOutputCoords();
            if (transform != null) {
                transform.getCalculatedTransformBatch(in, out, written, buffer.getScratch());
            } else {
                System.arraycopy(in, 0, out, 0, written * 3);
            }

            if (projector != null) {
                projector.getCalculatedProjectionBatch(out, in, written);
                consumer.accept(buffer, written);
            } else {
                consumer.accept(buffer, written);
            }
        }
    }

    /**
     * Streams only the newly written ring-buffer points through the spatial pipeline.
     *
     * <p>Uses the ring delta cursor maintained by {@link SpatialBuffer}.</p>
     *
     * @param buffer spatial ring buffer
     * @param transform optional transform
     * @param projector optional projector
     * @param consumer chunk consumer
     */
    public void mapRingDeltaProjected(SpatialBuffer buffer,
                                      Matrix4x4 transform,
                                      SpatialProjector projector,
                                      SpatialChunkConsumer consumer) {
        if (buffer == null || consumer == null) return;
        if (!buffer.isRingEnabled()) return;
        int delta = buffer.getRingDeltaCount();
        if (delta <= 0) return;
        int start = buffer.getRingDeltaStartLogical();
        if (delta > buffer.getPointCapacity()) {
            delta = buffer.getPointCapacity();
        }

        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();
        int outIdx = 0;
        for (int i = 0; i < delta; i++, outIdx += 3) {
            int logical = start + i;
            int phys = buffer.getRingPhysicalIndex(logical);
            if (phys < 0) continue;
            int base = phys * 3;
            out[outIdx] = in[base];
            out[outIdx + 1] = in[base + 1];
            out[outIdx + 2] = in[base + 2];
        }

        if (transform != null) {
            transform.getCalculatedTransformBatch(out, in, delta, buffer.getScratch());
        } else {
            System.arraycopy(out, 0, in, 0, delta * 3);
        }

        if (projector != null) {
            projector.getCalculatedProjectionBatch(in, out, delta);
            System.arraycopy(out, 0, in, 0, delta * 3);
        }

        consumer.accept(buffer, delta);
        buffer.consumeRingDelta();
    }

    private double getCalculatedZValue(int index, double xValue, double yValue, double[] weights) {
        return switch (zMode) {
            case FROM_Y -> yValue;
            case FROM_WEIGHT -> (weights == null || index >= weights.length) ? 0.0 : weights[index];
            case FROM_INDEX -> index;
            case CUSTOM -> {
                if (zComponentProvider == null) {
                    yield zValue;
                }
                double weight = (weights == null || index >= weights.length) ? 0.0 : weights[index];
                yield zComponentProvider.getCalculatedZ(index, xValue, yValue, weight);
            }
            case CONSTANT -> zValue;
        };
    }
}
