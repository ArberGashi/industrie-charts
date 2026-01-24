package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.KagiRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class KagiChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = DemoPanelUtils.generateRegimeCloseSeries(
                "AURX Kagi", 240, DemoPanelUtils.DEMO_SEED + 712, 96.0);

        return ArberChartBuilder.create()
                .withTitle("Kagi - AURX Trend Reversals")
                .addLayer(model, new KagiRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
