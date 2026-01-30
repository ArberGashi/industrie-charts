package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
/**
 * Spirometry renderer: visualizes flow-volume loops for pulmonary diagnostics.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class SpirometryRenderer extends BaseRenderer {

    // Zero-GC: cached members.
    private transient ArberColor spiroColor;
    private transient int themeKey;
    private final double[] sharedCoord = new double[2];
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];
    public SpirometryRenderer() {
        super("spirometry");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof FastMedicalModel fastModel)) return;
        ChartTheme theme = getResolvedTheme(context);
        ensureColors(theme);
        int size = fastModel.getPointCount();
        if (size < 2) return;
        ensureBufferCapacity(size);
        for (int i = 0; i < size; i++) {
            // X = volume, Y = flow.
            double volX = fastModel.getX(i);
            double flowY = fastModel.getY(i, 0);
            context.mapToPixel(volX, flowY, sharedCoord);
            pathX[i] = (float) sharedCoord[0];
            pathY[i] = (float) sharedCoord[1];
        }
        canvas.setColor(spiroColor);
        canvas.setStroke(2.0f);
        canvas.drawPolyline(pathX, pathY, size);
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

    private void ensureBufferCapacity(int capacity) {
        if (pathX.length >= capacity) return;
        int next = 1;
        while (next < capacity && next > 0) next <<= 1;
        if (next <= 0) next = capacity;
        pathX = new float[next];
        pathY = new float[next];
    }
}
