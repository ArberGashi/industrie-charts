package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * Professional, zero-allocation ECDF (Empirical CDF) Renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class ECDFRenderer extends BaseRenderer {

    public ECDFRenderer() {
        super("ecdf");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return new double[]{0.0, 1.02};
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "ecdf.values", n);
        for (int i = 0; i < n; i++) {
            values[i] = model.getY(i);
        }
        java.util.Arrays.sort(values, 0, n);

        double[] buf = pBuffer();
        Color c = seriesOrBase(model, context, 0);
        float alpha = ChartAssets.getFloat("chart.render.ecdf.alpha", 0.9f);
        float w = ChartAssets.getFloat("chart.render.ecdf.width", 2.0f);
        Stroke stroke = getCachedStroke(ChartScale.scale(w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g2.setStroke(stroke);
        if (!isMultiColor()) {
            Path2D path = getPathCache();
            boolean moved = false;
            path.reset();
            for (int i = 0; i < n; i++) {
                double x = values[i];
                double y = (double) (i + 1) / n;

                context.mapToPixel(x, y, buf);
                if (!moved) {
                    path.moveTo(buf[0], buf[1]);
                    moved = true;
                } else {
                    path.lineTo(buf[0], buf[1]);
                }
            }
            g2.setColor(ColorUtils.withAlpha(c, alpha));
            g2.draw(path);
            return;
        }

        double prevX = Double.NaN;
        double prevY = Double.NaN;
        for (int i = 0; i < n; i++) {
            double x = values[i];
            double y = (double) (i + 1) / n;
            context.mapToPixel(x, y, buf);
            if (i > 0) {
                Color segColor = themeSeries(context, i);
                if (segColor == null) segColor = c;
                g2.setColor(ColorUtils.withAlpha(segColor, alpha));
                g2.draw(getLine(prevX, prevY, buf[0], buf[1]));
            }
            prevX = buf[0];
            prevY = buf[1];
        }
    }
}
