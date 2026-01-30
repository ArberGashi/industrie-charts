package com.arbergashi.charts.engine.spatial;

/**
 * Packed style descriptor for spatial batch rendering.
 *
 * <p>Encodes ARGB color (upper 32 bits) and style metadata in the lower 32 bits.</p>
 *
 * <p>Layout (high to low):</p>
 * <ul>
 *   <li>Bits 63..32: ARGB (32-bit)</li>
 *   <li>Bits 31..16: Stroke width (Q8.8 fixed-point)</li>
 *   <li>Bits 15..8 : Dash pattern id (8-bit)</li>
 *   <li>Bits 7..0  : Marker/flags id (8-bit)</li>
 * </ul>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialStyleDescriptor {
    private static final int DEFAULT_ARGB = 0xFF000000;
    private static final float DEFAULT_STROKE = 1.0f;
    private static final int DEFAULT_DASH = 0;
    private static final int DEFAULT_MARKER = 0;
    private static final long DEFAULT_KEY = pack(DEFAULT_ARGB, DEFAULT_STROKE, DEFAULT_DASH, DEFAULT_MARKER);

    private final int argb;
    private final float strokeWidth;

    public SpatialStyleDescriptor(int argb, float strokeWidth) {
        this.argb = argb;
        this.strokeWidth = strokeWidth;
    }

    public static SpatialStyleDescriptor of(int argb, float strokeWidth) {
        return new SpatialStyleDescriptor(argb, strokeWidth);
    }

    public int getArgb() {
        return argb;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public long getPackedKey() {
        return pack(argb, strokeWidth, DEFAULT_DASH, DEFAULT_MARKER);
    }

    public static long pack(int argb, float strokeWidth, int dashId, int markerId) {
        int strokeFixed = toFixedQ8_8(strokeWidth);
        int dash = dashId & 0xFF;
        int marker = markerId & 0xFF;
        int lo = (strokeFixed << 16) | (dash << 8) | marker;
        return (((long) argb) << 32) | (lo & 0xFFFFFFFFL);
    }

    public static int unpackArgb(long packedKey) {
        return (int) (packedKey >>> 32);
    }

    public static float unpackStrokeWidth(long packedKey) {
        int lo = (int) packedKey;
        int fixed = (lo >>> 16) & 0xFFFF;
        return fromFixedQ8_8(fixed);
    }

    public static int unpackDashId(long packedKey) {
        int lo = (int) packedKey;
        return (lo >>> 8) & 0xFF;
    }

    public static int unpackMarkerId(long packedKey) {
        int lo = (int) packedKey;
        return lo & 0xFF;
    }

    private static int toFixedQ8_8(float value) {
        float clamped = Math.max(0f, Math.min(255.996f, value));
        return (int) (clamped * 256f + 0.5f);
    }

    private static float fromFixedQ8_8(int fixed) {
        return fixed / 256f;
    }

    public static long getDefaultKey() {
        return DEFAULT_KEY;
    }
}
