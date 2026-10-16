package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        second.getInputCoords()[0] = 9.0;
        second.getInputCoords()[3] = 9.0;

        consumer.accept(first, 2);
        consumer.accept(second, 2);
        consumer.flush();

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(4, batch.getPointCount());
        // Stability: first chunk points come before second chunk points.
        assertEquals(3.0, batch.getXData()[0], 1e-9);
        assertEquals(3.0, batch.getXData()[1], 1e-9);
        assertEquals(9.0, batch.getXData()[2], 1e-9);
        assertEquals(9.0, batch.getXData()[3], 1e-9);
    }

    @ParameterizedTest
    @EnumSource(value = SpatialDepthPolicy.Mode.class, names = {"SORTED_BACK_TO_FRONT", "SORTED_FRONT_TO_BACK"})
    void sortedModeSnapshotsReusableChunksIncludingStylesAndPathStarts(SpatialDepthPolicy.Mode mode) {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        DepthAwareBatchConsumer consumer = new DepthAwareBatchConsumer(builder).setDepthPolicy(() -> mode);
        SpatialBuffer buffer = new SpatialBuffer(2);
        long nearStyle = SpatialStyleDescriptor.pack(0xFFAA0000, 5, 0, 3);
        long farStyle = SpatialStyleDescriptor.pack(0xFF0000AA, 5, 0, 3);
        for (int chunk = 0; chunk < 2; chunk++) {
            for (int i = 0; i < 2; i++) {
                buffer.getInputCoords()[i * 3] = chunk * 20 + 10 + i;
                buffer.getInputCoords()[i * 3 + 1] = chunk * 20 + 20 + i;
                buffer.getInputCoords()[i * 3 + 2] = chunk == 0 ? 1 : 5;
                buffer.setPointMetadata(i, chunk == 0 ? nearStyle : farStyle, i == 0);
            }
            consumer.accept(buffer, 2);
        }
        buffer.getInputCoords()[0] = 999;
        buffer.clearPointMetadata();
        consumer.flush();
        boolean farFirst = mode == SpatialDepthPolicy.Mode.SORTED_BACK_TO_FRONT;
        for (int i = 0; i < 4; i++) {
            boolean far = i < 2 ? farFirst : !farFirst;
            assertEquals((far ? 30 : 10) + i % 2, builder.getBatch().getXData()[i]);
            assertEquals((far ? 40 : 20) + i % 2, builder.getBatch().getYData()[i]);
            assertEquals(far ? farStyle : nearStyle, builder.getBatch().getStyleKey(i));
            assertEquals(i % 2 == 0, builder.isMoveTo(i));
        }
        assertEquals(4, builder.getBatch().getPointCount());

        // A later frame must reuse snapshot storage without inheriting old point styles.
        builder.reset().setStyleKey(nearStyle);
        consumer.reset();
        consumer.accept(buffer, 2);
        buffer.getInputCoords()[0] = -999;
        consumer.flush();
        assertEquals(999, builder.getBatch().getXData()[0]);
        assertEquals(nearStyle, builder.getBatch().getStyleKey(0));
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
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
