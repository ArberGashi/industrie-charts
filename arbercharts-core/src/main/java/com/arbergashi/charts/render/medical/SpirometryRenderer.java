package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Spirometry renderer: visualizes flow-volume loops for pulmonary diagnostics.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class SpirometryRenderer extends BaseRenderer {

    // Zero-GC: cached members.
    private final Path2D.Double renderPath = new Path2D.Double();
    private transient Color spiroColor;
    private transient int themeKey;
    private final BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final double[] sharedCoord = new double[2];
    public SpirometryRenderer() {
        super("spirometry");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = resolveTheme(context);
        ensureColors(theme);
        int size = fastModel.getPointCount();
        if (size < 2) return;
        renderPath.reset();
        for (int i = 0; i < size; i++) {
            // X = volume, Y = flow.
            double volX = fastModel.getX(i);
            double flowY = fastModel.getY(i, 0);
            context.mapToPixel(volX, flowY, sharedCoord);
            if (i == 0) {
                renderPath.moveTo(sharedCoord[0], sharedCoord[1]);
            } else {
                renderPath.lineTo(sharedCoord[0], sharedCoord[1]);
            }
        }
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(spiroColor);
        g2.setStroke(stroke);
        g2.draw(renderPath);
        // Optional: lightly fill the area under the curve.
        // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        // g2.fill(renderPath);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
    }

    @Override
    public String getName() {
        return "Spirometry";
    }

    private void ensureColors(ChartTheme theme) {
        int key = System.identityHashCode(theme);
        if (key == themeKey && spiroColor != null) return;
        themeKey = key;
        spiroColor = theme.getSeriesColor(getLayerIndex());
    }
}
