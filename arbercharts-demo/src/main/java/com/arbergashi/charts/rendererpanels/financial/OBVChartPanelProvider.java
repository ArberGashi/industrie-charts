package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.OBVRenderer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.ui.ArberChartPanel;

public class OBVChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(200, DemoPanelUtils.DEMO_SEED + 913);
        DefaultChartModel price = bundle.priceModel;
        DefaultChartModel volume = bundle.volumeModel;

        DefaultChartModel obvModel = new DefaultChartModel("AURX OBV");
        int count = Math.min(price.getPointCount(), volume.getPointCount());
        for (int i = 0; i < count; i++) {
            double x = price.getX(i);
            double close = price.getY(i);
            double vol = volume.getY(i);
            obvModel.addPoint(x, close, vol, "");
        }

        return ArberChartBuilder.create()
                .withTitle("OBV - AURX On-Balance Volume")
                .addLayer(obvModel, new OBVRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
