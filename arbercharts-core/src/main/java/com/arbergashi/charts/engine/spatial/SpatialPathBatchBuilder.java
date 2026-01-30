package com.arbergashi.charts.engine.spatial;

/**
 * Builds render-ready path batches from spatial buffers with clipping transitions.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialPathBatchBuilder implements SpatialChunkConsumer {
    public enum ClippingMode {
        DISCARD,
        CLAMP
    }

    private final SpatialPathBatch batch = new SpatialPathBatch(256);
    private double zMin = 0.0;
    private ClippingMode clippingMode = ClippingMode.DISCARD;
    private ClippingStrategy clippingStrategy = (z, zMin) -> z > zMin;
    private long currentStyleKey = SpatialStyleDescriptor.getDefaultKey();
    private boolean autoReset = true;

    private long[] moveMask;
    private int moveMaskWords;
    private boolean lastVisible;
    private boolean lastPointValid;
    private double lastX;
    private double lastY;
    private double lastZ;
    private final SpatialBuffer lineBuffer = new SpatialBuffer(2);

    public SpatialPathBatch getBatch() {
        return batch;
    }

    public double getZMin() {
        return zMin;
    }

    public SpatialPathBatchBuilder setZMin(double zMin) {
        this.zMin = zMin;
        return this;
    }

    public ClippingMode getClippingMode() {
        return clippingMode;
    }

    public SpatialPathBatchBuilder setClippingMode(ClippingMode clippingMode) {
        this.clippingMode = (clippingMode != null) ? clippingMode : ClippingMode.DISCARD;
        this.clippingStrategy = switch (this.clippingMode) {
            case CLAMP -> new ClippingStrategy() {
                @Override
                public boolean isVisible(double z, double zMin) {
                    return true;
                }

                @Override
                public double getCalculatedZ(double z, double zMin) {
                    return (z < zMin) ? zMin : z;
                }
            };
            case DISCARD -> (z, min) -> z > min;
        };
        return this;
    }

    public ClippingStrategy getClippingStrategy() {
        return clippingStrategy;
    }

    public SpatialPathBatchBuilder setClippingStrategy(ClippingStrategy clippingStrategy) {
        if (clippingStrategy != null) {
            this.clippingStrategy = clippingStrategy;
        }
        return this;
    }

    public SpatialPathBatchBuilder reset() {
        batch.setPointCount(0);
        lastVisible = false;
        lastPointValid = false;
        return this;
    }

    public boolean isAutoReset() {
        return autoReset;
    }

    public SpatialPathBatchBuilder setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
        return this;
    }

    /**
     * Resets the path state without clearing accumulated batch points.
     */
    public SpatialPathBatchBuilder resetPathState() {
        lastVisible = false;
        lastPointValid = false;
        return this;
    }

    public boolean isMoveTo(int index) {
        if (index < 0 || index >= batch.getPointCount()) {
            return true;
        }
        int word = index >>> 6;
        int bit = index & 63;
        if (moveMask == null || word >= moveMaskWords) {
            return true;
        }
        return (moveMask[word] & (1L << bit)) != 0L;
    }

    public boolean isChunkPotentiallyVisible(SpatialBuffer buffer, int count) {
        if (buffer == null || count <= 0) {
            return false;
        }
        if (clippingMode == ClippingMode.CLAMP) {
            return true;
        }
        double[] in = buffer.getInputCoords();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0, j = 2; i < count; i++, j += 3) {
            double z = in[j];
            if (z < min) min = z;
            if (z > max) max = z;
        }
        return max > zMin && min != Double.POSITIVE_INFINITY;
    }

    public long getStyleKey() {
        return currentStyleKey;
    }

    public SpatialPathBatchBuilder setStyleKey(long styleKey) {
        this.currentStyleKey = styleKey;
        return this;
    }

    public SpatialPathBatchBuilder setStyleDescriptor(SpatialStyleDescriptor descriptor) {
        if (descriptor != null) {
            this.currentStyleKey = descriptor.getPackedKey();
        }
        return this;
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
        if (buffer == null || count <= 0) return;
        if (!isChunkPotentiallyVisible(buffer, count)) {
            lastVisible = false;
            lastPointValid = false;
            return;
        }
        batch.ensureCapacity(batch.getPointCount() + count);
        ensureMoveMaskCapacity(batch.getPointCount() + count);

        double[] in = buffer.getInputCoords();
        double[] xs = batch.getXData();
        double[] ys = batch.getYData();
        int outIndex = batch.getPointCount();

        for (int i = 0, j = 0; i < count; i++, j += 3) {
            double x = in[j];
            double y = in[j + 1];
            double z = in[j + 2];
            double clippedZ = clippingStrategy.getCalculatedZ(z, zMin);
            if (!clippingStrategy.isVisible(z, zMin)) {
                if (lastPointValid && lastVisible) {
                    double clipZ = zMin;
                    double dz = z - lastZ;
                    if (Math.abs(dz) > 1e-12) {
                        double t = (clipZ - lastZ) / dz;
                        double cx = lastX + t * (x - lastX);
                        double cy = lastY + t * (y - lastY);
                        xs[outIndex] = cx;
                        ys[outIndex] = cy;
                        batch.setStyleKey(outIndex, currentStyleKey);
                        outIndex++;
                    }
                }
                lastVisible = false;
                lastPointValid = true;
                lastX = x;
                lastY = y;
                lastZ = clippedZ;
                continue;
            }
            if (lastPointValid && !lastVisible) {
                double clipZ = zMin;
                double dz = z - lastZ;
                    if (Math.abs(dz) > 1e-12) {
                        double t = (clipZ - lastZ) / dz;
                        double cx = lastX + t * (x - lastX);
                        double cy = lastY + t * (y - lastY);
                        xs[outIndex] = cx;
                        ys[outIndex] = cy;
                        batch.setStyleKey(outIndex, currentStyleKey);
                        setMoveFlag(outIndex);
                        outIndex++;
                        lastVisible = true;
                    }
                }
            xs[outIndex] = x;
            ys[outIndex] = y;
            batch.setStyleKey(outIndex, currentStyleKey);
            if (!lastVisible) {
                setMoveFlag(outIndex);
            }
            lastVisible = true;
            lastPointValid = true;
            lastX = x;
            lastY = y;
            lastZ = clippedZ;
            outIndex++;
        }
        batch.setPointCount(outIndex);
    }

    /**
     * Appends a single line segment to the batch as an isolated path.
     *
     * <p>This helper resets the path state to avoid linking segments across calls.</p>
     *
     * @param x1 first x
     * @param y1 first y
     * @param z1 first z
     * @param x2 second x
     * @param y2 second y
     * @param z2 second z
     * @return this builder
     */
    public SpatialPathBatchBuilder setLineSegment(double x1, double y1, double z1,
                                                  double x2, double y2, double z2) {
        double[] in = lineBuffer.getInputCoords();
        in[0] = x1;
        in[1] = y1;
        in[2] = z1;
        in[3] = x2;
        in[4] = y2;
        in[5] = z2;
        resetPathState();
        accept(lineBuffer, 2);
        resetPathState();
        return this;
    }

    private void ensureMoveMaskCapacity(int requiredPoints) {
        int words = (requiredPoints + 63) >>> 6;
        if (moveMask == null || moveMask.length < words) {
            moveMask = new long[Math.max(1, words)];
        }
        moveMaskWords = words;
    }

    private void setMoveFlag(int index) {
        int word = index >>> 6;
        int bit = index & 63;
        if (moveMask != null && word < moveMaskWords) {
            moveMask[word] |= (1L << bit);
        }
    }
}
