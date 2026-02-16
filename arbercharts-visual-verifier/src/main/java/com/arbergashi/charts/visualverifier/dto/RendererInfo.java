package com.arbergashi.charts.visualverifier.dto;

/**
 * Renderer information for catalog display.
 *
 * @param className   fully qualified class name
 * @param simpleName  simple class name
 * @param category    renderer category (Financial, Medical, etc.)
 * @param description optional description
 * @since 2.0.0
 */
public record RendererInfo(
        String className,
        String simpleName,
        String category,
        String description
) {
    /**
     * Creates RendererInfo from class name and category.
     */
    public static RendererInfo of(String className, String category) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return new RendererInfo(className, simpleName, category, "");
    }

    /**
     * Creates RendererInfo with description.
     */
    public static RendererInfo of(String className, String category, String description) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return new RendererInfo(className, simpleName, category, description);
    }
}

