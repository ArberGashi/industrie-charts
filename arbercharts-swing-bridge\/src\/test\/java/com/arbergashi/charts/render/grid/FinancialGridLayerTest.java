package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FinancialGridLayer}.
 *
 * <p>These tests render the grid into a {@link BufferedImage} and verify that the layer draws
 * non-background pixels for typical financial ranges. The goal is to ensure the grid is stable
 * (no exceptions) and actually paints something for both numeric and time-based X axes.</p>
 */
public class FinancialGridLayerTest {

    @Test
    void rendersSomething_forNumericAxis() {
        FinancialGridLayer layer = new FinancialGridLayer();

        BufferedImage img = new BufferedImage(480, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            // Fill with a known background so we can detect drawing.
            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, img.getWidth(), img.getHeight());

            PlotContext ctx = context(
                    new Rectangle2D.Double(10, 10, 460, 300),
                    0.0, 100.0,   // numeric X
                    90.0, 110.0,  // typical price range
                    ChartThemes.getLightTheme()
            );

            assertDoesNotThrow(() -> layer.renderGrid(g2, ctx));

            int drawn = countNonTransparent(img);
            assertTrue(drawn > 500, "Expected grid to draw some pixels, but image looks empty (drawn=" + drawn + ")");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void drawsDayBoundaries_forEpochMillisAxis_whenRangeIsReasonable() {
        FinancialGridLayer layer = new FinancialGridLayer();

        BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, img.getWidth(), img.getHeight());

            // 5 days range in epoch millis (triggers day boundary heuristic)
            long start = 1700000000000L; // arbitrary fixed epoch millis
            long end = start + 5L * 24 * 60 * 60 * 1000;

            PlotContext ctx = context(
                    new Rectangle2D.Double(10, 10, 620, 340),
                    (double) start, (double) end,
                    90.0, 110.0,
                    ChartThemes.getDarkTheme()
            );

            assertDoesNotThrow(() -> layer.renderGrid(g2, ctx));

            int drawn = countNonTransparent(img);
            // Should be comfortably above trivial levels because we draw both horizontal and vertical grids.
            assertTrue(drawn > 800, "Expected time-grid to draw pixels (drawn=" + drawn + ")");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void doesNotThrow_onDegenerateRanges() {
        FinancialGridLayer layer = new FinancialGridLayer();

        BufferedImage img = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            PlotContext ctx = context(
                    new Rectangle2D.Double(0, 0, 200, 120),
                    1.0, 1.0,   // degenerate X
                    50.0, 50.0, // degenerate Y
                    ChartThemes.getDarkTheme()
            );
            assertDoesNotThrow(() -> layer.renderGrid(g2, ctx));
        } finally {
            g2.dispose();
        }
    }

    private static PlotContext context(Rectangle2D bounds, double minX, double maxX, double minY, double maxY, ChartTheme theme) {
        return new DefaultPlotContext(
                bounds,
                minX,
                maxX,
                minY,
                maxY,
                false,
                false,
                false,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                theme,
                null,
                null,
                null
        );
    }

    private static int countNonTransparent(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a != 0) count++;
            }
        }
        return count;
    }
}
