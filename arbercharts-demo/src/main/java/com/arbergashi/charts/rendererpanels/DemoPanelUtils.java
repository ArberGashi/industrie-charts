package com.arbergashi.charts.rendererpanels;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.util.ChartAssets;
import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

/**
 * Common utilities for demo panel providers.
 */
public class DemoPanelUtils {
    
    public static final long DEMO_SEED = 123456789L;
    private static boolean presentationConfigured = false;

    public record PresentationMeta(String title, String subtitle, String[] tags, String[] metrics) {}

    /**
     * Applies global demo presentation defaults (legend, crosshair, typography).
     */
    public static void configurePresentationDefaults() {
        if (presentationConfigured) return;
        presentationConfigured = true;

        ChartAssets.setProperty("Chart.legend.position", "TOP_RIGHT");
        ChartAssets.setProperty("Chart.legend.fontScale", "0.95");
        ChartAssets.setProperty("Chart.legend.padding", "8");
        ChartAssets.setProperty("Chart.legend.iconSize", "9");
        ChartAssets.setProperty("Chart.legend.soloEnabled", "true");
        ChartAssets.setProperty("Chart.crosshair.snap", "true");
    }

    public static String[] buildStandardMetrics(ArberChartPanel panel) {
        return buildMetrics(panel);
    }

    public static String[] buildMetrics(ArberChartPanel panel, String... extras) {
        int layers = panel != null ? panel.getLayerCount() : 0;
        String legend = panel != null && panel.isLegendVisible() ? "Legend: On" : "Legend: Off";
        ArrayList<String> items = new ArrayList<>();
        items.add("Series: " + layers);
        items.add("Crosshair: On");
        items.add(legend);
        if (extras != null) {
            for (String extra : extras) {
                if (extra != null && !extra.isBlank()) {
                    items.add(extra);
                }
            }
        }
        return items.toArray(new String[0]);
    }

    /**
     * Attaches a Timer to a panel with automatic lifecycle management.
     */
    public static void attachManagedTimer(ArberChartPanel panel, Timer timer) {
        timer.addActionListener(_ -> {
            if (panel.isShowing()) {
                panel.repaint();
            }
        });
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                timer.stop();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                if (!timer.isRunning()) {
                    timer.restart();
                }
            }
        });
        timer.start();
    }

    /**
     * Simple holder for generated price + volume models used across financial demos.
     */
    public static final class PriceBundle {
        public final DefaultChartModel priceModel;
        public final DefaultChartModel volumeModel;

        public PriceBundle(DefaultChartModel priceModel, DefaultChartModel volumeModel) {
            this.priceModel = priceModel;
            this.volumeModel = volumeModel;
        }
    }

    /**
     * Generates OHLC price series with a simple volatility clustering model and a volume series.
     */
    public static PriceBundle generatePriceSeries(int points, long seed) {
        DefaultChartModel price = new DefaultChartModel("Demo Stock");
        DefaultChartModel volume = new DefaultChartModel("Volume");
        Random r = new Random(seed);

        LocalDate date = LocalDate.of(2025, 1, 2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        double lastClose = 112.0;
        double vol = 0.010;
        double drift = 0.0004;
        for (int i = 0; i < points; i++) {
            if (i == points / 3) {
                drift = -0.0002;
                vol = 0.018;
            } else if (i == (points * 2) / 3) {
                drift = 0.0006;
                vol = 0.014;
            }

            double shock = r.nextGaussian() * 0.7;
            vol = Math.max(0.006, Math.min(0.035, vol * 0.985 + Math.abs(shock) * 0.0025));

            double ret = drift + vol * r.nextGaussian();
            double close = Math.max(10.0, lastClose * Math.exp(ret));
            double open = lastClose + (r.nextGaussian() * vol * lastClose * 0.6);
            double range = Math.abs(ret) * lastClose * 2.2 + vol * lastClose * (0.7 + r.nextDouble());
            double high = Math.max(open, close) + range * (0.35 + r.nextDouble() * 0.65);
            double low = Math.min(open, close) - range * (0.35 + r.nextDouble() * 0.65);
            if (low < 1.0) low = 1.0;

            double volAmt = 850_000 + Math.abs(ret) * 12_000_000 + r.nextInt(140_000);
            double direction = (close >= open) ? 1.0 : -1.0;
            String label = String.format("%s O%.2f H%.2f L%.2f C%.2f", fmt.format(date), open, high, low, close);

            price.addPoint(i, close, low, high, open, label);
            volume.addPoint(i, volAmt, direction, label);

            lastClose = close;
            date = date.plusDays(1);
        }

        return new PriceBundle(price, volume);
    }

    /**
     * Generates OHLC price series with minimal labels (clean demo backgrounds).
     */
    public static PriceBundle generatePriceSeriesMinimal(int points, long seed) {
        DefaultChartModel price = new DefaultChartModel("Demo Stock");
        DefaultChartModel volume = new DefaultChartModel("Volume");
        Random r = new Random(seed);

        double lastClose = 112.0;
        double vol = 0.010;
        double drift = 0.0004;
        for (int i = 0; i < points; i++) {
            if (i == points / 3) {
                drift = -0.0002;
                vol = 0.018;
            } else if (i == (points * 2) / 3) {
                drift = 0.0006;
                vol = 0.014;
            }

            double shock = r.nextGaussian() * 0.7;
            vol = Math.max(0.006, Math.min(0.035, vol * 0.985 + Math.abs(shock) * 0.0025));

            double ret = drift + vol * r.nextGaussian();
            double close = Math.max(10.0, lastClose * Math.exp(ret));
            double open = lastClose + (r.nextGaussian() * vol * lastClose * 0.6);
            double range = Math.abs(ret) * lastClose * 2.2 + vol * lastClose * (0.7 + r.nextDouble());
            double high = Math.max(open, close) + range * (0.35 + r.nextDouble() * 0.65);
            double low = Math.min(open, close) - range * (0.35 + r.nextDouble() * 0.65);
            if (low < 1.0) low = 1.0;

            double volAmt = 850_000 + Math.abs(ret) * 12_000_000 + r.nextInt(140_000);
            double direction = (close >= open) ? 1.0 : -1.0;

            price.addPoint(i, close, low, high, open, "");
            volume.addPoint(i, volAmt, direction, "");

            lastClose = close;
        }

        return new PriceBundle(price, volume);
    }

    /**
     * Generates a close-only series with trend regimes for indicator-style charts.
     */
    public static DefaultChartModel generateRegimeCloseSeries(String name, int points, long seed, double startPrice) {
        DefaultChartModel model = new DefaultChartModel(name);
        Random r = new Random(seed);
        LocalDate date = LocalDate.of(2025, 3, 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        double price = startPrice;
        double vol = 0.012;
        double drift = 0.0005;
        for (int i = 0; i < points; i++) {
            if (i == points / 4) {
                drift = 0.0001;
                vol = 0.018;
            } else if (i == points / 2) {
                drift = -0.0004;
                vol = 0.022;
            } else if (i == (points * 3) / 4) {
                drift = 0.0007;
                vol = 0.015;
            }

            double ret = drift + vol * r.nextGaussian();
            price = Math.max(8.0, price * Math.exp(ret));

            String label = String.format("%s Close %.2f", fmt.format(date), price);
            model.addPoint(i, price, 0, label);
            date = date.plusDays(1);
        }

        return model;
    }
}
