package com.arbergashi.charts.demo;

import com.arbergashi.charts.platform.swing.ArberChartPanel;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.function.Consumer;

/** Measures individual EDT paints while yielding to input between frames. */
final class DemoBenchmarkRunner {
    private static final String CLIENT_PROPERTY = DemoBenchmarkRunner.class.getName();
    private static final int WARMUP_FRAMES = 10;
    private static final int MEASURED_FRAMES = 100;

    private DemoBenchmarkRunner() {
    }

    static void run(RendererCatalogEntry entry, ArberChartPanel chartPanel, Consumer<String> statusUpdater) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Demo benchmarks must start on the EDT");
        }
        if (chartPanel.getClientProperty(CLIENT_PROPERTY) instanceof Benchmark previous) {
            previous.finish(null, true);
        }
        if (!canPaint(chartPanel)) {
            statusUpdater.accept("Benchmark cancelled: the chart is not visible.");
            return;
        }
        Benchmark benchmark = new Benchmark(entry, chartPanel, statusUpdater);
        chartPanel.putClientProperty(CLIENT_PROPERTY, benchmark);
        benchmark.start();
    }

    private static boolean canPaint(ArberChartPanel panel) {
        return panel.isShowing() && panel.getWidth() > 0 && panel.getHeight() > 0;
    }

    private static final class Benchmark {
        private final RendererCatalogEntry entry;
        private final ArberChartPanel panel;
        private final Consumer<String> statusUpdater;
        private final boolean animationsEnabled;
        private final Timer timer;
        private final HierarchyListener hierarchyListener;
        private final PropertyChangeListener motionListener;
        private int frames;
        private long totalNs;
        private long minNs = Long.MAX_VALUE;
        private long maxNs;
        private boolean finished;

        private Benchmark(RendererCatalogEntry entry, ArberChartPanel panel, Consumer<String> statusUpdater) {
            this.entry = entry;
            this.panel = panel;
            this.statusUpdater = statusUpdater;
            animationsEnabled = panel.isAnimationsEnabled();
            timer = new Timer(16, _ -> paintFrame());
            timer.setCoalesce(true);
            hierarchyListener = event -> {
                if ((event.getChangeFlags() & (HierarchyEvent.SHOWING_CHANGED
                        | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0 && !canPaint(panel)) {
                    finish("Benchmark cancelled: the chart is no longer visible.", true);
                }
            };
            motionListener = _ -> finish("Benchmark cancelled: the animation setting changed.", false);
        }

        private void start() {
            // Keep the measured scene stable and resume demo motion without a catch-up jump.
            panel.setAnimationsEnabled(false);
            panel.addHierarchyListener(hierarchyListener);
            panel.addPropertyChangeListener("animationsEnabled", motionListener);
            timer.start();
        }

        private void paintFrame() {
            if (finished) return;
            if (!canPaint(panel)) {
                finish("Benchmark cancelled: the chart is no longer visible.", true);
                return;
            }
            try {
                long start = System.nanoTime();
                panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight());
                long elapsed = System.nanoTime() - start;
                if (++frames <= WARMUP_FRAMES) return;
                totalNs += elapsed;
                minNs = Math.min(minNs, elapsed);
                maxNs = Math.max(maxNs, elapsed);
                if (frames == WARMUP_FRAMES + MEASURED_FRAMES) {
                    finish(String.format(Locale.ROOT,
                            "Paint benchmark: %s | %d iterations | Avg: %.2fms | Min: %.2fms | Max: %.2fms",
                            entry.simpleName(), MEASURED_FRAMES, totalNs / (MEASURED_FRAMES * 1_000_000.0),
                            minNs / 1_000_000.0, maxNs / 1_000_000.0), true);
                }
            } catch (RuntimeException exception) {
                finish("Benchmark failed: " + exception.getMessage(), true);
            }
        }

        private void finish(String message, boolean restoreAnimations) {
            if (finished) return;
            finished = true;
            timer.stop();
            panel.removeHierarchyListener(hierarchyListener);
            panel.removePropertyChangeListener("animationsEnabled", motionListener);
            if (panel.getClientProperty(CLIENT_PROPERTY) == this) panel.putClientProperty(CLIENT_PROPERTY, null);
            if (restoreAnimations) panel.setAnimationsEnabled(animationsEnabled);
            if (message != null) statusUpdater.accept(message);
        }
    }
}
