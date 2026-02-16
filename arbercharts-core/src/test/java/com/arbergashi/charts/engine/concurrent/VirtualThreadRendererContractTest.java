package com.arbergashi.charts.engine.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualThreadRendererContractTest {

    @Test
    void asyncExportsReportStableVersionTwoContract() {
        VirtualThreadRenderer renderer = VirtualThreadRenderer.createWithFallback();
        try {
            CompletionException pngFailure = assertThrows(CompletionException.class,
                () -> renderer.renderPngAsync(new Object()).join());
            CompletionException svgFailure = assertThrows(CompletionException.class,
                () -> renderer.renderSvgAsync(new Object()).join());

            UnsupportedOperationException pngCause =
                assertInstanceOf(UnsupportedOperationException.class, pngFailure.getCause());
            UnsupportedOperationException svgCause =
                assertInstanceOf(UnsupportedOperationException.class, svgFailure.getCause());

            assertTrue(pngCause.getMessage().contains("2.0.0"));
            assertTrue(svgCause.getMessage().contains("2.0.0"));
            assertFalse(pngCause.getMessage().contains("2.0.1"));
            assertFalse(svgCause.getMessage().contains("2.0.1"));
        } finally {
            renderer.shutdown();
        }
    }
}
