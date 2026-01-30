package com.arbergashi.charts.core.rendering;

/**
 * Headless render orchestrator.
 *
 * <p>Provides an entry point for server-side rendering without UI dependencies.</p>
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ArberRenderingEngine {
    public interface RenderTask {
        void render(ArberCanvas canvas);
    }

    public void render(ArberCanvas canvas, RenderTask task) {
        if (canvas == null || task == null) return;
        task.render(canvas);
    }
}
