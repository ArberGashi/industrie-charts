package com.arbergashi.charts.api;

import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.testutils.EdtTestUtils;
import com.arbergashi.charts.util.ColorRegistry;
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

        ArberChartPanel panel = EdtTestUtils.callOnEdt(() ->
                ArberChartBuilder.of()
                        .setTitle("POJO Binding Test")
                        .setLineSeries("Sensor Data", data, DataPoint::timestamp, DataPoint::value)
                        .setTheme(TestThemes.tailwindEmerald())
                        .build()
        );

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
        assertEquals(ColorRegistry.of(16, 185, 129, 255), emerald.getAccentColor());
    }
}
