package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Renderer for axes, ticks, and grid lines.
 *
 * <p>Ensures a professional representation of the coordinate system.
 * Separated from ArberChartPanel for better maintainability and performance optimization.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class AxisRenderer {

    private final Line2D.Double lineCache = new Line2D.Double();
    private final NiceScale niceScale = new NiceScale(0, 1);
    private final double[] mapBuf = new double[2];
    private BasicStroke gridStroke;
    private BasicStroke axisStroke;
    private Font axisFont;

    // Cache keys to avoid stale visuals across theme switches / DPI changes.
    private int cachedUiDpi = -1;
    private float cachedGridAlpha = Float.NaN;
    private float cachedGridStroke = Float.NaN;
    private float cachedAxisStroke = Float.NaN;
    private String cachedAxisFontKey = null;

    private void ensureCachesUpToDate() {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        float gridAlpha = ChartAssets.getFloat("Chart.axis.grid.alpha", 0.10f);
        float gridW = ChartAssets.getFloat("Chart.axis.grid.strokeWidth", 0.9f);
        float axisW = ChartAssets.getFloat("Chart.axis.strokeWidth", 1.0f);
        String fontKey = String.valueOf(UIManager.getFont("Chart.font"));

        boolean invalidate = (dpi != cachedUiDpi)
                || (Float.compare(gridAlpha, cachedGridAlpha) != 0)
                || (Float.compare(gridW, cachedGridStroke) != 0)
                || (Float.compare(axisW, cachedAxisStroke) != 0)
                || (cachedAxisFontKey == null || !cachedAxisFontKey.equals(fontKey));

        if (invalidate) {
            gridStroke = null;
            axisStroke = null;
            axisFont = null;
            cachedUiDpi = dpi;
            cachedGridAlpha = gridAlpha;
            cachedGridStroke = gridW;
            cachedAxisStroke = axisW;
            cachedAxisFontKey = fontKey;
        }
    }

    /**
     * Draws the background grid.
     *
     * @param g2 rendering context
     * @param context plot context with bounds and axis ranges
     */
    public void drawGrid(Graphics2D g2, PlotContext context) {
        if (!ChartAssets.getBoolean("chart.axis.grid.show", true)) return;
        ensureCachesUpToDate();

        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();
        Rectangle2D b = context.plotBounds();

        // Market-grade: separate minor/major grid emphasis.
        // Defaults intentionally subtle; theme supplies base grid token.
        float minorAlpha = ChartAssets.getFloat("Chart.axis.grid.minorAlpha", ChartAssets.getFloat("Chart.axis.grid.alpha", 0.10f));
        float majorAlpha = ChartAssets.getFloat("Chart.axis.grid.majorAlpha", Math.min(0.22f, minorAlpha + 0.10f));

        Color baseGridColor = ChartAssets.getColor("Chart.axis.grid.color", null);
        if (baseGridColor == null) {
            baseGridColor = theme.getGridColor() != null ? theme.getGridColor() : theme.getAxisLabelColor();
        }

        if (gridStroke == null) {
            float w = ChartAssets.getFloat("Chart.axis.grid.strokeWidth", 0.9f);
            gridStroke = new BasicStroke(ChartScale.scale(w));
        }

        // Minor grid
        g2.setStroke(gridStroke);
        g2.setColor(ColorUtils.withAlpha(baseGridColor, minorAlpha));
        drawGridLines(g2, context, b, true);
        drawGridLines(g2, context, b, false);

        // Major grid (optional): draw fewer ticks for emphasis if enabled.
        if (ChartAssets.getBoolean("Chart.axis.grid.major.enabled", false)) {
            int oldMaxX = ChartAssets.getInt("Chart.axis.grid.maxTicksX", 10);
            int oldMaxY = ChartAssets.getInt("Chart.axis.grid.maxTicksY", 8);
            int majorX = ChartAssets.getInt("Chart.axis.grid.majorTicksX", Math.max(3, oldMaxX / 2));
            int majorY = ChartAssets.getInt("Chart.axis.grid.majorTicksY", Math.max(3, oldMaxY / 2));

            g2.setColor(ColorUtils.withAlpha(baseGridColor, majorAlpha));

            // temporarily override tick density for major lines
            drawGridLinesWithMaxTicks(g2, context, b, true, majorX);
            drawGridLinesWithMaxTicks(g2, context, b, false, majorY);
        }
    }

    /**
     * Draws axis lines, ticks, and labels.
     *
     * @param g2 rendering context
     * @param context plot context with bounds and axis ranges
     */
    public void drawAxes(Graphics2D g2, PlotContext context) {
        ensureCachesUpToDate();
        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();

        Rectangle2D b = context.plotBounds();

        Color axisColor = ChartAssets.getColor("Chart.axis.label.color", null);
        g2.setColor(axisColor != null ? axisColor : theme.getAxisLabelColor());

        if (axisStroke == null) {
            float w = ChartAssets.getFloat("Chart.axis.strokeWidth", 1.0f);
            axisStroke = new BasicStroke(ChartScale.scale(w));
        }
        g2.setStroke(axisStroke);

        if (axisFont == null) {
            Font base = UIManager.getFont("Chart.font");
            if (base == null) base = UIManager.getFont("Label.font");
            if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            axisFont = base.deriveFont(Font.PLAIN, ChartScale.uiFontSize(base, 10));
        }
        g2.setFont(axisFont);

        // Optional frame alpha (Bloomberg-like subtle border)
        float frameAlpha = ChartAssets.getFloat("Chart.axis.frame.alpha", 0.35f);
        Color frameColor = ChartAssets.getColor("Chart.axis.frame.color", null);
        if (frameColor == null) frameColor = (axisColor != null ? axisColor : theme.getAxisLabelColor());

        Color old = g2.getColor();
        g2.setColor(ColorUtils.withAlpha(frameColor, frameAlpha));
        g2.draw(b);
        g2.setColor(old);

        drawTicks(g2, context, b, true);
        drawTicks(g2, context, b, false);
    }

    private void drawGridLinesWithMaxTicks(Graphics2D g2, PlotContext ctx, Rectangle2D b, boolean vertical, int maxTicks) {
        double min = vertical ? ctx.minX() : ctx.minY();
        double max = vertical ? ctx.maxX() : ctx.maxY();

        updateNiceScale(min, max);
        niceScale.setMaxTicks(Math.max(2, maxTicks));

        double[] p = new double[2];
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);

            if (vertical) {
                lineCache.setLine(p[0], b.getY(), p[0], b.getMaxY());
            } else {
                lineCache.setLine(b.getX(), p[1], b.getMaxX(), p[1]);
            }
            g2.draw(lineCache);
        }
    }

    private void drawGridLines(Graphics2D g2, PlotContext ctx, Rectangle2D b, boolean vertical) {
        double min = vertical ? ctx.minX() : ctx.minY();
        double max = vertical ? ctx.maxX() : ctx.maxY();

        updateNiceScale(min, max);
        niceScale.setMaxTicks(vertical ? 10 : 8);

        double[] p = new double[2];
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);

            if (vertical) {
                lineCache.setLine(p[0], b.getY(), p[0], b.getMaxY());
            } else {
                lineCache.setLine(b.getX(), p[1], b.getMaxX(), p[1]);
            }
            g2.draw(lineCache);
        }
    }

    private void drawTicks(Graphics2D g2, PlotContext ctx, Rectangle2D b, boolean vertical) {
        double min = vertical ? ctx.minX() : ctx.minY();
        double max = vertical ? ctx.maxX() : ctx.maxY();

        updateNiceScale(min, max);
        niceScale.setMaxTicks(vertical ? 10 : 8);

        double tickLen = ChartScale.scale(4.0);
        double textOffset = ChartScale.scale(15.0);
        FontMetrics fm = g2.getFontMetrics();
        double[] p = mapBuf;
        double step = niceScale.getTickSpacing();

        for (double val = niceScale.getNiceMin(); val <= niceScale.getNiceMax() + 0.0001 * step; val += step) {
            if (val < min || val > max) continue;

            ctx.mapToPixel(vertical ? val : 0, vertical ? 0 : val, p);
            String label = FormatUtils.formatAxisLabel(val);

            if (vertical) {
                // X-Axis Ticks (bottom)
                lineCache.setLine(p[0], b.getMaxY(), p[0], b.getMaxY() + tickLen);
                g2.draw(lineCache);

                float tw = fm.stringWidth(label);
                g2.drawString(label, (float) (p[0] - tw / 2), (float) (b.getMaxY() + textOffset));
            } else {
                // Y-Axis Ticks (left)
                lineCache.setLine(b.getX(), p[1], b.getX() - tickLen, p[1]);
                g2.draw(lineCache);

                float tw = fm.stringWidth(label);
                g2.drawString(label, (float) (b.getX() - tickLen - tw - 2), (float) (p[1] + fm.getAscent() / 2.0 - 2));
            }
        }
    }

    private void updateNiceScale(double min, double max) {
        niceScale.setRange(min, max);
    }
}
