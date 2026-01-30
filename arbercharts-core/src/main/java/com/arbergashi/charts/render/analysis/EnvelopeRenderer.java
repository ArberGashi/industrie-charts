package com.arbergashi.charts.render.analysis;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * Envelope renderer.
 *
 * <p>Draws an upper/lower envelope using a sliding window min/max of Y values.
 * Useful for quick volatility visualization.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class EnvelopeRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    // Buffers for Monotonic Queues (indices)
    // Window is capped at 201, so 256 is safe capacity
    private int[] minQ = new int[256];
    private int[] maxQ = new int[256];
    private double[] lowerX = new double[256];
    private double[] lowerY = new double[256];

    public EnvelopeRenderer() {
        super("envelope");
    }

    /**
     * Renders the envelope band using a sliding window min/max.
     *
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        int count = Math.min(model.getPointCount(), Math.min(xData.length, yData.length));
        if (count < 3) return;

        int window = Math.max(3, Math.min(201, (int) Math.round(Math.sqrt(count))));
        int half = window / 2;

        // Ensure capacity for buffers
        if (lowerX.length < count) {
            lowerX = RendererAllocationCache.getDoubleArray(this, "lowerX", count);
            lowerY = RendererAllocationCache.getDoubleArray(this, "lowerY", count);
        }
        if (minQ.length < count) {
            minQ = RendererAllocationCache.getIntArray(this, "env.minQ", count);
            maxQ = RendererAllocationCache.getIntArray(this, "env.maxQ", count);
        }

        int lowerCount = 0;
        float[] upperX = RendererAllocationCache.getFloatArray(this, "env.upper.x", count);
        float[] upperY = RendererAllocationCache.getFloatArray(this, "env.upper.y", count);
        int upperCount = 0;

        // Monotonic Queue State
        int minH = 0, minT = 0; // Head, Tail
        int maxH = 0, maxT = 0;
        int left = 0;
        int right = -1;

        for (int i = 0; i < count; i++) {
            int targetLeft = Math.max(0, i - half);
            int targetRight = Math.min(count - 1, i + half);

            // Add new elements to right
            while (right < targetRight) {
                right++;
                double val = yData[right];

                // Maintain MinQ (increasing)
                while (minH < minT && yData[minQ[minT - 1]] >= val) minT--;
                minQ[minT++] = right;

                // Maintain MaxQ (decreasing)
                while (maxH < maxT && yData[maxQ[maxT - 1]] <= val) maxT--;
                maxQ[maxT++] = right;
            }

            // Remove old elements from left
            while (minH < minT && minQ[minH] < targetLeft) minH++;
            while (maxH < maxT && maxQ[maxH] < targetLeft) maxH++;

            double min = yData[minQ[minH]];
            double max = yData[maxQ[maxH]];

            double x = xData[i];

            // Upper path
            context.mapToPixel(x, max, pBuffer);
            upperX[upperCount] = (float) pBuffer[0];
            upperY[upperCount] = (float) pBuffer[1];
            upperCount++;

            // Store lower path points
            context.mapToPixel(x, min, pBuffer);
            if (lowerCount >= lowerX.length) {
                lowerX = RendererAllocationCache.getDoubleArray(this, "lowerX", lowerCount + 1);
                lowerY = RendererAllocationCache.getDoubleArray(this, "lowerY", lowerCount + 1);
            }
            lowerX[lowerCount] = pBuffer[0];
            lowerY[lowerCount] = pBuffer[1];
            lowerCount++;
        }

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor strokeBase = isMultiColor() ? themeSeries(context, 1) : base;
        if (strokeBase == null) strokeBase = base;
        ArberColor fill = base;
        ArberColor stroke = strokeBase;

        canvas.setStroke((float) ChartScale.scale(1.5));
        canvas.setColor(stroke);
        if (upperCount > 1) canvas.drawPolyline(upperX, upperY, upperCount);

        // Draw lower line (reusing buffer)
        if (lowerCount > 1) {
            int bandCount = upperCount + lowerCount;
            float[] bandX = RendererAllocationCache.getFloatArray(this, "env.band.x", bandCount);
            float[] bandY = RendererAllocationCache.getFloatArray(this, "env.band.y", bandCount);
            int idx = 0;
            for (int i = 0; i < upperCount; i++) {
                bandX[idx] = upperX[i];
                bandY[idx] = upperY[i];
                idx++;
            }
            for (int i = lowerCount - 1; i >= 0; i--) {
                bandX[idx] = (float) lowerX[i];
                bandY[idx] = (float) lowerY[i];
                idx++;
            }
            canvas.setColor(fill);
            canvas.fillPolygon(bandX, bandY, idx);

            canvas.setColor(stroke);
            float[] lowerXF = RendererAllocationCache.getFloatArray(this, "env.lower.x", lowerCount);
            float[] lowerYF = RendererAllocationCache.getFloatArray(this, "env.lower.y", lowerCount);
            for (int i = 0; i < lowerCount; i++) {
                lowerXF[i] = (float) lowerX[i];
                lowerYF[i] = (float) lowerY[i];
            }
            canvas.drawPolyline(lowerXF, lowerYF, lowerCount);
        }
    }
}
