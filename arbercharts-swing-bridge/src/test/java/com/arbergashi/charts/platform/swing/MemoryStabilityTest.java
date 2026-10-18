package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryStabilityTest {
    @Test
    public void memoryStaysStableUnderPanZoom() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("arbercharts.stress"));
        long seconds = Long.getLong("arbercharts.stress.seconds",
                Integer.getInteger("arbercharts.stress.minutes", 10) * 60L);
        long warmupSeconds = Long.getLong("arbercharts.stress.warmupSeconds",
                Integer.getInteger("arbercharts.stress.warmupMinutes", 2) * 60L);
        long toleranceMb = Long.getLong("arbercharts.stress.toleranceMb", 4L);
        long logIntervalMillis = Long.getLong("arbercharts.stress.logIntervalMs", 60_000L);
        assertTrue(seconds > 0 && warmupSeconds >= 0, "The stress duration must be positive");
        assertTrue(toleranceMb >= 0 && logIntervalMillis > 0, "Invalid stress measurement settings");
        long durationNanos = Math.multiplyExact(seconds, 1_000_000_000L);
        long warmupNanos = Math.multiplyExact(warmupSeconds, 1_000_000_000L);
        long toleranceBytes = Math.multiplyExact(toleranceMb, 1024L * 1024L);

        View view = onEdt(() -> {
            DefaultChartModel model = new DefaultChartModel("Memory stress signal");
            model.setXYArrays(new double[]{0, 1, 2, 3, 4, 5}, new double[]{1, 2, 3, 2, 1, 0});
            ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer()).setAnimationsEnabled(false);
            panel.setSize(800, 600);
            panel.doLayout();
            View result = new View(panel, panel.getOverlayCanvasForTesting(),
                    new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB));
            paint(result);
            return result;
        });
        try {
            runStressLoop(view, warmupNanos, logIntervalMillis);
            long baseline = retainedHeap();
            System.out.println("MEMORY_STABILITY baselineBytes=" + baseline + " warmupSeconds=" + warmupSeconds);
            long frames = runStressLoop(view, durationNanos, logIntervalMillis);
            assertTrue(frames >= 4, "The run must complete a real pan/zoom cycle");
            long after = retainedHeap();
            System.out.println("MEMORY_STABILITY afterBytes=" + after + " frames=" + frames + " seconds=" + seconds);
            assertTrue(after - baseline <= toleranceBytes,
                    "Retained heap grew by " + (after - baseline) + " bytes after pan/zoom stress");
        } finally {
            onEdt(() -> {
                view.panel.removeNotify();
                return null;
            });
            view.surface.flush();
        }
    }

    private static long runStressLoop(View view, long durationNanos, long logIntervalMillis) throws Exception {
        long start = System.nanoTime();
        long nextLog = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(logIntervalMillis);
        long step = 0;
        double[] cycleStart = onEdt(() -> viewport(view.panel));
        while (System.nanoTime() - start < durationNanos) {
            // Finish each cycle so the next phase starts from the same visible data.
            for (int phase = 0; phase < 4; phase++) {
                int action = phase;
                onEdt(() -> {
                    double[] before = viewport(view.panel);
                    int x = 400;
                    int y = 250;
                    if (action == 0 || action == 3) {
                        view.canvas.dispatchEvent(new MouseWheelEvent(view.canvas, MouseEvent.MOUSE_WHEEL,
                                System.currentTimeMillis(), 0, x, y, 0, false,
                                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, action == 0 ? -1 : 1));
                    } else {
                        int delta = action == 1 ? 20 : -20;
                        view.canvas.dispatchEvent(new MouseEvent(view.canvas, MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                        view.canvas.dispatchEvent(new MouseEvent(view.canvas, MouseEvent.MOUSE_DRAGGED,
                                System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, x + delta, y, 0, false, MouseEvent.NOBUTTON));
                        view.canvas.dispatchEvent(new MouseEvent(view.canvas, MouseEvent.MOUSE_RELEASED,
                                System.currentTimeMillis(), 0, x + delta, y, 1, false, MouseEvent.BUTTON1));
                    }
                    paint(view);
                    double[] after = viewport(view.panel);
                    assertFalse(Arrays.equals(before, after), "Every dispatched gesture must change the actual view");
                    for (double value : after) assertTrue(Double.isFinite(value), "The stressed viewport must stay finite");
                    if (action == 3) {
                        assertArrayEquals(cycleStart, after, 1e-7,
                                "The inverse gesture cycle must keep the data visible instead of drifting away");
                    }
                    return null;
                });
                step++;
            }
            if (System.nanoTime() >= nextLog) {
                System.out.println("MEMORY_STABILITY usedBytes=" + usedHeap() + " frames=" + step);
                nextLog = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(logIntervalMillis);
            }
        }
        return step;
    }

    private static void paint(View view) {
        assertTrue(SwingUtilities.isEventDispatchThread(), "Swing painting and input must run on the EDT");
        Graphics2D graphics = view.surface.createGraphics();
        try {
            view.panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private static double[] viewport(ArberChartPanel panel) {
        var context = panel.getDebugContext();
        return new double[]{context.getMinX(), context.getMaxX(), context.getMinY(), context.getMaxY()};
    }

    private static long retainedHeap() throws Exception {
        onEdt(() -> null);
        System.gc();
        Thread.sleep(150);
        return usedHeap();
    }

    private static long usedHeap() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private record View(ArberChartPanel panel, Component canvas, BufferedImage surface) {
    }
}
