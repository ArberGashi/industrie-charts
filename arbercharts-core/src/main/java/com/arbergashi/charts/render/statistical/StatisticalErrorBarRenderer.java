package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Statistical Error Bar Renderer (JDK 25 Standard).
 * Extends plots with horizontal and vertical error bars.
 * Supports standard deviation, confidence intervals and custom ranges.
 * Data Mapping (ChartPoint):
 * x       → Central X position
 * y       → Central Y position
 * min     → Lower bound (Y) for vertical error
 * max     → Upper bound (Y) for vertical error
 * weight  → Horizontal error extent (+/-) from x
 * Features:
 * - Vertical and horizontal error bars
 * - Customizable cap width
 * - High-DPI support
 * - Clipping optimization
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public class StatisticalErrorBarRenderer extends BaseRenderer {

    // Buffer fields to avoid per-iteration allocation
    private final double[] centerBuf = new double[2];
    private final double[] topBuf = new double[2];
    private final double[] bottomBuf = new double[2];
    private final double[] leftBuf = new double[2];
    private final double[] rightBuf = new double[2];
    private boolean showVertical = true;
    private boolean showHorizontal = false;
    private float capWidth = 8.0f;
    public StatisticalErrorBarRenderer() {
        super("errorbar");
    }

    /**
     * Sets whether vertical error bars (using min/max) are rendered.
     */
    public void setShowVertical(boolean showVertical) {
        this.showVertical = showVertical;
    }

    /**
     * Sets whether horizontal error bars (using weight as +/- x error) are rendered.
     */
    public void setShowHorizontal(boolean showHorizontal) {
        this.showHorizontal = showHorizontal;
    }

    /**
     * Sets the cap width in logical pixels (scaled for HiDPI).
     */
    public void setCapWidth(float capWidth) {
        this.capWidth = capWidth;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        g2.setStroke(getCachedStroke(ChartScale.scale(1.0f)));

        double scaledCap = ChartScale.scale(capWidth) / 2.0;
        final Color color = getSeriesColor(model);
        int n = model.getPointCount();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            context.mapToPixel(x, y, centerBuf);
            Color pointColor = isMultiColor() ? themeSeries(context, i) : color;
            if (pointColor == null) pointColor = color;
            g2.setColor(pointColor);

            if (showVertical) {
                context.mapToPixel(x, model.getMax(i), topBuf);
                context.mapToPixel(x, model.getMin(i), bottomBuf);

                g2.draw(getLine(centerBuf[0], topBuf[1], centerBuf[0], bottomBuf[1]));
                g2.draw(getLine(centerBuf[0] - scaledCap, topBuf[1], centerBuf[0] + scaledCap, topBuf[1]));
                g2.draw(getLine(centerBuf[0] - scaledCap, bottomBuf[1], centerBuf[0] + scaledCap, bottomBuf[1]));
            }

            if (showHorizontal) {
                double errX = model.getWeight(i);
                if (errX > 0) {
                    context.mapToPixel(x - errX, y, leftBuf);
                    context.mapToPixel(x + errX, y, rightBuf);

                    g2.draw(getLine(leftBuf[0], centerBuf[1], rightBuf[0], centerBuf[1]));
                    g2.draw(getLine(leftBuf[0], centerBuf[1] - scaledCap, leftBuf[0], centerBuf[1] + scaledCap));
                    g2.draw(getLine(rightBuf[0], centerBuf[1] - scaledCap, rightBuf[0], centerBuf[1] + scaledCap));
                }
            }

            // Allocation-free highlight point
            double size = ChartScale.scale(8.0);
            g2.setColor(ColorUtils.withAlpha(pointColor, 0.5f));
            g2.fill(getEllipse(centerBuf[0] - size, centerBuf[1] - size, size * 2, size * 2));

            g2.setColor(pointColor);
            g2.fill(getEllipse(centerBuf[0] - size / 2, centerBuf[1] - size / 2, size, size));

            g2.setColor(themeBackground(context));
            g2.fill(getEllipse(centerBuf[0] - size / 4, centerBuf[1] - size / 4, size / 2, size / 2));

            g2.setColor(pointColor);
        }
    }
}
