package com.arbergashi.charts.ui.crosshair;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global crosshair synchronization for multi-chart setups.
 * Panels register as listeners and are notified when the crosshair position changes.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class CrosshairSyncController {
    private final List<CrosshairListener> listeners = new CopyOnWriteArrayList<>();
    private volatile double xPosition = Double.NaN;

    /**
     * Returns the current synchronized X position.
     *
     * @return X position in data space (NaN when unset)
     */
    public double getCrosshairPosition() {
        return xPosition;
    }

    /**
     * Updates the synchronized position and notifies all listeners.
     *
     * @param x new crosshair position in data space
     */
    public void setCrosshairPosition(double x) {
        this.xPosition = x;
        for (CrosshairListener l : listeners) l.onCrosshairMoved(x);
    }

    /**
     * Registers a listener for crosshair updates.
     *
     * @param l listener to add
     */
    public void addListener(CrosshairListener l) {
        listeners.add(l);
    }

    /**
     * Unregisters a listener.
     *
     * @param l listener to remove
     */
    public void removeListener(CrosshairListener l) {
        listeners.remove(l);
    }

    /**
     * Listener for synchronized crosshair updates.
     */
    public interface CrosshairListener {
        /**
         * Called when the synchronized crosshair position changes.
         *
         * @param xPosition crosshair X position in data space
         */
        void onCrosshairMoved(double xPosition);
    }
}
