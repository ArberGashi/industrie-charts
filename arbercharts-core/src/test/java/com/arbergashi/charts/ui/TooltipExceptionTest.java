package com.arbergashi.charts.ui;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TooltipExceptionTest {

    @Test
    public void testTooltipUpdateWithEmptyModel() {
        // GIVEN: A chart with an empty model
        DefaultChartModel model = new DefaultChartModel("Empty");
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(500, 400);
        panel.doLayout();
        
        // WHEN: Mouse moves over the panel (triggering tooltip update)
        MouseEvent moveEvent = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 
                0, 250, 200, 0, false);
        
        // THEN: It should not throw ArrayIndexOutOfBoundsException
        assertDoesNotThrow(() -> {
            for (java.awt.event.MouseMotionListener l : panel.getMouseMotionListeners()) {
                l.mouseMoved(moveEvent);
            }
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

        MouseEvent moveEvent = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 
                0, 250, 200, 0, false);

        // THEN: It should handle the inconsistency gracefully
        assertDoesNotThrow(() -> {
            for (java.awt.event.MouseMotionListener l : panel.getMouseMotionListeners()) {
                l.mouseMoved(moveEvent);
            }
        });
    }
}
