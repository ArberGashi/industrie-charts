package com.arbergashi.charts.platform.render;

import com.arbergashi.charts.api.RendererAffinity;
import com.arbergashi.charts.api.RendererCapability;
import com.arbergashi.charts.domain.render.RendererMetadata;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.ChartRenderer;

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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
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
     * Returns a renderer instance by id.
     * May return {@code null} if the renderer id is not registered.
     */
    public static ChartRenderer getRenderer(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.getRenderer(id);
    }

    /**
     * Preferred lookup method.
     */
    public static Optional<ChartRenderer> getOptionalRenderer(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.getOptionalRenderer(id);
    }

    /**
     * Strict lookup method.
     */
    public static ChartRenderer getRequiredRenderer(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.getRequiredRenderer(id);
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
            out.put(e.getKey(), new RendererMetadata(d.getId(), d.getNameKey(), d.getIconPath()));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    /**
     * Returns affinity information for a renderer id.
     */
    public static RendererAffinity getRendererAffinity(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.getRendererAffinity(id);
    }

    /**
     * Returns capability flags for a renderer id.
     */
    public static java.util.EnumSet<RendererCapability> getRendererCapabilities(String id) {
        return com.arbergashi.charts.internal.RendererRegistry.getRendererCapabilities(id);
    }

    /**
     * Sets affinity information for a renderer id.
     */
    public static void setRendererAffinity(String id, RendererAffinity affinity) {
        com.arbergashi.charts.internal.RendererRegistry.setRendererAffinity(id, affinity);
    }
}
