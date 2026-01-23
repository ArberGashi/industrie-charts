package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.DelaunayRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class DelaunayPanelProvider {
    public static JPanel create() {
        DefaultChartModel model = new DefaultChartModel("Triangulation Mesh");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 118);
        for (int i = 0; i < 25; i++) {
            model.addPoint(20 + rand.nextGaussian() * 10, 70 + rand.nextGaussian() * 8, 0, "Cluster A");
        }
        for (int i = 0; i < 20; i++) {
            model.addPoint(70 + rand.nextGaussian() * 12, 30 + rand.nextGaussian() * 10, 0, "Cluster B");
        }
        for (int i = 0; i < 10; i++) {
            model.addPoint(50 + rand.nextGaussian() * 18, 50 + rand.nextGaussian() * 18, 0, "Bridge");
        }
        DelaunayRenderer renderer = new DelaunayRenderer().setMultiColor(true);
        ArberChartPanel chart = new ArberChartPanel(model, renderer);

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new DelaunayPanel(chart, toggle);
    }

    private static final class DelaunayPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private DelaunayPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
