package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.WindRoseRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class WindRosePanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Wind Distribution");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 123);
        // WindRose expects x=directionDegrees, y=speed
        for (int i = 0; i < 200; i++) {
            double direction = rand.nextDouble() * 360;
            double speed = 5 + rand.nextDouble() * 20;
            if (rand.nextDouble() > 0.55) {
                direction = 260 + rand.nextGaussian() * 18;
                speed += 6;
            } else if (rand.nextDouble() > 0.78) {
                direction = 110 + rand.nextGaussian() * 14;
                speed += 4;
            }
            model.addPoint(direction, speed, 0, String.format("%.0f deg", direction));
        }
        return new ArberChartPanel(model, new WindRoseRenderer());
    }
}
