package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepthAwareBatchConsumerTest {
    @Test
    void sorted_back_to_front_orders_by_depth() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        DepthAwareBatchConsumer consumer = new DepthAwareBatchConsumer(builder)
                .setDepthPolicy(() -> SpatialDepthPolicy.Mode.SORTED_BACK_TO_FRONT);

        SpatialBuffer near = bufferWithZ(3, 1.0);
        SpatialBuffer mid = bufferWithZ(3, 5.0);
        SpatialBuffer far = bufferWithZ(3, 10.0);

        consumer.accept(near, 3);
        consumer.accept(far, 3);
        consumer.accept(mid, 3);
        consumer.flush();

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(9, batch.getPointCount());
        // Expect far (x=10), mid (x=5), near (x=1)
        assertEquals(10.0, batch.getXData()[0], 1e-9);
        assertEquals(10.0, batch.getXData()[1], 1e-9);
        assertEquals(10.0, batch.getXData()[2], 1e-9);
        assertEquals(5.0, batch.getXData()[3], 1e-9);
        assertEquals(5.0, batch.getXData()[4], 1e-9);
        assertEquals(5.0, batch.getXData()[5], 1e-9);
        assertEquals(1.0, batch.getXData()[6], 1e-9);
        assertEquals(1.0, batch.getXData()[7], 1e-9);
        assertEquals(1.0, batch.getXData()[8], 1e-9);
    }

    @Test
    void sorted_back_to_front_is_stable_for_equal_depth() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        DepthAwareBatchConsumer consumer = new DepthAwareBatchConsumer(builder)
                .setDepthPolicy(() -> SpatialDepthPolicy.Mode.SORTED_BACK_TO_FRONT);

        SpatialBuffer first = bufferWithZ(2, 3.0);
        SpatialBuffer second = bufferWithZ(2, 3.0);

        consumer.accept(first, 2);
        consumer.accept(second, 2);
        consumer.flush();

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(4, batch.getPointCount());
        // Stability: first chunk points come before second chunk points.
        assertEquals(3.0, batch.getXData()[0], 1e-9);
        assertEquals(3.0, batch.getXData()[1], 1e-9);
        assertEquals(3.0, batch.getXData()[2], 1e-9);
        assertEquals(3.0, batch.getXData()[3], 1e-9);
    }

    @Test
    void layered_passthrough_forwards_immediately() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        DepthAwareBatchConsumer consumer = new DepthAwareBatchConsumer(builder)
                .setDepthPolicy(() -> SpatialDepthPolicy.Mode.LAYERED);

        SpatialBuffer a = bufferWithZ(2, 2.0);
        SpatialBuffer b = bufferWithZ(2, 4.0);

        consumer.accept(a, 2);
        assertEquals(2, builder.getBatch().getPointCount());
        consumer.accept(b, 2);
        assertEquals(4, builder.getBatch().getPointCount());
    }

    private static SpatialBuffer bufferWithZ(int count, double z) {
        SpatialBuffer buffer = new SpatialBuffer(count);
        double[] in = buffer.getInputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            in[j] = z;
            in[j + 1] = z;
            in[j + 2] = z;
        }
        return buffer;
    }
}
