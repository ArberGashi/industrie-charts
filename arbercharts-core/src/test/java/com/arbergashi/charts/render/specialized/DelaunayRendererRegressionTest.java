package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;

import static com.arbergashi.charts.render.specialized.SpecializedRendererRegressionTest.context;
import static com.arbergashi.charts.render.specialized.SpecializedRendererRegressionTest.render;
import static com.arbergashi.charts.render.specialized.SpecializedRendererRegressionTest.sequence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelaunayRendererRegressionTest {
    private static final ArberRect BOUNDS = new ArberRect(0, 0, 640, 400);

    @Test
    void cachedTopologyRefreshesAfterSameSizePointReplacement() throws Exception {
        var renderer = new DelaunayRenderer();
        var model = grid();
        PlotContext context = context(BOUNDS, 0, 30, 0, 26);
        render(renderer, model, context);
        awaitBackgroundCache(renderer);
        for (int i = 0; i < model.count; i++) {
            int position = (i * 97) % model.count;
            model.xs[i] = position % 31;
            model.ys[i] = position / 31;
        }
        model.stamp++;
        var updated = render(renderer, model, context);
        assertLocalEdges(updated, 70);
    }

    @Test
    void cachedTopologyRefreshesWhenOnlyTheCoordinateTransformChanges() throws Exception {
        var renderer = new DelaunayRenderer();
        var model = grid();
        render(renderer, model, context(BOUNDS, 0, 30, 0, 26));
        awaitBackgroundCache(renderer);
        var transformed = render(renderer, model, context(BOUNDS, 0, 3, 0, 260));
        assertLocalEdges(transformed, 30);
    }

    @Test
    void sampledTreeIgnoresRetainedBufferCapacityAfterThePointCountShrinks() {
        var renderer = new DelaunayRenderer();
        var model = sequence(9001);
        for (int i = 0; i < model.count; i++) {
            model.xs[i] = i * 2;
            model.ys[i] = i % 2;
        }
        PlotContext context = context(new ArberRect(0, 0, 9000, 100), 0, 18000, 0, 2);
        render(renderer, model, context);
        model.count = 6001;
        model.stamp++;
        for (int i = 0; i < model.count; i++) model.xs[i] += 0.5;
        var updated = render(renderer, model, context);
        assertFalse(updated.lines.isEmpty());
        for (var line : updated.lines) {
            for (float x : line.xs()) {
                assertEquals(0.25, x - Math.floor(x), 0.0001,
                        "Every endpoint must belong to the current logical point set");
            }
        }
    }

    private static SpecializedRendererRegressionTest.ArrayModel grid() {
        var model = sequence(801);
        for (int i = 0; i < model.count; i++) {
            model.xs[i] = i % 31;
            model.ys[i] = i / 31;
        }
        return model;
    }

    private static void assertLocalEdges(SpecializedRendererRegressionTest.RecordingCanvas canvas, double maximumLength) {
        assertFalse(canvas.lines.isEmpty());
        for (var line : canvas.lines) {
            double length = Math.hypot(line.xs()[1] - line.xs()[0], line.ys()[1] - line.ys()[0]);
            assertTrue(length < maximumLength, "A stale edge connected distant points: " + length);
        }
    }

    private static void awaitBackgroundCache(DelaunayRenderer renderer) throws Exception {
        // Synchronization only: correctness is asserted through public canvas output above.
        Field cache = DelaunayRenderer.class.getDeclaredField("cachedEdges");
        cache.setAccessible(true);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (cache.get(renderer) == null) Thread.sleep(5);
        });
    }
}
