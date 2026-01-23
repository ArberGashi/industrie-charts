package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.ui.ArberChartPanel;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DockedLegendLayoutSmokeTest {

    @Test
    void canCreateChartWithDockedLegend() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = ArberChartBuilder.create()
                    .withTheme(ChartThemes.defaultDark())
                    .addLineSeries("S", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                    .build()
                    .withDockedLegend(LegendDockSide.RIGHT);

            assertNotNull(panel);

            panel.setSize(800, 600);
            panel.doLayout();

            assertTrue(panel.getLayout() instanceof BorderLayout);

            BorderLayout bl = (BorderLayout) panel.getLayout();
            Component center = bl.getLayoutComponent(BorderLayout.CENTER);
            Component east = bl.getLayoutComponent(BorderLayout.EAST);

            assertNotNull(center, "overlay canvas must be present in CENTER");
            assertNotNull(east, "docked legend must be present on EAST");
        });
    }

    @Test
    void canSwitchBackToOverlayLegend() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = ArberChartBuilder.create()
                    .addLineSeries("S", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                    .build();

            panel.withDockedLegend(LegendDockSide.LEFT);
            panel.withOverlayLegend(LegendPosition.TOP_RIGHT);

            panel.setSize(800, 600);
            panel.doLayout();

            assertTrue(panel.isLegendVisible());
        });
    }
}
