package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.StackedBarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class StackedBarPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel subscription = new DefaultChartModel("Subscriptions");
        DefaultChartModel services = new DefaultChartModel("Services");
        DefaultChartModel hardware = new DefaultChartModel("Hardware");

        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 40);
        String[] quarters = {"Q1 2025", "Q2 2025", "Q3 2025", "Q4 2025"};

        for (int i = 0; i < quarters.length; i++) {
            double subscriptionBase = 240 + i * 22;
            double servicesBase = 95 + i * 10;
            double hardwareBase = 62 + i * 3;
            subscription.addPoint(i, subscriptionBase + rand.nextGaussian() * 10, 0, quarters[i]);
            services.addPoint(i, servicesBase + rand.nextGaussian() * 7, 0, quarters[i]);
            hardware.addPoint(i, hardwareBase + rand.nextGaussian() * 5, 0, quarters[i]);
        }

        return ArberChartBuilder.create()
                .withTitle("Stacked Bar - Revenue Mix by Quarter (k$)")
                .addLayer(subscription, new StackedBarRenderer())
                .addLayer(services, new StackedBarRenderer())
                .addLayer(hardware, new StackedBarRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
