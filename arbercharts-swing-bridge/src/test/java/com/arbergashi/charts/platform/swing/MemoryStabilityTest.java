package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.model.ChartModel;
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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryStabilityTest {
    @Test
    public void memoryStaysStableUnderPanZoom() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("arbercharts.stress"));

        int minutes = Integer.getInteger("arbercharts.stress.minutes", 10);
        int warmupMinutes = Integer.getInteger("arbercharts.stress.warmupMinutes", 2);
        long durationNanos = minutes * 60L * 1_000_000_000L;
        long warmupNanos = warmupMinutes * 60L * 1_000_000_000L;
        long logIntervalMillis = Long.getLong("arbercharts.stress.logIntervalMs", 60_000L);
        long nextLog = System.currentTimeMillis() + logIntervalMillis;

        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2, 3, 4, 5};
            final double[] ys = {1, 2, 3, 2, 1, 0};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        com.arbergashi.charts.platform.swing.ArberChartPanel panel =
                new com.arbergashi.charts.platform.swing.ArberChartPanel(model, new LineRenderer()).setAnimationsEnabled(false);
        panel.setSize(800, 600);
        panel.doLayout();

        BufferedImage surface = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = surface.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        Component canvas = panel.getOverlayCanvasForTesting();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        runStressLoop(panel, canvas, surface, warmupNanos);
        System.gc();
        Thread.sleep(150);
        long baseline = getUsedHeap(memoryMXBean);
        System.out.println("MEMORY_STABILITY baseline=" + bytesToMb(baseline) + "MB warmupMinutes=" + warmupMinutes);
        long start = System.nanoTime();
        int step = 0;

        while (System.nanoTime() - start < durationNanos) {
            int x = 200 + (step % 100);
            int y = 150 + (step % 50);

            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK, x + 20, y, 0, false, MouseEvent.BUTTON1));

            int wheel = (step % 2 == 0) ? -1 : 1;
            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    0, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, wheel));

            Graphics2D frame = surface.createGraphics();
            try {
                panel.paint(frame);
            } finally {
                frame.dispose();
            }

            step++;

            long now = System.currentTimeMillis();
            if (now >= nextLog) {
                long current = getUsedHeap(memoryMXBean);
                System.out.println("MEMORY_STABILITY tick=" + bytesToMb(current) + "MB step=" + step);
                nextLog = now + logIntervalMillis;
            }
        }

        System.gc();
        Thread.sleep(150);
        long after = getUsedHeap(memoryMXBean);
        System.out.println("MEMORY_STABILITY after=" + bytesToMb(after) + "MB");

        long toleranceMb = Long.getLong("arbercharts.stress.toleranceMb", 4L);
        long deltaMb = bytesToMb(after) - bytesToMb(baseline);
        assertTrue(deltaMb <= toleranceMb, "Heap grew after pan/zoom stress run");
    }

    private long getUsedHeap(MemoryMXBean memoryMXBean) {
        MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
        return usage.getUsed();
    }

    private long bytesToMb(long bytes) {
        return bytes / (1024L * 1024L);
    }

    private void runStressLoop(com.arbergashi.charts.platform.swing.ArberChartPanel panel, Component canvas, BufferedImage surface, long durationNanos) {
        long start = System.nanoTime();
        int step = 0;

        while (System.nanoTime() - start < durationNanos) {
            int x = 200 + (step % 100);
            int y = 150 + (step % 50);

            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK, x + 20, y, 0, false, MouseEvent.BUTTON1));

            int wheel = (step % 2 == 0) ? -1 : 1;
            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    0, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, wheel));

            Graphics2D frame = surface.createGraphics();
            try {
                panel.paint(frame);
            } finally {
                frame.dispose();
            }

            step++;
        }
    }
}
