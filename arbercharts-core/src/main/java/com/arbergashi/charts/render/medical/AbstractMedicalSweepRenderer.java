package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;

import java.util.Optional;
import java.util.function.Function;
/**
 * Abstract base class for medical sweep renderers (ECG, PPG, IBP, NIRS).
 * Eliminates redundancy and encapsulates sweep-erase logic.
 *
 * @author Arber Gashi
 * @since 2025-06-01
 */
public abstract class AbstractMedicalSweepRenderer extends com.arbergashi.charts.render.BaseRenderer {
    /**
     * Translation function for renderer names. Can be set by the end user.
     * Default: Returns the key directly (no translation).
     */
    private static Function<String, String> nameTranslator = key -> key;
    protected final double[] sharedCoord = new double[2];
    private final ArberColor fallbackColor;
    private final float strokeWidth;
    private final int gapWidth;
    private transient float[] pathX = new float[0];
    private transient float[] pathY = new float[0];

    protected AbstractMedicalSweepRenderer(ArberColor color, float strokeWidth, int gapWidth) {
        super("medicalSweep");
        this.fallbackColor = color;
        this.strokeWidth = strokeWidth;
        this.gapWidth = gapWidth;
    }

    /**
     * End users can set their own translation function here.
     * Example: AbstractMedicalSweepRenderer.setNameTranslator(key -> MyI18n.get(key));
     */
    public static void setNameTranslator(Function<String, String> translator) {
        nameTranslator = translator != null ? translator : key -> key;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        int size = circleModel.getRawSize();
        if (size < 2) return;
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(getChannelIndex());
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        ensureBufferCapacity(size);
        canvas.setColor(getResolvedWaveColor(model, context));
        canvas.setStroke(strokeWidth);
        int count = 0;
        // Before the first wrap, only [0, size) contains samples. A full ring uses every slot.
        for (int i = 0; i < size; i++) {
            boolean skip = MedicalSweepSupport.isInGap(i, head, capacity, gapWidth)
                    || !Double.isFinite(rawX[i]) || !Double.isFinite(rawY[i]);
            if (i == head || skip) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
            }
            if (skip) continue;
            context.mapToPixel(rawX[i], rawY[i], sharedCoord);
            if (!Double.isFinite(sharedCoord[0]) || !Double.isFinite(sharedCoord[1])) {
                if (count > 1) canvas.drawPolyline(pathX, pathY, count);
                count = 0;
                continue;
            }
            pathX[count] = (float) sharedCoord[0];
            pathY[count] = (float) sharedCoord[1];
            count++;
        }
        if (count > 1) canvas.drawPolyline(pathX, pathY, count);
    }

    protected ArberColor getResolvedWaveColor(ChartModel model, PlotContext context) {
        if (model != null && model.getColor() != null) return model.getColor();
        if (context != null) {
            return getResolvedTheme(context).getSeriesColor(getLayerIndex());
        }
        return fallbackColor;
    }
    /**
         * Defaults to channel 0, can be overridden by subclasses.
    */
    protected int getChannelIndex() {
        return 0;
    }

    @Override
    public String getName() {
        // End users can define the translation logic themselves
        String key = "renderer." + getClass().getSimpleName().toLowerCase();
        return nameTranslator.apply(key);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return Optional.empty();
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(getChannelIndex());
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        int size = circleModel.getPointCount();
        if (size == 0) return Optional.empty();
        double minDistSq = Double.MAX_VALUE;
        int bestIdx = -1;
        // reuse sharedCoord buffer to avoid allocations
        double[] coord = this.sharedCoord;
        for (int i = 0; i < size; i++) {
            int idx = (head - size + i + capacity) % capacity;
            if (MedicalSweepSupport.isInGap(idx, head, capacity, gapWidth)
                    || !Double.isFinite(rawX[idx]) || !Double.isFinite(rawY[idx])) continue;
            context.mapToPixel(rawX[idx], rawY[idx], coord);
            double dx = coord[0] - pixel.x();
            double dy = coord[1] - pixel.y();
            double distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                bestIdx = i;
            }
        }
        // Only return if within a reasonable pixel threshold (e.g., 16px)
        if (minDistSq < 16 * 16) {
            return Optional.of(bestIdx);
        }
        return Optional.empty();
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
