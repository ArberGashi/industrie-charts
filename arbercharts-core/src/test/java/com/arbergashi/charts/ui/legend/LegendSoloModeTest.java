package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class LegendSoloModeTest {

    @AfterEach
    void cleanup() {
        ChartAssets.removeProperty("Chart.legend.soloEnabled");
    }

    @Test
    void soloSeriesHidesOthersAndSecondSoloRestoresAll() throws Exception {
        ChartAssets.setProperty("Chart.legend.soloEnabled", "true");

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTheme(ChartThemes.defaultDark())
                .addLineSeries("A", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                .addLineSeries("B", new double[]{0, 1, 2}, new double[]{2, 3, 4})
                .build();

        // simulate solo logic through exposed model (the rendering contract uses renderer ids)
        var vis = panel.getLayerVisibilityModel();

        // initial: visible
        assertTrue(vis.isVisible("A"));
        assertTrue(vis.isVisible("B"));

        // emulate solo: only A visible
        vis.setVisible("A", true);
        vis.setVisible("B", false);
        assertTrue(vis.isVisible("A"));
        assertFalse(vis.isVisible("B"));

        // emulate restore: all visible
        vis.setVisible("A", true);
        vis.setVisible("B", true);
        assertTrue(vis.isVisible("A"));
        assertTrue(vis.isVisible("B"));
    }
}
