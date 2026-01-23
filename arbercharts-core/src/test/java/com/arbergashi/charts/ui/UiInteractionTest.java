package com.arbergashi.charts.ui;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class UiInteractionTest {

    @Test
    public void wheelZoomAdjustsRange() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1};
            final double[] ys = {0, 1};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer()).withAnimations(false);
        panel.setSize(400, 300);
        panel.doLayout();
        // trigger layout/paint to update plot bounds
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        PlotContext before = panel.getDebugContext();
        double widthBefore = before.maxX() - before.minX();

        Component canvas = panel.getOverlayCanvasForTesting();
        MouseWheelEvent wheelIn = new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 200, 150, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1);
        canvas.dispatchEvent(wheelIn);

        PlotContext after = panel.getDebugContext();
        double widthAfter = after.maxX() - after.minX();
        assertTrue(widthAfter < widthBefore);
    }

    @Test
    public void dragPanShiftsRange() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 10};
            final double[] ys = {0, 10};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer()).withAnimations(false);
        panel.setSize(400, 300);
        panel.doLayout();
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        PlotContext before = panel.getDebugContext();
        double beforeMin = before.minX();
        double beforeMax = before.maxX();

        Component canvas = panel.getOverlayCanvasForTesting();
        canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 200, 150, 1, false, java.awt.event.MouseEvent.BUTTON1));
        canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 240, 150, 0, false, java.awt.event.MouseEvent.BUTTON1));

        PlotContext after = panel.getDebugContext();
        double afterMin = after.minX();
        double afterMax = after.maxX();

        assertNotEquals(beforeMin, afterMin);
        assertEquals(beforeMax - beforeMin, afterMax - afterMin, 1e-9);
    }

    @Test
    public void autoScaleHandlesZeroRangeY() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {5, 5, 5};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer()).withAnimations(false);
        panel.setSize(200, 200);
        panel.doLayout();
        panel.resetZoom();
        PlotContext ctx = panel.getDebugContext();
        assertNotEquals(ctx.minY(), ctx.maxY());
    }
}
