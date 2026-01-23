package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.model.DefaultChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to identify why PieRenderer shows only blue circles.
 */
public class PieRendererDebugTest {

    @Test
    public void testSegmentColors() {
        DefaultChartModel model = new DefaultChartModel("Test");
        model.addPoint(0, 40, 0, "Red");
        model.addPoint(1, 30, 0, "Green");
        model.addPoint(2, 20, 0, "Blue");
        model.addPoint(3, 10, 0, "Yellow");

        PieRenderer renderer = new PieRenderer();
        renderer.setTheme(ChartThemes.defaultLight());

        // Test getSegmentColor
        Color c0 = renderer.getSegmentColor(0);
        Color c1 = renderer.getSegmentColor(1);
        Color c2 = renderer.getSegmentColor(2);
        Color c3 = renderer.getSegmentColor(3);

        // They should all be DIFFERENT
        assertNotEquals(c0, c1, "Color 0 and 1 should be different");
        assertNotEquals(c1, c2, "Color 1 and 2 should be different");
        assertNotEquals(c2, c3, "Color 2 and 3 should be different");
        assertNotEquals(c0, c2, "Color 0 and 2 should be different");
    }

    @Test
    public void testAggregateData() {
        DefaultChartModel model = new DefaultChartModel("Test");
        model.addPoint(0, 40, 0, "Red");
        model.addPoint(1, 30, 0, "Green");
        model.addPoint(2, 20, 0, "Blue");
        model.addPoint(3, 10, 0, "Yellow");

        PieRenderer renderer = new PieRenderer();
        java.util.LinkedHashMap<String, Double> result = new java.util.LinkedHashMap<>();
        double total = renderer.aggregateData(model, result);

        assertEquals(100.0, total, 0.01);
        assertEquals(4, result.size(), "Should have 4 distinct segments");
        assertEquals(40.0, result.get("Red"), 0.01);
        assertEquals(30.0, result.get("Green"), 0.01);
        assertEquals(20.0, result.get("Blue"), 0.01);
        assertEquals(10.0, result.get("Yellow"), 0.01);
    }
}
