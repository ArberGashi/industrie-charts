package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultMultiDimensionalChartModel;
import com.arbergashi.charts.render.specialized.ParallelCoordinatesRenderer;
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

public class ParallelCoordinatesPanelProvider {
    public static JPanel create() {
        List<String> labels = List.of("Speed", "Weight", "Efficiency", "Price", "Reliability");
        List<double[]> allData = new ArrayList<>();
        List<double[]> premium = new ArrayList<>();
        List<double[]> balanced = new ArrayList<>();
        List<double[]> economy = new ArrayList<>();
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 122);
        
        for (int i = 0; i < 50; i++) {
            double[] point = new double[labels.size()];
            double classFactor = rand.nextDouble();
            double speed = 60 + classFactor * 35 + rand.nextGaussian() * 6;
            double weight = 70 - classFactor * 25 + rand.nextGaussian() * 8;
            double efficiency = 40 + classFactor * 40 + rand.nextGaussian() * 5;
            double price = 35 + classFactor * 45 + rand.nextGaussian() * 7;
            double reliability = 55 + classFactor * 30 + rand.nextGaussian() * 6;

            point[0] = speed;
            point[1] = weight;
            point[2] = efficiency;
            point[3] = price;
            point[4] = reliability;
            allData.add(point);
            if (classFactor > 0.66) {
                premium.add(point);
            } else if (classFactor > 0.33) {
                balanced.add(point);
            } else {
                economy.add(point);
            }
        }
        
        DefaultMultiDimensionalChartModel monoModel = new DefaultMultiDimensionalChartModel(allData, labels);
        monoModel.setName("All Segments");
        DefaultMultiDimensionalChartModel premiumModel = new DefaultMultiDimensionalChartModel(premium, labels);
        premiumModel.setName("Premium");
        DefaultMultiDimensionalChartModel balancedModel = new DefaultMultiDimensionalChartModel(balanced, labels);
        balancedModel.setName("Balanced");
        DefaultMultiDimensionalChartModel economyModel = new DefaultMultiDimensionalChartModel(economy, labels);
        economyModel.setName("Economy");

        ArberChartPanel chart = ArberChartBuilder.create()
                .withTitle("Product Portfolio – Parallel Coordinates")
                .withTooltips(true)
                .withLegend(true)
                .addLayer(economyModel, new ParallelCoordinatesRenderer())
                .addLayer(balancedModel, new ParallelCoordinatesRenderer())
                .addLayer(premiumModel, new ParallelCoordinatesRenderer())
                .build();

        JCheckBox groupedToggle = new JCheckBox("Grouped colors");
        groupedToggle.setSelected(true);
        groupedToggle.setOpaque(false);
        groupedToggle.addActionListener(e -> {
            chart.clearLayers();
            if (groupedToggle.isSelected()) {
                chart.addLayer(economyModel, new ParallelCoordinatesRenderer());
                chart.addLayer(balancedModel, new ParallelCoordinatesRenderer());
                chart.addLayer(premiumModel, new ParallelCoordinatesRenderer());
            } else {
                chart.addLayer(monoModel, new ParallelCoordinatesRenderer());
            }
            chart.resetZoom();
            chart.revalidate();
            chart.repaint();
        });

        return new ParallelCoordinatesPanel(chart, groupedToggle);
    }

    private static final class ParallelCoordinatesPanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private ParallelCoordinatesPanel(ArberChartPanel chartPanel, JCheckBox toggle) {
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
