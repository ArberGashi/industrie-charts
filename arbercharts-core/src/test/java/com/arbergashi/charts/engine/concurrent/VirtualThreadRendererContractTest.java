package com.arbergashi.charts.engine.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualThreadRendererContractTest {

    @Test
    void asyncExportsReportUnsupportedConvenienceOperationsAndHostAlternatives() {
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

            assertTrue(pngCause.getMessage().contains("PNG rendering through VirtualThreadRenderer is not implemented"));
            assertTrue(pngCause.getMessage().contains("ServerRenderService.renderToPng()"));
            assertTrue(svgCause.getMessage().contains("SVG rendering through VirtualThreadRenderer is not implemented"));
            assertTrue(svgCause.getMessage().contains("ChartExportService.exportSvg()"));
        } finally {
            renderer.shutdown();
        }
    }
}
