package com.arbergashi.charts.visualverifier.dto;

/**
 * Immutable render request for chart generation.
 *
 * @param renderer fully qualified renderer class name
 * @param width    chart width in pixels (1-4096)
 * @param height   chart height in pixels (1-4096)
 * @param theme    theme name (light, dark)
 * @since 2.0.0
 */
public record RenderRequest(
        String renderer,
        int width,
        int height,
        String theme
) {
    /**
     * Compact constructor with validation.
     */
    public RenderRequest {
        if (renderer == null || renderer.isBlank()) {
            throw new IllegalArgumentException("Renderer cannot be null or empty");
        }
        width = Math.clamp(width, 1, 4096);
        height = Math.clamp(height, 1, 4096);
        if (theme == null || theme.isBlank()) {
            theme = "light";
        }
    }

    /**
     * Creates a default request (800x600, light theme).
     */
    public static RenderRequest of(String renderer) {
        return new RenderRequest(renderer, 800, 600, "light");
    }

    /**
     * Creates a request with custom dimensions.
     */
    public static RenderRequest of(String renderer, int width, int height) {
        return new RenderRequest(renderer, width, height, "light");
    }
}

