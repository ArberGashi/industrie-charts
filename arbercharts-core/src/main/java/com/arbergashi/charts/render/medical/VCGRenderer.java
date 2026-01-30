package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.BaseRenderer;
/**
 * VCG renderer: visualizes a vectorcardiogram (VCG) as a 2D loop (X-Y projection).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class VCGRenderer extends BaseRenderer {
    // Cached objects to avoid allocations during rendering.
    // Zero-GC: cached coordinate array.
    private final double[] sharedCoord = new double[2];
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];
    public VCGRenderer() {
        super("vcg");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        // Use the fast model interface.
        if (!(model instanceof FastMedicalModel fastModel)) return;
        int size = fastModel.getPointCount();
        if (size < 2) return;
        ensureBufferCapacity(size);
        for (int i = 0; i < size; i++) {
            // In VCG, X and Y are often different leads (e.g., Lead I and aVF).
            double xVal = fastModel.getX(i);
            double yVal = fastModel.getY(i, 0); // Channel 0 used for VCG Y-axis.
            context.mapToPixel(xVal, yVal, sharedCoord);
            pathX[i] = (float) sharedCoord[0];
            pathY[i] = (float) sharedCoord[1];
        }
        ArberColor lineColor = (model.getColor() != null) ? model.getColor() : themeSeries(context, getLayerIndex());
        canvas.setColor(lineColor);
        canvas.setStroke(1.8f);
        canvas.drawPolyline(pathX, pathY, size);
    }

    @Override
    public String getName() {
        return "VCG";
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
