package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;
import java.util.Arrays;

public class BoxPlotChartPanelProvider {
    public static ArberChartPanel create() {
        // Response Time Distribution Across Microservices
        // Professional statistical analysis for DevOps/SRE teams
        OutlierBoxPlotModel model = new OutlierBoxPlotModel("Response Time (ms)");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 100);
        
        String[] services = {"Auth Service", "User API", "Order API", "Payment Gateway", "Search", "Notifications"};

        for (int i = 0; i < services.length; i++) {
            // Generate realistic response time distributions
            double baseLatency = 18 + i * 12 + rand.nextDouble() * 8;
            double median = baseLatency + rand.nextDouble() * 6;
            double iqr = 7 + rand.nextDouble() * 10;
            double q1 = median - iqr / 2;
            double q3 = median + iqr / 2;
            double min = Math.max(5, q1 - 1.5 * iqr + rand.nextDouble() * 5);
            double max = q3 + 1.5 * iqr + rand.nextDouble() * 18;
            double[] outliers = buildOutliers(rand, min, max, iqr);

            // addPoint(x, median, min, max, iqr, label)
            model.addBoxPlot(i, median, min, max, iqr,
                String.format("%s (p50: %.1fms)", services[i], median), outliers);
        }
        
        return ArberChartBuilder.create()
                .withTitle("Microservice Response Time Distribution")
                .addLayer(model, new BoxPlotRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }

    private static double[] buildOutliers(Random rand, double min, double max, double iqr) {
        int count = 1 + rand.nextInt(3);
        double[] outliers = new double[count];
        for (int i = 0; i < count; i++) {
            boolean high = rand.nextBoolean();
            outliers[i] = high ? max + (2.0 + rand.nextDouble() * 4.0) * iqr
                    : min - (2.0 + rand.nextDouble() * 3.0) * iqr;
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
