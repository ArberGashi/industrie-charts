package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialProjector;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

/**
 * Medical grid layer for EKG/ECG and other medical charts (clinical defaults).
 * Optimized for zero-allocation rendering.
 *
 * <p>All styling is theme-driven via UIManager properties for clinical-grade quality.</p>
 *
 * @author Arber Gashi
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class MedicalGridLayer implements GridLayer {
    // Theme property keys
    private static final String KEY_STEP_X_MINOR = "Chart.medicalGrid.stepXMinor";
    private static final String KEY_STEP_X_MAJOR = "Chart.medicalGrid.stepXMajor";
    private static final String KEY_STEP_Y_MINOR = "Chart.medicalGrid.stepYMinor";
    private static final String KEY_STEP_Y_MAJOR = "Chart.medicalGrid.stepYMajor";
    private static final String KEY_MINOR_ALPHA = "Chart.medicalGrid.minorAlpha";
    private static final String KEY_MAJOR_ALPHA = "Chart.medicalGrid.majorAlpha";
    private static final String KEY_MINOR_STROKE = "Chart.medicalGrid.minorStrokeWidth";
    private static final String KEY_MAJOR_STROKE = "Chart.medicalGrid.majorStrokeWidth";
    private static final String KEY_CENTER_ALPHA = "Chart.medicalGrid.centerLineAlpha";
    private static final String KEY_CENTER_STROKE = "Chart.medicalGrid.centerLineStrokeWidth";

    private final double[] d1 = new double[2];
    private final double[] d2 = new double[2];
    private final double[] spatialBuf = new double[3];
    private final GridBatchCanvas batchCanvas = new GridBatchCanvas();
    private final float[] lineXs = new float[2];
    private final float[] lineYs = new float[2];

    private SpatialProjector spatialProjector;
    private GridBatchConfig gridBatchConfig = new GridBatchConfig();

    public SpatialProjector getSpatialProjector() {
        return spatialProjector;
    }

    public MedicalGridLayer setSpatialProjector(SpatialProjector spatialProjector) {
        this.spatialProjector = spatialProjector;
        return this;
    }

    public GridBatchConfig getGridBatchConfig() {
        return gridBatchConfig;
    }

    public MedicalGridLayer setGridBatchConfig(GridBatchConfig gridBatchConfig) {
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

    @Override
    public void renderGrid(ArberCanvas canvas, PlotContext context) {
        ChartTheme theme = (context != null && context.getTheme() != null) ? context.getTheme() : ChartThemes.getDarkTheme();

        double minX = context.getMinX(), maxX = context.getMaxX();
        double minY = context.getMinY(), maxY = context.getMaxY();

        // IMPORTANT: constants must remain unchanged (0.04 / 0.2 etc.)
        double stepXMinor = ChartAssets.getUIFloat(KEY_STEP_X_MINOR, ChartAssets.getFloat(KEY_STEP_X_MINOR, 0.04f));
        double stepXMajor = ChartAssets.getUIFloat(KEY_STEP_X_MAJOR, ChartAssets.getFloat(KEY_STEP_X_MAJOR, 0.20f));
        double stepYMinor = ChartAssets.getUIFloat(KEY_STEP_Y_MINOR, ChartAssets.getFloat(KEY_STEP_Y_MINOR, 0.10f));
        double stepYMajor = ChartAssets.getUIFloat(KEY_STEP_Y_MAJOR, ChartAssets.getFloat(KEY_STEP_Y_MAJOR, 0.50f));

        ArberColor base = theme.getGridColor();
        float minorAlpha = ChartAssets.getUIFloat(KEY_MINOR_ALPHA, 0.22f);
        float majorAlpha = ChartAssets.getUIFloat(KEY_MAJOR_ALPHA, 0.50f);
        float centerAlpha = ChartAssets.getUIFloat(KEY_CENTER_ALPHA, 0.42f);

        float minorStroke = ChartAssets.getUIFloat(KEY_MINOR_STROKE, 0.45f);
        float majorStroke = ChartAssets.getUIFloat(KEY_MAJOR_STROKE, 0.95f);
        float centerStroke = ChartAssets.getUIFloat(KEY_CENTER_STROKE, 1.1f);

        ArberColor minorColor = ColorRegistry.applyAlpha(base, minorAlpha);
        ArberColor majorColor = ColorRegistry.applyAlpha(base, majorAlpha);
        ArberColor centerColor = ColorRegistry.applyAlpha(theme.getAxisLabelColor(), centerAlpha);

        // Minor grid: draw only if enough pixel spacing (avoid moiré)
        mapToPixel(context, minX + stepXMinor, minY, d1);
        mapToPixel(context, minX, minY, d2);
        double pixelDistX = Math.abs(d1[0] - d2[0]);
        mapToPixel(context, minX, minY + stepYMinor, d1);
        double pixelDistY = Math.abs(d1[1] - d2[1]);

        canvas.setColor(minorColor);
        canvas.setStroke(ChartScale.scale(minorStroke));
        drawGrid(canvas, context, minX, maxX, minY, maxY,
                pixelDistX > ChartScale.scale(5) ? stepXMinor : 0.0,
                pixelDistY > ChartScale.scale(5) ? stepYMinor : 0.0);

        canvas.setColor(majorColor);
        canvas.setStroke(ChartScale.scale(majorStroke));
        drawGrid(canvas, context, minX, maxX, minY, maxY, stepXMajor, stepYMajor);

        if (context.isGridVisibleY() && minY <= 0 && maxY >= 0) {
            canvas.setColor(centerColor);
            canvas.setStroke(ChartScale.scale(centerStroke));
            mapToPixel(context, minX, 0.0, d1);
            mapToPixel(context, maxX, 0.0, d2);
            drawLine(canvas, d1[0], d1[1], d2[0], d2[1]);
        }
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        batchCanvas.render(this, builder, context, config != null ? config : gridBatchConfig);
    }

    private void drawGrid(ArberCanvas canvas, PlotContext context, double minX, double maxX, double minY, double maxY,
                          double stepX, double stepY) {
        if (context.isGridVisibleX() && Double.isFinite(stepX) && stepX > 0) {
            double startX = Math.ceil(minX / stepX) * stepX;
            for (int i = 0; i < 10000; i++) {
                double x = Math.fma(i, stepX, startX);
                if (x > maxX || !Double.isFinite(x)) break;
                mapToPixel(context, x, minY, d1);
                mapToPixel(context, x, maxY, d2);
                drawLine(canvas, d1[0], d1[1], d2[0], d2[1]);
            }
        }
        if (context.isGridVisibleY() && Double.isFinite(stepY) && stepY > 0) {
            double startY = Math.ceil(minY / stepY) * stepY;
            for (int i = 0; i < 10000; i++) {
                double y = Math.fma(i, stepY, startY);
                if (y > maxY || !Double.isFinite(y)) break;
                mapToPixel(context, minX, y, d1);
                mapToPixel(context, maxX, y, d2);
                drawLine(canvas, d1[0], d1[1], d2[0], d2[1]);
            }
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
