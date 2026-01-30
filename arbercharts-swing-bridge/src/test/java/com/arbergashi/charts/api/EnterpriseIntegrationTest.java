package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.financial.FibonacciRetracementRenderer;
import com.arbergashi.charts.render.financial.IchimokuCloudRenderer;
import com.arbergashi.charts.render.financial.VolumeProfileRenderer;
import com.arbergashi.charts.render.medical.ECGRhythmRenderer;
import com.arbergashi.charts.util.ColorRegistry;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class EnterpriseIntegrationTest {

    @Test
    public void testEnterpriseRenderersSmoke() {
        DefaultChartModel model = new DefaultChartModel("Enterprise Data");
        for (int i = 0; i < 60; i++) {
            // x, y, weight (volume), min, max, label
            model.setPoint(i, 10 + Math.sin(i * 0.2) * 5, 5, 20, 100, null);
        }

        ArberChartPanel panel = new ArberChartPanel(model, new IchimokuCloudRenderer());
        panel.setTheme(TestThemes.highContrast());
        panel.setLayer(model, new FibonacciRetracementRenderer());
        panel.setLayer(model, new VolumeProfileRenderer());
        panel.setLayer(model, new ECGRhythmRenderer());

        panel.setSize(800, 600);
        BufferedImage bi = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            // Ensure painting does not throw
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        assertNotNull(bi);
    }

    @Test
    public void testHighContrastTheme() {
        ChartTheme hc = TestThemes.highContrast();
        assertEquals(ColorRegistry.of(0, 0, 0, 255), hc.getBackground());
        assertEquals(ColorRegistry.of(255, 255, 255, 255), hc.getForeground());
        assertEquals(ColorRegistry.of(255, 255, 0, 255), hc.getAccentColor());
    }

    @Test
    public void testAccessibilityIntegration() {
        DefaultChartModel model = new DefaultChartModel("A11y Test");
        ArberChartPanel panel = new ArberChartPanel(model, new ECGRhythmRenderer());
        panel.setTheme(ChartThemes.getDarkTheme());

        assertNotNull(panel.getAccessibleContext());
        assertEquals("Arber Chart View", panel.getAccessibleContext().getAccessibleName());
        assertTrue(panel.getAccessibleContext().getAccessibleDescription().contains("interactive chart"));
    }
}
