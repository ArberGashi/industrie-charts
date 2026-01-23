package com.arbergashi.charts.ui;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.util.ChartScale;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ChartUiExtendedTest {

    @Test
    public void testDpiScalingEffect() {
        ChartModel model = createTestModel();
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(400, 300);
        panel.doLayout();

        // Baseline scale 1.0
        ChartScale.setScaleFactor(1.0f);
        BufferedImage bi0 = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2_0 = bi0.createGraphics();
        try {
            panel.paint(g2_0);
        } finally {
            g2_0.dispose();
        }
        double x1 = panel.getDebugContext().plotBounds().getX();

        // High DPI scale 2.0
        ChartScale.setScaleFactor(2.0f);
        panel.resetZoom();
        // We need to trigger recalculation. In ArberChartPanel, it happens during paint.
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        double x2 = panel.getDebugContext().plotBounds().getX();

        // When scale factor increases, insets (margins) are typically larger (if they use ChartScale),
        // which might decrease the plotting area if the panel size remains constant.
        // Let's verify that the scaling factor actually influenced something.
        assertNotEquals(x1, x2, "X offset should change with scaling if insets are scaled");

        // Reset scale factor for other tests
        ChartScale.setScaleFactor(1.0f);
    }

    @Test
    public void testPanelLayoutChange() {
        ChartModel model = createTestModel();
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());

        panel.setSize(400, 300);
        panel.doLayout();
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        panel.paint(g2);
        g2.dispose();
        double w1 = panel.getDebugContext().plotBounds().getWidth();
        double h1 = panel.getDebugContext().plotBounds().getHeight();

        panel.setSize(800, 600);
        panel.doLayout();
        bi = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        panel.paint(g2);
        g2.dispose();
        double w2 = panel.getDebugContext().plotBounds().getWidth();
        double h2 = panel.getDebugContext().plotBounds().getHeight();

        assertTrue(w2 > w1);
        assertTrue(h2 > h1);
    }

    @Test
    public void testLegendOverlaySmoke() {
        ChartModel model = createTestModel();
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(400, 300);
        panel.doLayout();

        Component legend = null;
        Component canvas = panel.getOverlayCanvasForTesting();
        if (canvas instanceof Container container) {
            for (Component c : container.getComponents()) {
                if (c instanceof com.arbergashi.charts.ui.legend.InteractiveLegendOverlay) {
                    legend = c;
                    break;
                }
            }
        }

        assertNotNull(legend);
        legend.setSize(400, 300);

        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            // Should not throw exception
            legend.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    private ChartModel createTestModel() {
        return new ChartModel() {
            @Override
            public String getName() {
                return "TestSeries";
            }

            @Override
            public int getPointCount() {
                return 10;
            }

            @Override
            public double[] getXData() {
                return new double[10];
            }

            @Override
            public double[] getYData() {
                return new double[10];
            }

            @Override
            public void addChangeListener(ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
            }
        };
    }
}
