package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Optional;

/**
 * Enterprise ErrorBar Renderer - ArberGashi Engine.
 * Visualizes scientific uncertainties without hardcoded font values.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public class ErrorBarRenderer extends BaseRenderer {

    private final double[] selectionBuf = new double[2];
    private final double[] pixMidBuf = new double[2];
    private final double[] pixMaxBuf = new double[2];
    private Path2D lastRenderedBars;

    public ErrorBarRenderer() {
        super("errorbar");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int count0 = model.getPointCount();
        if (count0 == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] maxData = model.getHighData();
        double[] minData = model.getLowData();

        int count = count0;
        count = Math.min(count, xData.length);
        count = Math.min(count, yData.length);
        count = Math.min(count, maxData.length);
        count = Math.min(count, minData.length);
        if (count == 0) return;

        float strokeWidth = com.arbergashi.charts.util.ChartScale.scale(1.2f);
        g.setStroke(getCachedStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        double capWidth = com.arbergashi.charts.util.ChartScale.scale(8.0);

        final double[] pixMax = this.pixMaxBuf;
        final double[] pixMin = this.selectionBuf;

        // Draw each error bar with its own color
        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double yMaxD = maxData[i];
            double yMinD = minData[i];
            if (!Double.isFinite(x) || !Double.isFinite(yMaxD) || !Double.isFinite(yMinD)) continue;

            context.mapToPixel(x, yMaxD, pixMax);
            context.mapToPixel(x, yMinD, pixMin);

            if (!Double.isFinite(pixMax[0]) || !Double.isFinite(pixMax[1]) || !Double.isFinite(pixMin[0]) || !Double.isFinite(pixMin[1])) continue;

            double px = pixMax[0];
            double yMax = pixMax[1];
            double yMin = pixMin[1];

            // Each error bar gets a distinct color from the theme palette
            Color errorColor = seriesOrBase(model, context, i);
            g.setColor(errorColor);

            Path2D.Double path = (Path2D.Double) getPathCache();
            path.reset();

            path.moveTo(px, yMax);
            path.lineTo(px, yMin);

            double halfCap = capWidth / 2.0;
            path.moveTo(px - halfCap, yMax);
            path.lineTo(px + halfCap, yMax);

            path.moveTo(px - halfCap, yMin);
            path.lineTo(px + halfCap, yMin);

            g.draw(path);

            String label = model.getLabel(i);
            if (label != null && !label.isEmpty()) {
                renderSignificance(g, label, px, yMax);
            }

            // Store last path (for hit testing - only stores the last one)
            if (i == count - 1) {
                this.lastRenderedBars = path;
            }
        }
    }

    private void renderSignificance(Graphics2D g, String label, double x, double yMax) {
        Font themeFont = UIManager.getFont("Chart.font");
        if (themeFont == null) themeFont = g.getFont();

        Font font = themeFont.deriveFont(Font.BOLD, ChartScale.uiFontSize(themeFont, 12));
        FontMetrics fm = g.getFontMetrics(font);

        float tx = (float) (x - fm.stringWidth(label) / 2.0);
        float ty = (float) (yMax - ChartScale.scale(6.0));

        // Mandatory label caching
        drawLabel(g, label, font, g.getColor(), tx, ty);
    }

    public Shape getRenderedShape(ChartModel model, PlotContext context) {
        return lastRenderedBars;
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int count0 = model.getPointCount();
        double threshold = ChartScale.scale(10.0);

        final double[] pixMid = this.pixMidBuf;
        final double[] pixMax = this.pixMaxBuf;
        final double[] pixMin = this.selectionBuf;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] maxData = model.getHighData();
        double[] minData = model.getLowData();

        int count = count0;
        count = Math.min(count, xData.length);
        count = Math.min(count, yData.length);
        count = Math.min(count, maxData.length);
        count = Math.min(count, minData.length);
        if (count == 0) return Optional.empty();

        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, pixMid);
            if (!Double.isFinite(pixMid[0]) || !Double.isFinite(pixMid[1])) continue;

            if (Math.abs(pixel.getX() - pixMid[0]) < threshold) {
                double yMaxD = maxData[i];
                double yMinD = minData[i];
                if (!Double.isFinite(yMaxD) || !Double.isFinite(yMinD)) continue;

                context.mapToPixel(x, yMaxD, pixMax);
                context.mapToPixel(x, yMinD, pixMin);

                if (!Double.isFinite(pixMax[1]) || !Double.isFinite(pixMin[1])) continue;

                double minY = Math.min(pixMax[1], pixMin[1]) - threshold;
                double maxY = Math.max(pixMax[1], pixMin[1]) + threshold;

                if (pixel.getY() >= minY && pixel.getY() <= maxY) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }
}
