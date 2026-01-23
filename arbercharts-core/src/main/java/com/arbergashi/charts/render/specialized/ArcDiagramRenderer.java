package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Arc diagram renderer: draws arcs between points laid out on a single axis.
 * Optimized to reuse Path2D and avoid allocations in the draw loop.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ArcDiagramRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("arc_diagram", new RendererDescriptor("arc_diagram", "renderer.arc_diagram", "/icons/arc.svg"), ArcDiagramRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient Path2D.Double arcPath;

    public ArcDiagramRenderer() {
        super("arc_diagram");
    }

    private Path2D.Double getArcPath() {
        if (arcPath == null) arcPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
        return arcPath;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle2D bounds = context.plotBounds();
        Rectangle clip = g2.getClipBounds();
        double baseY = bounds.getY() + bounds.getHeight() * 0.7;

        // Precompute pixel x positions
        double[] xs = RendererAllocationCache.getDoubleArray(this, "xs", count);
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], pBuffer);
            xs[i] = pBuffer[0];
        }

        g2.setStroke(getSeriesStroke());
        Color baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            g2.setColor(baseColor);
        }

        Path2D.Double path = getArcPath();
        // Draw arcs limited to a small neighbor window and clip visibility test
        int neighborLimit = Math.min(30, Math.max(1, count / 15));
        // adapt neighbor window based on density: reduce work for very large n
        if (count > 2000) neighborLimit = Math.min(neighborLimit, 5);
        if (count > 8000) neighborLimit = 2;
        // decimation: skip points when extremely dense
        int decimation = 1;
        if (count > 3000) decimation = (int) Math.ceil(count / 2000.0);
        for (int i = 0; i < count; i++) {
            if ((i % decimation) != 0) continue;
            int maxJ = Math.min(count - 1, i + neighborLimit);
            for (int j = i + 1; j <= maxJ; j++) {
                double x1 = xs[i];
                double x2 = xs[j];
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                if (clip != null && (maxX < clip.getX() || minX > (clip.getX() + clip.getWidth()))) continue;

                // skip very short arcs when many points
                if (count > 3000 && Math.abs(maxX - minX) < 2.0) continue;
                double mid = (x1 + x2) / 2.0;
                // JDK 25: Use Math.clamp() for arc height calculation
                double height = MathUtils.clamp((maxX - minX) / 2.0, 6, bounds.getHeight() / 2.0);

                if (isMultiColor()) {
                    Color arcColor = themeSeries(context, i);
                    if (arcColor == null) arcColor = baseColor;
                    g2.setColor(arcColor);
                }
                path.reset();
                path.moveTo(x1, baseY);
                path.quadTo(mid, baseY - height, x2, baseY);
                g2.draw(path);
            }
        }
    }
}
