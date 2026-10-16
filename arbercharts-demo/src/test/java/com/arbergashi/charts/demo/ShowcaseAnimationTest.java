package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.standard.LineRenderer;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseAnimationTest {
    @Test
    void hiddenCardsPauseAndResumeWithoutAnElapsedTimeJump() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel chart = chart();
            CardLayout cards = new CardLayout();
            JPanel parent = new JPanel(cards);
            parent.add(chart, "chart");
            parent.add(new JPanel(), "other");
            List<Double> times = new ArrayList<>();
            ShowcaseAnimation animation = new ShowcaseAnimation(chart, (elapsed, delta) -> times.add(elapsed), clock::get);
            assertFalse(animation.isRunning());
            parent.addNotify();
            try {
                assertTrue(animation.isRunning());
                clock.set(16_000_000L);
                animation.tick();
                assertEquals(0.016, times.getLast(), 1e-12);
                cards.show(parent, "other");
                assertFalse(animation.isRunning());
                clock.set(10_000_000_000L);
                animation.tick();
                assertEquals(1, times.size());
                cards.show(parent, "chart");
                assertTrue(animation.isRunning());
                clock.addAndGet(16_000_000L);
                animation.tick();
                assertEquals(0.032, times.getLast(), 1e-12);
            } finally {
                parent.removeNotify();
                animation.close();
            }
        });
    }

    @Test
    void equivalentFrameIntervalsProduceEquivalentSignalTime() throws Exception {
        onEdt(() -> {
            ArberChartPanel chart = chart();
            chart.addNotify();
            AtomicLong clock = new AtomicLong();
            double[] elapsed = new double[2];
            double[] samples = new double[2];
            ShowcaseAnimation regular = new ShowcaseAnimation(chart, (time, delta) -> {
                elapsed[0] = time;
                samples[0] += delta * 250;
            }, clock::get);
            ShowcaseAnimation delayed = new ShowcaseAnimation(chart, (time, delta) -> {
                elapsed[1] = time;
                samples[1] += delta * 250;
            }, clock::get);
            try {
                for (long nanos : new long[]{8_000_000L, 16_000_000L, 32_000_000L, 40_000_000L}) {
                    clock.set(nanos);
                    regular.tick();
                }
                delayed.tick();
                assertEquals(0.04, elapsed[0], 1e-12);
                assertEquals(elapsed[0], elapsed[1], 1e-12);
                assertEquals(10, samples[0], 1e-10);
                assertEquals(samples[0], samples[1], 1e-10);
            } finally {
                regular.close();
                delayed.close();
                chart.removeNotify();
            }
        });
    }

    @Test
    void disabledMotionSuspendsTicksUntilExplicitlyReenabled() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel chart = chart();
            chart.addNotify();
            int[] updates = {0};
            ShowcaseAnimation animation = new ShowcaseAnimation(chart, (_, _) -> updates[0]++, clock::get);
            try {
                chart.setAnimationsEnabled(false);
                assertFalse(animation.isRunning());
                clock.set(100_000_000L);
                animation.tick();
                assertEquals(0, updates[0]);
                chart.setAnimationsEnabled(true);
                assertTrue(animation.isRunning());
                clock.addAndGet(16_000_000L);
                animation.tick();
                assertEquals(1, updates[0]);
            } finally {
                chart.removeNotify();
                animation.close();
            }
        });
    }

    @Test
    void detachReleasesTheTimerAndEveryInstalledListener() throws Exception {
        onEdt(() -> {
            ArberChartPanel chart = chart();
            chart.addNotify();
            int hierarchyListeners = chart.getHierarchyListeners().length;
            int motionListeners = chart.getPropertyChangeListeners("animationsEnabled").length;
            ShowcaseAnimation animation = new ShowcaseAnimation(chart, (_, _) -> {}, System::nanoTime);
            assertEquals(hierarchyListeners + 1, chart.getHierarchyListeners().length);
            assertEquals(motionListeners + 1, chart.getPropertyChangeListeners("animationsEnabled").length);
            chart.removeNotify();
            assertFalse(animation.isRunning());
            assertEquals(hierarchyListeners, chart.getHierarchyListeners().length);
            assertEquals(motionListeners, chart.getPropertyChangeListeners("animationsEnabled").length);
            animation.close();
        });
    }

    @Test
    void aLongEdtStallCannotTriggerAnUnboundedCatchupBurst() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel chart = chart();
            chart.addNotify();
            double[] delta = {0};
            ShowcaseAnimation animation = new ShowcaseAnimation(chart, (_, seconds) -> delta[0] = seconds, clock::get);
            try {
                clock.set(3_600_000_000_000L);
                animation.tick();
                assertEquals(0.25, delta[0], 1e-12);
                clock.addAndGet(16_000_000L);
                animation.tick();
                assertEquals(0.016, delta[0], 1e-12);
            } finally {
                chart.removeNotify();
                animation.close();
            }
        });
    }

    @Test
    void aFailingFrameStopsAndReleasesItsListeners() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel chart = chart();
            chart.addNotify();
            int listeners = chart.getHierarchyListeners().length;
            ShowcaseAnimation animation = new ShowcaseAnimation(chart, (_, _) -> {
                throw new IllegalStateException("Synthetic frame failure");
            }, clock::get);
            try {
                clock.set(16_000_000L);
                assertThrows(IllegalStateException.class, animation::tick);
                assertFalse(animation.isRunning());
                assertEquals(listeners, chart.getHierarchyListeners().length);
            } finally {
                chart.removeNotify();
                animation.close();
            }
        });
    }

    @Test
    void animationCannotBeCreatedOutsideTheEdt() {
        assertThrows(IllegalStateException.class, () -> new ShowcaseAnimation(chart(), (_, _) -> {}, System::nanoTime));
    }

    private static ArberChartPanel chart() {
        DefaultChartModel model = new DefaultChartModel("Signal");
        model.setXYArrays(new double[]{0, 1}, new double[]{0, 1});
        return new ArberChartPanel(model, new LineRenderer());
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }
}
