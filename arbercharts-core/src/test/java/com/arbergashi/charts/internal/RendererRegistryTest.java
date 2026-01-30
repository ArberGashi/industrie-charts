package com.arbergashi.charts.internal;

import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RendererRegistryTest {

    @BeforeEach
    void setUp() {
        RendererRegistry.clearForTests();
    }

    @AfterEach
    void tearDown() {
        RendererRegistry.clearForTests();
    }

    @Test
    void registerAndLookupRenderer() {
        RendererDescriptor descriptor = new RendererDescriptor("test", "renderer.test", "/icons/test.svg");
        RendererRegistry.register("test", descriptor, DummyRenderer::new);

        ChartRenderer renderer = RendererRegistry.getRenderer("test");
        assertNotNull(renderer);
        assertEquals("dummy", renderer.getName());

        Optional<ChartRenderer> opt = RendererRegistry.getOptionalRenderer("test");
        assertTrue(opt.isPresent());

        Map<String, RendererDescriptor> descriptors = RendererRegistry.descriptors();
        assertTrue(descriptors.containsKey("test"));
    }

    @Test
    void duplicateRegistrationThrows() {
        RendererDescriptor descriptor = new RendererDescriptor("dup", "renderer.dup", "/icons/dup.svg");
        RendererRegistry.register("dup", descriptor, DummyRenderer::new);
        assertThrows(IllegalStateException.class, () ->
                RendererRegistry.register("dup", descriptor, DummyRenderer::new));
    }

    @Test
    void requiredRendererThrowsWhenMissing() {
        assertThrows(IllegalArgumentException.class, () -> RendererRegistry.getRequiredRenderer("missing"));
    }

    @Test
    void optionalRendererEmptyWhenMissing() {
        assertTrue(RendererRegistry.getOptionalRenderer(null).isEmpty());
        assertTrue(RendererRegistry.getOptionalRenderer("missing").isEmpty());
    }

    @Test
    void descriptorsAreUnmodifiable() {
        RendererDescriptor descriptor = new RendererDescriptor("test", "renderer.test", "/icons/test.svg");
        RendererRegistry.register("test", descriptor, DummyRenderer::new);
        Map<String, RendererDescriptor> descriptors = RendererRegistry.descriptors();
        assertThrows(UnsupportedOperationException.class, () -> descriptors.put("x", descriptor));
    }

    private static final class DummyRenderer implements ChartRenderer {
        @Override
        public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return "dummy";
        }
    }
}
