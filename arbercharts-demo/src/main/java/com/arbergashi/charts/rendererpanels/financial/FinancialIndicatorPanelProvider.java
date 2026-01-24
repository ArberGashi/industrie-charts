package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.VolumeRenderer;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

/**
 * Base provider for financial indicator demos.
 * Generates realistic OHLC price data with volume for professional-grade visualization.
 */
public class FinancialIndicatorPanelProvider {
    public static ArberChartPanel create(ChartRenderer renderer) {
        return create(renderer, "Indicator - AURX Analysis", true);
    }

    public static ArberChartPanel create(ChartRenderer renderer, String title) {
        return create(renderer, title, true);
    }

    public static ArberChartPanel create(ChartRenderer renderer, String title, boolean includeVolume) {
        long seed = DemoPanelUtils.DEMO_SEED + renderer.getClass().getName().hashCode();
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(200, seed);
        bundle.priceModel.setName("AURX Daily");
        bundle.volumeModel.setName("AURX Volume");

        ArberChartBuilder builder = ArberChartBuilder.create()
                .withTitle(title)
                .addLayer(bundle.priceModel, renderer)
                .withTooltips(true)
                .withLegend(true);

        if (includeVolume) {
            builder.addLayer(bundle.volumeModel, new VolumeRenderer());
        }

        return builder.build().withAnimations(true);
    }
}
