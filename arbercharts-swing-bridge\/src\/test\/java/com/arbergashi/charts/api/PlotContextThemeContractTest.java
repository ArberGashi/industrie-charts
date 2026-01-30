package com.arbergashi.charts.api;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link PlotContext}.
 *
 * <p>Framework policy:</p>
 * <ul>
 *   <li>{@link PlotContext#theme()} must never return {@code null} during rendering.</li>
 *   <li>The default implementation must provide a stable fallback theme.</li>
 * </ul>
 */
public class PlotContextThemeContractTest {

    @Test
    void theme_defaultIsNonNull() {
        PlotContext ctx = new PlotContext() {
            @Override public double getMinX() { return 0; }
            @Override public double getMaxX() { return 1; }
            @Override public double getMinY() { return 0; }
            @Override public double getMaxY() { return 1; }
            @Override public Rectangle2D getPlotBounds() { return new Rectangle2D.Double(0, 0, 100, 100); }
            @Override public void mapToPixel(double x, double y, double[] out) { out[0] = 0; out[1] = 0; }
            @Override public void mapToData(double pixelX, double pixelY, double[] dest) { dest[0] = 0; dest[1] = 0; }
        };

        assertNotNull(ctx.getTheme());
        assertSame(ChartThemes.getDarkTheme(), ctx.getTheme(), "Default theme should be the stable core fallback");
    }
}
