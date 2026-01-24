package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.StepAreaRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class StepAreaPanelProvider {
    public static ArberChartPanel create() {
        // Autoscaling tiers over a day (step changes, not smooth)
        DefaultChartModel cpu = new DefaultChartModel("CPU Usage (%)");
        DefaultChartModel memory = new DefaultChartModel("Memory (%)");
        DefaultChartModel disk = new DefaultChartModel("Disk I/O (%)");

        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 41);
        int hours = 24;
        for (int i = 0; i < hours; i++) {
            double tierBoost;
            if (i < 6) {
                tierBoost = 0.0;
            } else if (i < 10) {
                tierBoost = 10.0;
            } else if (i < 17) {
                tierBoost = 22.0;
            } else if (i < 21) {
                tierBoost = 12.0;
            } else {
                tierBoost = 4.0;
            }
            double cpuVal = 26 + tierBoost + rand.nextGaussian() * 2.5;
            double memVal = 40 + tierBoost * 0.7 + rand.nextGaussian() * 2.0;
            double diskVal = 15 + tierBoost * 0.4 + rand.nextGaussian() * 1.5;
            cpu.addPoint(i, cpuVal, 0, String.format("%02d:00", i));
            memory.addPoint(i, memVal, 0, String.format("%02d:00", i));
            disk.addPoint(i, diskVal, 0, String.format("%02d:00", i));
        }

        return ArberChartBuilder.create()
                .withTitle("Step Area - Autoscaling Tiers")
                .addLayer(cpu, new StepAreaRenderer())
                .addLayer(memory, new StepAreaRenderer())
                .addLayer(disk, new StepAreaRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
