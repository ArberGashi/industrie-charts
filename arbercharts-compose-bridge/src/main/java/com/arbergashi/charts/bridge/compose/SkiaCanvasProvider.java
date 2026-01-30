package com.arbergashi.charts.bridge.compose;

import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberCanvasProvider;

/**
 * Placeholder for Skia-backed canvas.
 *
 * <p>Compose/Skia bindings are supplied in the Compose demo module.</p>
 */
public final class SkiaCanvasProvider implements ArberCanvasProvider {
    public SkiaCanvasProvider() {}

    @Override
    public String getId() {
        return "compose";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isSupported() {
        try {
            Class.forName("org.jetbrains.skia.Canvas");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
