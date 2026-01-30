package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batch rendering smoke tests for specialized grids.
 */
public class SpecializedGridBatchTest {

    @Test
    void ternaryGrid_emitsBatchPoints() {
        TernaryGridLayer layer = new TernaryGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = context(new Rectangle2D.Double(10, 10, 460, 320),
                0, 1, 0, 1, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertTrue(builder.getBatch().getPointCount() > 10, "Expected ternary batch to emit points");
    }

    @Test
    void isometricGrid_emitsBatchPoints() {
        IsometricGridLayer layer = new IsometricGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = context(new Rectangle2D.Double(0, 0, 420, 260),
                0, 100, 0, 100, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertTrue(builder.getBatch().getPointCount() > 10, "Expected isometric batch to emit points");
    }

    @Test
    void geoGrid_emitsBatchPoints() {
        GeoGridLayer layer = new GeoGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = context(new Rectangle2D.Double(5, 5, 480, 300),
                -180, 180, -90, 90, ChartThemes.getDarkTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertTrue(builder.getBatch().getPointCount() > 10, "Expected geo batch to emit points");
    }

    @Test
    void isometricGrid_handlesTightBounds() {
        IsometricGridLayer layer = new IsometricGridLayer();
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        PlotContext ctx = context(new Rectangle2D.Double(0, 0, 120, 90),
                0, 10, 0, 10, ChartThemes.getLightTheme());

        assertDoesNotThrow(() -> layer.renderGridBatch(builder, ctx, new GridBatchConfig()));
        assertTrue(builder.getBatch().getPointCount() > 0, "Expected isometric batch to emit points");
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
}
