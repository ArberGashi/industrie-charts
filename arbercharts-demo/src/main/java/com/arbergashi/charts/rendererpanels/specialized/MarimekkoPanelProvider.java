package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.MarimekkoRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class MarimekkoPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Market Segmentation");
        // X = category, Y = segment value, weight = category width
        // Mobile Category (Width=48)
        model.addPoint(0, 42, 48, "iOS");
        model.addPoint(0, 58, 48, "Android");
        // Desktop Category (Width=32)
        model.addPoint(1, 62, 32, "Windows");
        model.addPoint(1, 26, 32, "macOS");
        model.addPoint(1, 12, 32, "Linux");
        // Tablet Category (Width=20)
        model.addPoint(2, 72, 20, "iPadOS");
        model.addPoint(2, 28, 20, "Android Tablet");
        
        return new ArberChartPanel(model, new MarimekkoRenderer());
    }
}
