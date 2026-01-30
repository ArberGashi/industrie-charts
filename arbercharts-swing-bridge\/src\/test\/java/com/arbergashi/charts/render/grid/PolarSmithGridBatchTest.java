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

public class PolarSmithGridBatchTest {

    @Test
    void smithGrid_clampKeepsSegments_atHighZMin() {
        SmithChartGridLayer layer = new SmithChartGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.CLAMP);

        PlotContext ctx = context(new Rectangle2D.Double(10, 10, 520, 380),
                -1, 1, -1, 1, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() > 20, "Expected Smith grid batch to emit points in CLAMP");
        assertFinite(builder.getBatch());
    }

    @Test
    void smithGrid_discardDropsAll_atHighZMin() {
        SmithChartGridLayer layer = new SmithChartGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.DISCARD);

        PlotContext ctx = context(new Rectangle2D.Double(10, 10, 520, 380),
                -1, 1, -1, 1, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() == 0, "Expected Smith grid batch to be discarded at high zMin");
    }

    @Test
    void polarGrid_clampKeepsSegments_atHighZMin() {
        PolarGridLayer layer = new PolarGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.CLAMP);

        PlotContext ctx = context(new Rectangle2D.Double(10, 10, 480, 360),
                0, 1, 0, 1, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() > 20, "Expected Polar grid batch to emit points in CLAMP");
        assertFinite(builder.getBatch());
    }

    @Test
    void polarGrid_discardDropsAll_atHighZMin() {
        PolarGridLayer layer = new PolarGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        GridBatchConfig config = new GridBatchConfig()
                .setZMin(2.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.DISCARD);

        PlotContext ctx = context(new Rectangle2D.Double(10, 10, 480, 360),
                0, 1, 0, 1, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, config));
        assertTrue(builder.getBatch().getPointCount() == 0, "Expected Polar grid batch to be discarded at high zMin");
    }

    private static PlotContext context(Rectangle2D bounds, double minX, double maxX,
                                       double minY, double maxY, ChartTheme theme) {
        return new DefaultPlotContext(
                bounds,
                minX,
                maxX,
                minY,
                maxY,
                false,
                false,
                false,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
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
