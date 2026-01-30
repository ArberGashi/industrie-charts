package com.arbergashi.charts.api;

import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.render.specialized.SmithChartTransform;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CoordinateBatchIntegrityTest {
    private static final double EPS = 1e-9;

    @Test
    void batchMappingMatchesScalarForLinearTransformer() {
        PlotContext context = createContext();
        CoordinateTransformer transformer = new CoordinateTransformer() {
            @Override
            public void mapToPixel(PlotContext ctx, double x, double y, double[] out) {
                ctx.mapToPixel(x, y, out);
            }

            @Override
            public void mapToData(PlotContext ctx, double pixelX, double pixelY, double[] out) {
                ctx.mapToData(pixelX, pixelY, out);
            }
        };

        double[] input = createInputPoints(13);
        double[] outBatch = new double[input.length];
        SpatialBuffer spatial = new SpatialBuffer(13);

        transformer.mapToPixelBatch(context, input, outBatch, 13);
        transformer.mapToSpatialBuffer(context, input, spatial, 13);

        double[] tmp = new double[2];
        for (int i = 0; i < 13; i++) {
            int idx = i * 2;
            transformer.mapToPixel(context, input[idx], input[idx + 1], tmp);
            assertEquals(tmp[0], outBatch[idx], EPS);
            assertEquals(tmp[1], outBatch[idx + 1], EPS);

            int sIdx = i * 3;
            assertEquals(tmp[0], spatial.getInputCoords()[sIdx], EPS);
            assertEquals(tmp[1], spatial.getInputCoords()[sIdx + 1], EPS);
            assertEquals(0.0, spatial.getInputCoords()[sIdx + 2], EPS);
        }
    }

    @Test
    void batchMappingMatchesScalarForSmithTransform() {
        PlotContext context = createContext();
        CoordinateTransformer transformer = new SmithChartTransform();

        double[] input = createInputPoints(13);
        double[] outBatch = new double[input.length];
        SpatialBuffer spatial = new SpatialBuffer(13);

        transformer.mapToPixelBatch(context, input, outBatch, 13);
        transformer.mapToSpatialBuffer(context, input, spatial, 13);

        double[] tmp = new double[2];
        for (int i = 0; i < 13; i++) {
            int idx = i * 2;
            transformer.mapToPixel(context, input[idx], input[idx + 1], tmp);
            assertEquals(tmp[0], outBatch[idx], EPS);
            assertEquals(tmp[1], outBatch[idx + 1], EPS);

            int sIdx = i * 3;
            assertEquals(tmp[0], spatial.getInputCoords()[sIdx], EPS);
            assertEquals(tmp[1], spatial.getInputCoords()[sIdx + 1], EPS);
            assertEquals(0.0, spatial.getInputCoords()[sIdx + 2], EPS);
        }
    }

    @Test
    void batchMappingDoesNotAllocateExcessively() {
        PlotContext context = createContext();
        CoordinateTransformer transformer = new CoordinateTransformer() {
            @Override
            public void mapToPixel(PlotContext ctx, double x, double y, double[] out) {
                ctx.mapToPixel(x, y, out);
            }

            @Override
            public void mapToData(PlotContext ctx, double pixelX, double pixelY, double[] out) {
                ctx.mapToData(pixelX, pixelY, out);
            }
        };

        Object bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(mx.isThreadAllocatedMemorySupported());
        mx.setThreadAllocatedMemoryEnabled(true);

        double[] input = createInputPoints(13);
        SpatialBuffer spatial = new SpatialBuffer(13);

        // warm-up
        transformer.mapToSpatialBuffer(context, input, spatial, 13);

        long before = mx.getThreadAllocatedBytes(Thread.currentThread().threadId());
        for (int i = 0; i < 100; i++) {
            transformer.mapToSpatialBuffer(context, input, spatial, 13);
        }
        long after = mx.getThreadAllocatedBytes(Thread.currentThread().threadId());
        long delta = after - before;

        assertTrue(delta < 16_384L, "batch mapping should not allocate in hot path");
    }

    @Test
    void customZProviderMapsIntoSpatialBuffer() {
        com.arbergashi.charts.engine.spatial.SpatialDataMapper mapper = new com.arbergashi.charts.engine.spatial.SpatialDataMapper()
                .setZMode(com.arbergashi.charts.engine.spatial.SpatialDataMapper.ZMode.CUSTOM)
                .setZComponentProvider((index, x, y, weight) -> x * y);

        com.arbergashi.charts.engine.spatial.SpatialBuffer buffer =
                new com.arbergashi.charts.engine.spatial.SpatialBuffer(5);

        double[] input = createInputPoints(5);
        mapper.mapToSpatialBuffer(createModel(input), 0, 5, buffer);

        double[] out = buffer.getInputCoords();
        for (int i = 0; i < 5; i++) {
            int idx = i * 2;
            int sIdx = i * 3;
            double x = input[idx];
            double y = input[idx + 1];
            assertEquals(x, out[sIdx], EPS);
            assertEquals(y, out[sIdx + 1], EPS);
            assertEquals(x * y, out[sIdx + 2], EPS);
        }
    }

    @Test
    void customZProviderFallsBackWhenMissing() {
        com.arbergashi.charts.engine.spatial.SpatialDataMapper mapper = new com.arbergashi.charts.engine.spatial.SpatialDataMapper()
                .setZMode(com.arbergashi.charts.engine.spatial.SpatialDataMapper.ZMode.CUSTOM)
                .setZValue(7.5);

        com.arbergashi.charts.engine.spatial.SpatialBuffer buffer =
                new com.arbergashi.charts.engine.spatial.SpatialBuffer(3);

        double[] input = createInputPoints(3);
        mapper.mapToSpatialBuffer(createModel(input), 0, 3, buffer);

        double[] out = buffer.getInputCoords();
        for (int i = 0; i < 3; i++) {
            int sIdx = i * 3;
            assertEquals(7.5, out[sIdx + 2], EPS);
        }
    }

    @Test
    void customZProviderHandlesTailChunk() {
        com.arbergashi.charts.engine.spatial.SpatialDataMapper mapper = new com.arbergashi.charts.engine.spatial.SpatialDataMapper()
                .setZMode(com.arbergashi.charts.engine.spatial.SpatialDataMapper.ZMode.CUSTOM)
                .setZComponentProvider((index, x, y, weight) -> index + y);

        com.arbergashi.charts.engine.spatial.SpatialBuffer buffer =
                new com.arbergashi.charts.engine.spatial.SpatialBuffer(5);

        double[] input = createInputPoints(13);
        com.arbergashi.charts.model.ChartModel model = createModel(input);
        int written = mapper.mapToSpatialBuffer(model, 10, 5, buffer);
        assertEquals(3, written);

        double[] out = buffer.getInputCoords();
        for (int i = 0; i < written; i++) {
            int modelIdx = 10 + i;
            double y = input[modelIdx * 2 + 1];
            assertEquals(modelIdx + y, out[i * 3 + 2], EPS);
        }
    }

    private static PlotContext createContext() {
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 500, 250);
        return new DefaultPlotContext(
                bounds,
                0.0,
                100.0,
                0.0,
                50.0,
                false,
                false,
                false,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                ChartThemes.getDarkTheme(),
                null,
                null,
                null
        );
    }

    private static double[] createInputPoints(int count) {
        double[] data = new double[count * 2];
        for (int i = 0; i < count; i++) {
            data[i * 2] = i * 1.75 + 0.1;
            data[i * 2 + 1] = (i % 5) * 2.25 + 0.2;
        }
        return data;
    }

    private static com.arbergashi.charts.model.ChartModel createModel(double[] input) {
        int count = input.length / 2;
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = input[i * 2];
            ys[i] = input[i * 2 + 1];
        }
        return new com.arbergashi.charts.model.ChartModel() {
            @Override
            public String getName() {
                return "batch_integrity_model";
            }

            @Override
            public void setChangeListener(com.arbergashi.charts.model.ChartModel.ChartModelListener listener) {
                // no-op
            }

            @Override
            public void removeChangeListener(com.arbergashi.charts.model.ChartModel.ChartModelListener listener) {
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
