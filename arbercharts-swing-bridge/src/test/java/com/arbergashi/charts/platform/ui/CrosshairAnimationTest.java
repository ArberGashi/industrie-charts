package com.arbergashi.charts.platform.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairAnimationTest {
    @Test
    void measurementMovesImmediatelyWithItsLabels() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            HighPrecisionCrosshair crosshair = crosshair(clock);
            try {
                crosshair.updatePosition(40, 40, "40", "40");
                clock.set(120_000_000L);
                crosshair.advanceAnimation(clock.get());
                crosshair.updatePosition(200, 140, "200", "140");
                BufferedImage image = paint(crosshair);
                assertTrue(nonTransparent(image, 194, 134, 13, 13) > 20,
                        "The marker must already surround the newly reported coordinate");
                assertEquals(0, nonTransparent(image, 34, 34, 13, 13),
                        "No stale marker or crosshair may trail behind the new labels");
            } finally {
                crosshair.removeNotify();
            }
        });
    }

    @Test
    void fadeHasTheSamePixelsAtTheSameTimeRegardlessOfTimerCadence() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            HighPrecisionCrosshair regular = crosshair(clock);
            HighPrecisionCrosshair delayed = crosshair(clock);
            try {
                regular.updatePosition(180, 100, "180", "100");
                delayed.updatePosition(180, 100, "180", "100");
                for (long nanos : new long[]{16_000_000L, 32_000_000L, 48_000_000L, 60_000_000L}) {
                    regular.advanceAnimation(nanos);
                }
                delayed.advanceAnimation(60_000_000L);
                BufferedImage first = paint(regular);
                BufferedImage second = paint(delayed);
                assertArrayEquals(first.getRGB(0, 0, 320, 240, null, 0, 320),
                        second.getRGB(0, 0, 320, 240, null, 0, 320));
                assertTrue(nonTransparent(first, 0, 0, 320, 240) > 100);
            } finally {
                regular.removeNotify();
                delayed.removeNotify();
            }
        });
    }

    @Test
    void aDelayedHideFinishesWithoutACompetingTimer() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            HighPrecisionCrosshair crosshair = crosshair(clock);
            try {
                crosshair.updatePosition(100, 100, "X", "Y");
                clock.set(120_000_000L);
                crosshair.advanceAnimation(clock.get());
                crosshair.hideCrosshair();
                crosshair.advanceAnimation(5_000_000_000L);
                assertFalse(crosshair.isVisible());
                assertFalse(timer(crosshair).isRunning());
            } finally {
                crosshair.removeNotify();
            }
        });
    }

    @Test
    void reenteringDuringFadeOutPreservesTheCurrentOpacity() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            HighPrecisionCrosshair crosshair = crosshair(clock);
            try {
                crosshair.updatePosition(100, 100, "X", "Y");
                clock.set(120_000_000L);
                crosshair.advanceAnimation(clock.get());
                crosshair.hideCrosshair();
                clock.set(160_000_000L);
                crosshair.advanceAnimation(clock.get());
                BufferedImage before = paint(crosshair);
                crosshair.updatePosition(100, 100, "X", "Y");
                BufferedImage after = paint(crosshair);
                assertArrayEquals(before.getRGB(0, 0, 320, 240, null, 0, 320),
                        after.getRGB(0, 0, 320, 240, null, 0, 320));
                clock.set(300_000_000L);
                crosshair.advanceAnimation(clock.get());
                assertTrue(crosshair.isVisible());
                assertFalse(timer(crosshair).isRunning());
            } finally {
                crosshair.removeNotify();
            }
        });
    }

    @Test
    void reenteringAfterAnUndeliveredHideCompletionRemainsVisible() throws Exception {
        onEdt(() -> {
            AtomicLong clock = new AtomicLong();
            HighPrecisionCrosshair crosshair = crosshair(clock);
            try {
                crosshair.updatePosition(100, 100, "X", "Y");
                clock.set(120_000_000L);
                crosshair.advanceAnimation(clock.get());
                crosshair.hideCrosshair();
                clock.set(500_000_000L);
                crosshair.updatePosition(200, 140, "New X", "New Y");
                assertTrue(crosshair.isVisible());
                crosshair.advanceAnimation(700_000_000L);
                assertTrue(nonTransparent(paint(crosshair), 194, 134, 13, 13) > 20);
            } finally {
                crosshair.removeNotify();
            }
        });
    }

    @Test
    void disablingMotionSettlesBothShowAndHideImmediately() throws Exception {
        onEdt(() -> {
            HighPrecisionCrosshair crosshair = crosshair(new AtomicLong());
            try {
                crosshair.updatePosition(100, 100, "X", "Y");
                crosshair.setAnimationsEnabled(false);
                assertTrue(crosshair.isVisible());
                assertFalse(timer(crosshair).isRunning());
                assertTrue(nonTransparent(paint(crosshair), 94, 94, 13, 13) > 20);
                crosshair.hideCrosshair();
                assertFalse(crosshair.isVisible());
                assertFalse(timer(crosshair).isRunning());
                crosshair.setEnabled(false);
                crosshair.updatePosition(120, 100, "X", "Y");
                assertFalse(crosshair.isVisible());
                assertFalse(timer(crosshair).isRunning());
            } finally {
                crosshair.removeNotify();
            }
        });
    }

    @Test
    void detachingClearsTheTimerAndStaleHoverState() throws Exception {
        onEdt(() -> {
            HighPrecisionCrosshair crosshair = crosshair(new AtomicLong());
            crosshair.updatePosition(100, 100, "X", "Y");
            assertTrue(timer(crosshair).isRunning());
            crosshair.removeNotify();
            assertFalse(timer(crosshair).isRunning());
            assertFalse(crosshair.isVisible());
            crosshair.addNotify();
            assertFalse(crosshair.isVisible());
            crosshair.removeNotify();
        });
    }

    private static HighPrecisionCrosshair crosshair(AtomicLong clock) {
        HighPrecisionCrosshair crosshair = new HighPrecisionCrosshair(clock::get);
        crosshair.setSize(320, 240);
        crosshair.addNotify();
        return crosshair;
    }

    private static BufferedImage paint(HighPrecisionCrosshair crosshair) {
        BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            crosshair.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int nonTransparent(BufferedImage image, int left, int top, int width, int height) {
        int count = 0;
        for (int y = top; y < top + height; y++) {
            for (int x = left; x < left + width; x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) count++;
            }
        }
        return count;
    }

    private static Timer timer(HighPrecisionCrosshair crosshair) throws ReflectiveOperationException {
        Field field = HighPrecisionCrosshair.class.getDeclaredField("animationTimer");
        field.setAccessible(true);
        return (Timer) field.get(crosshair);
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
