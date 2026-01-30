package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * <h1>BulletChartRenderer - Compact KPI Visualization</h1>
 *
 * <p>Enterprise-grade bullet chart renderer for comparing actual performance
 * against targets with qualitative performance ranges.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Actual vs Target:</b> Visual comparison of performance</li>
 *   <li><b>Qualitative Ranges:</b> 3-tier background (poor/good/excellent)</li>
 *   <li><b>Compact Design:</b> Space-efficient KPI dashboard layout</li>
 *   <li><b>Clear Target Marker:</b> Red line for goal visualization</li>
 *   <li><b>Professional Styling:</b> Grayscale ranges, black bar, red marker</li>
 * </ul>
 *
 * <h2>Data Mapping (ChartPoint):</h2>
 * <pre>
 * Point 0 (required)  → Actual performance value
 * Point 1 (optional)  → Target/goal value
 * Additional points   → Ignored
 * </pre>
 *
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li><b>Render Time:</b> {@code &lt; 1ms} (constant)</li>
 *   <li><b>Complexity:</b> O(1) - fixed number of elements</li>
 *   <li><b>Memory:</b> Zero allocations (shape pooling)</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * @see ChartModel
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class BulletChartRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("bullet", new RendererDescriptor("bullet", "renderer.bullet", "/icons/bullet.svg"), BulletChartRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient ArberColor[] rangeColors;
    private transient ArberColor actualColor;
    private transient ArberColor targetColor;
    private transient int themeKey;
    private transient boolean multiColorKey;

    public BulletChartRenderer() {
        super("bullet");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ensureUiColors(context);

        ArberRect bounds = context.getPlotBounds();
        double w = bounds.width();
        double h = bounds.height();

        // take first point as actual, second as target, rest ignored
        double ax = xData[0];
        double ay = yData[0];
        double tx = (count > 1) ? xData[1] : ax;
        double ty = (count > 1) ? yData[1] : ay;

        context.mapToPixel(ax, ay, pBuffer);
        double actualX = pBuffer[0];

        context.mapToPixel(tx, ty, pBuffer);
        double targetX = pBuffer[0];

        // qualitative ranges: 3 blocks
        double[] ranges = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "bullet.ranges", 3);
        ranges[0] = 0.6 * w;
        ranges[1] = 0.9 * w;
        ranges[2] = w;

        double y = bounds.y() + h / 2.0;
        double barH = Math.max(10.0, h / 6.0);

        for (int i = 0; i < ranges.length; i++) {
            double rw = ranges[i];
            canvas.setColor(rangeColors[i]);
            canvas.fillRect((float) bounds.x(), (float) (y - barH / 2.0), (float) rw, (float) barH);
        }

        // actual bar
        canvas.setColor(actualColor);
        float actualW = (float) Math.max(0.0, actualX - bounds.x());
        canvas.fillRect((float) bounds.x(), (float) (y - barH / 4.0), actualW, (float) (barH / 2.0));

        // target marker
        canvas.setColor(targetColor);
        drawLine(canvas, targetX, y - barH / 2.0, targetX, y + barH / 2.0);
    }

    private void ensureUiColors(PlotContext context) {
        int key = System.identityHashCode(getResolvedTheme(context));
        boolean multi = isMultiColor();
        if (key == themeKey && rangeColors != null && multiColorKey == multi) return;
        themeKey = key;
        multiColorKey = multi;

        rangeColors = new ArberColor[3];
        if (multi) {
            ArberColor c0 = themeSeries(context, 0);
            ArberColor c1 = themeSeries(context, 1);
            ArberColor c2 = themeSeries(context, 2);
            if (c0 == null) c0 = themeAccent(context);
            if (c1 == null) c1 = c0;
            if (c2 == null) c2 = c1;
            rangeColors[0] = ColorRegistry.applyAlpha(c0, 0.35f);
            rangeColors[1] = ColorRegistry.applyAlpha(c1, 0.5f);
            rangeColors[2] = ColorRegistry.applyAlpha(c2, 0.65f);

            actualColor = themeSeries(context, 3);
            if (actualColor == null) actualColor = themeAccent(context);
            targetColor = themeSeries(context, 4);
            if (targetColor == null) targetColor = themeAccent(context);
            return;
        }

        rangeColors[0] = ColorRegistry.applyAlpha(themeGrid(context), 0.35f);
        rangeColors[1] = ColorRegistry.applyAlpha(themeGrid(context), 0.5f);
        rangeColors[2] = ColorRegistry.applyAlpha(themeGrid(context), 0.65f);

        actualColor = themeForeground(context);
        targetColor = themeAccent(context);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "bullet.line.x", 2);
        float[] ys = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "bullet.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }
}
