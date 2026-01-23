package com.arbergashi.charts.ui.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.NiceScale;

import java.awt.*;
import java.awt.geom.Line2D;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Grid layer tuned for financial charts: clear horizontal price lines,
 * subtle vertical time lines and an optional faint separator for volume areas.
 *
 * <p>All styling is theme-driven via UIManager properties for Bloomberg-grade quality.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class FinancialGridLayer implements GridLayer {

    // Theme property keys
    private static final String KEY_MINOR_ALPHA = "Chart.financialGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.financialGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.financialGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.financialGrid.majorStrokeWidth";
    private static final String KEY_MINOR_DASH_ON = "Chart.financialGrid.minorDash.on";
    private static final String KEY_MINOR_DASH_OFF = "Chart.financialGrid.minorDash.off";
    private static final String KEY_FRAME_ALPHA = "Chart.financialGrid.frameAlpha";
    private static final String KEY_ZERO_ALPHA = "Chart.financialGrid.zeroLineAlpha";
    private static final String KEY_VOLUME_SEP_ALPHA = "Chart.financialGrid.volumeSeparatorAlpha";

    private final Line2D.Double line = new Line2D.Double();
    private final double[] buf = new double[2];

    @Override
    public void renderGrid(Graphics2D g, PlotContext context) {
        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();
        Rectangle bounds = context.plotBounds().getBounds();

        // Crisp lines
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldStroke = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = Math.max(1, bounds.width);
        int h = Math.max(1, bounds.height);

        Color base = theme.getGridColor();

        // Theme-driven alpha and stroke values
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.20f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        float minorDashOn = ChartAssets.getUIFloat(KEY_MINOR_DASH_ON, 2.0f);
        float minorDashOff = ChartAssets.getUIFloat(KEY_MINOR_DASH_OFF, 5.0f);
        float frameAlpha = ChartAssets.getUIFloat(KEY_FRAME_ALPHA, 0.50f);
        float zeroAlpha = ChartAssets.getUIFloat(KEY_ZERO_ALPHA, 0.40f);
        float volumeSepAlpha = ChartAssets.getUIFloat(KEY_VOLUME_SEP_ALPHA, 0.28f);

        Color minor = ColorUtils.withAlpha(base, minorAlpha);
        Color major = ColorUtils.withAlpha(base, majorAlpha);
        Color emphasis = ColorUtils.withAlpha(theme.getAxisLabelColor(), zeroAlpha);

        // Major/minor tick density
        int majorYTicks = Math.max(4, Math.min(10, h / 70));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));

        int majorXTicks = Math.max(4, Math.min(12, w / 90));
        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));

        // --- Horizontal price lines: minor then major ---
        NiceScale yMinorScale = new NiceScale(context.minY(), context.maxY());
        yMinorScale.setMaxTicks(minorYTicks);
        g.setColor(minor);
        g.setStroke(new BasicStroke(ChartScale.scale(minorStrokeWidth)));
        for (double tick : yMinorScale.getTicks()) {
            context.mapToPixel(context.minX(), tick, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        NiceScale yMajorScale = new NiceScale(context.minY(), context.maxY());
        yMajorScale.setMaxTicks(majorYTicks);
        g.setColor(major);
        g.setStroke(new BasicStroke(ChartScale.scale(majorStrokeWidth)));
        for (double tick : yMajorScale.getTicks()) {
            context.mapToPixel(context.minX(), tick, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        // --- Vertical time lines: minor dashed then major dashed stronger ---
        NiceScale xMinorScale = new NiceScale(context.minX(), context.maxX());
        xMinorScale.setMaxTicks(minorXTicks);
        Stroke minorDash = new BasicStroke(
                ChartScale.scale(minorStrokeWidth), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff)}, 0.0f
        );
        g.setStroke(minorDash);
        g.setColor(minor);
        double lastX = Double.NEGATIVE_INFINITY;
        double minPixelSpacing = ChartScale.scale(5);
        for (double tick : xMinorScale.getTicks()) {
            context.mapToPixel(tick, context.minY(), buf);
            double x = buf[0];
            if (x >= bounds.getMinX() && x <= bounds.getMaxX()) {
                if (Double.isFinite(lastX) && Math.abs(x - lastX) < minPixelSpacing) continue;
                line.setLine(x, bounds.getMinY(), x, bounds.getMaxY());
                g.draw(line);
                lastX = x;
            }
        }

        NiceScale xMajorScale = new NiceScale(context.minX(), context.maxX());
        xMajorScale.setMaxTicks(majorXTicks);
        Stroke majorDash = new BasicStroke(
                ChartScale.scale(majorStrokeWidth), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff * 0.7f)}, 0.0f
        );
        g.setStroke(majorDash);
        g.setColor(major);
        lastX = Double.NEGATIVE_INFINITY;
        for (double tick : xMajorScale.getTicks()) {
            context.mapToPixel(tick, context.minY(), buf);
            double x = buf[0];
            if (x >= bounds.getMinX() && x <= bounds.getMaxX()) {
                if (Double.isFinite(lastX) && Math.abs(x - lastX) < ChartScale.scale(10)) continue;
                line.setLine(x, bounds.getMinY(), x, bounds.getMaxY());
                g.draw(line);
                lastX = x;
            }
        }

        // --- Semantic emphasis: y = 0 (for returns/oscillators) ---
        if (context.minY() <= 0 && context.maxY() >= 0) {
            g.setColor(emphasis);
            g.setStroke(new BasicStroke(ChartScale.scale(1.05f)));
            context.mapToPixel(context.minX(), 0.0, buf);
            double y = buf[1];
            if (y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                line.setLine(bounds.getMinX(), y, bounds.getMaxX(), y);
                g.draw(line);
            }
        }

        // Volume separator (theme-derived, subtle)
        if (h > 120) {
            g.setStroke(new BasicStroke(ChartScale.scale(1.0f)));
            g.setColor(ColorUtils.withAlpha(base, volumeSepAlpha));
            int ySep = bounds.y + (int) Math.round(h * 0.72);
            g.drawLine(bounds.x, ySep, bounds.x + bounds.width, ySep);
        }

        // Optional: emphasize session/day boundaries when X looks like epoch timestamps.
        drawDayBoundariesIfTimeAxis(g, context, emphasis);

        // Frame/border
        g.setColor(ColorUtils.withAlpha(base, frameAlpha));
        g.setStroke(new BasicStroke(ChartScale.scale(1.0f)));
        g.draw(context.plotBounds());

        // restore hints
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawDayBoundariesIfTimeAxis(Graphics2D g, PlotContext context, Color emphasis) {
        double minX = context.minX();
        double maxX = context.maxX();
        if (!(Double.isFinite(minX) && Double.isFinite(maxX)) || maxX <= minX) return;

        // Heuristic: treat as epoch milliseconds or seconds if values are large.
        double absMax = Math.max(Math.abs(minX), Math.abs(maxX));
        final boolean millis = absMax >= 1.0e11;
        final boolean seconds = absMax >= 1.0e9 && absMax < 1.0e11;
        if (!(millis || seconds)) return;

        Rectangle b = context.plotBounds().getBounds();
        if (b.width < 220) return;

        final long minMs = millis ? (long) minX : (long) (minX * 1000.0);
        final long maxMs = millis ? (long) maxX : (long) (maxX * 1000.0);
        if (maxMs <= minMs) return;

        // Pick UTC for consistent day boundaries (no locale/timezone surprises in framework core).
        LocalDate startDay = Instant.ofEpochMilli(minMs).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDay = Instant.ofEpochMilli(maxMs).atZone(ZoneOffset.UTC).toLocalDate();

        // Guard: if range is too large, skip to avoid overdraw
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDay, endDay);
        if (days > 120) return;

        Stroke oldStroke = g.getStroke();
        Color oldColor = g.getColor();
        g.setColor(emphasis);
        g.setStroke(new BasicStroke(
                ChartScale.scale(1.0f),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{ChartScale.scale(4.0f), ChartScale.scale(4.0f)},
                0.0f
        ));

        double lastDrawX = Double.NEGATIVE_INFINITY;
        double minPixelSpacing = ChartScale.scale(30);

        // Draw each day start inside (minX, maxX]
        for (LocalDate day = startDay.plusDays(1); !day.isAfter(endDay); day = day.plusDays(1)) {
            long dayStartMs = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            double xData = millis ? (double) dayStartMs : (dayStartMs / 1000.0);

            context.mapToPixel(xData, context.minY(), buf);
            double x = buf[0];
            if (x >= b.getMinX() && x <= b.getMaxX()) {
                if (!Double.isFinite(lastDrawX) || Math.abs(x - lastDrawX) >= minPixelSpacing) {
                    line.setLine(x, b.getMinY(), x, b.getMaxY());
                    g.draw(line);
                    lastDrawX = x;
                }
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }
}
