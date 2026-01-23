package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultTernaryChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.specialized.TernaryContourRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TernaryContourPanelProvider {
    public static ArberChartPanel create() {
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 136);
        List<TernaryChartModel.TernaryPoint> data = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            double a = 0.35 + rand.nextGaussian() * 0.12;
            double b = 0.30 + rand.nextGaussian() * 0.10;
            a = Math.max(0.05, Math.min(0.85, a));
            b = Math.max(0.05, Math.min(0.85, b));
            double c = Math.max(0.05, 1 - a - b);
            double norm = a + b + c;
            data.add(new DefaultTernaryChartModel.DefaultTernaryPoint(a / norm, b / norm, c / norm));
        }
        DefaultTernaryChartModel model = new DefaultTernaryChartModel(data, List.of("A", "B", "C"));
        return com.arbergashi.charts.api.ArberChartBuilder.create()
                .addLayer(model, new TernaryContourRenderer())
                .build();
    }
}
