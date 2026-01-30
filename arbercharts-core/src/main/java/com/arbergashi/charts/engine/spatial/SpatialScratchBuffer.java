package com.arbergashi.charts.engine.spatial;

/**
 * Scratch buffers for SIMD paths (no allocations in hot loops).
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialScratchBuffer {
    private final int laneCount;
    private final double[] scratchX;
    private final double[] scratchY;
    private final double[] scratchZ;
    private final double[] scratchOutX;
    private final double[] scratchOutY;
    private final double[] scratchOutZ;
    private final double[] scratchW;

    public SpatialScratchBuffer(int laneCount) {
        if (laneCount <= 0) {
            throw new IllegalArgumentException("laneCount must be > 0");
        }
        this.laneCount = laneCount;
        this.scratchX = new double[laneCount];
        this.scratchY = new double[laneCount];
        this.scratchZ = new double[laneCount];
        this.scratchOutX = new double[laneCount];
        this.scratchOutY = new double[laneCount];
        this.scratchOutZ = new double[laneCount];
        this.scratchW = new double[laneCount];
    }

    public int getLaneCount() {
        return laneCount;
    }

    public double[] getScratchX() {
        return scratchX;
    }

    public double[] getScratchY() {
        return scratchY;
    }

    public double[] getScratchZ() {
        return scratchZ;
    }

    public double[] getScratchOutX() {
        return scratchOutX;
    }

    public double[] getScratchOutY() {
        return scratchOutY;
    }

    public double[] getScratchOutZ() {
        return scratchOutZ;
    }

    public double[] getScratchW() {
        return scratchW;
    }

    public boolean isAlignedForLaneCount(int count) {
        return laneCount == count;
    }
}
