package com.arbergashi.charts.platform.swing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.standard.LineRenderer;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class FreezeMeasurementTest {

    @Test
    void freezeAllowsMeasurementWithoutStoppingCapture() {
        Object prevEnabled = UIManager.get("Chart.medical.bpm.alarmEnabled");
        Object prevLow = UIManager.get("Chart.medical.bpm.low");
        DefaultChartModel model = new DefaultChartModel("freeze-measure");
        for (int i = 0; i < 240; i++) {
            model.setXY(i * 0.04, Math.sin(i * 0.05));
        }

        try {
            UIManager.put("Chart.medical.bpm.alarmEnabled", Boolean.TRUE);
            // Force a LOW alarm regardless of the measured BPM.
            UIManager.put("Chart.medical.bpm.low", 1_000d);

            ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
            panel.setSize(900, 600);
            panel.doLayout();

            panel.setFreeze(true);
            panel.setFreezeScrub(0.5);

            paint(panel);
            paint(panel);
            assertTrue(panel.isFrozen(), "Freeze should be active once the vault has frames");

            JComponent overlay = panel.getOverlayCanvasForTesting();
            dispatchClick(overlay, 320, 260);
            dispatchClick(overlay, 620, 300);

            assertTrue(panel.getFreezeMeasurementLabel() != null, "Measurement label should be available");
            String label = panel.getFreezeMeasurementLabel().getText();
            assertTrue(label != null && label.contains("!"), "BPM guard should mark alarms");
        } finally {
            if (prevEnabled == null) UIManager.put("Chart.medical.bpm.alarmEnabled", null);
            else UIManager.put("Chart.medical.bpm.alarmEnabled", prevEnabled);
            if (prevLow == null) UIManager.put("Chart.medical.bpm.low", null);
            else UIManager.put("Chart.medical.bpm.low", prevLow);
        }
    }

    private static void paint(ArberChartPanel panel) {
        BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g2 = img.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    private static void dispatchClick(JComponent target, int x, int y) {
        long now = System.currentTimeMillis();
        MouseEvent press = new MouseEvent(target, MouseEvent.MOUSE_PRESSED, now, 0, x, y, 1, false, MouseEvent.BUTTON1);
        target.dispatchEvent(press);
    }
}
