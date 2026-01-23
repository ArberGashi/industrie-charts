package com.arbergashi.charts.ui.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Medical grid layer for EKG/ECG and other medical charts (1mm/0.04s grid).
 * Optimized for zero-allocation rendering.
 *
 * <p>All styling is theme-driven via UIManager properties for clinical-grade quality.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class MedicalGridLayer implements GridLayer {
    private static final double MM = 0.04;
    private static final double MM5 = 0.20;

    // Theme property keys
    private static final String KEY_MINOR_ALPHA = "Chart.medicalGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.medicalGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.medicalGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.medicalGrid.majorStrokeWidth";
    private static final String KEY_CENTER_ALPHA = "Chart.medicalGrid.centerLineAlpha";
    private static final String KEY_CENTER_STROKE = "Chart.medicalGrid.centerLineStrokeWidth";

    private final double[] d1 = new double[2];
    private final double[] d2 = new double[2];

    // Cached colors derived from the active chart theme.
    private ChartTheme cachedTheme;
    private Color cachedMajorColor;
    private Color cachedMinorColor;
    private Color cachedCenterLineColor;
    private float cachedMinorStroke;
    private float cachedMajorStroke;
    private float cachedCenterStroke;

    @Override
    public void renderGrid(Graphics2D g, PlotContext context) {
        updateCachedColors(context);

        // Prefer crisp thin lines for medical grid
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldStroke = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double minX = context.minX(), maxX = context.maxX();
        double minY = context.minY(), maxY = context.maxY();

        // 1) Minor grid first (behind major grid)
        context.mapToPixel(minX + MM, minY, d1);
        context.mapToPixel(minX, minY, d2);
        double pixelDist = Math.abs(d1[0] - d2[0]);

        // LOD: only draw minor grid if enough pixel spacing to avoid moiré noise
        if (pixelDist > ChartScale.scale(5)) {
            g.setColor(cachedMinorColor);
            g.setStroke(new BasicStroke(ChartScale.scale(cachedMinorStroke)));
            drawGrid(g, context, minX, maxX, minY, maxY, MM);
        }

        // 2) Major grid (5mm)
        g.setColor(cachedMajorColor);
        g.setStroke(new BasicStroke(ChartScale.scale(cachedMajorStroke)));
        drawGrid(g, context, minX, maxX, minY, maxY, MM5);

        // 3) Baseline / centerline emphasis (only if it actually intersects the visible range)
        if (minY <= 0 && maxY >= 0) {
            g.setColor(cachedCenterLineColor);
            g.setStroke(new BasicStroke(ChartScale.scale(cachedCenterStroke)));
            context.mapToPixel(minX, 0.0, d1);
            context.mapToPixel(maxX, 0.0, d2);
            g.drawLine((int) Math.round(d1[0]), (int) Math.round(d1[1]),
                    (int) Math.round(d2[0]), (int) Math.round(d2[1]));
        }

        // restore hints
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void updateCachedColors(PlotContext context) {
        ChartTheme theme = (context != null && context.theme() != null) ? context.theme() : ChartThemes.defaultDark();

        // Always re-read from UIManager to ensure theme switches are reflected
        Color base = theme.getGridColor();

        // Theme-driven alpha and stroke values
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.22f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.50f);
        float centerAlpha = ChartAssets.getUIFloat(KEY_CENTER_ALPHA, 0.42f);

        cachedMinorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.45f);
        cachedMajorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.95f);
        cachedCenterStroke = ChartAssets.getUIFloat(KEY_CENTER_STROKE, 1.1f);

        cachedMajorColor = ColorUtils.withAlpha(base, majorAlpha);
        cachedMinorColor = ColorUtils.withAlpha(base, minorAlpha);
        cachedCenterLineColor = ColorUtils.withAlpha(theme.getAxisLabelColor(), centerAlpha);

        cachedTheme = theme;
    }

    private void drawGrid(Graphics2D g, PlotContext context, double minX, double maxX, double minY, double maxY, double step) {
        // Mathematically correct start point for the grid
        if (!(Double.isFinite(step) && step > 0)) return;
        double startX = Math.floor(minX / step) * step;

        for (double x = startX; x <= maxX; x += step) {
            context.mapToPixel(x, minY, d1);
            context.mapToPixel(x, maxY, d2);
            g.drawLine((int) Math.round(d1[0]), (int) Math.round(d1[1]),
                    (int) Math.round(d2[0]), (int) Math.round(d2[1]));
        }

        double startY = Math.floor(minY / step) * step;
        for (double y = startY; y <= maxY; y += step) {
            context.mapToPixel(minX, y, d1);
            context.mapToPixel(maxX, y, d2);
            g.drawLine((int) Math.round(d1[0]), (int) Math.round(d1[1]),
                    (int) Math.round(d2[0]), (int) Math.round(d2[1]));
        }
    }
}
