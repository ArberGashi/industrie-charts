package com.arbergashi.charts.ui.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Grid layer for analysis renderers (ACF, FFT, etc.) that use custom axis ranges.
 *
 * <p>This grid layer draws grid lines only (no axis labels), because some analysis renderers
 * handle their own coordinate systems and labeling.</p>
 *
 * <p>Styling is theme-driven and can be tuned via {@link ChartAssets} without changing code.
 * This enables product-grade UI (TradingView/Bloomberg-like subtle grids) while keeping the
 * renderer layer free from hard-coded aesthetics.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class AnalysisGridLayer implements GridLayer {

    // Backwards-compatible keys
    private static final String KEY_STROKE_WIDTH = "Chart.analysisGrid.strokeWidth";
    private static final String KEY_DASH_ON = "Chart.analysisGrid.dash.on";
    private static final String KEY_DASH_OFF = "Chart.analysisGrid.dash.off";
    private static final String KEY_ALPHA = "Chart.analysisGrid.alpha";

    // New keys (optional)
    private static final String KEY_MAJOR_EVERY_X = "Chart.analysisGrid.majorEveryX";
    private static final String KEY_MAJOR_EVERY_Y = "Chart.analysisGrid.majorEveryY";
    private static final String KEY_MAJOR_ALPHA = "Chart.analysisGrid.majorAlpha";
    private static final String KEY_MAJOR_STROKE_WIDTH = "Chart.analysisGrid.majorStrokeWidth";

    private static final String KEY_MINOR_ALPHA = "Chart.analysisGrid.minorAlpha";
    private static final String KEY_MINOR_STROKE_WIDTH = "Chart.analysisGrid.minorStrokeWidth";
    private static final String KEY_MINOR_DASH_ON = "Chart.analysisGrid.minorDash.on";
    private static final String KEY_MINOR_DASH_OFF = "Chart.analysisGrid.minorDash.off";

    private static final String KEY_MAX_HLINES = "Chart.analysisGrid.maxHLines";
    private static final String KEY_MAX_VLINES = "Chart.analysisGrid.maxVLines";

    private final Line2D.Double line = new Line2D.Double();

    @Override
    public void renderGrid(Graphics2D g2, PlotContext context) {
        double plotX = context.plotBounds().getX();
        double plotY = context.plotBounds().getY();
        double plotW = context.plotBounds().getWidth();
        double plotH = context.plotBounds().getHeight();

        ChartTheme theme = context.theme() != null ? context.theme() : ChartThemes.defaultDark();

        // Turn off AA for crisp thin lines
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Color themeGrid = theme.getGridColor();

        // Density adapts to plot size (avoid noise in small panels)
        // Use UIManager first, then fallback to ChartAssets, then defaults
        int maxHLines = ChartAssets.getUIInt(KEY_MAX_HLINES, ChartAssets.getInt(KEY_MAX_HLINES, 8));
        int maxVLines = ChartAssets.getUIInt(KEY_MAX_VLINES, ChartAssets.getInt(KEY_MAX_VLINES, 14));

        int hLines = Math.max(4, Math.min(maxHLines, (int) Math.round(plotH / 40.0)));
        int vLines = Math.max(6, Math.min(maxVLines, (int) Math.round(plotW / 60.0)));

        // --- Minor grid style (theme-driven) ---
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE_WIDTH,
            ChartAssets.getFloat(KEY_MINOR_STROKE_WIDTH,
                ChartAssets.getFloat(KEY_STROKE_WIDTH, 0.6f)));
        float minorDashOn = ChartAssets.getUIFloat(KEY_MINOR_DASH_ON,
            ChartAssets.getFloat(KEY_MINOR_DASH_ON,
                ChartAssets.getFloat(KEY_DASH_ON, 2.0f)));
        float minorDashOff = ChartAssets.getUIFloat(KEY_MINOR_DASH_OFF,
            ChartAssets.getFloat(KEY_MINOR_DASH_OFF,
                ChartAssets.getFloat(KEY_DASH_OFF, 3.0f)));

        // alpha precedence: UI > ChartAssets > default
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
            ChartAssets.getFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat(KEY_ALPHA, 0.12f)));
        Color minorColor = ColorUtils.withAlpha(themeGrid, minorAlpha);

        Stroke minorStroke = new BasicStroke(
                ChartScale.scale(minorStrokeWidth),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{ChartScale.scale(minorDashOn), ChartScale.scale(minorDashOff)},
                0.0f
        );

        // --- Major grid style (theme-driven) ---
        int majorEveryX = ChartAssets.getUIInt(KEY_MAJOR_EVERY_X,
            ChartAssets.getInt(KEY_MAJOR_EVERY_X, 4));
        int majorEveryY = ChartAssets.getUIInt(KEY_MAJOR_EVERY_Y,
            ChartAssets.getInt(KEY_MAJOR_EVERY_Y, 3));
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE_WIDTH,
            ChartAssets.getFloat(KEY_MAJOR_STROKE_WIDTH, 0.75f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
            ChartAssets.getFloat(KEY_MAJOR_ALPHA, Math.min(1f, minorAlpha + 0.12f)));
        Color majorColor = ColorUtils.withAlpha(themeGrid, majorAlpha);

        Stroke majorStroke = new BasicStroke(
                ChartScale.scale(majorStrokeWidth),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER
        );

        // Horizontal grid lines
        for (int i = 0; i <= hLines; i++) {
            double y = plotY + i * plotH / hLines;
            line.setLine(plotX, y, plotX + plotW, y);

            boolean isMajor = majorEveryY > 0 && (i % majorEveryY == 0);
            g2.setColor(isMajor ? majorColor : minorColor);
            g2.setStroke(isMajor ? majorStroke : minorStroke);
            g2.draw(line);
        }

        // Vertical grid lines
        for (int i = 0; i <= vLines; i++) {
            double x = plotX + i * plotW / vLines;
            line.setLine(x, plotY, x, plotY + plotH);

            boolean isMajor = majorEveryX > 0 && (i % majorEveryX == 0);
            g2.setColor(isMajor ? majorColor : minorColor);
            g2.setStroke(isMajor ? majorStroke : minorStroke);
            g2.draw(line);
        }

        // Restore AA
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }
}
