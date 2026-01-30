package com.arbergashi.charts.api;

import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.testutils.EdtTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FluentApiTest {

    @Test
    public void testBuilderPattern() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {10, 20, 15, 25, 30};

        ArberChartPanel panel = EdtTestUtils.callOnEdt(() ->
                ArberChartBuilder.of()
                        .setTitle("Test Chart")
                        .setLineSeries("Revenue", x, y)
                        .setDarkMode()
                        .setLegend(true)
                        .setTooltips(true)
                        .build()
        );

        assertNotNull(panel);
        assertEquals(1, panel.getLayerCount());
        assertTrue(panel.isLegendVisible());
    }

    @Test
    public void testEmptyLayersFail() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                EdtTestUtils.callOnEdt(() -> ArberChartBuilder.of().build())
        );
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }
}
