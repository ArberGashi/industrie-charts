package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.DendrogramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class DendrogramPanelProvider {
    public static JPanel create() {
        DefaultChartModel model = new DefaultChartModel("Phylogenetic Tree");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 120);
        String[] species = {
            "Canis lupus", "Canis latrans", "Vulpes vulpes", "Urocyon cinereoargenteus",
            "Panthera leo", "Panthera tigris", "Panthera pardus", "Acinonyx jubatus",
            "Ursus arctos", "Ursus maritimus", "Mustela putorius", "Lutra lutra"
        };
        for (int i = 0; i < species.length; i++) {
            double height = 60 + rand.nextDouble() * 35;
            model.addPoint(i * 8, height, 0, species[i]);
        }
        DendrogramRenderer renderer = new DendrogramRenderer().setMultiColor(true);
        ArberChartPanel chart = new ArberChartPanel(model, renderer);

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new DendrogramPanel(chart, toggle);
    }

    private static final class DendrogramPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private DendrogramPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
