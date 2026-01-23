package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Locally weighted scatter-plot smoothing (LOESS) renderer.
 *
 * <p>This renderer draws a smoothed trend curve for noisy time series. It uses a lightweight,
 * allocation-free sliding-window weighted average approximation that is stable under high zoom.
 * The implementation is intentionally conservative to keep the hot path zero-allocation.</p>
 *
 * <p><b>Performance:</b> O(n) for n points; uses one reusable {@link java.awt.geom.Path2D} per renderer instance.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class LoessRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public LoessRenderer() {
        super("loess");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Color base = seriesOrBase(model, context, 0);
        Color accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;
        g2.setStroke(getSeriesStroke());

        // Window size (odd, >= 3)
        int w = Math.max(3, Math.min(101, (int) Math.round(Math.sqrt(count))));
        if ((w & 1) == 0) w++;
        int half = w / 2;

        Path2D path = getPathCache();
        boolean started = false;

        for (int i = 0; i < count; i++) {
            int a = Math.max(0, i - half);
            int b = Math.min(count - 1, i + half);

            double xi = xData[i];

            // Tri-cubic weights (approx) with stable normalization.
            double sumW = 0.0;
            double sumY = 0.0;
            double x0 = xData[a];
            double x1 = xData[b];
            double span = Math.max(1e-12, x1 - x0);

            for (int j = a; j <= b; j++) {
                double xj = xData[j];
                double t = Math.abs((xj - xi) / span);
                if (t >= 1.0) continue;
                double u = 1.0 - t * t * t;
                double wj = u * u * u;
                sumW += wj;
                sumY = Math.fma(wj, yData[j], sumY);
            }
            double yi = (sumW > 0.0) ? (sumY / sumW) : yData[i];

            context.mapToPixel(xi, yi, pBuffer);
            double px = pBuffer[0];
            double py = pBuffer[1];

            if (!started) {
                path.moveTo(px, py);
                started = true;
            } else {
                path.lineTo(px, py);
            }
        }

        if (!started) return;
        if (isMultiColor() && accent != base) {
            g2.setColor(ColorUtils.withAlpha(accent, 0.45f));
            g2.draw(path);
        }
        g2.setColor(base);
        g2.draw(path);
    }
}
