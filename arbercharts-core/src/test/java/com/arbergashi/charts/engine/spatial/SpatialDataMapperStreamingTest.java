package com.arbergashi.charts.engine.spatial;

import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialDataMapperStreamingTest {
    private static final double EPS = 1e-9;

    @Test
    void mapAll_streams_in_chunks_with_correct_state() {
        int total = 13;
        SpatialDataMapper mapper = new SpatialDataMapper()
                .setZMode(SpatialDataMapper.ZMode.FROM_INDEX);
        SpatialBuffer buffer = new SpatialBuffer(5);
        ChartModel model = createModel(total);

        List<Integer> chunkSizes = new ArrayList<>();
        int[] offsetHolder = new int[]{0};
        mapper.mapAll(model, buffer, (buf, written) -> {
            chunkSizes.add(written);
            double[] coords = buf.getInputCoords();
            int offset = offsetHolder[0];
            for (int i = 0; i < written; i++) {
                int modelIndex = offset + i;
                int idx = i * 3;
                double expectedX = model.getXData()[modelIndex];
                double expectedY = model.getYData()[modelIndex];
                assertEquals(expectedX, coords[idx], EPS);
                assertEquals(expectedY, coords[idx + 1], EPS);
                assertEquals(modelIndex, coords[idx + 2], EPS);
            }
            offsetHolder[0] += written;
        });

        assertEquals(List.of(5, 5, 3), chunkSizes);
        assertEquals(total, offsetHolder[0]);
    }

    @Test
    void mapAllProjected_applies_transform_and_projector_per_chunk() {
        int total = 12;
        SpatialDataMapper mapper = new SpatialDataMapper()
                .setZMode(SpatialDataMapper.ZMode.CONSTANT)
                .setZValue(1.5);
        SpatialBuffer buffer = new SpatialBuffer(5);
        ChartModel model = createModel(total);

        Matrix4x4 transform = new Matrix4x4()
                .setIdentity()
                .setTranslation(1.0, 2.0, 3.0);
        OrthographicProjector projector = new OrthographicProjector()
                .setScale(2.0)
                .setCenterX(10.0)
                .setCenterY(20.0);

        int[] offsetHolder = new int[]{0};
        mapper.mapAllProjected(model, buffer, transform, projector, (buf, written) -> {
            double[] coords = buf.getInputCoords();
            int offset = offsetHolder[0];
            for (int i = 0; i < written; i++) {
                int modelIndex = offset + i;
                double x = model.getXData()[modelIndex] + 1.0;
                double y = model.getYData()[modelIndex] + 2.0;
                double z = 1.5 + 3.0;

                double expectedX = 10.0 + x * 2.0;
                double expectedY = 20.0 - y * 2.0;
                int idx = i * 3;
                assertEquals(expectedX, coords[idx], EPS);
                assertEquals(expectedY, coords[idx + 1], EPS);
                assertEquals(z, coords[idx + 2], EPS);
            }
            offsetHolder[0] += written;
        });

        assertEquals(total, offsetHolder[0]);
        assertTrue(offsetHolder[0] > 0);
    }

    @Test
    void mapAllProjected_matches_scalar_path_within_tolerance() {
        int total = 101;
        SpatialDataMapper mapper = new SpatialDataMapper()
                .setZMode(SpatialDataMapper.ZMode.FROM_Y);
        SpatialBuffer buffer = new SpatialBuffer(16);
        ChartModel model = createModel(total);

        Matrix4x4 transform = new Matrix4x4()
                .setIdentity()
                .setTranslation(0.75, -1.25, 2.5);
        PerspectiveProjector projector = new PerspectiveProjector()
                .setScale(1.1)
                .setCenterX(100.0)
                .setCenterY(75.0)
                .setZBias(0.8);

        List<double[]> scalarResults = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            double x = model.getXData()[i];
            double y = model.getYData()[i];
            double z = y;
            Vector3D transformed = transform.getCalculatedTransform(new Vector3D(x, y, z));
            Vector3D projected = projector.getCalculatedProjection(transformed);
            scalarResults.add(new double[]{projected.getX(), projected.getY(), projected.getZ()});
        }

        int[] offsetHolder = new int[]{0};
        mapper.mapAllProjected(model, buffer, transform, projector, (buf, written) -> {
            double[] coords = buf.getInputCoords();
            int offset = offsetHolder[0];
            for (int i = 0; i < written; i++) {
                double[] expected = scalarResults.get(offset + i);
                int idx = i * 3;
                assertEquals(expected[0], coords[idx], EPS);
                assertEquals(expected[1], coords[idx + 1], EPS);
                assertEquals(expected[2], coords[idx + 2], EPS);
            }
            offsetHolder[0] += written;
        });

        assertEquals(total, offsetHolder[0]);
    }

    private static ChartModel createModel(int count) {
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = i * 1.25 + 0.1;
            ys[i] = (i % 4) * 2.5 + 0.2;
        }
        return new ChartModel() {
            @Override
            public String getName() {
                return "streaming_test_model";
            }

            @Override
            public void setChangeListener(ChartModelListener listener) {
                // no-op
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
                // no-op
            }

            @Override
            public int getPointCount() {
                return count;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }
        };
    }
}
