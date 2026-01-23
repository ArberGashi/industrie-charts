package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.ui.ArberChartPanel;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class LegendVisibilityToggleTest {

    @Test
    void visibilityModelDefaultsToVisibleAndToggles() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LayerVisibilityModel m = new LayerVisibilityModel();
            assertTrue(m.isVisible("line"));
            m.setVisible("line", false);
            assertFalse(m.isVisible("line"));
            m.toggle("line");
            assertTrue(m.isVisible("line"));
        });
    }

    @Test
    void chartPanelExposesVisibilityModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = ArberChartBuilder.create()
                    .withTheme(ChartThemes.defaultDark())
                    .addLineSeries("S", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                    .build();
            assertNotNull(panel.getLayerVisibilityModel());
        });
    }
}
