package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.grid.AnalysisGridLayer;
import com.arbergashi.charts.testutils.EdtTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ArberChartBuilderTest {

    @Test
    void build_appliesGridLayer_whenConfigured() {
        DefaultChartModel model = new DefaultChartModel("m");
        model.setPoint(0, 0, 0, null);
        model.setPoint(1, 1, 0, null);

        ArberChartPanel panel = EdtTestUtils.callOnEdt(() ->
                ArberChartBuilder.of()
                        .setTitle("t")
                        .setGridLayer(new AnalysisGridLayer())
                        .setLayer(model, new LineRenderer())
                        .build()
        );

        assertNotNull(panel);
        assertEquals(1, panel.getLayerCount());
    }

    @Test
    void build_supportsTypicalFluentConfiguration_withoutExceptions() {
        DefaultChartModel model = new DefaultChartModel("series");
        model.setPoint(0, 0, 0, null);
        model.setPoint(1, 1, 0, null);

        ArberChartPanel panel = EdtTestUtils.callOnEdt(() ->
                ArberChartBuilder.of()
                        .setTitle("Demo")
                        .setTheme(ChartThemes.getDarkTheme())
                        .setGridLayer(new AnalysisGridLayer())
                        .setTooltips(true)
                        .setLegend(true)
                        .setLayer(model, new LineRenderer())
                        .build()
        );

        assertNotNull(panel);
        assertTrue(panel.isLegendVisible());
        assertEquals(1, panel.getLayerCount());
    }

    @Test
    void build_appliesLocale_whenConfigured() {
        DefaultChartModel model = new DefaultChartModel("series");
        model.setPoint(0, 0, 0, null);
        model.setPoint(1, 1, 0, null);

        ArberChartPanel panel = EdtTestUtils.callOnEdt(() ->
                ArberChartBuilder.of()
                        .setLocale(Locale.US)
                        .setLayer(model, new LineRenderer())
                        .build()
        );

        assertNotNull(panel);
        assertEquals(Locale.US, panel.getLocale());
    }
}
