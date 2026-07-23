package com.arbergashi.charts.render;

import com.arbergashi.charts.platform.render.RendererRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies deterministic registry population: after bootstrap, every
 * self-registered id resolves regardless of prior class-loading order.
 */
class RendererBootstrapTest {

    @Test
    void bootstrapPopulatesRegistryDeterministically() {
        RendererBootstrap.ensureRegistered();

        for (String id : new String[]{
                "candlestick_hollow", "point_and_figure_advanced", "renko",
                "sunburst", "voronoi", "heatmap", "wind_rose"}) {
            assertNotNull(RendererRegistry.getRenderer(id), "registry missing id: " + id);
        }
    }

    @Test
    void bootstrapIsIdempotent() {
        RendererBootstrap.ensureRegistered();
        RendererBootstrap.ensureRegistered();
        assertNotNull(RendererRegistry.getRenderer("sunburst"));
    }
}
