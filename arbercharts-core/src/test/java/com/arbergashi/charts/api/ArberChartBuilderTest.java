package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.ui.grid.AnalysisGridLayer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ArberChartBuilderTest {

    @Test
    void build_appliesGridLayer_whenConfigured() {
        DefaultChartModel model = new DefaultChartModel("m");
        model.addPoint(0, 0, 0, null);
        model.addPoint(1, 1, 0, null);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("t")
                .withGridLayer(new AnalysisGridLayer())
                .addLayer(model, new LineRenderer())
                .build();

        assertNotNull(panel);
        assertEquals(1, panel.getLayerCount());
    }

    @Test
    void build_supportsTypicalFluentConfiguration_withoutExceptions() {
        DefaultChartModel model = new DefaultChartModel("series");
        model.addPoint(0, 0, 0, null);
        model.addPoint(1, 1, 0, null);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Demo")
                .withTheme(ChartThemes.defaultDark())
                .withGridLayer(new AnalysisGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .addLayer(model, new LineRenderer())
                .build();

        assertNotNull(panel);
        assertTrue(panel.isLegendVisible());
        assertEquals(1, panel.getLayerCount());
    }

    @Test
    void build_appliesLocale_whenConfigured() {
        DefaultChartModel model = new DefaultChartModel("series");
        model.addPoint(0, 0, 0, null);
        model.addPoint(1, 1, 0, null);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withLocale(Locale.US)
                .addLayer(model, new LineRenderer())
                .build();

        assertNotNull(panel);
        assertEquals(Locale.US, panel.getLocale());
    }
}
