package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke tests for UI painting.
 *
 * <p>Framework goal: core UI components must not throw during paint for empty/simple models,
 * even when embedded in real applications that trigger repaints at unpredictable times.</p>
 */
public class ChartPanelPaintSmokeTest {

    @Test
    void arberChartPanel_paint_doesNotThrow_forSimpleModel() {
        DefaultChartModel model = new DefaultChartModel("s");
        model.setPoint(0, 0, 0, null);
        model.setPoint(1, 1, 0, null);

        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        panel.setSize(400, 250);

        BufferedImage img = new BufferedImage(400, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> panel.paint(g2)));
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            g2.dispose();
        }
    }

    @Test
    void defaultPlotContext_mapToPixel_and_mapToData_handleZeroRanges() {
        ChartModel model = new ChartModel() {
            @Override public String getName() { return "z"; }
            @Override public int getPointCount() { return 1; }
            @Override public double[] getXData() { return new double[]{1.0}; }
            @Override public double[] getYData() { return new double[]{2.0}; }
            @Override public void setChangeListener(ChartModel.ChartModelListener listener) { }
            @Override public void removeChangeListener(ChartModel.ChartModelListener listener) { }
        };

        PlotContext ctx = new DefaultPlotContext(new ArberRect(0, 0, 100, 100), model,
                1.0, 1.0, 2.0, 2.0);

        double[] out = new double[2];
        assertDoesNotThrow(() -> ctx.mapToPixel(1.0, 2.0, out));
        assertDoesNotThrow(() -> ctx.mapToData(50, 50, out));
    }
}
