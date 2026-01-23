package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.BulletChartRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class BulletChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Sales Performance KPI");
        // BulletChartRenderer: Point 0 = actual, Point 1 = target
        model.addPoint(88, 0, 0, "Actual Sales");
        model.addPoint(95, 0, 0, "Target Goal");
        
        return new ArberChartPanel(model, new BulletChartRenderer());
    }
}
