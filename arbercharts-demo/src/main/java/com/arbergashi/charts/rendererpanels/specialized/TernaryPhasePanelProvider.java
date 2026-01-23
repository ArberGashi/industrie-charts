package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultTernaryChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.specialized.TernaryPhasediagramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.rendererpanels.ChartHost;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class TernaryPhasePanelProvider {
    public static JPanel create() {
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 135);
        List<TernaryChartModel.TernaryPoint> data = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double sand = 0.25 + rand.nextDouble() * 0.45;
            double clay = 0.10 + rand.nextDouble() * (0.45 - sand * 0.3);
            double silt = Math.max(0.0, 1.0 - sand - clay);
            data.add(new DefaultTernaryChartModel.DefaultTernaryPoint(sand, silt, clay));
        }
        DefaultTernaryChartModel model = new DefaultTernaryChartModel(data, List.of("Sand", "Silt", "Clay"));
        TernaryPhasediagramRenderer renderer = new TernaryPhasediagramRenderer().setMultiColor(true);
        ArberChartPanel chart = ArberChartBuilder.create()
                .addLayer(model, renderer)
                .build();

        JCheckBox toggle = new JCheckBox("Multi-color");
        toggle.setSelected(true);
        toggle.setOpaque(false);
        toggle.addActionListener(e -> {
            renderer.setMultiColor(toggle.isSelected());
            chart.repaint();
        });

        return new TernaryPanel(chart, toggle);
    }

    private static final class TernaryPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private TernaryPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
