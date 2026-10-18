package com.arbergashi.charts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleChartContractTest {

    @Test
    void startStreamingReportsUnsupportedConvenienceOperationAndManualAlternative() {
        SimpleChart chart = new SimpleChart();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
            () -> chart.startStreaming(() -> 1.0));

        assertTrue(ex.getMessage().contains("SimpleChart is not implemented"));
        assertTrue(ex.getMessage().contains("addPoint() or addData()"));
    }

    @Test
    void exportMethodsReportUnsupportedConvenienceOperationsAndHostAlternatives() {
        SimpleChart chart = new SimpleChart();

        UnsupportedOperationException png = assertThrows(UnsupportedOperationException.class,
            () -> chart.exportToPNG("a.png"));
        UnsupportedOperationException svg = assertThrows(UnsupportedOperationException.class,
            () -> chart.exportToSVG("a.svg"));

        assertTrue(png.getMessage().contains("PNG export through SimpleChart is not implemented"));
        assertTrue(png.getMessage().contains("ServerRenderService.renderToPng()"));
        assertTrue(svg.getMessage().contains("SVG export through SimpleChart is not implemented"));
        assertTrue(svg.getMessage().contains("ChartExportService.exportSvg()"));
    }
}
