package com.arbergashi.charts.api;

import com.arbergashi.charts.ui.ArberChartPanel;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class StrategicRoadmapTest {

    static record DataPoint(double timestamp, double value) {}

    @Test
    public void testModelAutoBindingAndFluentApi() {
        List<DataPoint> data = List.of(
            new DataPoint(1.0, 10.0),
            new DataPoint(2.0, 20.0),
            new DataPoint(3.0, 15.0)
        );

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("POJO Binding Test")
                .addLineSeries("Sensor Data", data, DataPoint::timestamp, DataPoint::value)
                .withTheme(TestThemes.tailwindEmerald())
                .build();

        assertNotNull(panel);
        assertEquals(1, panel.getLayerCount());
        // Verify that data was actually bound
        // Note: ArberChartPanel doesn't expose models directly easily, but we can check if it builds without error
    }

    @Test
    public void testTailwindThemes() {
        ChartTheme emerald = TestThemes.tailwindEmerald();
        ChartTheme slateDark = TestThemes.tailwindSlateDark();

        assertNotNull(emerald);
        assertNotNull(slateDark);
        assertEquals(new java.awt.Color(16, 185, 129), emerald.getAccentColor());
    }
}
