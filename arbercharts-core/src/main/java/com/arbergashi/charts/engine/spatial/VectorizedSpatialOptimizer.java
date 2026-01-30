package com.arbergashi.charts.engine.spatial;

import com.arbergashi.charts.api.PlotContext;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD-aware optimizer for spatial buffers.
 *
 * <p>Filters points outside the visible bounds using Vector API comparisons
 * while keeping the compaction zero-allocation and in-place.</p>
 *
 * <p>Configuration keys (via {@link com.arbergashi.charts.util.ChartAssets}):</p>
 * <ul>
 *   <li>{@code Chart.ai.optimizer.enabled}</li>
 *   <li>{@code Chart.ai.optimizer.minPixelDistance}</li>
 *   <li>{@code Chart.ai.optimizer.minPixelDistanceX}</li>
 *   <li>{@code Chart.ai.optimizer.minPixelDistanceY}</li>
 *   <li>{@code Chart.ai.optimizer.preservePeaks}</li>
 *   <li>{@code Chart.ai.optimizer.exportPeaks}</li>
 *   <li>{@code Chart.ai.optimizer.depthNear}</li>
 *   <li>{@code Chart.ai.optimizer.depthFar}</li>
 *   <li>{@code Chart.ai.optimizer.depthScale}</li>
 *   <li>{@code Chart.ai.optimizer.zDampingFactor}</li>
 *   <li>{@code Chart.ai.optimizer.zDampingMode} (LINEAR/EXPONENTIAL)</li>
 * </ul>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class VectorizedSpatialOptimizer {
    public enum DepthDampingMode {
        LINEAR,
        EXPONENTIAL
    }
    private double minDistanceSq = 0.0;
    private double minDistanceSqX = 0.0;
    private double minDistanceSqY = 0.0;
    private double depthNear = 0.0;
    private double depthFar = 0.0;
    private double depthScale = 0.0;
    private boolean depthEnabled = false;
    private double depthDampingFactor = 0.0;
    private DepthDampingMode depthDampingMode = DepthDampingMode.LINEAR;
    private boolean hasLast;
    private double lastX;
    private double lastY;
    private boolean preservePeaks = true;
    private boolean exportPeaks = false;

    /**
     * Sets the minimum pixel distance between retained points.
     *
     * @param minPixelDistance minimum distance in pixels
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setMinPixelDistance(double minPixelDistance) {
        double value = Math.max(0.0, minPixelDistance);
        this.minDistanceSq = value * value;
        this.minDistanceSqX = 0.0;
        this.minDistanceSqY = 0.0;
        return this;
    }

    /**
     * Sets independent minimum pixel distances for X and Y.
     *
     * @param minPixelDistanceX minimum distance for X in pixels
     * @param minPixelDistanceY minimum distance for Y in pixels
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setMinPixelDistance(double minPixelDistanceX, double minPixelDistanceY) {
        double x = Math.max(0.0, minPixelDistanceX);
        double y = Math.max(0.0, minPixelDistanceY);
        this.minDistanceSq = 0.0;
        this.minDistanceSqX = x * x;
        this.minDistanceSqY = y * y;
        return this;
    }

    /**
     * Enables depth-based scaling for the pixel distance filter.
     *
     * @param nearDepth minimum depth (closest)
     * @param farDepth maximum depth (farthest)
     * @param scale depth scale factor (0 disables)
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setDepthAttenuation(double nearDepth, double farDepth, double scale) {
        if (Double.isFinite(scale) && scale > 0.0 && Double.isFinite(nearDepth) && Double.isFinite(farDepth)
                && farDepth > nearDepth) {
            this.depthNear = nearDepth;
            this.depthFar = farDepth;
            this.depthScale = scale;
            this.depthEnabled = true;
        } else {
            this.depthEnabled = false;
            this.depthNear = 0.0;
            this.depthFar = 0.0;
            this.depthScale = 0.0;
        }
        return this;
    }

    /**
     * Sets the depth-based damping factor and mode used to scale the pixel-distance threshold.
     *
     * @param dampingFactor factor in the range {@code [0..1]} (values &lt;= 0 disable damping)
     * @param mode damping mode (linear or exponential)
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setDepthDamping(double dampingFactor, DepthDampingMode mode) {
        if (Double.isFinite(dampingFactor) && dampingFactor > 0.0) {
            this.depthDampingFactor = dampingFactor;
            this.depthDampingMode = (mode != null) ? mode : DepthDampingMode.LINEAR;
        } else {
            this.depthDampingFactor = 0.0;
            this.depthDampingMode = DepthDampingMode.LINEAR;
        }
        return this;
    }

    /**
     * Enables or disables local peak preservation.
     *
     * @param preservePeaks true to force retention of local min/max points
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setPreservePeaks(boolean preservePeaks) {
        this.preservePeaks = preservePeaks;
        return this;
    }

    /**
     * Enables or disables exporting peak anchors into the spatial buffer metadata.
     *
     * @param exportPeaks true to capture peak anchors
     * @return this optimizer
     */
    public VectorizedSpatialOptimizer setExportPeaks(boolean exportPeaks) {
        this.exportPeaks = exportPeaks;
        return this;
    }

    /**
     * Resets the internal state for a new stream.
     */
    public void reset() {
        hasLast = false;
        lastX = 0.0;
        lastY = 0.0;
    }

    /**
     * Applies a bounds filter to the spatial buffer.
     *
     * @param buffer     spatial buffer containing xyz triples
     * @param count      number of points in the buffer
     * @param context    plot context for bounds
     * @param pixelSpace if true, bounds are derived from plot pixel bounds
     * @return new point count after filtering
     */
    public int applyBoundsFilter(SpatialBuffer buffer, int count, PlotContext context, boolean pixelSpace) {
        if (buffer == null || context == null || count <= 0) return Math.max(0, count);

        SpatialPeakMetadata peaks = null;
        if (exportPeaks) {
            peaks = buffer.getPeakMetadata();
            if (peaks != null) {
                peaks.reset();
            }
        }

        double minX;
        double maxX;
        double minY;
        double maxY;
        if (pixelSpace) {
            var bounds = context.getPlotBounds();
            minX = bounds.minX();
            maxX = bounds.maxX();
            minY = bounds.minY();
            maxY = bounds.maxY();
        } else {
            minX = context.getMinX();
            maxX = context.getMaxX();
            minY = context.getMinY();
            maxY = context.getMaxY();
        }

        double[] in = buffer.getInputCoords();
        if (in == null) return 0;

        SpatialScratchBuffer scratch = buffer.getScratch();
        int laneCount = scratch.getLaneCount();
        double[] sx = scratch.getScratchX();
        double[] sy = scratch.getScratchY();
        double[] sz = scratch.getScratchZ();

        VectorSpecies<Double> species = VectorIntrinsics.getPreferredSpecies();
        int write = 0;

        for (int i = 0; i < count; i += laneCount) {
            int remaining = Math.min(laneCount, count - i);
            for (int j = 0; j < remaining; j++) {
                int base = (i + j) * 3;
                sx[j] = in[base];
                sy[j] = in[base + 1];
                sz[j] = in[base + 2];
            }

            VectorMask<Double> inRange = species.indexInRange(0, remaining);
            DoubleVector vx = DoubleVector.fromArray(species, sx, 0);
            DoubleVector vy = DoubleVector.fromArray(species, sy, 0);

            VectorMask<Double> mask = vx.compare(VectorOperators.GE, minX)
                    .and(vx.compare(VectorOperators.LE, maxX))
                    .and(vy.compare(VectorOperators.GE, minY))
                    .and(vy.compare(VectorOperators.LE, maxY))
                    .and(inRange);

            for (int j = 0; j < remaining; j++) {
                if (!mask.laneIsSet(j)) continue;
                double x = sx[j];
                double y = sy[j];
                double z = sz[j];
                if (hasLast) {
                    double dx = x - lastX;
                    double dy = y - lastY;
                    double scale = 1.0;
                    if (depthEnabled) {
                        double t = (z - depthNear) / (depthFar - depthNear);
                        if (t < 0.0) t = 0.0;
                        if (t > 1.0) t = 1.0;
                        scale += depthScale * t;
                        if (depthDampingFactor > 0.0) {
                            double mod = (depthDampingMode == DepthDampingMode.EXPONENTIAL) ? (t * t) : t;
                            scale *= (1.0 + depthDampingFactor * mod);
                        }
                    }
                    if (minDistanceSqX > 0.0 || minDistanceSqY > 0.0) {
                        double minXLocal = minDistanceSqX * scale * scale;
                        double minYLocal = minDistanceSqY * scale * scale;
                        if ((dx * dx) < minXLocal && (dy * dy) < minYLocal) {
                            boolean isPeak = preservePeaks && isLocalPeak(in, i + j, count);
                            if (!isPeak) {
                                continue;
                            }
                            if (peaks != null) {
                                peaks.add(x, y, z);
                            }
                        }
                    } else if (minDistanceSq > 0.0) {
                        double min = minDistanceSq * scale * scale;
                        if ((dx * dx + dy * dy) < min) {
                            boolean isPeak = preservePeaks && isLocalPeak(in, i + j, count);
                            if (!isPeak) {
                                continue;
                            }
                            if (peaks != null) {
                                peaks.add(x, y, z);
                            }
                        }
                    }
                }
                int outBase = write * 3;
                in[outBase] = x;
                in[outBase + 1] = y;
                in[outBase + 2] = z;
                write++;
                lastX = x;
                lastY = y;
                hasLast = true;
            }
        }

        return write;
    }

    /**
     * Applies a bounds filter to a ring buffer window and compacts hits to the front.
     *
     * @param buffer spatial buffer (ring-enabled)
     * @param startLogicalIndex logical start index in the ring
     * @param count number of points to scan
     * @param context plot context for bounds
     * @param pixelSpace if true, bounds are derived from plot pixel bounds
     * @return new point count after filtering
     */
    public int applyBoundsFilterDelta(SpatialBuffer buffer,
                                      int startLogicalIndex,
                                      int count,
                                      PlotContext context,
                                      boolean pixelSpace) {
        if (buffer == null || context == null || count <= 0) return Math.max(0, count);
        if (!buffer.isRingEnabled()) {
            return applyBoundsFilter(buffer, count, context, pixelSpace);
        }
        double minX;
        double maxX;
        double minY;
        double maxY;
        if (pixelSpace) {
            var bounds = context.getPlotBounds();
            minX = bounds.minX();
            maxX = bounds.maxX();
            minY = bounds.minY();
            maxY = bounds.maxY();
        } else {
            minX = context.getMinX();
            maxX = context.getMaxX();
            minY = context.getMinY();
            maxY = context.getMaxY();
        }

        double[] in = buffer.getInputCoords();
        if (in == null) return 0;

        int total = Math.min(count, buffer.getRingCount());
        int write = 0;
        for (int i = 0; i < total; i++) {
            int logical = startLogicalIndex + i;
            int physical = buffer.getRingPhysicalIndex(logical);
            if (physical < 0) continue;
            int base = physical * 3;
            double x = in[base];
            double y = in[base + 1];
            double z = in[base + 2];
            if (x < minX || x > maxX || y < minY || y > maxY) {
                continue;
            }
            int outBase = write * 3;
            in[outBase] = x;
            in[outBase + 1] = y;
            in[outBase + 2] = z;
            write++;
        }
        return write;
    }

    private boolean isLocalPeak(double[] input, int index, int count) {
        if (input == null || index <= 0 || index >= count - 1) {
            return false;
        }
        int prev = (index - 1) * 3 + 1;
        int curr = index * 3 + 1;
        int next = (index + 1) * 3 + 1;
        double prevY = input[prev];
        double currY = input[curr];
        double nextY = input[next];
        return (currY >= prevY && currY >= nextY) || (currY <= prevY && currY <= nextY);
    }
}
