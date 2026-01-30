package com.arbergashi.charts.core.rendering;

/**
 * Simple voxel data holder for bulk rendering.
 *
 * <p>Intended for SIMD-friendly traversal; no UI dependencies.</p>
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class VoxelBuffer {
    private final float[] x;
    private final float[] y;
    private final float[] z;
    private final int[] argb;
    private final int count;

    public VoxelBuffer(float[] x, float[] y, float[] z, int[] argb, int count) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.argb = argb;
        this.count = count;
    }

    public float[] x() { return x; }
    public float[] y() { return y; }
    public float[] z() { return z; }
    public int[] argb() { return argb; }
    public int count() { return count; }
}
