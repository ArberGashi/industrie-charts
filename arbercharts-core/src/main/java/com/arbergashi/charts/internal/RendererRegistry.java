package com.arbergashi.charts.internal;

import com.arbergashi.charts.render.ChartRenderer;

import java.util.Collections;
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

    /**
     * Creates a renderer instance by id.
     *
     * <p><b>Compatibility:</b> This method may return {@code null} if the id is not registered.
     * Prefer {@link #createOptional(String)} or {@link #require(String)} in new code.</p>
     */
    public static ChartRenderer create(String id) {
        return createOptional(id).orElse(null);
    }

    /**
     * Creates a renderer instance by id.
     *
     * @param id registry id
     * @return an Optional containing a new renderer instance if registered
     */
    public static Optional<ChartRenderer> createOptional(String id) {
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
    public static ChartRenderer require(String id) {
        return createOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Renderer not registered: " + id));
    }

    public static synchronized Map<String, RendererDescriptor> descriptors() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(METADATA));
    }

    /**
     * Test/support hook: clears all registrations.
     *
     * <p>Not intended for normal application code.</p>
     */
    static synchronized void clearForTests() {
        FACTORIES.clear();
        METADATA.clear();
    }
}
