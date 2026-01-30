package com.arbergashi.charts.engine.spatial;

import java.util.Arrays;

/**
 * Buffering consumer that optionally sorts chunks by depth before forwarding.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class DepthAwareBatchConsumer implements SpatialChunkConsumer {
    private final SpatialPathBatchBuilder builder;
    private SpatialDepthPolicy depthPolicy;

    private SpatialBuffer[] buffers;
    private int[] counts;
    private long[] sortKeys;
    private int size;

    private final double[] depthTmp = new double[3];

    public DepthAwareBatchConsumer(SpatialPathBatchBuilder builder) {
        this.builder = builder;
        this.depthPolicy = () -> SpatialDepthPolicy.Mode.LAYERED;
        this.buffers = new SpatialBuffer[8];
        this.counts = new int[8];
        this.sortKeys = new long[8];
    }

    public SpatialDepthPolicy getDepthPolicy() {
        return depthPolicy;
    }

    public DepthAwareBatchConsumer setDepthPolicy(SpatialDepthPolicy depthPolicy) {
        if (depthPolicy != null) {
            this.depthPolicy = depthPolicy;
        }
        return this;
    }

    public DepthAwareBatchConsumer reset() {
        size = 0;
        return this;
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
        if (buffer == null || count <= 0) return;
        if (!depthPolicy.isSorted()) {
            builder.accept(buffer, count);
            return;
        }
        ensureCapacity(size + 1);
        buffers[size] = buffer;
        counts[size] = count;
        double[] stats = SpatialDepthUtils.getCalculatedZBounds(buffer, count, depthTmp);
        double depth = stats[2];
        sortKeys[size] = packDepthKey(depth, size, depthPolicy.getMode());
        size++;
    }

    public SpatialPathBatchBuilder flush() {
        if (!depthPolicy.isSorted() || size == 0) {
            size = 0;
            return builder;
        }
        Arrays.sort(sortKeys, 0, size);
        for (int i = 0; i < size; i++) {
            int index = (int) sortKeys[i];
            builder.accept(buffers[index], counts[index]);
        }
        size = 0;
        return builder;
    }

    private void ensureCapacity(int required) {
        if (required <= buffers.length) {
            return;
        }
        int newCap = buffers.length;
        while (newCap < required) {
            newCap = newCap + (newCap >> 1) + 1;
        }
        buffers = Arrays.copyOf(buffers, newCap);
        counts = Arrays.copyOf(counts, newCap);
        sortKeys = Arrays.copyOf(sortKeys, newCap);
    }

    private static long packDepthKey(double depth, int index, SpatialDepthPolicy.Mode mode) {
        int depthBits = Float.floatToIntBits((float) depth);
        int sortable = depthBits ^ ((depthBits >> 31) & 0x7fffffff);
        if (mode == SpatialDepthPolicy.Mode.SORTED_BACK_TO_FRONT) {
            sortable = -sortable;
        }
        return (((long) sortable) << 32) | (index & 0xffffffffL);
    }
}
