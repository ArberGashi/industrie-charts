package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PanelModelLifecycleTest {
    @Test
    void detachingUnsubscribesEveryLayerAndReattachingConnectsExactlyOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CountingModel primary = new CountingModel();
            CountingModel underneath = new CountingModel();
            ArberChartPanel panel = new ArberChartPanel(primary, new LineRenderer());
            panel.setPreDataLayer(underneath, new LineRenderer());
            assertEquals(1, primary.listeners.size(), "Unrealized panels must already observe their initial model");
            assertEquals(1, underneath.listeners.size());
            for (int cycle = 0; cycle < 3; cycle++) {
                panel.addNotify();
                assertEquals(1, primary.listeners.size());
                assertEquals(1, underneath.listeners.size());
                panel.removeNotify();
                assertEquals(0, primary.listeners.size());
                assertEquals(0, underneath.listeners.size());
            }
        });
    }

    @Test
    void layersAddedWhileDetachedWaitForReattachment() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CountingModel first = new CountingModel();
            CountingModel later = new CountingModel();
            ArberChartPanel panel = new ArberChartPanel(first, new LineRenderer());
            panel.addNotify();
            panel.removeNotify();
            panel.setLayer(later, new LineRenderer());
            panel.setPreDataLayer(later, new LineRenderer());
            assertEquals(0, later.listeners.size());
            panel.addNotify();
            assertEquals(1, first.listeners.size());
            assertEquals(2, later.listeners.size(), "One subscription for each distinct layer");
            panel.removeNotify();
            assertEquals(0, first.listeners.size());
            assertEquals(0, later.listeners.size());
        });
    }

    @Test
    void clearLayersAlsoClearsPreDataLayersAndTheirSubscriptions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CountingModel model = new CountingModel();
            ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
            panel.setPreDataLayer(model, new LineRenderer());
            assertEquals(2, model.listeners.size());
            panel.clearLayers();
            assertEquals(0, panel.getLayerCount());
            assertEquals(0, model.listeners.size());
            panel.addNotify();
            panel.removeNotify();
            panel.addNotify();
            assertEquals(0, model.listeners.size(), "Cleared pre-data layers must not be reconnected");
            panel.removeNotify();
        });
    }

    private static final class CountingModel extends DefaultChartModel {
        private final List<ChartModelListener> listeners = new ArrayList<>();

        CountingModel() {
            super("Lifecycle");
            setXYArrays(new double[]{0, 1}, new double[]{0, 1});
        }

        @Override
        public void setChangeListener(ChartModelListener listener) {
            listeners.add(listener);
            super.setChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChartModelListener listener) {
            listeners.remove(listener);
            super.removeChangeListener(listener);
        }
    }
}
