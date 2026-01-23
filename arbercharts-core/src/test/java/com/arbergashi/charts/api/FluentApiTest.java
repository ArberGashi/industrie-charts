package com.arbergashi.charts.api;

import com.arbergashi.charts.ui.ArberChartPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FluentApiTest {

    @Test
    public void testBuilderPattern() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {10, 20, 15, 25, 30};

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Test Chart")
                .addLineSeries("Revenue", x, y)
                .withDarkMode()
                .withLegend(true)
                .withTooltips(true)
                .build();

        assertNotNull(panel);
        assertEquals(1, panel.getLayerCount());
        assertTrue(panel.isLegendVisible());
    }

    @Test
    public void testEmptyLayersFail() {
        assertThrows(IllegalStateException.class, () -> {
            ArberChartBuilder.create().build();
        });
    }
}
