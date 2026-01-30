package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Draws a clinical calibration pulse (e.g., 1 mV / 200 ms).
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class CalibrationRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.calibration.enabled";
    private static final String KEY_MV = "Chart.calibration.mv";
    private static final String KEY_MS = "Chart.calibration.ms";
    private static final String KEY_MARGIN_MM = "Chart.calibration.marginMm";
    private static final String KEY_ALPHA = "Chart.calibration.alpha";
    private static final String KEY_STROKE = "Chart.calibration.strokeWidth";
    private static final String KEY_COLOR = "Chart.calibration.color";
    private static final String KEY_PIXELS_PER_MM = "Chart.calibration.pixelsPerMm";

    private final double[] dataBuf = new double[2];
    private final double[] px0 = new double[2];
    private final double[] px1 = new double[2];
    private final double[] px2 = new double[2];
    private final double[] px3 = new double[2];
    private final double[] px4 = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    public CalibrationRenderer() {
        super("calibration");
    }

    @Override
    public boolean supportsEmptyState() {
        return true;
    }

    @Override
    public void renderEmptyState(ArberCanvas canvas, ChartModel model, PlotContext context) {
        drawCalibration(canvas, context);
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        drawCalibration(canvas, context);
    }

    private void drawCalibration(ArberCanvas canvas, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, false)) {
            return;
        }

        ArberRect pb = context.getPlotBounds();
        if (pb == null || pb.width() <= 0.0 || pb.height() <= 0.0) {
            return;
        }

        double pixelsPerMm = ChartAssets.getFloat(KEY_PIXELS_PER_MM, 3.78f);
        if (!Double.isFinite(pixelsPerMm) || pixelsPerMm <= 0.0) {
            return;
        }

        double marginMm = ChartAssets.getFloat(KEY_MARGIN_MM, 6.0f);
        double marginPx = pixelsPerMm * marginMm;
        double anchorPxX = pb.x() + marginPx;
        double anchorPxY = pb.maxY() - marginPx;

        context.mapToData(anchorPxX, anchorPxY, dataBuf);
        double startX = dataBuf[0];
        double startY = dataBuf[1];

        double widthUnits = ChartAssets.getFloat(KEY_MS, 200.0f) / 1000.0;
        double heightUnits = ChartAssets.getFloat(KEY_MV, 1.0f);
        double tailUnits = widthUnits * 0.25;

        double x1 = startX;
        double y1 = startY + heightUnits;
        double x2 = startX + widthUnits;
        double y2 = y1;
        double x3 = x2;
        double y3 = startY;
        double x4 = x3 + tailUnits;
        double y4 = startY;

        context.mapToPixel(startX, startY, px0);
        context.mapToPixel(x1, y1, px1);
        context.mapToPixel(x2, y2, px2);
        context.mapToPixel(x3, y3, px3);
        context.mapToPixel(x4, y4, px4);

        ChartTheme theme = (context.getTheme() != null) ? context.getTheme() : getTheme();
        ArberColor base = ChartAssets.getColor(KEY_COLOR,
                theme != null ? theme.getAxisLabelColor() : ColorRegistry.of(160, 160, 160, 255));
        float alpha = ChartAssets.getFloat(KEY_ALPHA, 0.92f);
        ArberColor strokeColor = ColorRegistry.applyAlpha(base, alpha);
        float strokeWidth = ChartAssets.getFloat(KEY_STROKE, 1.2f);

        canvas.setColor(strokeColor);
        canvas.setStroke(ChartScale.scale(strokeWidth));
        lineX[0] = (float) context.snapPixel(px0[0]);
        lineY[0] = (float) context.snapPixel(px0[1]);
        lineX[1] = (float) context.snapPixel(px1[0]);
        lineY[1] = (float) context.snapPixel(px1[1]);
        canvas.drawPolyline(lineX, lineY, 2);

        lineX[0] = (float) context.snapPixel(px1[0]);
        lineY[0] = (float) context.snapPixel(px1[1]);
        lineX[1] = (float) context.snapPixel(px2[0]);
        lineY[1] = (float) context.snapPixel(px2[1]);
        canvas.drawPolyline(lineX, lineY, 2);

        lineX[0] = (float) context.snapPixel(px2[0]);
        lineY[0] = (float) context.snapPixel(px2[1]);
        lineX[1] = (float) context.snapPixel(px3[0]);
        lineY[1] = (float) context.snapPixel(px3[1]);
        canvas.drawPolyline(lineX, lineY, 2);

        lineX[0] = (float) context.snapPixel(px3[0]);
        lineY[0] = (float) context.snapPixel(px3[1]);
        lineX[1] = (float) context.snapPixel(px4[0]);
        lineY[1] = (float) context.snapPixel(px4[1]);
        canvas.drawPolyline(lineX, lineY, 2);
    }
}
