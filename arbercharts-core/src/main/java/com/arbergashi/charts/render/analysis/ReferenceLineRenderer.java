package com.arbergashi.charts.render.analysis;

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

/**
 * Reference line renderer for horizontal and vertical guide lines.
 *
 * <p>Configuration:
 * <ul>
 *   <li><code>chart.render.refline.y</code> - Y reference line value (default: 0)</li>
 *   <li><code>chart.render.refline.x</code> - X reference line value (optional)</li>
 *   <li><code>chart.render.refline.width</code> - line width (default: 1.0)</li>
 * </ul>
 *
 * @author Arber Gashi
 * @since 2026-01-01
 */
public final class ReferenceLineRenderer extends BaseRenderer {

    public ReferenceLineRenderer() {
        super("referenceLine");
    }

    private static double parseDoubleSafe(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        String yVal = ChartAssets.getString("chart.render.refline.y", "0");
        String xVal = ChartAssets.getString("chart.render.refline.x", "");

        if ((yVal == null || yVal.isBlank()) && (xVal == null || xVal.isBlank())) {
            return;
        }

        final ChartTheme theme = getResolvedTheme(context);
        ArberColor base = isMultiColor() ? themeSeries(context, 0) : theme.getAxisLabelColor();
        if (base == null) {
            base = theme.getAxisLabelColor();
        }
        float w = ChartAssets.getFloat("chart.render.refline.width", 1.0f);
        float sw = ChartScale.scale(w);
        canvas.setColor(base);
        canvas.setStroke(sw);

        ArberRect bounds = context.getPlotBounds();
        float[] xs = RendererAllocationCache.getFloatArray(this, "refline.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "refline.line.y", 2);

        if (yVal != null && !yVal.isBlank()) {
            double y = parseDoubleSafe(yVal, 0.0);
            final double[] pyBuf = pBuffer();
            context.mapToPixel(0.0, y, pyBuf);
            double py = Math.max(bounds.y(), Math.min(bounds.maxY(), pyBuf[1]));
            xs[0] = (float) bounds.x();
            ys[0] = (float) py;
            xs[1] = (float) bounds.maxX();
            ys[1] = (float) py;
            canvas.drawPolyline(xs, ys, 2);
        }

        if (xVal != null && !xVal.isBlank()) {
            double x = parseDoubleSafe(xVal, 0.0);
            final double[] pxBuf = pBuffer();
            context.mapToPixel(x, context.getMinY(), pxBuf);
            double px = Math.max(bounds.x(), Math.min(bounds.maxX(), pxBuf[0]));
            xs[0] = (float) px;
            ys[0] = (float) bounds.y();
            xs[1] = (float) px;
            ys[1] = (float) bounds.maxY();
            canvas.drawPolyline(xs, ys, 2);
        }
    }
}
