package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract base class for medical sweep renderers (ECG, PPG, IBP, NIRS).
 * Eliminates redundancy and encapsulates sweep-erase logic.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public abstract class AbstractMedicalSweepRenderer extends com.arbergashi.charts.render.BaseRenderer {
    /**
     * Translation function for renderer names. Can be set by the end user.
     * Default: Returns the key directly (no translation).
     */
    private static Function<String, String> nameTranslator = key -> key;
    protected final Path2D.Double renderPath = new Path2D.Double();
    protected final double[] sharedCoord = new double[2];
    private final Color fallbackColor;
    private final BasicStroke stroke;
    private final int gapWidth;

    protected AbstractMedicalSweepRenderer(Color color, float strokeWidth, int gapWidth) {
        super("medicalSweep");
        this.fallbackColor = color;
        this.stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
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
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        if (!(model instanceof CircularFastMedicalModel circleModel)) return;
        double[] rawX = circleModel.getXData();
        double[] rawY = circleModel.getRawChannelArray(getChannelIndex());
        int head = circleModel.getRawHeadIndex();
        int capacity = circleModel.getRawCapacity();
        int gapEnd = (head + gapWidth) % capacity;
        boolean wrap = head > gapEnd;
        renderPath.reset();
        boolean firstPointAfterGap = true;
        for (int i = 0; i < capacity; i++) {
            if (wrap ? (i >= head || i < gapEnd) : (i >= head && i < gapEnd)) {
                firstPointAfterGap = true;
                continue;
            }
            context.mapToPixel(rawX[i], rawY[i], sharedCoord);
            if (firstPointAfterGap) {
                renderPath.moveTo(sharedCoord[0], sharedCoord[1]);
                firstPointAfterGap = false;
            } else {
                renderPath.lineTo(sharedCoord[0], sharedCoord[1]);
            }
        }
        setupQualityHints(g);
        g.setColor(resolveWaveColor(model, context));
        g.setStroke(stroke);
        g.draw(renderPath);
    }

    protected Color resolveWaveColor(ChartModel model, PlotContext context) {
        if (model != null && model.getColor() != null) return model.getColor();
        if (context != null) {
            return resolveTheme(context).getSeriesColor(getLayerIndex());
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
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
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
            context.mapToPixel(rawX[idx], rawY[idx], coord);
            double dx = coord[0] - pixel.getX();
            double dy = coord[1] - pixel.getY();
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
}
