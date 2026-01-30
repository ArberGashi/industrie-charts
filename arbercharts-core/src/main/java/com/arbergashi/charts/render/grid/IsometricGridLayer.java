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
 * Isometric grid layer.
 *
 * <p>Renders three-axis isometric grid (vertical + two 60° diagonals).</p>
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class IsometricGridLayer extends DefaultGridLayer {
    private static final String KEY_MINOR_ALPHA = "Chart.isometricGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.isometricGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.isometricGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.isometricGrid.majorStrokeWidth";
    private static final String KEY_SPACING = "Chart.isometricGrid.spacing";
    private static final String KEY_MAJOR_EVERY = "Chart.isometricGrid.majorEvery";

    private static final double ISO_SLOPE = Math.sqrt(3.0); // 60 degrees

    private final double[] clipBuf = new double[4];
    private final double[] clipPts = new double[8];
    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        if (context == null) return;
        ArberRect bounds = context.getPlotBounds();

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
        double spacing = ChartAssets.getFloat(KEY_SPACING, 40.0f);
        int majorEvery = Math.max(2, ChartAssets.getInt(KEY_MAJOR_EVERY, 5));

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        renderIsometricLines(canvas, bounds, spacing, majorEvery, minorColor, majorColor, minorStroke, majorStroke);
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        GridBatchConfig effective = (config != null) ? config : getGridBatchConfig();
        builder.setZMin(effective.getZMin())
                .setClippingMode(effective.getClippingMode());

        ArberRect bounds = context.getPlotBounds();
        double spacing = ChartAssets.getFloat(KEY_SPACING, 40.0f);
        int majorEvery = Math.max(2, ChartAssets.getInt(KEY_MAJOR_EVERY, 5));

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

        ArberColor minorColor = ColorRegistry.applyAlpha(gridBase, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(gridBase, majorAlpha);

        renderIsometricLines(builder, bounds, spacing, majorEvery, minorColor, majorColor, minorStroke, majorStroke);
    }

    private void renderIsometricLines(ArberCanvas canvas, ArberRect bounds, double spacing, int majorEvery,
                                      ArberColor minorColor, ArberColor majorColor, float minorStroke, float majorStroke) {
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();

        double width = bounds.width();
        double height = bounds.height();
        int vCount = Math.max(2, (int) Math.ceil(width / spacing));
        int dCount = Math.max(2, (int) Math.ceil((width + height) / spacing));

        // Vertical lines
        for (int i = 0; i <= vCount; i++) {
            double x = minX + i * spacing;
            boolean isMajor = (i % majorEvery == 0);
            canvas.setColor(isMajor ? majorColor : minorColor);
            canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
            drawLine(canvas, x, minY, x, maxY);
        }

        // Diagonal lines (slope +ISO_SLOPE)
        for (int i = -dCount; i <= dCount; i++) {
            double b = minY - ISO_SLOPE * minX + i * spacing;
            drawClippedLine(canvas, minX, minY, maxX, maxY, ISO_SLOPE, b, i, majorEvery, minorColor, majorColor, minorStroke, majorStroke);
        }

        // Diagonal lines (slope -ISO_SLOPE)
        for (int i = -dCount; i <= dCount; i++) {
            double b = minY + ISO_SLOPE * minX + i * spacing;
            drawClippedLine(canvas, minX, minY, maxX, maxY, -ISO_SLOPE, b, i, majorEvery, minorColor, majorColor, minorStroke, majorStroke);
        }
    }

    private void renderIsometricLines(SpatialPathBatchBuilder builder, ArberRect bounds, double spacing, int majorEvery,
                                      ArberColor minorColor, ArberColor majorColor, float minorStroke, float majorStroke) {
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();

        double width = bounds.width();
        double height = bounds.height();
        int vCount = Math.max(2, (int) Math.ceil(width / spacing));
        int dCount = Math.max(2, (int) Math.ceil((width + height) / spacing));

        for (int i = 0; i <= vCount; i++) {
            double x = minX + i * spacing;
            boolean isMajor = (i % majorEvery == 0);
            ArberColor color = isMajor ? majorColor : minorColor;
            float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
            builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
            builder.setLineSegment(x, minY, 1.0, x, maxY, 1.0);
        }

        for (int i = -dCount; i <= dCount; i++) {
            double b = minY - ISO_SLOPE * minX + i * spacing;
            if (clipLine(minX, minY, maxX, maxY, ISO_SLOPE, b, clipBuf)) {
                boolean isMajor = (Math.abs(i) % majorEvery == 0);
                ArberColor color = isMajor ? majorColor : minorColor;
                float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
                builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
                builder.setLineSegment(clipBuf[0], clipBuf[1], 1.0, clipBuf[2], clipBuf[3], 1.0);
            }
        }

        for (int i = -dCount; i <= dCount; i++) {
            double b = minY + ISO_SLOPE * minX + i * spacing;
            if (clipLine(minX, minY, maxX, maxY, -ISO_SLOPE, b, clipBuf)) {
                boolean isMajor = (Math.abs(i) % majorEvery == 0);
                ArberColor color = isMajor ? majorColor : minorColor;
                float strokeWidth = ChartScale.scale(isMajor ? majorStroke : minorStroke);
                builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
                builder.setLineSegment(clipBuf[0], clipBuf[1], 1.0, clipBuf[2], clipBuf[3], 1.0);
            }
        }
    }

    private void drawClippedLine(ArberCanvas canvas,
                                 double minX, double minY, double maxX, double maxY,
                                 double m, double b, int i, int majorEvery,
                                 ArberColor minorColor, ArberColor majorColor,
                                 float minorStroke, float majorStroke) {
        if (!clipLine(minX, minY, maxX, maxY, m, b, clipBuf)) return;
        boolean isMajor = (Math.abs(i) % majorEvery == 0);
        canvas.setColor(isMajor ? majorColor : minorColor);
        canvas.setStroke(ChartScale.scale(isMajor ? majorStroke : minorStroke));
        drawLine(canvas, clipBuf[0], clipBuf[1], clipBuf[2], clipBuf[3]);
    }

    private boolean clipLine(double minX, double minY, double maxX, double maxY,
                             double m, double b, double[] out) {
        int count = 0;

        // x = minX
        double y = m * minX + b;
        if (y >= minY && y <= maxY) {
            clipPts[count++] = minX;
            clipPts[count++] = y;
        }
        // x = maxX
        y = m * maxX + b;
        if (y >= minY && y <= maxY) {
            clipPts[count++] = maxX;
            clipPts[count++] = y;
        }
        // y = minY
        double x = (minY - b) / m;
        if (x >= minX && x <= maxX) {
            clipPts[count++] = x;
            clipPts[count++] = minY;
        }
        // y = maxY
        x = (maxY - b) / m;
        if (x >= minX && x <= maxX) {
            clipPts[count++] = x;
            clipPts[count++] = maxY;
        }

        if (count < 4) return false;
        out[0] = clipPts[0];
        out[1] = clipPts[1];
        out[2] = clipPts[2];
        out[3] = clipPts[3];
        return true;
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
