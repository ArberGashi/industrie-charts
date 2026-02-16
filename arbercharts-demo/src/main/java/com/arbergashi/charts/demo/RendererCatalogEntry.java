package com.arbergashi.charts.demo;

/**
 * Represents a single renderer entry in the demo catalog.
 *
 * <p>Contains metadata about a renderer including its category,
 * fully-qualified class name, and display name.
 *
 * @param category   the renderer category (e.g., "financial", "medical", "standard")
 * @param className  the fully-qualified Java class name
 * @param simpleName the simple class name for display
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
public record RendererCatalogEntry(String category, String className, String simpleName) {

    /**
     * Creates a catalog entry from category and class name.
     *
     * <p>The simple name is automatically extracted from the class name.
     *
     * @param category  the renderer category
     * @param className the fully-qualified class name
     * @return a new catalog entry
     */
    static RendererCatalogEntry of(String category, String className) {
        int idx = className.lastIndexOf('.');
        String simple = idx >= 0 ? className.substring(idx + 1) : className;
        return new RendererCatalogEntry(category, className, simple);
    }
}
