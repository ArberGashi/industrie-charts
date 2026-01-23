package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Quantile regression renderer.
 * Draws multiple percentiles (e.g., 10%, 50%, 90%) as lines.
 * Expects multivariate data in min/max/weight slots for the quantiles.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class QuantileRegressionRenderer extends BaseRenderer {

    public QuantileRegressionRenderer() {
        super("quantileRegression");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count <= 0) return;

        Color baseColor = getSeriesColor(model);
        Color medianColor = isMultiColor() ? themeSeries(context, 0) : baseColor;
        Color highColor = isMultiColor() ? themeSeries(context, 1) : baseColor;
        Color lowColor = isMultiColor() ? themeSeries(context, 2) : baseColor;
        if (medianColor == null) medianColor = baseColor;
        if (highColor == null) highColor = baseColor;
        if (lowColor == null) lowColor = baseColor;

        // 50% Quantil (Median) - component 1
        drawQuantilePath(g2, model, context, medianColor, 2.5f, 1.0f, 1);

        // 90% Quantil - max -> component 4
        drawQuantilePath(g2, model, context, highColor, 1.5f, 0.6f, 4);

        // 10% Quantil - min -> component 3
        drawQuantilePath(g2, model, context, lowColor, 1.5f, 0.6f, 3);
    }

    private void drawQuantilePath(Graphics2D g2, ChartModel model, PlotContext context,
                                  Color color, float width, float alpha, int component) {
        Path2D.Double path = getPathCache();
        path.reset();
        boolean first = true;
        double[] buf = pBuffer();
        int count = model.getPointCount();
        for (int i = 0; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            // If component is not x/y, use getValue fallback
            double val = (component == 0) ? x : (component == 1) ? y : model.getValue(i, component);
            context.mapToPixel(x, val, buf);
            if (first) {
                path.moveTo(buf[0], buf[1]);
                first = false;
            } else {
                path.lineTo(buf[0], buf[1]);
            }
        }

        g2.setColor(ColorUtils.withAlpha(color, alpha));
        g2.setStroke(getCachedStroke(ChartScale.scale(width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }
}
