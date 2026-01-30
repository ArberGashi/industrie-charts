package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.util.ChartScale;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreDataLayerBandTest {

    @Test
    void preDataLayerDoesNotAffectViewBounds() {
        DefaultChartModel model = new DefaultChartModel("base");
        for (int i = 0; i < 100; i++) {
            model.setXY(i, Math.sin(i * 0.05));
        }

        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(800, 600);
        panel.doLayout();

        paint(panel);
        double minX1 = panel.getDebugContext().getMinX();
        double maxX1 = panel.getDebugContext().getMaxX();
        double minY1 = panel.getDebugContext().getMinY();
        double maxY1 = panel.getDebugContext().getMaxY();

        panel.setPreDataLayer(model, new PredictiveShadowRenderer());
        paint(panel);

        assertEquals(minX1, panel.getDebugContext().getMinX(), 1e-9);
        assertEquals(maxX1, panel.getDebugContext().getMaxX(), 1e-9);
        assertEquals(minY1, panel.getDebugContext().getMinY(), 1e-9);
        assertEquals(maxY1, panel.getDebugContext().getMaxY(), 1e-9);
    }

    private static void paint(ArberChartPanel panel) {
        BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            ChartScale.applyScale(1.0f, () -> panel.paint(g2));
        } finally {
            g2.dispose();
        }
    }
}
