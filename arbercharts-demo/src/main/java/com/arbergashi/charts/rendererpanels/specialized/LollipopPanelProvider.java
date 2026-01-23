package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.LollipopRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class LollipopPanelProvider {
    public static JPanel create() {
        DefaultChartModel model = new DefaultChartModel("Feature Importance");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 114);
        String[] features = {"Latency", "Reliability", "Security", "UX", "Cost", "Scalability", "Observability"};
        for (int i = 0; i < features.length; i++) {
            double score = 35 + rand.nextDouble() * 55;
            model.addPoint(i, score, 0, String.format("%s (%.1f)", features[i], score));
        }
        LollipopRenderer renderer = new LollipopRenderer().setMultiColor(true);
        ArberChartPanel chart = new ArberChartPanel(model, renderer);

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new LollipopPanel(chart, toggle);
    }

    private static final class LollipopPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private LollipopPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
