package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.VolumeRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class VolumeChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(180, DemoPanelUtils.DEMO_SEED + 715);
        DefaultChartModel model = bundle.volumeModel;
        model.setName("AURX Trading Volume (shares)");

        return ArberChartBuilder.create()
                .withTitle("Volume - AURX Trading Volume")
                .addLayer(model, new VolumeRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
