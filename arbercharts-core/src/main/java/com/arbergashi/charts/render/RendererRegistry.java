package com.arbergashi.charts.render;

import com.arbergashi.charts.internal.RendererDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Public facade for the renderer registry.
 *
 * <p><b>Framework policy:</b> There is exactly one registry of renderer factories.
 * The single source of truth is {@link com.arbergashi.charts.internal.RendererRegistry}.
 * This class exists as the stable public entry point for framework consumers.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-17
 */
public final class RendererRegistry {

    private RendererRegistry() {
    }

    /**
     * Registers a renderer factory.
     */
    public static void register(String id, RendererDescriptor descriptor, Supplier<? extends ChartRenderer> factory) {
        com.arbergashi.charts.internal.RendererRegistry.register(id, descriptor, factory);
    }

    /**
     * Backwards-compatible creation method.
     * May return {@code null} if the renderer id is not registered.
     */
    public static ChartRenderer create(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.create(id);
    }

    /**
     * Preferred creation method.
     */
    public static Optional<ChartRenderer> createOptional(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.createOptional(id);
    }

    /**
     * Strict creation method.
     */
    public static ChartRenderer require(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.require(id);
    }

    /**
     * Returns immutable renderer descriptors.
     */
    public static Map<String, ? extends RendererDescriptor> descriptors() {
        return com.arbergashi.charts.internal.RendererRegistry.descriptors();
    }

    /**
     * Returns renderer metadata as stable public API objects.
     *
     * <p>This avoids leaking {@code com.arbergashi.charts.internal.*} types into consuming
     * applications while keeping the internal registry as the single source of truth.</p>
     */
    public static Map<String, RendererMetadata> metadata() {
        Map<String, com.arbergashi.charts.internal.RendererDescriptor> internal = com.arbergashi.charts.internal.RendererRegistry.descriptors();
        Map<String, RendererMetadata> out = new LinkedHashMap<>();
        for (var e : internal.entrySet()) {
            var d = e.getValue();
            out.put(e.getKey(), new RendererMetadata(d.id(), d.nameKey(), d.iconPath()));
        }
        return java.util.Collections.unmodifiableMap(out);
    }
}