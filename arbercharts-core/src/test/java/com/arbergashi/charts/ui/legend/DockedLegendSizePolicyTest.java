package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DockedLegendSizePolicyTest {

    private void clearKey(String key) {
        ChartAssets.removeProperty(key);
    }

    @AfterEach
    void cleanup() {
        clearKey("Chart.legend.dock.density");
        clearKey("Chart.legend.dock.preferredWidth");
        clearKey("Chart.legend.dock.preferredHeight");
        clearKey("Chart.legend.dock.right.preferredWidth");
        clearKey("Chart.legend.dock.left.preferredWidth");
        clearKey("Chart.legend.dock.top.preferredHeight");
        clearKey("Chart.legend.dock.bottom.preferredHeight");
        clearKey("Chart.legend.dock.minWidth");
        clearKey("Chart.legend.dock.minHeight");
        clearKey("Chart.legend.dock.maxWidth");
        clearKey("Chart.legend.dock.maxHeight");
    }

    @Test
    void compactDefaultsAreSideAware() {
        ChartAssets.setProperty("Chart.legend.dock.density", "compact");

        DockedLegendPanel right = new DockedLegendPanel(context(), ChartThemes.defaultDark());
        right.setDockSide(LegendDockSide.RIGHT);
        Dimension prefRight = right.getPreferredSize();

        DockedLegendPanel top = new DockedLegendPanel(context(), ChartThemes.defaultDark());
        top.setDockSide(LegendDockSide.TOP);
        Dimension prefTop = top.getPreferredSize();

        assertTrue(prefRight.width >= 200, "right dock should have a meaningful width");
        assertTrue(prefTop.height >= 100, "top dock should have a meaningful height");
    }

    @Test
    void denseDefaultsAreLargerThanCompact() {
        ChartAssets.setProperty("Chart.legend.dock.density", "compact");
        DockedLegendPanel compact = new DockedLegendPanel(context(), ChartThemes.defaultDark());
        compact.setDockSide(LegendDockSide.RIGHT);
        Dimension compactPref = compact.getPreferredSize();

        ChartAssets.setProperty("Chart.legend.dock.density", "dense");
        DockedLegendPanel dense = new DockedLegendPanel(context(), ChartThemes.defaultDark());
        dense.setDockSide(LegendDockSide.RIGHT);
        Dimension densePref = dense.getPreferredSize();

        assertTrue(densePref.width >= compactPref.width, "dense width should be >= compact width");
        assertTrue(densePref.height >= compactPref.height, "dense height should be >= compact height");
    }

    @Test
    void propertiesOverrideDefaults() {
        ChartAssets.setProperty("Chart.legend.dock.density", "compact");
        ChartAssets.setProperty("Chart.legend.dock.right.preferredWidth", "420");

        DockedLegendPanel p = new DockedLegendPanel(context(), ChartThemes.defaultDark());
        p.setDockSide(LegendDockSide.RIGHT);
        Dimension pref = p.getPreferredSize();

        assertTrue(pref.width >= 300, "property override should increase preferred width");
    }

    private static LegendChartContext context() {
        return new LegendChartContext() {
            @Override
            public com.arbergashi.charts.model.ChartModel getModel() {
                return null;
            }

            @Override
            public java.util.List<com.arbergashi.charts.render.BaseRenderer> getRenderers() {
                return java.util.List.of();
            }
        };
    }
}
