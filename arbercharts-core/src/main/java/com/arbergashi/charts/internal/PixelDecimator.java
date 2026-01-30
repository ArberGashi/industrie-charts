package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.PlotContext;
/**
 * Algorithm for data reduction for display purposes.
 *
 * <p>With very large datasets (e.g., 1 million points), it is inefficient to draw lines
 * for every point when the screen is only 2000 pixels wide.
 * The decimator reduces the data to min/max values per pixel column.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class PixelDecimator {

    private PixelDecimator() {
    }

    /**
     * Reduces the input data to approx. 2 points per pixel column (Min and Max).
     *
     * @param xIn     Input X
     * @param yIn     Input Y
     * @param count   Number of points
     * @param context PlotContext (for width)
     * @param outX    Output buffer (must be large enough, e.g., 4 * width)
     * @param outY    Output buffer
     * @return Number of points in the output buffer
     */
    public static int decimate(double[] xIn, double[] yIn, int count, PlotContext context, double[] outX, double[] outY) {
        double width = context.getPlotBounds().width();
        if (width <= 0 || count < 2) return 0;

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double range = maxX - minX;
        if (range <= 0) return 0;

        // Scaling factor: Data X to pixel bucket
        double scale = width / range;

        int lastBucket = -1;
        double bucketMinY = Double.MAX_VALUE;
        double bucketMaxY = -Double.MAX_VALUE;
        // We store the X values where Min/Max occurred to avoid distortion
        double bucketMinX = 0;
        double bucketMaxX = 0;

        int outIdx = 0;
        int maxOut = outX.length - 2; // Reserve for last point

        for (int i = 0; i < count; i++) {
            double x = xIn[i];
            double y = yIn[i];

            // Calculate pixel bucket (int cast is floor)
            int bucket = (int) ((x - minX) * scale);

            if (bucket != lastBucket) {
                // Write old bucket (if not the very first iteration)
                if (lastBucket != -1) {
                    if (outIdx >= maxOut) break; // Buffer full

                    // We write Min and Max as a vertical line for this bucket
                    // Order: Min then Max (or vice-versa, visually irrelevant for a line)
                    outX[outIdx] = bucketMinX;
                    outY[outIdx] = bucketMinY;
                    outIdx++;
                    outX[outIdx] = bucketMaxX;
                    outY[outIdx] = bucketMaxY;
                    outIdx++;
                }

                // Start new bucket
                lastBucket = bucket;
                bucketMinY = y;
                bucketMinX = x;
                bucketMaxY = y;
                bucketMaxX = x;
            } else {
                // In the same bucket: update min/max
                if (y < bucketMinY) {
                    bucketMinY = y;
                    bucketMinX = x;
                }
                if (y > bucketMaxY) {
                    bucketMaxY = y;
                    bucketMaxX = x;
                }
            }
        }

        // Write last bucket
        if (lastBucket != -1 && outIdx < outX.length - 1) {
            outX[outIdx] = bucketMinX;
            outY[outIdx] = bucketMinY;
            outIdx++;
            outX[outIdx] = bucketMaxX;
            outY[outIdx] = bucketMaxY;
            outIdx++;
        }

        return outIdx;
    }
}
