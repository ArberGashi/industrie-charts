package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Rug plot renderer: short ticks on the X-axis to show distributions.
 * Very cheap to render and useful as additional context.
 *
 * @author Arber Gashi
 * @version 1.0.1
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class RugPlotRenderer extends BaseRenderer {

    public RugPlotRenderer() {
        super("rug");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        double len = ChartScale.scale(ChartAssets.getFloat("chart.render.rug.length", 8.0f));
        float w = ChartAssets.getFloat("chart.render.rug.width", 1.5f);

        ChartTheme theme = getResolvedTheme(context);
        ArberColor c = ColorUtils.applyAlpha(theme.getAxisLabelColor(), 0.75f);
        canvas.setStroke(ChartScale.scale(w));

        // Always draw at the bottom of the visible plot area, regardless of data range.
        ArberRect bounds = context.getPlotBounds();
        double y0 = bounds.y() + bounds.height();

        double[] xs = model.getXData();
        double[] buf = pBuffer();
        for (int i = 0; i < n; i++) {
            // We only care about the x-value for a rug plot.
            context.mapToPixel(xs[i], 0, buf);
            double x = buf[0];

            // Simple clipping
            if (x < bounds.x() || x > bounds.maxX()) {
                continue;
            }

            ArberColor tickColor = isMultiColor() ? themeSeries(context, i) : c;
            if (tickColor == null) tickColor = c;
            canvas.setColor(tickColor);
            drawLine(canvas, x, y0, x, y0 - len);
        }
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "rug.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "rug.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

}
