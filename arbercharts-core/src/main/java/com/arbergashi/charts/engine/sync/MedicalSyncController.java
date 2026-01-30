package com.arbergashi.charts.engine.sync;

import com.arbergashi.charts.model.CircularFastMedicalModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * Synchronizes multiple {@link CircularFastMedicalModel} instances for medical multi-channel dashboards.
 * Ensures all models advance in lockstep (same head index and timestamp).
 * Thread-safe implementation using {@link CopyOnWriteArrayList}.
  * Platform-independent and headless-certified. No AWT/Swing dependencies.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public class MedicalSyncController {
    private final List<CircularFastMedicalModel> models = new CopyOnWriteArrayList<>();
    private final int capacity;

    public MedicalSyncController(int capacity) {
        this.capacity = capacity;
    }

    public void registerModel(CircularFastMedicalModel model) {
        if (model.getCapacity() != this.capacity) {
            throw new IllegalArgumentException("All synchronized models must use the same capacity.");
        }
        models.add(model);
    }

    /**
     * Pushes a new sample across all channels at the same timestamp.
     *
     * @param x    The timestamp shared across all models.
     * @param data A 2D array: {@code [modelIndex][channelIndex]}.
     */
    public void tick(double x, double[][] data) {
        // Iteration over CopyOnWriteArrayList is safe and efficient for read-heavy scenarios
        for (int i = 0; i < models.size(); i++) {
            if (i < data.length) {
                models.get(i).add(x, data[i]);
            }
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getModelCount() {
        return models.size();
    }

    public CircularFastMedicalModel getModel(int idx) {
        return models.get(idx);
    }
}
