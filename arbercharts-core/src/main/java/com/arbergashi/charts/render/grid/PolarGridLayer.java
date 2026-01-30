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
 * Polar grid layer.
 *
 * <p>Renders concentric rings and radial spokes for polar charts.</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PolarGridLayer extends DefaultGridLayer {
    private static final String KEY_MINOR_ALPHA = "Chart.polarGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.polarGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.polarGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.polarGrid.majorStrokeWidth";
    private static final String KEY_RINGS = "Chart.polarGrid.rings";
    private static final String KEY_SPOKES = "Chart.polarGrid.spokes";

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
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double radius = Math.min(bounds.width(), bounds.height()) * 0.5 * 0.92;

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

        int rings = ChartAssets.getInt(KEY_RINGS,
                Math.max(3, Math.min(8, (int) Math.round(radius / 40.0))));
        int spokes = ChartAssets.getInt(KEY_SPOKES,
                Math.max(8, Math.min(24, (int) Math.round(radius / 25.0))));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        for (int i = 1; i <= rings; i++) {
            double r = radius * i / rings;
            boolean isMajor = (i == rings) || (i % Math.max(1, rings / 2) == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
            drawCircle(canvas, cx, cy, r, 120);
        }

        for (int i = 0; i < spokes; i++) {
            double angle = (Math.PI * 2.0) * i / spokes;
            double px = cx + Math.cos(angle) * radius;
            double py = cy - Math.sin(angle) * radius;
            boolean isMajor = (i % Math.max(1, spokes / 4) == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
            drawLine(canvas, cx, cy, px, py);
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
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double radius = Math.min(bounds.width(), bounds.height()) * 0.5 * 0.92;

        int rings = ChartAssets.getInt(KEY_RINGS,
                Math.max(3, Math.min(8, (int) Math.round(radius / 40.0))));
        int spokes = ChartAssets.getInt(KEY_SPOKES,
                Math.max(8, Math.min(24, (int) Math.round(radius / 25.0))));

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

        for (int i = 1; i <= rings; i++) {
            double r = radius * i / rings;
            boolean isMajor = (i == rings) || (i % Math.max(1, rings / 2) == 0);
            int argb = ColorRegistry.applyAlpha(gridBase, isMajor ? majorAlpha : minorAlpha).argb();
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            long styleKey = SpatialStyleDescriptor.pack(argb, strokeWidth, 0, 0);
            builder.setStyleKey(styleKey);
            addCircleSegments(builder, cx, cy, r, 120);
        }

        for (int i = 0; i < spokes; i++) {
            double angle = (Math.PI * 2.0) * i / spokes;
            double px = cx + Math.cos(angle) * radius;
            double py = cy - Math.sin(angle) * radius;
            boolean isMajor = (i % Math.max(1, spokes / 4) == 0);
            int argb = ColorRegistry.applyAlpha(gridBase, isMajor ? majorAlpha : minorAlpha).argb();
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            long styleKey = SpatialStyleDescriptor.pack(argb, strokeWidth, 2, 0);
            builder.setStyleKey(styleKey);
            builder.setLineSegment(cx, cy, 1.0, px, py, 1.0);
        }
    }

    private void drawCircle(ArberCanvas canvas, double cx, double cy, double radius, int segments) {
        double prevX = 0;
        double prevY = 0;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * i / segments;
            double px = cx + Math.cos(t) * radius;
            double py = cy - Math.sin(t) * radius;
            if (i > 0) {
                drawLine(canvas, prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }
    }

    private void addCircleSegments(SpatialPathBatchBuilder builder, double cx, double cy, double radius, int segments) {
        double prevX = 0;
        double prevY = 0;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * i / segments;
            double px = cx + Math.cos(t) * radius;
            double py = cy - Math.sin(t) * radius;
            if (i > 0) {
                builder.setLineSegment(prevX, prevY, 1.0, px, py, 1.0);
            }
            prevX = px;
            prevY = py;
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
