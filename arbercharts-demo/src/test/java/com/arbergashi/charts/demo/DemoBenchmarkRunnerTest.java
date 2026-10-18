package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoBenchmarkRunnerTest {
    private final RendererCatalogEntry entry = RendererCatalogEntry.of("standard", LineRenderer.class.getName());
    private final List<String> results = new ArrayList<>();
    private final CountDownLatch completed = new CountDownLatch(1);
    private CountingPanel panel;
    private JPanel parent;
    private CardLayout cards;
    private int hierarchyListeners;
    private int motionListeners;

    @BeforeEach
    void createVisibleChart() throws Exception {
        onEdt(() -> {
            panel = new CountingPanel();
            panel.setAnimationsEnabled(true);
            panel.setSize(640, 360);
            cards = new CardLayout();
            parent = new JPanel(cards);
            parent.add(panel, "chart");
            parent.add(new JPanel(), "other");
            parent.addNotify();
            hierarchyListeners = panel.getHierarchyListeners().length;
            motionListeners = panel.getPropertyChangeListeners("animationsEnabled").length;
        });
    }

    @AfterEach
    void disposeChart() throws Exception {
        onEdt(() -> parent.removeNotify());
    }

    @Test
    void measuresPaintsOnTheEdtAndLetsQueuedInputRunBetweenFrames() throws Exception {
        onEdt(this::start);
        awaitCompletion();
        onEdt(() -> {
            assertEquals(110, panel.paints.get());
            assertFalse(panel.paintedOutsideEdt.get(), "Swing paints must stay on the EDT");
            assertFalse(panel.paintedWithMotion.get(), "Model updates must pause while the scene is measured");
            assertFalse(panel.skippedQueuedInput.get(), "The benchmark must yield between individual paints");
            assertEquals(110, panel.queuedInput.get());
            assertEquals(1, results.size());
            assertTrue(results.getFirst().contains("100 iterations"));
            assertTrue(panel.isAnimationsEnabled());
            assertReleased();
        });
    }

    @Test
    void selectingAnotherCardCancelsTheRunAndRestoresMotion() throws Exception {
        onEdt(() -> {
            start();
            assertFalse(panel.isAnimationsEnabled());
            cards.show(parent, "other");
            assertTrue(results.getFirst().startsWith("Benchmark cancelled:"));
            assertTrue(panel.isAnimationsEnabled());
            assertReleased();
        });
        awaitCompletion();
    }

    @Test
    void removingTheChartReleasesTheRunWithoutChangingReducedMotion() throws Exception {
        onEdt(() -> {
            panel.setAnimationsEnabled(false);
            start();
            parent.remove(panel);
            assertTrue(results.getFirst().startsWith("Benchmark cancelled:"));
            assertFalse(panel.isAnimationsEnabled());
            assertReleased();
        });
        awaitCompletion();
    }

    @Test
    void restartingReplacesThePreviousRunWithoutDuplicatePaintsOrResults() throws Exception {
        onEdt(() -> {
            start();
            start();
        });
        awaitCompletion();
        onEdt(() -> {
            assertEquals(110, panel.paints.get());
            assertEquals(1, results.size());
            assertTrue(panel.isAnimationsEnabled());
            assertReleased();
        });
    }

    @Test
    void aFailedPaintRestoresMotionAndReleasesItsListeners() throws Exception {
        onEdt(() -> {
            panel.failure = new IllegalStateException("Synthetic paint failure");
            start();
        });
        awaitCompletion();
        onEdt(() -> {
            assertEquals(1, panel.paints.get());
            assertEquals(List.of("Benchmark failed: Synthetic paint failure"), results);
            assertTrue(panel.isAnimationsEnabled());
            assertReleased();
        });
    }

    @Test
    void changingMotionDuringTheRunPreservesTheNewSetting() throws Exception {
        onEdt(() -> {
            panel.setAnimationsEnabled(false);
            start();
            panel.setAnimationsEnabled(true);
            assertTrue(results.getFirst().contains("animation setting changed"));
            assertTrue(panel.isAnimationsEnabled(), "Cancellation must respect the explicit setting change");
            assertReleased();
        });
        awaitCompletion();
    }

    @Test
    void hiddenChartsCannotProduceMisleadingSuccessfulMeasurements() throws Exception {
        onEdt(() -> {
            cards.show(parent, "other");
            start();
            assertEquals(0, panel.paints.get());
            assertTrue(results.getFirst().contains("not visible"));
            assertTrue(panel.isAnimationsEnabled());
            assertReleased();
        });
        awaitCompletion();
    }

    @Test
    void cannotStartOutsideTheEdt() {
        assertThrows(IllegalStateException.class, this::start);
    }

    private void start() {
        DemoBenchmarkRunner.run(entry, panel, message -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            results.add(message);
            completed.countDown();
        });
    }

    private void assertReleased() {
        assertNull(panel.getClientProperty(DemoBenchmarkRunner.class.getName()));
        assertEquals(hierarchyListeners, panel.getHierarchyListeners().length);
        assertEquals(motionListeners, panel.getPropertyChangeListeners("animationsEnabled").length);
    }

    private void awaitCompletion() throws InterruptedException {
        assertTrue(completed.await(10, TimeUnit.SECONDS), "The benchmark must complete or cancel promptly");
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static final class CountingPanel extends ArberChartPanel {
        private final AtomicInteger paints = new AtomicInteger();
        private final AtomicInteger queuedInput = new AtomicInteger();
        private final AtomicBoolean paintedOutsideEdt = new AtomicBoolean();
        private final AtomicBoolean paintedWithMotion = new AtomicBoolean();
        private final AtomicBoolean skippedQueuedInput = new AtomicBoolean();
        private RuntimeException failure;

        private CountingPanel() {
            super(new DefaultChartModel("Benchmark fixture"), new LineRenderer());
        }

        @Override
        public void paintImmediately(int x, int y, int width, int height) {
            int frame = paints.incrementAndGet();
            if (!SwingUtilities.isEventDispatchThread()) paintedOutsideEdt.set(true);
            if (isAnimationsEnabled()) paintedWithMotion.set(true);
            if (queuedInput.get() != frame - 1) skippedQueuedInput.set(true);
            SwingUtilities.invokeLater(queuedInput::incrementAndGet);
            if (failure != null) throw failure;
        }
    }
}
