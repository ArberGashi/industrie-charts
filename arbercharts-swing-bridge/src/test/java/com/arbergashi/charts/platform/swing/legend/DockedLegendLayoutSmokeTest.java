package com.arbergashi.charts.platform.swing.legend;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.domain.legend.LegendDockSide;
import com.arbergashi.charts.domain.legend.LegendPosition;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DockedLegendLayoutSmokeTest {

    @Test
    void canCreateChartWithDockedLegend() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = ArberChartBuilder.of()
                    .setTheme(ChartThemes.getDarkTheme())
                    .setLineSeries("S", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                    .build()
                    .setDockedLegend(LegendDockSide.RIGHT);

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
            ArberChartPanel panel = ArberChartBuilder.of()
                    .setLineSeries("S", new double[]{0, 1, 2}, new double[]{1, 2, 3})
                    .build();

            panel.setDockedLegend(LegendDockSide.LEFT);
            panel.setOverlayLegend(LegendPosition.TOP_RIGHT);

            panel.setSize(800, 600);
            panel.doLayout();

            assertTrue(panel.isLegendVisible());
        });
    }
}
