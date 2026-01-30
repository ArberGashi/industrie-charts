package com.arbergashi.charts.engine.spatial;

/**
 * Reusable coordinate buffers for spatial projections.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialBuffer {
    private final double[] inputCoords;
    private final double[] outputCoords;
    private final int pointCapacity;
    private final SpatialScratchBuffer scratch;
    private final SpatialPeakMetadata peakMetadata;
    private boolean ringEnabled = false;
    private int ringStart = 0;
    private int ringCount = 0;
    private long ringWriteSeq = 0L;
    private long ringConsumedSeq = 0L;

    public SpatialBuffer(int pointCapacity) {
        if (pointCapacity <= 0) {
            throw new IllegalArgumentException("pointCapacity must be > 0");
        }
        this.pointCapacity = pointCapacity;
        this.inputCoords = new double[pointCapacity * 3];
        this.outputCoords = new double[pointCapacity * 3];
        this.scratch = new SpatialScratchBuffer(VectorIntrinsics.getLaneCount());
        this.peakMetadata = new SpatialPeakMetadata(pointCapacity);
    }

    public int getPointCapacity() {
        return pointCapacity;
    }

    public double[] getInputCoords() {
        return inputCoords;
    }

    public double[] getOutputCoords() {
        return outputCoords;
    }

    public SpatialScratchBuffer getScratch() {
        return scratch;
    }

    public SpatialPeakMetadata getPeakMetadata() {
        return peakMetadata;
    }

    public boolean isRingEnabled() {
        return ringEnabled;
    }

    public SpatialBuffer setRingEnabled(boolean ringEnabled) {
        if (this.ringEnabled != ringEnabled) {
            this.ringEnabled = ringEnabled;
            resetRing();
        }
        return this;
    }

    public int getRingStart() {
        return ringStart;
    }

    public int getRingCount() {
        return ringCount;
    }

    public void resetRing() {
        ringStart = 0;
        ringCount = 0;
        ringWriteSeq = 0L;
        ringConsumedSeq = 0L;
    }

    /**
     * Writes a point into the ring buffer and advances the cursor.
     *
     * @return physical index written
     */
    public int writeRing(double x, double y, double z) {
        int index;
        if (ringCount < pointCapacity) {
            index = (ringStart + ringCount) % pointCapacity;
            ringCount++;
        } else {
            index = ringStart;
            ringStart = (ringStart + 1) % pointCapacity;
        }
        int base = index * 3;
        inputCoords[base] = x;
        inputCoords[base + 1] = y;
        inputCoords[base + 2] = z;
        ringWriteSeq++;
        return index;
    }

    public int getRingPhysicalIndex(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= ringCount) {
            return -1;
        }
        return (ringStart + logicalIndex) % pointCapacity;
    }

    public void getRingPoint(int logicalIndex, double[] out) {
        if (out == null || out.length < 3) {
            throw new IllegalArgumentException("out must have length >= 3");
        }
        int index = getRingPhysicalIndex(logicalIndex);
        if (index < 0) {
            out[0] = 0.0;
            out[1] = 0.0;
            out[2] = 0.0;
            return;
        }
        int base = index * 3;
        out[0] = inputCoords[base];
        out[1] = inputCoords[base + 1];
        out[2] = inputCoords[base + 2];
    }

    public int getRingDeltaCount() {
        long delta = ringWriteSeq - ringConsumedSeq;
        if (delta <= 0) return 0;
        if (delta > pointCapacity) {
            delta = pointCapacity;
        }
        return (int) delta;
    }

    public int getRingDeltaStartLogical() {
        int delta = getRingDeltaCount();
        if (delta <= 0) return 0;
        int start = ringCount - delta;
        return Math.max(0, start);
    }

    public void consumeRingDelta() {
        int delta = getRingDeltaCount();
        if (delta <= 0) return;
        if (delta >= pointCapacity) {
            ringConsumedSeq = ringWriteSeq;
        } else {
            ringConsumedSeq += delta;
        }
    }

    /**
     * Forces the next delta projection to include the full ring window.
     */
    public void resetRingDeltaCursor() {
        if (!ringEnabled) return;
        long target = ringWriteSeq - ringCount;
        ringConsumedSeq = Math.max(0L, target);
    }

    public boolean isAlignedForLaneCount(int laneCount) {
        if (laneCount <= 0) return false;
        return (pointCapacity % laneCount) == 0;
    }

    public int getAlignedPointCount(int laneCount) {
        if (laneCount <= 0) return 0;
        return (pointCapacity / laneCount) * laneCount;
    }
}
