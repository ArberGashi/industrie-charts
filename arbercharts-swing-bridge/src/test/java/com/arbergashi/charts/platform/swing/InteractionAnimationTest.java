package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.AnimationProfile;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionAnimationTest {
    @Test
    void fractionalTrackpadDeltasKeepTheirDirectionAndMagnitude() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = panel(new AtomicLong());
            try {
                panel.setAnimationsEnabled(false);
                double initial = range(panel);
                wheel(panel, -0.25);
                assertEquals(initial * Math.pow(1.1, -0.25), range(panel), 1e-10);
                wheel(panel, 0.0);
                assertEquals(initial * Math.pow(1.1, -0.25), range(panel), 1e-10);
                wheel(panel, 2.25);
                assertEquals(initial * Math.pow(1.1, 2.0), range(panel), 1e-10);
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void rapidWheelEventsAccumulateAgainstTheDestinationAndKeepThePointerAnchor() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel panel = panel(clock);
            try {
                double initial = range(panel);
                double anchor = pointerValue(panel);
                Timer originalTimer = timer(panel);
                wheel(panel, -1.0);
                wheel(panel, -1.0);
                clock.set(45_000_000L);
                wheel(panel, 0.25);
                panel.setAnimationsEnabled(false);
                assertEquals(initial * Math.pow(1.1, -1.75), range(panel), 1e-9);
                assertEquals(anchor, pointerValue(panel), 1e-9);
                assertEquals(originalTimer, timer(panel));
                assertFalse(timer(panel).isRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void retargetingAtAnotherPointerPositionAnchorsTheVisibleDataValue() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = panel(new AtomicLong());
            try {
                double initial = range(panel);
                wheel(panel, -1, 400);
                double visibleAnchor = pointerValue(panel, 250);
                wheel(panel, -1, 250);
                panel.setAnimationsEnabled(false);
                assertEquals(initial / Math.pow(1.1, 2), range(panel), 1e-10);
                assertEquals(visibleAnchor, pointerValue(panel, 250), 1e-9);
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void zoomProgressDependsOnElapsedTimeRatherThanFrameCount() throws Exception {
        onEdt(() -> {
            ArberChartPanel regular = panel(new AtomicLong());
            ArberChartPanel delayed = panel(new AtomicLong());
            try {
                double initial = range(regular);
                wheel(regular, -1);
                wheel(delayed, -1);
                for (long nanos : new long[]{16_000_000L, 32_000_000L, 48_000_000L, 90_000_000L}) {
                    regular.advanceZoomAnimation(nanos);
                }
                delayed.advanceZoomAnimation(90_000_000L);
                assertEquals(range(regular), range(delayed), 1e-12);
                assertTrue(range(regular) < initial && range(regular) > initial / 1.1);
                delayed.advanceZoomAnimation(5_000_000_000L);
                assertEquals(initial / 1.1, range(delayed), 1e-10);
                assertFalse(timer(delayed).isRunning());
            } finally {
                regular.removeNotify();
                delayed.removeNotify();
            }
        });
    }

    @Test
    void retargetStartsAtTheCurrentViewportWithoutAJump() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel panel = panel(clock);
            try {
                wheel(panel, -1);
                clock.set(80_000_000L);
                panel.advanceZoomAnimation(clock.get());
                double before = range(panel);
                wheel(panel, -1);
                assertEquals(before, range(panel), 1e-12);
                clock.addAndGet(10_000_000L);
                panel.advanceZoomAnimation(clock.get());
                assertTrue(range(panel) < before);
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void resetCannotBeOverwrittenByAnEarlierZoom() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = panel(new AtomicLong());
            try {
                double initial = range(panel);
                wheel(panel, -1);
                panel.advanceZoomAnimation(60_000_000L);
                panel.resetZoom();
                panel.advanceZoomAnimation(500_000_000L);
                assertEquals(initial, range(panel), 1e-10);
                assertFalse(timer(panel).isRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void draggingTakesOwnershipOfTheViewportAndCancelsZoom() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            ArberChartPanel panel = panel(clock);
            try {
                wheel(panel, -1);
                clock.set(60_000_000L);
                panel.advanceZoomAnimation(clock.get());
                JComponent canvas = panel.getOverlayCanvasForTesting();
                canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1,
                        InputEvent.BUTTON1_DOWN_MASK, 400, 200, 400, 200, 1, false, MouseEvent.BUTTON1));
                canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 2,
                        InputEvent.BUTTON1_DOWN_MASK, 440, 200, 440, 200, 1, false, MouseEvent.BUTTON1));
                double afterDrag = panel.getDebugContext().getMinX();
                panel.advanceZoomAnimation(500_000_000L);
                assertEquals(afterDrag, panel.getDebugContext().getMinX(), 1e-12);
                assertFalse(timer(panel).isRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void disablingMotionSettlesPendingZoomAndPropagatesTheTacticalProfile() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = panel(new AtomicLong());
            try {
                double initial = range(panel);
                wheel(panel, -2);
                panel.setAnimationsEnabled(false);
                assertFalse(panel.isAnimationsEnabled());
                assertFalse(timer(panel).isRunning());
                assertEquals(initial / Math.pow(1.1, 2), range(panel), 1e-10);
                assertEquals(AnimationProfile.TACTICAL, panel.getDebugContext().getAnimationProfile());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void removingAndHidingTheViewReleaseTheZoomTimer() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = panel(new AtomicLong());
            try {
                wheel(panel, -1);
                assertTrue(timer(panel).isRunning());
                panel.setVisible(false);
                assertFalse(timer(panel).isRunning());
                panel.setVisible(true);
                wheel(panel, -1);
                assertTrue(timer(panel).isRunning());
                panel.removeNotify();
                assertFalse(timer(panel).isRunning());
            } finally {
                if (panel.isDisplayable()) panel.removeNotify();
            }
        });
    }

    @Test
    void reducedMotionPropertyProvidesAnInstantDefault() throws Exception {
        onEdt(() -> {
            String previous = System.getProperty("arbercharts.reduceMotion");
            try {
                System.setProperty("arbercharts.reduceMotion", "true");
                ArberChartPanel panel = panel(new AtomicLong());
                try {
                    assertFalse(panel.isAnimationsEnabled());
                    wheel(panel, -1);
                    assertFalse(timer(panel).isRunning());
                } finally {
                    panel.removeNotify();
                }
            } finally {
                if (previous == null) System.clearProperty("arbercharts.reduceMotion");
                else System.setProperty("arbercharts.reduceMotion", previous);
            }
        });
    }

    private static ArberChartPanel panel(AtomicLong clock) {
        DefaultChartModel model = new DefaultChartModel("Animation");
        model.setXYArrays(new double[]{0, 10}, new double[]{0, 10});
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer(), clock::get);
        panel.setLegend(false);
        panel.setSize(800, 500);
        panel.addNotify();
        panel.doLayout();
        panel.getDebugContext();
        return panel;
    }

    private static double range(ArberChartPanel panel) {
        PlotContext context = panel.getDebugContext();
        return context.getMaxX() - context.getMinX();
    }

    private static double pointerValue(ArberChartPanel panel) {
        return pointerValue(panel, 400);
    }

    private static double pointerValue(ArberChartPanel panel, int pointerX) {
        PlotContext context = panel.getDebugContext();
        double relative = (pointerX - context.getPlotBounds().getX()) / context.getPlotBounds().getWidth();
        return context.getMinX() + range(panel) * relative;
    }

    private static void wheel(ArberChartPanel panel, double rotation) {
        wheel(panel, rotation, 400);
    }

    private static void wheel(ArberChartPanel panel, double rotation, int pointerX) {
        JComponent canvas = panel.getOverlayCanvasForTesting();
        canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, 1L, 0,
                pointerX, 200, pointerX, 200, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                (int) rotation, rotation));
    }

    private static Timer timer(ArberChartPanel panel) throws ReflectiveOperationException {
        Field field = ArberChartPanel.class.getDeclaredField("zoomAnimationTimer");
        field.setAccessible(true);
        return (Timer) field.get(panel);
    }

    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
