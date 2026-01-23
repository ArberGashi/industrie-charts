package com.arbergashi.charts.render.analysis;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import java.awt.*;
import java.awt.geom.Path2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Envelope renderer.
 *
 * <p>Draws an upper/lower envelope using a sliding window min/max of Y values.
 * Useful for quick volatility visualization.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class EnvelopeRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    // Buffers for Monotonic Queues (indices)
    // Window is capped at 201, so 256 is safe capacity
    private final int[] minQ = new int[256];
    private final int[] maxQ = new int[256];
    private double[] lowerX = new double[256];
    private double[] lowerY = new double[256];

    public EnvelopeRenderer() {
        super("envelope");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        int window = Math.max(3, Math.min(201, (int) Math.round(Math.sqrt(count))));
        int half = window / 2;

        Path2D upper = getPathCache();

        // Ensure capacity for lower buffer
        if (lowerX.length < count) {
            int newCap = Math.max(count, lowerX.length * 2);
            lowerX = RendererAllocationCache.getDoubleArray(this, "lowerX", newCap);
            lowerY = RendererAllocationCache.getDoubleArray(this, "lowerY", newCap);
        }

        boolean started = false;
        int lowerCount = 0;

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
            if (!started) {
                upper.moveTo(pBuffer[0], pBuffer[1]);
                started = true;
            } else {
                upper.lineTo(pBuffer[0], pBuffer[1]);
            }

            // Store lower path points
            context.mapToPixel(x, min, pBuffer);
            lowerX[lowerCount] = pBuffer[0];
            lowerY[lowerCount] = pBuffer[1];
            lowerCount++;
        }

        Color base = seriesOrBase(model, context, 0);
        Color strokeBase = isMultiColor() ? themeSeries(context, 1) : base;
        if (strokeBase == null) strokeBase = base;
        Color fill = ColorUtils.withAlpha(base, 0.12f);
        Color stroke = ColorUtils.withAlpha(strokeBase, 0.65f);

        g2.setStroke(getCachedStroke((float) ChartScale.scale(1.5)));
        g2.setColor(stroke);
        if (started) g2.draw(upper);

        // Draw lower line (reusing buffer)
        if (lowerCount > 1) {
            Path2D lowerPath = getPathCache(); // We can reuse this if we draw upper first
            // Actually upper is already drawn. But we need upper for the fill.
            // Strategy: Draw upper. Append lower (reversed) to upper. Fill. Draw lower.

            // 1. Append lower points to upper to form the band
            for (int i = lowerCount - 1; i >= 0; i--) {
                upper.lineTo(lowerX[i], lowerY[i]);
            }
            upper.closePath();
            g2.setColor(fill);
            g2.fill(upper);

            // 2. Draw lower stroke (optional, but consistent)
            g2.setColor(stroke);
            // We can't reuse 'upper' anymore as it is closed.
            // We can reuse the path cache if we reset it, but 'upper' IS the path cache.
            // Since we need to draw the lower line, we can just iterate and drawLine for Zero-GC
            // or use a second path cache if available. 
            // For strict Zero-GC without second cache:
            for (int i = 0; i < lowerCount - 1; i++) {
                g2.draw(getLine(lowerX[i], lowerY[i], lowerX[i + 1], lowerY[i + 1]));
            }
        }
    }
}
