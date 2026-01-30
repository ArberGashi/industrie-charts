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
 * Geographic grid layer.
 *
 * <p>Renders latitude/longitude lines with safe spacing to avoid pole singularities.</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class GeoGridLayer extends DefaultGridLayer {
    private static final String KEY_MINOR_ALPHA = "Chart.geoGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.geoGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.geoGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.geoGrid.majorStrokeWidth";
    private static final String KEY_LAT_LINES = "Chart.geoGrid.latLines";
    private static final String KEY_LON_LINES = "Chart.geoGrid.lonLines";
    private static final String KEY_POLE_EPS = "Chart.geoGrid.poleEpsilon";

    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        if (context == null) return;
        ArberRect bounds = context.getPlotBounds();
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();

        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));

        int latLines = Math.max(4, ChartAssets.getInt(KEY_LAT_LINES, 8));
        int lonLines = Math.max(6, ChartAssets.getInt(KEY_LON_LINES, 12));
        double poleEps = ChartAssets.getFloat(KEY_POLE_EPS, 2.0f);

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        // Latitude (horizontal)
        for (int i = 0; i <= latLines; i++) {
            double y = minY + i * (maxY - minY) / latLines;
            if (y - minY < poleEps || maxY - y < poleEps) {
                continue;
            }
            boolean isMajor = (i % Math.max(1, latLines / 2) == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
            drawLine(canvas, minX, y, maxX, y);
        }

        // Longitude (vertical)
        for (int i = 0; i <= lonLines; i++) {
            double x = minX + i * (maxX - minX) / lonLines;
            boolean isMajor = (i % Math.max(1, lonLines / 3) == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
            drawLine(canvas, x, minY, x, maxY);
        }
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        GridBatchConfig effective = (config != null) ? config : getGridBatchConfig();
        builder.setZMin(effective.getZMin())
                .setClippingMode(effective.getClippingMode());

        ArberRect bounds = context.getPlotBounds();
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor gridBase = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.minorAlpha", 0.08f));
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA,
                ChartAssets.getFloat("Chart.defaultGrid.majorAlpha", Math.min(0.25f, minorAlpha + 0.12f)));
        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.minorStrokeWidth", 0.6f));
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE,
                ChartAssets.getFloat("Chart.defaultGrid.majorStrokeWidth", 0.8f));

        int latLines = Math.max(4, ChartAssets.getInt(KEY_LAT_LINES, 8));
        int lonLines = Math.max(6, ChartAssets.getInt(KEY_LON_LINES, 12));
        double poleEps = ChartAssets.getFloat(KEY_POLE_EPS, 2.0f);

        for (int i = 0; i <= latLines; i++) {
            double y = minY + i * (maxY - minY) / latLines;
            if (y - minY < poleEps || maxY - y < poleEps) {
                continue;
            }
            boolean isMajor = (i % Math.max(1, latLines / 2) == 0);
            int argb = ColorRegistry.applyAlpha(gridBase, isMajor ? majorAlpha : minorAlpha).argb();
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            int dashId = 1;
            long styleKey = SpatialStyleDescriptor.pack(argb, strokeWidth, dashId, 0);
            builder.setStyleKey(styleKey);
            builder.setLineSegment(minX, y, 1.0, maxX, y, 1.0);
        }

        for (int i = 0; i <= lonLines; i++) {
            double x = minX + i * (maxX - minX) / lonLines;
            boolean isMajor = (i % Math.max(1, lonLines / 3) == 0);
            int argb = ColorRegistry.applyAlpha(gridBase, isMajor ? majorAlpha : minorAlpha).argb();
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            int dashId = 2;
            long styleKey = SpatialStyleDescriptor.pack(argb, strokeWidth, dashId, 0);
            builder.setStyleKey(styleKey);
            builder.setLineSegment(x, minY, 1.0, x, maxY, 1.0);
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
