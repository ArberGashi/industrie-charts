package com.arbergashi.charts.internal;

import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Market-readiness gate: registering the same renderer id twice must fail fast.
 *
 * <p>Duplicate ids cause non-deterministic UI/registry behavior and are not acceptable in a framework.</p>
 */
public class RendererRegistryDuplicateIdContractTest {

    @Test
    void register_duplicateId_mustThrow() {
        RendererRegistry.clearForTests();

        RendererRegistry.register(
                "dup",
                new RendererDescriptor("dup", "DUP", "/icons/dup.svg"),
                () -> new NoopRenderer("dup")
        );

        assertThrows(IllegalStateException.class, () -> RendererRegistry.register(
                "dup",
                new RendererDescriptor("dup", "DUP2", "/icons/dup2.svg"),
                () -> new NoopRenderer("dup2")
        ));
    }

    private static final class NoopRenderer implements ChartRenderer {
        private final String name;

        private NoopRenderer(String name) {
            this.name = name;
        }

        @Override
        public void render(java.awt.Graphics2D g2, com.arbergashi.charts.model.ChartModel model, com.arbergashi.charts.api.PlotContext context) {
            // no-op
        }

        @Override
        public java.util.Optional<Integer> getPointAt(java.awt.geom.Point2D pixel, com.arbergashi.charts.model.ChartModel model, com.arbergashi.charts.api.PlotContext context) {
            return java.util.Optional.empty();
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
