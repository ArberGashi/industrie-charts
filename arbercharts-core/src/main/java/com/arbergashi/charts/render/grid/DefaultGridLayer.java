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
 * Default grid layer for general-purpose charts.
 * Renders a clean, adaptive grid using NiceScale for tick positioning.
 *
 * <p>All styling is theme-driven via UIManager properties (e.g., FlatLaf themes).
 * This enables Bloomberg/SciChart-grade grid quality without code changes.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class DefaultGridLayer implements GridLayer {

    // Theme property keys
    private static final String KEY_MINOR_ALPHA = "Chart.defaultGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.defaultGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.defaultGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.defaultGrid.majorStrokeWidth";
    private static final String KEY_FRAME_ALPHA = "Chart.defaultGrid.frameAlpha";
    private static final String KEY_FRAME_STROKE = "Chart.defaultGrid.frameStrokeWidth";
    private static final String KEY_ZERO_ALPHA = "Chart.defaultGrid.zeroLineAlpha";
    private static final String KEY_ZERO_STROKE = "Chart.defaultGrid.zeroLineStrokeWidth";

    // Reusable buffers to avoid per-frame allocations
    private final double[] buf = new double[3];
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

    public DefaultGridLayer setSpatialProjector(SpatialProjector spatialProjector) {
        this.spatialProjector = spatialProjector;
        return this;
    }

    public GridBatchConfig getGridBatchConfig() {
        return gridBatchConfig;
    }

    public DefaultGridLayer setGridBatchConfig(GridBatchConfig gridBatchConfig) {
        if (gridBatchConfig != null) {
            this.gridBatchConfig = gridBatchConfig;
        }
        return this;
    }

    protected void mapToPixel(PlotContext context, double x, double y, double[] out) {
        if (spatialProjector == null) {
            context.mapToPixel(x, y, out);
            return;
        }
        spatialProjector.getCalculatedProjection(x, y, 0.0, spatialBuf);
        out[0] = spatialBuf[0];
        out[1] = spatialBuf[1];
    }

    protected void mapToSpatial(PlotContext context, double x, double y, double z, double[] out) {
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

        // Target spacing in pixels (roughly): ~80px major, ~40px minor
        int majorXTicks = Math.max(3, Math.min(12, w / 90));
        int majorYTicks = Math.max(3, Math.min(10, h / 70));

        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));

        ArberColor base = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.22f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        float minorStrokeWidth = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f);
        float majorStrokeWidth = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f);
        float frameAlpha = ChartAssets.getUIFloat(KEY_FRAME_ALPHA, 0.52f);
        float frameStrokeWidth = ChartAssets.getUIFloat(KEY_FRAME_STROKE, 1.0f);
        float zeroAlpha = ChartAssets.getUIFloat(KEY_ZERO_ALPHA, 0.45f);
        float zeroStrokeWidth = ChartAssets.getUIFloat(KEY_ZERO_STROKE, 1.1f);

        ArberColor minorColor = ColorRegistry.applyAlpha(base, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(base, majorAlpha);
        ArberColor frameColor = ColorRegistry.applyAlpha(theme.getAxisLabelColor(), frameAlpha);
        ArberColor zeroLineColor = ColorRegistry.applyAlpha(base, zeroAlpha);

        // --- Y grid: minor then major ---
        yMinorScale.setRange(context.getMinY(), context.getMaxY());
        yMinorScale.setMaxTicks(minorYTicks);
        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStrokeWidth));
        double yMinorSpacing = yMinorScale.getTickSpacing();
        if (yMinorSpacing > 0) {
            double tick = yMinorScale.getNiceMin();
            double end = yMinorScale.getNiceMax();
            double epsilon = yMinorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMinorSpacing) {
                mapToPixel(context, context.getMinX(), tick, buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    drawLine(canvas, minX, y, maxX, y);
                }
            }
        }

        yMajorScale.setRange(context.getMinY(), context.getMaxY());
        yMajorScale.setMaxTicks(majorYTicks);
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStrokeWidth));
        double yMajorSpacing = yMajorScale.getTickSpacing();
        if (yMajorSpacing > 0) {
            double tick = yMajorScale.getNiceMin();
            double end = yMajorScale.getNiceMax();
            double epsilon = yMajorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMajorSpacing) {
                mapToPixel(context, context.getMinX(), tick, buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    drawLine(canvas, minX, y, maxX, y);
                }
            }
        }

        // --- X grid: minor then major ---
        xMinorScale.setRange(context.getMinX(), context.getMaxX());
        xMinorScale.setMaxTicks(minorXTicks);
        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStrokeWidth));
        double xMinorSpacing = xMinorScale.getTickSpacing();
        if (xMinorSpacing > 0) {
            double tick = xMinorScale.getNiceMin();
            double end = xMinorScale.getNiceMax();
            double epsilon = xMinorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMinorSpacing) {
                mapToPixel(context, tick, context.getMinY(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    drawLine(canvas, x, minY, x, maxY);
                }
            }
        }

        xMajorScale.setRange(context.getMinX(), context.getMaxX());
        xMajorScale.setMaxTicks(majorXTicks);
        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStrokeWidth));
        double xMajorSpacing = xMajorScale.getTickSpacing();
        if (xMajorSpacing > 0) {
            double tick = xMajorScale.getNiceMin();
            double end = xMajorScale.getNiceMax();
            double epsilon = xMajorSpacing * 0.5;
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMajorSpacing) {
                mapToPixel(context, tick, context.getMinY(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    drawLine(canvas, x, minY, x, maxY);
                }
            }
        }

        // Zero line
        if (context.getMinY() <= 0 && context.getMaxY() >= 0) {
            mapToPixel(context, context.getMinX(), 0, buf);
            double y = snap(buf[1]);
            canvas.setColor(zeroLineColor);
            canvas.setStroke(ChartScale.scale(zeroStrokeWidth));
            drawLine(canvas, minX, y, maxX, y);
        }

        // Frame
        canvas.setColor(frameColor);
        canvas.setStroke(ChartScale.scale(frameStrokeWidth));
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

        int majorXTicks = Math.max(3, Math.min(12, w / 90));
        int majorYTicks = Math.max(3, Math.min(10, h / 70));
        int minorXTicks = Math.max(majorXTicks * 2, Math.min(majorXTicks * 3, w / 45));
        int minorYTicks = Math.max(majorYTicks * 2, Math.min(majorYTicks * 3, h / 35));

        ChartTheme theme = context.getTheme() != null ? context.getTheme() : ChartThemes.getDarkTheme();
        ArberColor base = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.22f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.42f);
        ArberColor minorColor = ColorRegistry.applyAlpha(base, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(base, majorAlpha);

        // minor Y
        yMinorScale.setRange(context.getMinY(), context.getMaxY());
        yMinorScale.setMaxTicks(minorYTicks);
        double yMinorSpacing = yMinorScale.getTickSpacing();
        if (yMinorSpacing > 0) {
            double tick = yMinorScale.getNiceMin();
            double end = yMinorScale.getNiceMax();
            double epsilon = yMinorSpacing * 0.5;
            builder.setStyleKey(SpatialStyleDescriptor.pack(minorColor.argb(), ChartScale.scale(ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f)), 0, 0));
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMinorSpacing) {
                mapToSpatial(context, context.getMinX(), tick, config.getGridZ(), buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    builder.setLineSegment(minX, y, config.getGridZ(), maxX, y, config.getGridZ());
                }
            }
        }

        // major Y
        yMajorScale.setRange(context.getMinY(), context.getMaxY());
        yMajorScale.setMaxTicks(majorYTicks);
        double yMajorSpacing = yMajorScale.getTickSpacing();
        if (yMajorSpacing > 0) {
            double tick = yMajorScale.getNiceMin();
            double end = yMajorScale.getNiceMax();
            double epsilon = yMajorSpacing * 0.5;
            builder.setStyleKey(SpatialStyleDescriptor.pack(majorColor.argb(), ChartScale.scale(ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f)), 0, 0));
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += yMajorSpacing) {
                mapToSpatial(context, context.getMinX(), tick, config.getGridZ(), buf);
                double y = snap(buf[1]);
                if (y >= minY && y <= maxY) {
                    builder.setLineSegment(minX, y, config.getGridZ(), maxX, y, config.getGridZ());
                }
            }
        }

        // minor X
        xMinorScale.setRange(context.getMinX(), context.getMaxX());
        xMinorScale.setMaxTicks(minorXTicks);
        double xMinorSpacing = xMinorScale.getTickSpacing();
        if (xMinorSpacing > 0) {
            double tick = xMinorScale.getNiceMin();
            double end = xMinorScale.getNiceMax();
            double epsilon = xMinorSpacing * 0.5;
            builder.setStyleKey(SpatialStyleDescriptor.pack(minorColor.argb(), ChartScale.scale(ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.55f)), 0, 0));
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMinorSpacing) {
                mapToSpatial(context, tick, context.getMinY(), config.getGridZ(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    builder.setLineSegment(x, minY, config.getGridZ(), x, maxY, config.getGridZ());
                }
            }
        }

        // major X
        xMajorScale.setRange(context.getMinX(), context.getMaxX());
        xMajorScale.setMaxTicks(majorXTicks);
        double xMajorSpacing = xMajorScale.getTickSpacing();
        if (xMajorSpacing > 0) {
            double tick = xMajorScale.getNiceMin();
            double end = xMajorScale.getNiceMax();
            double epsilon = xMajorSpacing * 0.5;
            builder.setStyleKey(SpatialStyleDescriptor.pack(majorColor.argb(), ChartScale.scale(ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.85f)), 0, 0));
            for (int i = 0; i < 10000 && tick <= end + epsilon; i++, tick += xMajorSpacing) {
                mapToSpatial(context, tick, context.getMinY(), config.getGridZ(), buf);
                double x = snap(buf[0]);
                if (x >= minX && x <= maxX) {
                    builder.setLineSegment(x, minY, config.getGridZ(), x, maxY, config.getGridZ());
                }
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
