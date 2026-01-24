package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Arrays;
import java.util.Random;

public class BoxPlotChartPanelProvider {
    public static ArberChartPanel create() {
        OutlierBoxPlotModel model = new OutlierBoxPlotModel("Release Cycle (days)");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 100);

        String[] teams = {"Core Platform", "Mobile", "Payments", "Data", "Search", "Infra"};

        for (int i = 0; i < teams.length; i++) {
            double base = 10 + i * 4 + rand.nextDouble() * 3;
            double median = base + rand.nextDouble() * 2.5;
            double iqr = 3.5 + rand.nextDouble() * 2.5;
            double min = Math.max(2, median - (iqr * 1.5) - rand.nextDouble());
            double max = median + (iqr * 1.5) + rand.nextDouble() * 2.5;
            double[] outliers = buildOutliers(rand, min, max, iqr);

            model.addBoxPlot(i, median, min, max, iqr,
                    String.format("%s (p50: %.1f d)", teams[i], median), outliers);
        }

        return ArberChartBuilder.create()
                .withTitle("Release Cycle Variability by Team")
                .addLayer(model, new BoxPlotRenderer())
                .withTooltips(true)
                .withLegend(true)
                .yAxis(axis -> axis
                        .setUnitSuffix("d")
                        .setTicks(6))
                .build().withAnimations(true);
    }

    private static double[] buildOutliers(Random rand, double min, double max, double iqr) {
        int count = 1 + rand.nextInt(3);
        double[] outliers = new double[count];
        for (int i = 0; i < count; i++) {
            boolean high = rand.nextBoolean();
            outliers[i] = high ? max + (1.5 + rand.nextDouble() * 2.5) * iqr
                    : min - (1.5 + rand.nextDouble() * 2.0) * iqr;
        }
        Arrays.sort(outliers);
        return outliers;
    }

    private static final class OutlierBoxPlotModel extends DefaultChartModel implements BoxPlotOutlierModel {
        private double[][] outliers = new double[16][];

        OutlierBoxPlotModel(String name) {
            super(name);
        }

        void addBoxPlot(double x, double median, double min, double max, double iqr, String label, double[] outlierValues) {
            addPoint(x, median, min, max, iqr, label);
            int idx = getPointCount() - 1;
            ensureCapacity(idx + 1);
            outliers[idx] = outlierValues;
        }

        @Override
        public double[] getOutliers(int index) {
            if (index < 0 || index >= getPointCount()) return EMPTY_DOUBLE;
            double[] vals = outliers[index];
            return vals != null ? vals : EMPTY_DOUBLE;
        }

        private void ensureCapacity(int size) {
            if (outliers.length >= size) return;
            double[][] next = new double[Math.max(size, outliers.length * 2)][];
            System.arraycopy(outliers, 0, next, 0, outliers.length);
            outliers = next;
        }
    }
}
