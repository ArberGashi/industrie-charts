package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.core.rendering.ArberCanvasProvider;

public final class AwtCanvasProvider implements ArberCanvasProvider {
    @Override
    public String getId() {
        return "swing";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
