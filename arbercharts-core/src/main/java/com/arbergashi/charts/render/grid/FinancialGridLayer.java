package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialProjector;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.NiceScale;

/**
 * Grid layer tuned for financial charts: clear horizontal price lines,
 * subtle vertical time lines and an optional faint separator for volume areas.
 *
 * <p>All styling is theme-driven via UIManager properties for Bloomberg-grade quality.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class FinancialGridLayer implements GridLayer {

    private static final String KEY_MINOR_ALPHA = "Chart.financialGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.financialGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.financialGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.financialGrid.majorStrokeWidth";
    private static final String KEY_FRAME_ALPHA = "Chart.financialGrid.frameAlpha";
    private static final String KEY_ZERO_ALPHA = "Chart.financialGrid.zeroLineAlpha";
    private static final String KEY_VOLUME_SEP_ALPHA = "Chart.financialGrid.volumeSeparatorAlpha";

    private final double[] buf = new double[2];
    private final double[] spatialBuf = new double[3];
    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];
    private final NiceScale yMinorScale = new NiceScale(0, 1);
    private final NiceScale yMajorScale = new NiceScale(0, 1);
    private final NiceScale xMinorScale = new NiceScale(0, 1);
    private final NiceScale xMajorScale = new NiceScale(0, 1);

    private SpatialProjector spatialProjector;
    private GridBatchConfig gridBatchConfig = new GridBatchConfig();

    public SpatialProjector getSpatialProjector() {
        return spatialProjector;
    }

    public FinancialGridLayer setSpatialProjector(SpatialProjector spatialProjector) {
        this.spatialProjector = spatialProjector;
        return this;
    }

    public GridBatchConfig getGridBatchConfig() {
        return gridBatchConfig;
    }

    public FinancialGridLayer setGridBatchConfig(GridBatchConfig gridBatchConfig) {
        if (gridBatchConfig != null) {
            this.gridBatchConfig = gridBatchConfig;
        }
        return this;
    }

    private void mapToPixel(PlotContext context, double x, double y, double[] out) {
        if (spatialProjector == null) {
            context.mapToPixel(x, y, out);
            return;
        }
        spatialProjector.getCalculatedProjection(x, y, 0.0, spatialBuf);
        out[0] = spatialBuf[0];
        out[1] = spatialBuf[1];
    }

    private void mapToSpatial(PlotContext context, double x, double y, double z, double[] out) {
        if (spatialProjector == null) {
            context.mapToPixel(x, y, out);
            out[2] = z;
            return;
        }
        spatialProjector.getCalculatedProjection(x, y, z, out);
    }

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberRect bounds = context.getPlotBounds();
        double minX = bounds.x();
        double maxX = bounds.maxX();
        double minY = bounds.y();
        double maxY = bounds.maxY();

        int w = Math.max(1, (int) Math.ceil(bounds.width()));
        int h = Math.max(1, (int) Math.ceil(bounds.height()));

        ArberColor base = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.20f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        float frameAlpha = ChartAssets.getUIFloat(KEY_FRAME_ALPHA, 0.50f);
        float zeroAlpha = ChartAssets.getUIFloat(KEY_ZERO_ALPHA, 0.40f);
        float volumeSepAlpha = ChartAssets.getUIFloat(KEY_VOLUME_SEP_ALPHA, 0.28f);

        ArberColor minorColor = ColorRegistry.applyAlpha(base, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(base, majorAlpha);
        ArberColor frameColor = ColorRegistry.applyAlpha(theme.getAxisLabelColor(), frameAlpha);
        ArberColor zeroColor = ColorRegistry.applyAlpha(base, zeroAlpha);
        ArberColor volumeColor = ColorRegistry.applyAlpha(base, volumeSepAlpha);

        int majorYTicks = Math.max(4, Math.min(10, h / 70));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));
        int majorXTicks = Math.max(4, Math.min(12, w / 90));
        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));

        // Horizontal lines
        yMinorScale.setRange(context.getMinY(), context.getMaxY());
        yMinorScale.setMaxTicks(minorYTicks);
        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStrokeWidth));
        drawScaleY(canvas, context, yMinorScale, minX, maxX, minY, maxY);

        yMajorScale.setRange(context.getMinY(), context.getMaxY());
        yMajorScale.setMaxTicks(majorYTicks);
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStrokeWidth));
        drawScaleY(canvas, context, yMajorScale, minX, maxX, minY, maxY);

        // Vertical lines
        xMinorScale.setRange(context.getMinX(), context.getMaxX());
        xMinorScale.setMaxTicks(minorXTicks);
        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStrokeWidth));
        drawScaleX(canvas, context, xMinorScale, minX, maxX, minY, maxY);

        xMajorScale.setRange(context.getMinX(), context.getMaxX());
        xMajorScale.setMaxTicks(majorXTicks);
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStrokeWidth));
        drawScaleX(canvas, context, xMajorScale, minX, maxX, minY, maxY);

        // Zero line
        if (context.getMinY() <= 0 && context.getMaxY() >= 0) {
            mapToPixel(context, context.getMinX(), 0, buf);
            double y = snap(buf[1]);
            canvas.setColor(zeroColor);
            canvas.setStroke(ChartScale.scale(majorStrokeWidth));
            drawLine(canvas, minX, y, maxX, y);
        }

        // Volume separator (bottom 25% line)
        double sepY = minY + (maxY - minY) * 0.75;
        canvas.setColor(volumeColor);
        canvas.setStroke(ChartScale.scale(minorStrokeWidth));
        drawLine(canvas, minX, sepY, maxX, sepY);

        // Frame
        canvas.setColor(frameColor);
        canvas.setStroke(ChartScale.scale(majorStrokeWidth));
        drawLine(canvas, minX, minY, maxX, minY);
        drawLine(canvas, minX, maxY, maxX, maxY);
        drawLine(canvas, minX, minY, minX, maxY);
        drawLine(canvas, maxX, minY, maxX, maxY);
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        ArberRect bounds = context.getPlotBounds();
        double minX = bounds.x();
        double maxX = bounds.maxX();
        double minY = bounds.y();
        double maxY = bounds.maxY();

        int w = Math.max(1, (int) Math.ceil(bounds.width()));
        int h = Math.max(1, (int) Math.ceil(bounds.height()));

        int majorYTicks = Math.max(4, Math.min(10, h / 70));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));
        int majorXTicks = Math.max(4, Math.min(12, w / 90));
        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor base = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.20f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        ArberColor minorColor = ColorRegistry.applyAlpha(base, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(base, majorAlpha);

        yMinorScale.setRange(context.getMinY(), context.getMaxY());
        yMinorScale.setMaxTicks(minorYTicks);
        drawScaleYBatch(builder, context, config, yMinorScale, minorColor, ChartScale.scale(minorStrokeWidth), minX, maxX, minY, maxY);

        yMajorScale.setRange(context.getMinY(), context.getMaxY());
        yMajorScale.setMaxTicks(majorYTicks);
        drawScaleYBatch(builder, context, config, yMajorScale, majorColor, ChartScale.scale(majorStrokeWidth), minX, maxX, minY, maxY);

        xMinorScale.setRange(context.getMinX(), context.getMaxX());
        xMinorScale.setMaxTicks(minorXTicks);
        drawScaleXBatch(builder, context, config, xMinorScale, minorColor, ChartScale.scale(minorStrokeWidth), minX, maxX, minY, maxY);

        xMajorScale.setRange(context.getMinX(), context.getMaxX());
        xMajorScale.setMaxTicks(majorXTicks);
        drawScaleXBatch(builder, context, config, xMajorScale, majorColor, ChartScale.scale(majorStrokeWidth), minX, maxX, minY, maxY);
    }

    private void drawScaleY(ArberCanvas canvas, PlotContext context, NiceScale scale, double minX, double maxX, double minY, double maxY) {
        double spacing = scale.getTickSpacing();
        if (!(spacing > 0)) return;
        double tick = scale.getNiceMin();
        double end = scale.getNiceMax();
        double epsilon = spacing * 0.5;
        for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += spacing) {
            mapToPixel(context, context.getMinX(), tick, buf);
            double y = snap(buf[1]);
            if (y >= minY && y <= maxY) {
                drawLine(canvas, minX, y, maxX, y);
            }
        }
    }

    private void drawScaleX(ArberCanvas canvas, PlotContext context, NiceScale scale, double minX, double maxX, double minY, double maxY) {
        double spacing = scale.getTickSpacing();
        if (!(spacing > 0)) return;
        double tick = scale.getNiceMin();
        double end = scale.getNiceMax();
        double epsilon = spacing * 0.5;
        for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += spacing) {
            mapToPixel(context, tick, context.getMinY(), buf);
            double x = snap(buf[0]);
            if (x >= minX && x <= maxX) {
                drawLine(canvas, x, minY, x, maxY);
            }
        }
    }

    private void drawScaleYBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config,
                                 NiceScale scale, ArberColor color, float strokeWidth, double minX, double maxX, double minY, double maxY) {
        double spacing = scale.getTickSpacing();
        if (!(spacing > 0)) return;
        double tick = scale.getNiceMin();
        double end = scale.getNiceMax();
        double epsilon = spacing * 0.5;
        builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
        for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += spacing) {
            mapToSpatial(context, context.getMinX(), tick, config.getGridZ(), buf);
            double y = snap(buf[1]);
            if (y >= minY && y <= maxY) {
                builder.setLineSegment(minX, y, config.getGridZ(), maxX, y, config.getGridZ());
            }
        }
    }

    private void drawScaleXBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config,
                                 NiceScale scale, ArberColor color, float strokeWidth, double minX, double maxX, double minY, double maxY) {
        double spacing = scale.getTickSpacing();
        if (!(spacing > 0)) return;
        double tick = scale.getNiceMin();
        double end = scale.getNiceMax();
        double epsilon = spacing * 0.5;
        builder.setStyleKey(SpatialStyleDescriptor.pack(color.argb(), strokeWidth, 0, 0));
        for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += spacing) {
            mapToSpatial(context, tick, context.getMinY(), config.getGridZ(), buf);
            double x = snap(buf[0]);
            if (x >= minX && x <= maxX) {
                builder.setLineSegment(x, minY, config.getGridZ(), x, maxY, config.getGridZ());
            }
        }
    }

    private double snap(double v) {
        return Math.round(v) + 0.5;
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineXs[0] = (float) x1;
        lineYs[0] = (float) y1;
        lineXs[1] = (float) x2;
        lineYs[1] = (float) y2;
        canvas.drawPolyline(lineXs, lineYs, 2);
    }
}
