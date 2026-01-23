package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.function.Function;

/**
 * Ventilator waveform renderer: visualizes pressure, volume, and flow curves.
 * Renders three synchronized traces with zero-allocation hot paths.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class VentilatorWaveformRenderer extends BaseRenderer {
    /**
     * Name translation function. Can be set by end users.
     * Default returns the key unchanged (no translation).
     */
    private static Function<String, String> nameTranslator = key -> key;
    private final Path2D.Double[] renderPaths = {new Path2D.Double(), new Path2D.Double(), new Path2D.Double()};
    private final Color[] colors = new Color[3];
    private final BasicStroke stroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    // Cached coordinate arrays as members to avoid allocations in the render loop.
    private final double[] sharedPixelCoord = new double[2];
    private transient int themeKey;

    public VentilatorWaveformRenderer() {
        super("ventilator_waveform");
    }

    /**
     * Allows end users to set a custom name translator.
     * Example: {@code VentilatorWaveformRenderer.setNameTranslator(key -> MyI18n.get(key));}
     */
    public static void setNameTranslator(Function<String, String> translator) {
        nameTranslator = translator != null ? translator : key -> key;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = resolveTheme(context);
        ensureColors(theme);
        int totalPoints = fastModel.getPointCount();
        if (totalPoints < 2) return;
        for (Path2D.Double p : renderPaths) p.reset();
        for (int i = 0; i < totalPoints; i++) {
            double x = fastModel.getX(i);
            for (int c = 0; c < 3; c++) {
                double yVal = fastModel.getY(i, c);
                context.mapToPixel(x, yVal, sharedPixelCoord);
                double xPix = sharedPixelCoord[0];
                double yPix = sharedPixelCoord[1];
                if (i == 0) {
                    renderPaths[c].moveTo(xPix, yPix);
                } else {
                    renderPaths[c].lineTo(xPix, yPix);
                }
            }
        }
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int c = 0; c < 3; c++) {
            g2.setColor(colors[c]);
            g2.setStroke(stroke);
            g2.draw(renderPaths[c]);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
    }

    @Override
    public String getName() {
        String key = "renderer." + getClass().getSimpleName().toLowerCase();
        return nameTranslator.apply(key);
    }

    private void ensureColors(ChartTheme theme) {
        int key = System.identityHashCode(theme);
        if (key == themeKey && colors[0] != null) return;
        themeKey = key;
        for (int i = 0; i < colors.length; i++) {
            colors[i] = theme.getSeriesColor(i);
        }
    }
}
