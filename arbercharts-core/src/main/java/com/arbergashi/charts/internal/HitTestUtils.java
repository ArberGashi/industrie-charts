package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartScale;

import com.arbergashi.charts.api.types.ArberPoint;
import java.util.Optional;
/**
 * Internal utility for fast, allocation-free hit-testing.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class HitTestUtils {

    private HitTestUtils() {
    }

    /**
     * Finds the index of the nearest data point to a given pixel location.
     * This method is zero-allocation and works directly on the model's primitive data.
     *
     * @param pixel   The pixel coordinate to test against.
     * @param model   The data model.
     * @param context The plot context for coordinate transformation.
     * @return An Optional containing the index of the nearest point, or empty if not found.
     */
    public static Optional<Integer> nearestPointIndex(ArberPoint pixel, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) {
            return Optional.empty();
        }

        double minDistanceSq = Double.MAX_VALUE;
        int bestIndex = -1;
        double[] pointPixel = new double[2];

        for (int i = 0; i < n; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), pointPixel);

            double dx = pixel.x() - pointPixel[0];
            double dy = pixel.y() - pointPixel[1];
            double distSq = dx * dx + dy * dy;

            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                bestIndex = i;
            }
        }

        // Check if the nearest point is within a reasonable threshold
        double threshold = ChartScale.scale(15.0);
        if (bestIndex != -1 && minDistanceSq < threshold * threshold) {
            return Optional.of(bestIndex);
        }

        return Optional.empty();
    }
}
