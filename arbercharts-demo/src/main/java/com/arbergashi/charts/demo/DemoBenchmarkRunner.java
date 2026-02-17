package com.arbergashi.charts.demo;

import com.arbergashi.charts.platform.swing.ArberChartPanel;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

final class DemoBenchmarkRunner {

    private DemoBenchmarkRunner() {
    }

    static void run(RendererCatalogEntry entry, ArberChartPanel chartPanel, Consumer<String> statusUpdater) {
        new Thread(() -> {
            try {
                int iterations = 100;
                long totalNs = 0;
                long minNs = Long.MAX_VALUE;
                long maxNs = 0;

                for (int i = 0; i < 10; i++) {
                    chartPanel.repaint();
                    chartPanel.paintImmediately(0, 0, chartPanel.getWidth(), chartPanel.getHeight());
                }

                for (int i = 0; i < iterations; i++) {
                    long start = System.nanoTime();
                    chartPanel.paintImmediately(0, 0, chartPanel.getWidth(), chartPanel.getHeight());
                    long elapsed = System.nanoTime() - start;
                    totalNs += elapsed;
                    minNs = Math.min(minNs, elapsed);
                    maxNs = Math.max(maxNs, elapsed);
                }

                double avgMs = (totalNs / iterations) / 1_000_000.0;
                double minMs = minNs / 1_000_000.0;
                double maxMs = maxNs / 1_000_000.0;

                String result = String.format(
                        "Benchmark: %s | %d iterations | Avg: %.2fms | Min: %.2fms | Max: %.2fms",
                        entry.simpleName(), iterations, avgMs, minMs, maxMs
                );

                SwingUtilities.invokeLater(() -> statusUpdater.accept(result));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> statusUpdater.accept("Benchmark failed: " + ex.getMessage()));
            }
        }, "demo-benchmark").start();
    }
}
