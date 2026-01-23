package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.RenkoRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class RenkoChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(220, DemoPanelUtils.DEMO_SEED + 711);
        DefaultChartModel base = bundle.priceModel;
        DefaultChartModel model = new DefaultChartModel("AURX Renko");
        double baseline = base.getPointCount() > 0 ? base.getY(0) : 0.0;
        int count = base.getPointCount();
        for (int i = 0; i < count; i++) {
            double x = base.getX(i);
            double y = base.getY(i) - baseline;
            double low = base.getMin(i) - baseline;
            double high = base.getMax(i) - baseline;
            double open = base.getWeight(i) - baseline;
            model.addPoint(x, y, low, high, open, "");
        }

        return ArberChartBuilder.create()
                .withTitle("Renko Chart - AURX Brick Trends")
                .addLayer(model, new RenkoRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
