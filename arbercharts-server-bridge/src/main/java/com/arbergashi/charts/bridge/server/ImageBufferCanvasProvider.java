package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.core.rendering.ArberCanvasProvider;

public final class ImageBufferCanvasProvider implements ArberCanvasProvider {
    @Override
    public String getId() {
        return "server";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
