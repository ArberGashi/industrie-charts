package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.JoyplotRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class JoyplotPanelProvider {
    public static JPanel create() {
        DefaultChartModel model = new DefaultChartModel("Terrain Profile");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 113);
        for (int i = 0; i < 200; i++) {
            double peakA = 18 * Math.exp(-Math.pow((i - 45) / 18.0, 2));
            double peakB = 24 * Math.exp(-Math.pow((i - 130) / 22.0, 2));
            double ridge = 10 + peakA + peakB + rand.nextGaussian() * 1.5;
            model.addPoint(i, ridge, 0, String.format("x=%d", i));
        }
        JoyplotRenderer renderer = new JoyplotRenderer().setMultiColor(true);
        ArberChartPanel chart = new ArberChartPanel(model, renderer);

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new JoyplotPanel(chart, toggle);
    }

    private static final class JoyplotPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private JoyplotPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
            super(new BorderLayout());
            this.chartPanel = chartPanel;

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            controls.setOpaque(false);
            controls.add(toggle);

            add(controls, BorderLayout.NORTH);
            add(chartPanel, BorderLayout.CENTER);
        }

        @Override
        public ArberChartPanel getChartPanel() {
            return chartPanel;
        }
    }
}
