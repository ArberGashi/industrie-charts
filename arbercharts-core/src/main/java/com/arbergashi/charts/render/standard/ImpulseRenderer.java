package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

/**
 * Impulse/Stem Plot Renderer: line from a baseline to each point plus an optional dot.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class ImpulseRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public ImpulseRenderer() {
        super("impulse");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        ArberColor c = getSeriesColor(model);
        double baselineValue = 0.0;
        String baselineStr = ChartAssets.getString("chart.render.impulse.baseline", "0");
        try {
            baselineValue = Double.parseDouble(baselineStr.trim().replace(',', '.'));
        } catch (Exception ignored) {
        }

        context.mapToPixel(0, baselineValue, p0);
        ArberRect bounds = context.getPlotBounds();
        double baselineY = MathUtils.clamp(p0[1], bounds.minY(), bounds.maxY());

        float w = ChartAssets.getFloat("chart.render.impulse.width", 1.2f);
        float sw = ChartScale.scale(w);
        canvas.setStroke(sw);
        canvas.setColor(c);

        boolean showDot = ChartAssets.getBoolean("chart.render.impulse.dot", true);
        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.impulse.radius", 2.8f));

        if (ChartAssets.getBoolean("chart.render.impulse.showBaseline", false)) {
            canvas.setColor(themeGrid(context));
            float[] bx = { (float) bounds.minX(), (float) bounds.maxX() };
            float[] by = { (float) baselineY, (float) baselineY };
            canvas.drawPolyline(bx, by, 2);
            canvas.setColor(c);
        }

        float[] lineX = RendererAllocationCache.getFloatArray(this, "impulse.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "impulse.line.y", 2);
        float[] dotX = RendererAllocationCache.getFloatArray(this, "impulse.dot.x", 8);
        float[] dotY = RendererAllocationCache.getFloatArray(this, "impulse.dot.y", 8);

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            double x = p0[0];
            double y = p0[1];

            lineX[0] = (float) x;
            lineY[0] = (float) baselineY;
            lineX[1] = (float) x;
            lineY[1] = (float) y;
            canvas.drawPolyline(lineX, lineY, 2);
            if (showDot) {
                buildOctagon((float) x, (float) y, (float) r, dotX, dotY);
                canvas.fillPolygon(dotX, dotY, 8);
            }
        }
    }

    private static void buildOctagon(float cx, float cy, float r, float[] xs, float[] ys) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0 / 8.0);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
    }
}
