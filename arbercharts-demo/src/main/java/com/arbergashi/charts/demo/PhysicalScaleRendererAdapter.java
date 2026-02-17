package com.arbergashi.charts.demo;

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

final class PhysicalScaleRendererAdapter extends BaseRenderer {
    PhysicalScaleRendererAdapter() {
        super("physical_scale_adapter");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.width() <= 1 || bounds.height() <= 1) {
            return;
        }
        double pixelsPerMm = ChartAssets.getFloat("Chart.scale.pixelsPerMm", 3.78f);
        if (!Double.isFinite(pixelsPerMm) || pixelsPerMm <= 0.0) {
            return;
        }

        double mm = 10.0;
        double length = pixelsPerMm * mm;
        double margin = ChartScale.scale(18.0);
        double x0 = bounds.x() + margin;
        double y0 = bounds.maxY() - margin;
        double x1 = x0 + length;

        ChartTheme theme = getResolvedTheme(context);
        ArberColor color = theme != null ? theme.getAccentColor() : ColorRegistry.of(77, 163, 255, 255);
        canvas.setColor(color);
        canvas.setStroke(ChartScale.scale(1.8f));
        float[] xs = {(float) x0, (float) x1};
        float[] ys = {(float) y0, (float) y0};
        canvas.drawPolyline(xs, ys, 2);
        float tick = ChartScale.scale(5.0f);
        float[] tx = {(float) x0, (float) x0};
        float[] ty = {(float) (y0 - tick), (float) (y0 + tick)};
        canvas.drawPolyline(tx, ty, 2);
        tx[0] = (float) x1;
        tx[1] = (float) x1;
        canvas.drawPolyline(tx, ty, 2);
    }
}
