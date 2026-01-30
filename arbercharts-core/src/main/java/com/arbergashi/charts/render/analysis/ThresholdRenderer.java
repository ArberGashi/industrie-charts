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
 * <h1>ThresholdRenderer - Visual Threshold Indicator</h1>
 *
 * <p>Enterprise-grade threshold renderer for highlighting regions above or
 * below a critical value with semi-transparent fills and optional reference lines.</p>
 *
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li><b>Render Time:</b> {@code &lt; 0.5ms} (constant time)</li>
 *   <li><b>Complexity:</b> O(1) - draws single rectangle</li>
 *   <li><b>Memory:</b> Zero allocations (shape pooling)</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ThresholdRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    private double cachedY;
    private String lastYStr;

    public ThresholdRenderer() {
        super("threshold");
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
        return false; // Overlay renderer
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        String yStr = ChartAssets.getString("chart.render.threshold.y", "0");
        if (lastYStr == null || !lastYStr.equals(yStr)) {
            cachedY = parseDoubleSafe(yStr, 0.0);
            lastYStr = yStr;
        }
        double y = cachedY;
        String mode = ChartAssets.getString("chart.render.threshold.mode", "above");

        context.mapToPixel(0, y, pBuffer);
        double py = pBuffer[1];
        // JDK 25: Use Math.clamp() to constrain y position to bounds
        ArberRect bounds = context.getPlotBounds();
        py = Math.clamp(py, bounds.y(), bounds.maxY());

        final ChartTheme theme = getResolvedTheme(context);
        ArberColor base = isMultiColor() ? themeSeries(context, 0) : theme.getAccentColor();
        if (base == null) base = theme.getAccentColor();
        float alpha = ChartAssets.getFloat("chart.render.threshold.alpha", 0.08f);
        ArberColor fill = base;

        float rx;
        float ry;
        float rw;
        float rh;
        if ("below".equalsIgnoreCase(mode)) {
            rx = (float) bounds.x();
            ry = (float) py;
            rw = (float) bounds.width();
            rh = (float) (bounds.maxY() - py);
        } else {
            rx = (float) bounds.x();
            ry = (float) bounds.y();
            rw = (float) bounds.width();
            rh = (float) (py - bounds.y());
        }

        canvas.setColor(fill);
        canvas.fillRect(rx, ry, rw, rh);

        // Optional: draw the threshold line itself
        if (ChartAssets.getBoolean("chart.render.threshold.line", true)) {
            float w = ChartAssets.getFloat("chart.render.threshold.width", 1.0f);
            canvas.setStroke(ChartScale.scale(w));
            canvas.setColor(theme.getAxisLabelColor());
            float[] xs = RendererAllocationCache.getFloatArray(this, "threshold.line.x", 2);
            float[] ys = RendererAllocationCache.getFloatArray(this, "threshold.line.y", 2);
            xs[0] = (float) bounds.x();
            ys[0] = (float) py;
            xs[1] = (float) bounds.maxX();
            ys[1] = (float) py;
            canvas.drawPolyline(xs, ys, 2);
        }
    }
}
