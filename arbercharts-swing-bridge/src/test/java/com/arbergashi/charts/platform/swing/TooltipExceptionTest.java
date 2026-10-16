package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TooltipExceptionTest {

    @Test
    public void testTooltipUpdateWithEmptyModel() {
        // GIVEN: A chart with an empty model
        DefaultChartModel model = new DefaultChartModel("Empty");
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(500, 400);
        panel.doLayout();
        
        // WHEN: Mouse moves over the panel (triggering tooltip update)
        JComponent canvas = panel.getOverlayCanvasForTesting();
        MouseEvent moveEvent = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, 250, 200, 0, false);
        
        // THEN: It should not throw ArrayIndexOutOfBoundsException
        assertDoesNotThrow(() -> {
            assertTrue(canvas.getMouseMotionListeners().length > 0, "Exercise the installed input controller");
            SwingUtilities.invokeAndWait(() -> canvas.dispatchEvent(moveEvent));
        });
    }

    @Test
    public void testTooltipUpdateWithInconsistentModel() {
        // GIVEN: A model that reports a count but returns empty data (simulated inconsistency)
        ChartModel brokenModel = new DefaultChartModel("Broken") {
            @Override
            public int getPointCount() {
                return 10; // Reports points
            }

            @Override
            public double[] getXData() {
                return new double[0]; // But gives nothing
            }
            
            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        
        ArberChartPanel panel = new ArberChartPanel(brokenModel, new LineRenderer());
        panel.setSize(500, 400);
        panel.doLayout();

        JComponent canvas = panel.getOverlayCanvasForTesting();
        MouseEvent moveEvent = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, 250, 200, 0, false);

        // THEN: It should handle the inconsistency gracefully
        assertDoesNotThrow(() -> {
            assertTrue(canvas.getMouseMotionListeners().length > 0, "Exercise the installed input controller");
            SwingUtilities.invokeAndWait(() -> canvas.dispatchEvent(moveEvent));
        });
    }
}
