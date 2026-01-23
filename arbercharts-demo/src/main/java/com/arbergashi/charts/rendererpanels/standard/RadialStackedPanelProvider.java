package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.RadialStackedRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class RadialStackedPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel committed = new DefaultChartModel("Committed");
        DefaultChartModel stretch = new DefaultChartModel("Stretch");
        
        String[] cats = {"Q1", "Q2", "Q3", "Q4"};
        double[] v1 = {120, 155, 170, 200};
        double[] v2 = {40, 65, 55, 85};
        
        for (int i = 0; i < cats.length; i++) {
            // X stores the base (stack bottom), Y stores the value
            committed.addPoint(0, v1[i], 0, cats[i]);
            stretch.addPoint(v1[i], v2[i], 0, cats[i]);
        }
        
        return ArberChartBuilder.create()
                .withTitle("Radial Stacked - Quarterly Targets")
                .addLayer(committed, new RadialStackedRenderer())
                .addLayer(stretch, new RadialStackedRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
