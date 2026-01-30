package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.SpatialPathBatch;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogarithmicGridBatchTest {

    @Test
    void logGrid_handlesInvalidY_inBatch() {
        LogarithmicGridLayer layer = new LogarithmicGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = logContext(new Rectangle2D.Double(10, 10, 460, 320),
                1, 1000, 0, 100, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertFinite(builder.getBatch());
        assertTrue(builder.getBatch().getPointCount() > 10, "Expected log grid batch to emit points");
    }

    @Test
    void logGrid_crossClipping_discard() {
        LogarithmicGridLayer layer = new LogarithmicGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.DISCARD);

        PlotContext ctx = logContext(new Rectangle2D.Double(0, 0, 400, 280),
                1, 100, 0, 50, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() == 0, "Expected discard at high zMin");
    }

    @Test
    void logGrid_crossClipping_clamp() {
        LogarithmicGridLayer layer = new LogarithmicGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.CLAMP);

        PlotContext ctx = logContext(new Rectangle2D.Double(0, 0, 400, 280),
                1, 100, 0, 50, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() > 0, "Expected clamp to keep segments");
        assertFinite(builder.getBatch());
    }

    @Test
    void logGrid_compactRange_keepsSegments() {
        LogarithmicGridLayer layer = new LogarithmicGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = logContext(new Rectangle2D.Double(0, 0, 320, 180),
                1, 1000000, 0.0001, 1.0, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertTrue(builder.getBatch().getPointCount() > 10, "Expected batch segments on compressed log range");
    }

    private static PlotContext logContext(Rectangle2D bounds, double minX, double maxX,
                                          double minY, double maxY, ChartTheme theme) {
        return new DefaultPlotContext(
                bounds,
                minX,
                maxX,
                minY,
                maxY,
                false,
                true,
                false,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LOGARITHMIC,
                theme,
                null,
                null,
                null
        );
    }

    private static void assertFinite(SpatialPathBatch batch) {
        double[] xs = batch.getXData();
        double[] ys = batch.getYData();
        int count = batch.getPointCount();
        for (int i = 0; i < count; i++) {
            assertTrue(Double.isFinite(xs[i]), "Non-finite X at " + i);
            assertTrue(Double.isFinite(ys[i]), "Non-finite Y at " + i);
        }
    }
}
