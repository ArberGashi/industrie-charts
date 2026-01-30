package com.arbergashi.charts.api;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

public class PlotContextTest {

    @Test
    public void cartesianInverseTransform() {
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 20, 300, 200);
        CartesianPlotContext ctx = new CartesianPlotContext(bounds, 0.0, 10.0, 0.0, 100.0);

        double[] pix = new double[2];
        ctx.mapToPixel(5.0, 50.0, pix);
        assertTrue(pix[0] >= bounds.getX() && pix[0] <= bounds.getMaxX());
        assertTrue(pix[1] >= bounds.getY() && pix[1] <= bounds.getMaxY());

        double[] data = new double[2];
        ctx.mapToData(pix[0], pix[1], data);
        assertEquals(5.0, data[0], 1e-6);
        assertEquals(50.0, data[1], 1e-6);

        // plotBounds should return the same instance we passed in
        assertSame(bounds, ctx.getPlotBounds());
    }

    @Test
    public void zeroRangeAxesAreStable() {
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 300);
        DefaultPlotContext ctx = new DefaultPlotContext(
                bounds,
                5.0,
                5.0,
                2.0,
                2.0,
                false,
                false,
                false,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR
        );

        double[] pix = new double[2];
        ctx.mapToPixel(5.0, 2.0, pix);
        assertTrue(Double.isFinite(pix[0]));
        assertTrue(Double.isFinite(pix[1]));

        double[] data = new double[2];
        ctx.mapToData(pix[0], pix[1], data);
        assertTrue(Double.isFinite(data[0]));
        assertTrue(Double.isFinite(data[1]));
    }

    @Test
    public void renderHintsAreAccessible() {
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 100);
        ChartRenderHints hints = new ChartRenderHints().setStrokeWidth(2.0f);
        DefaultPlotContext ctx = new DefaultPlotContext(
                bounds,
                null,
                0.0,
                10.0,
                0.0,
                10.0,
                ChartThemes.getDarkTheme(),
                hints
        );

        assertSame(hints, ctx.getRenderHints());
        assertEquals(2.0f, ctx.getRenderHints().getStrokeWidth());
    }

    @Test
    public void invertedAxesMirrorMapping() {
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 100);
        DefaultPlotContext ctx = new DefaultPlotContext(
                bounds,
                0.0,
                10.0,
                0.0,
                10.0,
                false,
                true,
                true,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                ChartThemes.getDarkTheme(),
                null,
                null,
                null
        );

        double[] pix = new double[2];
        ctx.mapToPixel(0.0, 0.0, pix);
        assertEquals(bounds.getMaxX(), pix[0], 1e-6);
        assertEquals(bounds.getY(), pix[1], 1e-6);

        double[] data = new double[2];
        ctx.mapToData(pix[0], pix[1], data);
        assertEquals(0.0, data[0], 1e-6);
        assertEquals(0.0, data[1], 1e-6);
    }
}
