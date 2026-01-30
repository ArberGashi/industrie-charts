package com.arbergashi.charts.core.testing;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;

/**
 * Test-scope data generator for large-scale stress tests.
 */
public final class LargeScaleDataGenerator {
    private LargeScaleDataGenerator() {
    }

    public static DefaultFinancialChartModel generateFinancialCandles(String name, int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel(name);
        double price = 100.0;
        for (int i = 0; i < points; i++) {
            double t = i;
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(t, open, high, low, close);
            price = close;
        }
        return model;
    }

    public static DefaultChartModel generateLineSeries(String name, int points) {
        DefaultChartModel model = new DefaultChartModel(name);
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.05) * 50.0 + Math.cos(i * 0.03) * 20.0;
            model.setXY(x, y);
        }
        return model;
    }
}
