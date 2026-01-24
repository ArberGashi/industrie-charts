package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.ScatterRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class ScatterPlotPanelProvider {
    public static ArberChartPanel create() {
        // Clustered scatter with class separation and mild overlap.
        DefaultChartModel clusterA = new DefaultChartModel("Cluster A");
        DefaultChartModel clusterB = new DefaultChartModel("Cluster B");
        DefaultChartModel clusterC = new DefaultChartModel("Cluster C");
        DefaultChartModel clusterD = new DefaultChartModel("Cluster D");
        DefaultChartModel noise = new DefaultChartModel("Noise");

        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 5);

        // Cluster 1
        for (int i = 0; i < 90; i++) {
            double x = 22 + rand.nextGaussian() * 4.5;
            double y = 35 + rand.nextGaussian() * 4.0;
            clusterA.addPoint(x, y, 0, "Cluster A");
        }

        // Cluster 2
        for (int i = 0; i < 70; i++) {
            double x = 68 + rand.nextGaussian() * 6.5;
            double y = 74 + rand.nextGaussian() * 5.8;
            clusterB.addPoint(x, y, 0, "Cluster B");
        }

        // Cluster 3
        for (int i = 0; i < 60; i++) {
            double x = 75 + rand.nextGaussian() * 5.0;
            double y = 25 + rand.nextGaussian() * 4.5;
            clusterC.addPoint(x, y, 0, "Cluster C");
        }

        // Cluster 4
        for (int i = 0; i < 55; i++) {
            double x = 30 + rand.nextGaussian() * 4.0;
            double y = 78 + rand.nextGaussian() * 5.0;
            clusterD.addPoint(x, y, 0, "Cluster D");
        }

        // Noise
        for (int i = 0; i < 40; i++) {
            noise.addPoint(rand.nextDouble() * 100, rand.nextDouble() * 100, 0, "Noise / outlier");
        }

        return ArberChartBuilder.create()
                .withTitle("Scatter Plot - Cluster Separation")
                .addLayer(clusterA, new ScatterRenderer())
                .addLayer(clusterB, new ScatterRenderer())
                .addLayer(clusterC, new ScatterRenderer())
                .addLayer(clusterD, new ScatterRenderer())
                .addLayer(noise, new ScatterRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
