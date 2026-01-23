package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.ChernoffFacesRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ChernoffFacesPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Emotional Analytics");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 112);
        String[] teams = {"Support", "Sales", "Engineering", "Design", "Ops", "Security", "Finance", "HR", "PM", "QA"};
        for (int i = 0; i < teams.length; i++) {
            double happiness = 30 + rand.nextDouble() * 35;
            double stress = 15 + rand.nextDouble() * 25;
            model.addPoint(happiness, stress, 0, teams[i]);
        }
        return new ArberChartPanel(model, new ChernoffFacesRenderer());
    }
}
