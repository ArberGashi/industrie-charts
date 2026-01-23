package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.DependencyWheelRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class DependencyWheelPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Module Dependencies");
        model.addPoint(0, 1, 0, "Gateway:Auth");
        model.addPoint(1, 1, 0, "Auth:User");
        model.addPoint(2, 1, 0, "User:Profile");
        model.addPoint(3, 1, 0, "Catalog:Search");
        model.addPoint(4, 1, 0, "Checkout:Payments");
        model.addPoint(5, 1, 0, "Payments:Fraud");
        model.addPoint(6, 1, 0, "Orders:Shipping");
        model.addPoint(7, 1, 0, "Orders:Notifications");
        return new ArberChartPanel(model, new DependencyWheelRenderer());
    }
}
