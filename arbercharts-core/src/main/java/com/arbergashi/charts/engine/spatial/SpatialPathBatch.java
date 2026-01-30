package com.arbergashi.charts.engine.spatial;

/**
 * Zero-allocation batch container for projected 2D path coordinates.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialPathBatch {
    private double[] xs;
    private double[] ys;
    private long[] styleKeys;
    private int pointCount;
    private long[] visibilityMask;

    public SpatialPathBatch() {
        this(256);
    }

    public SpatialPathBatch(int initialCapacity) {
        int cap = Math.max(1, initialCapacity);
        this.xs = new double[cap];
        this.ys = new double[cap];
    }

    public int getPointCount() {
        return pointCount;
    }

    public SpatialPathBatch setPointCount(int pointCount) {
        this.pointCount = Math.max(0, pointCount);
        return this;
    }

    public double[] getXData() {
        return xs;
    }

    public double[] getYData() {
        return ys;
    }

    public int getCapacity() {
        return xs.length;
    }

    public boolean isVisible(int index) {
        if (index < 0 || index >= pointCount || visibilityMask == null) {
            return true;
        }
        int word = index >>> 6;
        int bit = index & 63;
        if (word >= visibilityMask.length) {
            return true;
        }
        return (visibilityMask[word] & (1L << bit)) != 0L;
    }

    public SpatialPathBatch ensureCapacity(int required) {
        if (required <= xs.length) {
            return this;
        }
        int newCap = xs.length;
        while (newCap < required) {
            newCap = newCap + (newCap >> 1) + 1;
        }
        double[] nextX = new double[newCap];
        double[] nextY = new double[newCap];
        long[] nextStyles = (styleKeys != null) ? new long[newCap] : null;
        System.arraycopy(xs, 0, nextX, 0, pointCount);
        System.arraycopy(ys, 0, nextY, 0, pointCount);
        if (styleKeys != null) {
            System.arraycopy(styleKeys, 0, nextStyles, 0, pointCount);
        }
        xs = nextX;
        ys = nextY;
        styleKeys = nextStyles;
        return this;
    }

    public SpatialPathBatch ensureVisibilityCapacity(int required) {
        int words = (required + 63) >>> 6;
        if (visibilityMask == null || visibilityMask.length < words) {
            visibilityMask = new long[Math.max(1, words)];
        }
        return this;
    }

    public SpatialPathBatch ensureStyleCapacity(int required) {
        if (styleKeys == null || styleKeys.length < required) {
            long[] next = new long[Math.max(1, required)];
            if (styleKeys != null) {
                System.arraycopy(styleKeys, 0, next, 0, pointCount);
            }
            styleKeys = next;
        }
        return this;
    }

    public long getStyleKey(int index) {
        if (styleKeys == null || index < 0 || index >= pointCount) {
            return SpatialStyleDescriptor.getDefaultKey();
        }
        return styleKeys[index];
    }

    public SpatialPathBatch setStyleKey(int index, long styleKey) {
        if (index < 0) {
            return this;
        }
        ensureStyleCapacity(index + 1);
        styleKeys[index] = styleKey;
        return this;
    }

    /**
     * Loads projected coordinates from a spatial buffer into this path batch.
     *
     * @param buffer spatial buffer containing projected coordinates
     * @param count  number of points to load
     * @return this batch
     */
    public SpatialPathBatch setFromBuffer(SpatialBuffer buffer, int count) {
        if (buffer == null || count <= 0) {
            pointCount = 0;
            return this;
        }
        ensureCapacity(count);
        ensureStyleCapacity(count);
        double[] source = buffer.getInputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            xs[i] = source[j];
            ys[i] = source[j + 1];
            styleKeys[i] = SpatialStyleDescriptor.getDefaultKey();
        }
        pointCount = count;
        return this;
    }

    /**
     * Loads projected coordinates and marks visibility using a Z threshold.
     *
     * <p>Points with z &lt;= zMin are marked as not visible.</p>
     *
     * @param buffer spatial buffer containing projected coordinates
     * @param count  number of points to load
     * @param zMin   minimum Z for visibility
     * @return this batch
     */
    public SpatialPathBatch setFromBufferClipped(SpatialBuffer buffer, int count, double zMin) {
        if (buffer == null || count <= 0) {
            pointCount = 0;
            return this;
        }
        ensureCapacity(count);
        ensureVisibilityCapacity(count);
        ensureStyleCapacity(count);
        int words = (count + 63) >>> 6;
        for (int w = 0; w < words; w++) {
            visibilityMask[w] = 0L;
        }
        double[] source = buffer.getInputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            xs[i] = source[j];
            ys[i] = source[j + 1];
            styleKeys[i] = SpatialStyleDescriptor.getDefaultKey();
            double z = source[j + 2];
            if (z > zMin) {
                int word = i >>> 6;
                int bit = i & 63;
                visibilityMask[word] |= (1L << bit);
            }
        }
        pointCount = count;
        return this;
    }
}
