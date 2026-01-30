package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.CoordinateTransformProvider;
import com.arbergashi.charts.api.RendererAffinity;
import com.arbergashi.charts.api.RendererCapability;
import com.arbergashi.charts.api.SpatialTransformRegistry;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
/**
 * Simple registry for renderer factories.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>This registry is intentionally decoupled from concrete renderer packages.</li>
 *   <li>Renderers (or a module/bootstrapper) should register themselves explicitly.</li>
 *   <li>The registry does not attempt to {@code Class.forName(...)} arbitrary renderers.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class RendererRegistry {
    private static final Map<String, Supplier<? extends ChartRenderer>> FACTORIES = new LinkedHashMap<>();
    private static final Map<String, RendererDescriptor> METADATA = new LinkedHashMap<>();
    private static final Map<String, RendererAffinity> AFFINITIES = new LinkedHashMap<>();

    private RendererRegistry() {
    }

    public static synchronized void register(String id, RendererDescriptor descriptor, Supplier<? extends ChartRenderer> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(factory, "factory");

        if (FACTORIES.containsKey(id) || METADATA.containsKey(id)) {
            throw new IllegalStateException("Renderer id already registered: " + id);
        }

        FACTORIES.put(id, factory);
        METADATA.put(id, descriptor);
    }

    public static synchronized void register(String id, RendererDescriptor descriptor, Supplier<? extends ChartRenderer> factory, RendererAffinity affinity) {
        register(id, descriptor, factory);
        if (affinity != null) {
            AFFINITIES.put(id, affinity);
        }
    }

    /**
     * Creates a renderer instance by id.
     *
     * <p><b>Compatibility:</b> This method may return {@code null} if the id is not registered.
     * Prefer {@link #getOptionalRenderer(String)} or {@link #getRequiredRenderer(String)} in new code.</p>
     */
    public static ChartRenderer getRenderer(String id) {
        return getOptionalRenderer(id).orElse(null);
    }

    /**
     * Creates a renderer instance by id.
     *
     * @param id registry id
     * @return an Optional containing a new renderer instance if registered
     */
    public static Optional<ChartRenderer> getOptionalRenderer(String id) {
        if (id == null) return Optional.empty();
        Supplier<? extends ChartRenderer> s;
        synchronized (RendererRegistry.class) {
            s = FACTORIES.get(id);
        }
        return (s == null) ? Optional.empty() : Optional.ofNullable(s.get());
    }

    /**
     * Strict variant: returns a renderer or throws if the id is not registered.
     * This is preferred for framework integrations where missing renderers represent configuration errors.
     */
    public static ChartRenderer getRequiredRenderer(String id) {
        return getOptionalRenderer(id)
                .orElseThrow(() -> new IllegalArgumentException("Renderer not registered: " + id));
    }

    public static synchronized Map<String, RendererDescriptor> descriptors() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(METADATA));
    }

    public static RendererAffinity getRendererAffinity(String id) {
        if (id == null) {
            return null;
        }
        RendererAffinity affinity;
        synchronized (RendererRegistry.class) {
            affinity = AFFINITIES.get(id);
        }
        if (affinity != null) {
            return affinity;
        }
        RendererAffinity inferred = inferAffinity(id);
        synchronized (RendererRegistry.class) {
            if (inferred != null) {
                AFFINITIES.put(id, inferred);
            }
        }
        return inferred;
    }

    public static EnumSet<RendererCapability> getRendererCapabilities(String id) {
        RendererAffinity affinity = getRendererAffinity(id);
        return affinity == null ? EnumSet.noneOf(RendererCapability.class) : affinity.getCapabilities();
    }

    public static synchronized void setRendererAffinity(String id, RendererAffinity affinity) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(affinity, "affinity");
        AFFINITIES.put(id, affinity);
    }

    /**
     * Test/support hook: clears all registrations.
     *
     * <p>Not intended for normal application code.</p>
     */
    static synchronized void clearForTests() {
        FACTORIES.clear();
        METADATA.clear();
        AFFINITIES.clear();
    }

    private static RendererAffinity inferAffinity(String id) {
        Supplier<? extends ChartRenderer> supplier;
        synchronized (RendererRegistry.class) {
            supplier = FACTORIES.get(id);
        }
        if (supplier == null) {
            return null;
        }
        ChartRenderer renderer = supplier.get();
        if (renderer == null) {
            return null;
        }
        EnumSet<RendererCapability> capabilities = EnumSet.noneOf(RendererCapability.class);
        if (renderer instanceof SpatialChunkRenderer) {
            capabilities.add(RendererCapability.SPATIAL_BATCH);
        }
        if (renderer instanceof CoordinateTransformProvider) {
            capabilities.add(RendererCapability.COORDINATE_TRANSFORM_PROVIDER);
        }
        RendererAffinity affinity = new RendererAffinity(capabilities, SpatialTransformRegistry.getResolvedTransform(renderer, null),
                SpatialDepthPolicies.getLayered());
        return affinity;
    }
}
