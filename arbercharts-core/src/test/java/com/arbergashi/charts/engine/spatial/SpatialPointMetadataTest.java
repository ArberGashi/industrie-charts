package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialPointMetadataTest {
    @Test
    void wrapperPreservesPointStylesAndPathStartsAcrossGrowthAndReuse() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        SpatialChunkConsumer wrapper = builder::accept;
        SpatialBuffer buffer = new SpatialBuffer(3);
        for (int i = 0; i < 3; i++) {
            buffer.getInputCoords()[i * 3] = i;
            buffer.getInputCoords()[i * 3 + 2] = 1;
            buffer.setPointMetadata(i, SpatialStyleDescriptor.pack(0xFF001100 + i, 5, 0, 3), true);
        }
        for (int chunk = 0; chunk < 100; chunk++) wrapper.accept(buffer, 3);
        assertEquals(300, builder.getBatch().getPointCount());
        for (int i = 0; i < 300; i++) {
            assertTrue(builder.isMoveTo(i));
            assertEquals(0xFF001100 + i % 3, SpatialStyleDescriptor.unpackArgb(builder.getBatch().getStyleKey(i)));
        }
        builder.reset();
        buffer.clearPointMetadata();
        wrapper.accept(buffer, 3);
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
        assertFalse(builder.isMoveTo(2));
        for (int i = 0; i < 3; i++) assertEquals(builder.getStyleKey(), builder.getBatch().getStyleKey(i));
    }

    @Test
    void clippingHasRoomForBothBoundaryIntersectionsAndHandlesChunkExit() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        SpatialBuffer buffer = new SpatialBuffer(400);
        for (int i = 0; i < 400; i++) {
            buffer.getInputCoords()[i * 3] = i;
            buffer.getInputCoords()[i * 3 + 2] = i % 2 == 0 ? 1 : -1;
        }
        builder.accept(buffer, 400);
        assertEquals(599, builder.getBatch().getPointCount());
        builder.reset();
        builder.accept(buffer, 1);
        buffer.getInputCoords()[0] = 2;
        buffer.getInputCoords()[2] = -1;
        builder.accept(buffer, 1);
        assertEquals(2, builder.getBatch().getPointCount());
        assertEquals(1, builder.getBatch().getXData()[1], 1e-9);

        builder.reset();
        buffer.getInputCoords()[0] = 0;
        buffer.getInputCoords()[2] = -2;
        builder.accept(buffer, 1);
        buffer.getInputCoords()[0] = 10;
        buffer.getInputCoords()[2] = 2;
        builder.accept(buffer, 1);
        assertEquals(2, builder.getBatch().getPointCount());
        assertEquals(5, builder.getBatch().getXData()[0], 1e-9);
        assertEquals(10, builder.getBatch().getXData()[1], 1e-9);
    }
}
