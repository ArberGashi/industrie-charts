package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import javax.swing.*;
import java.awt.*;

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
 */
public final class BulletChartRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("bullet", new RendererDescriptor("bullet", "renderer.bullet", "/icons/bullet.svg"), BulletChartRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient Color[] rangeColors;
    private transient Color actualColor;
    private transient Color targetColor;
    private transient int uiKey;
    private transient boolean multiColorKey;

    public BulletChartRenderer() {
        super("bullet");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ensureUiColors(context);

        Rectangle bounds = context.plotBounds().getBounds();
        double w = bounds.getWidth();
        double h = bounds.getHeight();

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

        double y = bounds.getY() + h / 2.0;
        double barH = Math.max(10.0, h / 6.0);

        for (int i = 0; i < ranges.length; i++) {
            double rw = ranges[i];
            g2.setColor(rangeColors[i]);
            g2.fill(getRect(bounds.getX(), y - barH / 2.0, rw, barH));
        }

        // actual bar
        g2.setColor(actualColor);
        g2.fill(getRect(bounds.getX(), y - barH / 4.0, actualX, barH / 2.0));

        // target marker
        g2.setColor(targetColor);
        g2.draw(getLine(targetX, y - barH / 2.0, targetX, y + barH / 2.0));
    }

    private void ensureUiColors(PlotContext context) {
        int key = System.identityHashCode(UIManager.getDefaults());
        boolean multi = isMultiColor();
        if (key == uiKey && rangeColors != null && multiColorKey == multi) return;
        uiKey = key;
        multiColorKey = multi;

        rangeColors = new Color[3];
        if (multi) {
            Color c0 = themeSeries(context, 0);
            Color c1 = themeSeries(context, 1);
            Color c2 = themeSeries(context, 2);
            if (c0 == null) c0 = themeAccent(context);
            if (c1 == null) c1 = c0;
            if (c2 == null) c2 = c1;
            rangeColors[0] = com.arbergashi.charts.util.ColorUtils.withAlpha(c0, 0.35f);
            rangeColors[1] = com.arbergashi.charts.util.ColorUtils.withAlpha(c1, 0.5f);
            rangeColors[2] = com.arbergashi.charts.util.ColorUtils.withAlpha(c2, 0.65f);

            actualColor = themeSeries(context, 3);
            if (actualColor == null) actualColor = themeAccent(context);
            targetColor = themeSeries(context, 4);
            if (targetColor == null) targetColor = themeAccent(context);
            return;
        }

        Color c1 = UIManager.getColor("Chart.bullet.range1");
        Color c2 = UIManager.getColor("Chart.bullet.range2");
        Color c3 = UIManager.getColor("Chart.bullet.range3");

        if (c1 != null && c2 != null && c3 != null) {
            rangeColors[0] = c1;
            rangeColors[1] = c2;
            rangeColors[2] = c3;
        } else {
            // Fallback: Adapt to dark/light theme based on panel background
            Color bg = UIManager.getColor("Panel.background");
            if (bg == null) bg = themeBackground(context);
            boolean dark = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 < 128;

            if (dark) {
                rangeColors[0] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.45f);
                rangeColors[1] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.6f);
                rangeColors[2] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.75f);
            } else {
                rangeColors[0] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.35f);
                rangeColors[1] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.5f);
                rangeColors[2] = com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.65f);
            }
        }

        actualColor = UIManager.getColor("Label.foreground"); // Black in light, White in dark
        if (actualColor == null) actualColor = themeForeground(context);

        targetColor = UIManager.getColor("Actions.Red"); // FlatLaf standard red
        if (targetColor == null) targetColor = themeAccent(context);
    }
}
