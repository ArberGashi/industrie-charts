package com.arbergashi.charts.demo;

import com.arbergashi.charts.platform.swing.ArberChartPanel;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeListener;
import java.util.function.LongSupplier;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Runs demo updates only while their chart is visible and motion is enabled. */
final class ShowcaseAnimation implements AutoCloseable {
    private static final String CLIENT_PROPERTY = ShowcaseAnimation.class.getName();
    private static final double MAX_FRAME_SECONDS = 0.25;
    private final ArberChartPanel panel;
    private final Frame frame;
    private final LongSupplier clock;
    private final Timer timer;
    private final HierarchyListener hierarchyListener;
    private final PropertyChangeListener motionListener;
    private boolean wasDisplayable;
    private boolean closed;
    private long lastNanos;
    private double elapsedSeconds;

    static ShowcaseAnimation install(ArberChartPanel panel, Frame frame) {
        close(panel);
        ShowcaseAnimation animation = new ShowcaseAnimation(panel, frame, System::nanoTime);
        panel.putClientProperty(CLIENT_PROPERTY, animation);
        return animation;
    }

    static void close(ArberChartPanel panel) {
        if (panel.getClientProperty(CLIENT_PROPERTY) instanceof ShowcaseAnimation animation) animation.close();
    }

    ShowcaseAnimation(ArberChartPanel panel, Frame frame, LongSupplier clock) {
        requireEdt();
        this.panel = panel;
        this.frame = frame;
        this.clock = clock;
        timer = new Timer(16, _ -> tick());
        timer.setCoalesce(true);
        hierarchyListener = event -> {
            if ((event.getChangeFlags() & (HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) != 0) {
                updateState();
            }
        };
        motionListener = _ -> updateState();
        panel.addHierarchyListener(hierarchyListener);
        panel.addPropertyChangeListener("animationsEnabled", motionListener);
        updateState();
    }

    private void updateState() {
        if (closed) return;
        if (panel.isDisplayable()) wasDisplayable = true;
        else if (wasDisplayable) {
            close();
            return;
        }
        if (panel.isShowing() && panel.isAnimationsEnabled()) {
            if (!timer.isRunning()) {
                lastNanos = clock.getAsLong();
                timer.start();
            }
        } else {
            timer.stop();
        }
    }

    void tick() {
        requireEdt();
        if (closed || !timer.isRunning()) return;
        updateState();
        if (!timer.isRunning()) return;
        long now = clock.getAsLong();
        double delta = Math.max(0.0, (now - lastNanos) * 1.0e-9);
        lastNanos = now;
        if (delta == 0.0) return;
        // A stalled EDT resumes calmly instead of emitting an unbounded catch-up burst.
        delta = Math.min(MAX_FRAME_SECONDS, delta);
        elapsedSeconds += delta;
        try {
            frame.update(elapsedSeconds, delta);
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    boolean isRunning() {
        return timer.isRunning();
    }

    @Override
    public void close() {
        requireEdt();
        if (closed) return;
        closed = true;
        timer.stop();
        panel.removeHierarchyListener(hierarchyListener);
        panel.removePropertyChangeListener("animationsEnabled", motionListener);
        if (panel.getClientProperty(CLIENT_PROPERTY) == this) panel.putClientProperty(CLIENT_PROPERTY, null);
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Demo animation must run on the EDT");
    }

    @FunctionalInterface
    interface Frame {
        void update(double elapsedSeconds, double deltaSeconds);
    }
}
