package com.arbergashi.charts.internal;

import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link RendererRegistry}.
 */
public class RendererRegistryContractTest {

    @Test
    void createOptional_and_require_behaveAsSpecified() {
        // Ensure clean state.
        RendererRegistry.clearForTests();

        assertTrue(RendererRegistry.getOptionalRenderer("missing").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> RendererRegistry.getRequiredRenderer("missing"));
        assertTrue(RendererRegistry.getOptionalRenderer("missing").isEmpty(), "createOptional must remain empty for unknown ids");

        RendererRegistry.register(
                "ok",
                new RendererDescriptor("ok", "OK", "icons/ok.svg"),
                () -> new ChartRenderer() {
                    @Override
                    public void render(java.awt.Graphics2D g2, com.arbergashi.charts.model.ChartModel model, com.arbergashi.charts.api.PlotContext context) {
                    }

                    @Override
                    public java.util.Optional<Integer> getPointAt(java.awt.geom.Point2D pixel, com.arbergashi.charts.model.ChartModel model, com.arbergashi.charts.api.PlotContext context) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public String getName() {
                        return "ok";
                    }
                }
        );

        assertNotNull(RendererRegistry.getRenderer("ok"));
        assertTrue(RendererRegistry.getOptionalRenderer("ok").isPresent());
        assertNotNull(RendererRegistry.getRequiredRenderer("ok"));

        // Legacy compatibility: create() may return null for unknown ids.
        // We validate unknown behavior via createOptional()/require() contracts above.
    }
}
