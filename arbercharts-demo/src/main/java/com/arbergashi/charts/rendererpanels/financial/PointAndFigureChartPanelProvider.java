package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.PointAndFigureRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class PointAndFigureChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel base = DemoPanelUtils.generateRegimeCloseSeries(
                "AURX Point and Figure", 260, DemoPanelUtils.DEMO_SEED + 713, 84.0);
        DefaultChartModel model = new DefaultChartModel(base.getName());
        double prev = base.getY(0);
        for (int i = 0; i < base.getPointCount(); i++) {
            double x = base.getX(i);
            double y = base.getY(i);
            double weight = y >= prev ? 1.0 : -1.0;
            String label = base.getLabel(i);
            model.addPoint(x, y, weight, label);
            prev = y;
        }

        return ArberChartBuilder.create()
                .withTitle("Point & Figure - AURX Supply/Demand")
                .addLayer(model, new PointAndFigureRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
