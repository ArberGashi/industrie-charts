package com.arbergashi.charts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleChartContractTest {

    @Test
    void startStreamingReportsStableVersionTwoContract() {
        SimpleChart chart = new SimpleChart();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
            () -> chart.startStreaming(() -> 1.0));

        assertTrue(ex.getMessage().contains("2.0.0"));
        assertFalse(ex.getMessage().contains("2.0.1"));
    }

    @Test
    void exportMethodsReportStableVersionTwoContract() {
        SimpleChart chart = new SimpleChart();

        UnsupportedOperationException png = assertThrows(UnsupportedOperationException.class,
            () -> chart.exportToPNG("a.png"));
        UnsupportedOperationException svg = assertThrows(UnsupportedOperationException.class,
            () -> chart.exportToSVG("a.svg"));

        assertTrue(png.getMessage().contains("2.0.0"));
        assertTrue(svg.getMessage().contains("2.0.0"));
        assertFalse(png.getMessage().contains("2.0.1"));
        assertFalse(svg.getMessage().contains("2.0.1"));
    }
}
