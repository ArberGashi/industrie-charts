package com.arbergashi.charts.core.rendering;

/**
 * Test-only provider to validate BridgeFactory headless selection.
 */
public final class ServerTestCanvasProvider implements ArberCanvasProvider {
    @Override
    public String getId() {
        return "server";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
