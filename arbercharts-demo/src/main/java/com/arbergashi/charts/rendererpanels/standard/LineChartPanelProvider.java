package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class LineChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel web = new DefaultChartModel("Web");
        DefaultChartModel mobile = new DefaultChartModel("Mobile");
        DefaultChartModel api = new DefaultChartModel("API");

        Random r = new Random(DemoPanelUtils.DEMO_SEED + 1);
        int hours = 24 * 14;
        for (int i = 0; i < hours; i++) {
            int day = i / 24;
            int hour = i % 24;
            boolean weekend = (day % 7) >= 5;

            double diurnal = Math.sin((hour - 8) * Math.PI / 12.0);
            double peakBoost = (hour == 11 || hour == 16) ? 12.0 : 0.0;
            double weekendDamp = weekend ? 0.78 : 1.0;

            double webReq = 120 + weekendDamp * (35 * diurnal + peakBoost) + r.nextGaussian() * 4.5;
            double mobileReq = 90 + weekendDamp * (28 * Math.sin((hour - 6) * Math.PI / 12.0)) + r.nextGaussian() * 3.5;
            double apiReq = 60 + weekendDamp * (20 * Math.cos((hour + 1) * Math.PI / 12.0)) + r.nextGaussian() * 3.0;

            String label = String.format("Day %d %02d:00", day + 1, hour);
            web.addPoint(i, webReq, 0, label);
            mobile.addPoint(i, mobileReq, 0, label);
            api.addPoint(i, apiReq, 0, label);
        }

        return ArberChartBuilder.create()
                .withTitle("Line Chart - Traffic by Channel (k req/min)")
                .addLayer(web, new LineRenderer())
                .addLayer(mobile, new LineRenderer())
                .addLayer(api, new LineRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
