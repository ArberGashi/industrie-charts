package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.ParetoRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class ParetoPanelProvider {
    public static JPanel create() {
        DefaultChartModel model = new DefaultChartModel("Quality Defects");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 121);
        String[] causes = {"Software Bug", "Config Error", "Network Issue", "Vendor Outage", "Hardware Failure", "Other"};
        double[] frequencies = {132, 88, 60, 42, 28, 12}; // already sorted for clear pareto
        for (int i = 0; i < causes.length; i++) {
            model.addPoint(i, frequencies[i], 0, causes[i]);
        }
        ParetoRenderer renderer = new ParetoRenderer().setMultiColor(true);
        ArberChartPanel chart = new ArberChartPanel(model, renderer);

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new ParetoPanel(chart, toggle);
    }

    private static final class ParetoPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private ParetoPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
