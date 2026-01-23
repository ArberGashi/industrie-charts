package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * VCG renderer: visualizes a vectorcardiogram (VCG) as a 2D loop (X-Y projection).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class VCGRenderer extends BaseRenderer {
    // Cached objects to avoid allocations during rendering.
    private final Path2D.Double renderPath = new Path2D.Double();
    private final BasicStroke stroke = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    // Zero-GC: cached coordinate array.
    private final double[] sharedCoord = new double[2];
    public VCGRenderer() {
        super("vcg");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        // Use the fast model interface.
        if (!(model instanceof FastMedicalModel fastModel)) return;
        int size = fastModel.getPointCount();
        if (size < 2) return;
        renderPath.reset();
        for (int i = 0; i < size; i++) {
            // In VCG, X and Y are often different leads (e.g., Lead I and aVF).
            double xVal = fastModel.getX(i);
            double yVal = fastModel.getY(i, 0); // Channel 0 used for VCG Y-axis.
            context.mapToPixel(xVal, yVal, sharedCoord);
            if (i == 0) {
                renderPath.moveTo(sharedCoord[0], sharedCoord[1]);
            } else {
                renderPath.lineTo(sharedCoord[0], sharedCoord[1]);
            }
        }
        // High-quality rendering for medical diagnostics.
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color lineColor = (model.getColor() != null) ? model.getColor() : themeSeries(context, getLayerIndex());
        g2.setColor(lineColor);
        g2.setStroke(stroke);
        g2.draw(renderPath);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
    }

    @Override
    public String getName() {
        return "VCG";
    }
}
