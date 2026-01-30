package com.arbergashi.charts.core.rendering;

/**
 * Service provider for ArberCanvas backends.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public interface ArberCanvasProvider {
    /**
     * Unique id (e.g. "swing", "compose", "server").
     */
    String getId();

    /**
     * Priority (higher wins). Used when multiple providers are present.
     */
    int getPriority();

    /**
     * True if provider is available in current environment.
     */
    boolean isSupported();
}
