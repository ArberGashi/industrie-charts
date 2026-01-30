package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
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

    private final GridBatchConfig gridBatchConfig = new GridBatchConfig();
    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        ArberRect bounds = context.getPlotBounds();
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotW = bounds.width();
        double plotH = bounds.height();

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();

        ArberColor themeGrid = theme.getGridColor();

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
        // alpha precedence: UI > ChartAssets > default
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
            ChartAssets.getFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat(KEY_ALPHA, 0.12f)));
        ArberColor minorColor = ColorRegistry.applyAlpha(themeGrid, minorAlpha);

        // --- Major grid style (theme-driven) ---
        int majorEveryX = ChartAssets.getUIInt(KEY_MAJOR_EVERY_X,
            ChartAssets.getInt(KEY_MAJOR_EVERY_X, 4));
        int majorEveryY = ChartAssets.getUIInt(KEY_MAJOR_EVERY_Y,
            ChartAssets.getInt(KEY_MAJOR_EVERY_Y, 3));
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE_WIDTH,
            ChartAssets.getFloat(KEY_MAJOR_STROKE_WIDTH, 0.75f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
            ChartAssets.getFloat(KEY_MAJOR_ALPHA, Math.min(1f, minorAlpha + 0.12f)));
        ArberColor majorColor = ColorRegistry.applyAlpha(themeGrid, majorAlpha);

        // Horizontal grid lines
        for (int i = 0; i <= hLines; i++) {
            double y = plotY + i * plotH / hLines;
            boolean isMajor = majorEveryY > 0 && (i % majorEveryY == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStrokeWidth : minorStrokeWidth));
            drawLine(canvas, plotX, y, plotX + plotW, y);
        }

        // Vertical grid lines
        for (int i = 0; i <= vLines; i++) {
            double x = plotX + i * plotW / vLines;
            boolean isMajor = majorEveryX > 0 && (i % majorEveryX == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStrokeWidth : minorStrokeWidth));
            drawLine(canvas, x, plotY, x, plotY + plotH);
        }
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        GridBatchConfig effective = (config != null) ? config : gridBatchConfig;
        builder.setZMin(effective.getZMin())
                .setClippingMode(effective.getClippingMode());

        ArberRect bounds = context.getPlotBounds();
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotW = bounds.width();
        double plotH = bounds.height();

        int maxHLines = ChartAssets.getUIInt(KEY_MAX_HLINES, ChartAssets.getInt(KEY_MAX_HLINES, 8));
        int maxVLines = ChartAssets.getUIInt(KEY_MAX_VLINES, ChartAssets.getInt(KEY_MAX_VLINES, 14));

        int hLines = Math.max(4, Math.min(maxHLines, (int) Math.round(plotH / 40.0)));
        int vLines = Math.max(6, Math.min(maxVLines, (int) Math.round(plotW / 60.0)));

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor themeGrid = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
            ChartAssets.getFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat(KEY_ALPHA, 0.12f)));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
            ChartAssets.getFloat(KEY_MAJOR_ALPHA, Math.min(1f, minorAlpha + 0.12f)));
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE_WIDTH,
            ChartAssets.getFloat(KEY_MINOR_STROKE_WIDTH,
                ChartAssets.getFloat(KEY_STROKE_WIDTH, 0.6f)));
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE_WIDTH,
            ChartAssets.getFloat(KEY_MAJOR_STROKE_WIDTH, 0.75f));

        ArberColor minorColor = ColorRegistry.applyAlpha(themeGrid, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(themeGrid, majorAlpha);

        int majorEveryX = ChartAssets.getUIInt(KEY_MAJOR_EVERY_X,
            ChartAssets.getInt(KEY_MAJOR_EVERY_X, 4));
        int majorEveryY = ChartAssets.getUIInt(KEY_MAJOR_EVERY_Y,
            ChartAssets.getInt(KEY_MAJOR_EVERY_Y, 3));

        // Horizontal lines
        for (int i = 0; i <= hLines; i++) {
            double y = plotY + i * plotH / hLines;
            boolean isMajor = majorEveryY > 0 && (i % majorEveryY == 0);
            ArberColor color = isMajor ? majorColor : minorColor;
            float strokeWidth = ChartScale.scale(isMajor ? majorStrokeWidth : minorStrokeWidth);
            builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
            builder.setLineSegment(plotX, y, 1.0, plotX + plotW, y, 1.0);
        }

        // Vertical lines
        for (int i = 0; i <= vLines; i++) {
            double x = plotX + i * plotW / vLines;
            boolean isMajor = majorEveryX > 0 && (i % majorEveryX == 0);
            ArberColor color = isMajor ? majorColor : minorColor;
            float strokeWidth = ChartScale.scale(isMajor ? majorStrokeWidth : minorStrokeWidth);
            builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
            builder.setLineSegment(x, plotY, 1.0, x, plotY + plotH, 1.0);
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
