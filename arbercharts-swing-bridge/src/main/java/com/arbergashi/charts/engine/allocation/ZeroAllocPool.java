package com.arbergashi.charts.engine.allocation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reuses AWT strokes, colors and scratch buffers within each calling thread.
 *
 * <p>Provided by {@code arbercharts-swing-bridge} since 2.2.0. The package and
 * public signatures are retained for existing desktop callers. Core renderers
 * use platform-neutral types and must not depend on this class.</p>
 *
 * <p>Cache misses and buffer growth allocate memory. Stroke lookups create cache
 * keys, and color lookups may box keys, so reuse is not a guarantee of an
 * allocation-free rendering loop. Measure allocations for the actual workload.</p>
 *
 * <pre>{@code
 * g.setStroke(ZeroAllocPool.getStroke(2.0f));
 * g.setColor(ZeroAllocPool.getColor(255, 0, 0));
 * }</pre>
 *
 * @since 2.0.0
 */
public final class ZeroAllocPool {

    /**
     * Thread-local stroke cache to avoid contention.
     * Each thread owns its cache and its hit/miss counters.
     */
    private static final ThreadLocal<StrokeCache> STROKE_CACHE =
        ThreadLocal.withInitial(StrokeCache::new);

    /**
     * Thread-local color cache for zero-contention access.
     */
    private static final ThreadLocal<ColorCache> COLOR_CACHE =
        ThreadLocal.withInitial(ColorCache::new);

    /**
     * Thread-local scratch buffers for geometry operations.
     * Buffers are allocated on first use and grow when necessary.
     */
    private static final BufferPool BUFFER_POOL = new BufferPool();

    private ZeroAllocPool() {
        // Utility class
    }

    /**
     * Gets a cached BasicStroke with the specified width.
     *
     * <p>Cache misses create strokes; subsequent calls reuse the cached instance.
     *
     * @param width stroke width
     * @return cached BasicStroke instance
     */
    public static BasicStroke getStroke(float width) {
        return STROKE_CACHE.get().getStroke(width);
    }

    /**
     * Gets a cached BasicStroke with width and dash pattern.
     *
     * @param width stroke width
     * @param dashPattern dash pattern; do not mutate it after the first lookup
     * @return cached BasicStroke instance
     */
    public static BasicStroke getStroke(float width, float[] dashPattern) {
        return STROKE_CACHE.get().getStroke(width, dashPattern);
    }

    /**
     * Gets a cached Color from RGB values.
     *
     * <p>Cache misses create colors; subsequent calls reuse the cached instance.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return cached Color instance
     */
    public static Color getColor(int r, int g, int b) {
        return COLOR_CACHE.get().getColor(r, g, b);
    }

    /**
     * Gets a cached Color from RGB and alpha values.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @param a alpha component (0-255)
     * @return cached Color instance
     */
    public static Color getColor(int r, int g, int b, int a) {
        return COLOR_CACHE.get().getColor(r, g, b, a);
    }

    /**
     * Gets a reusable double array for geometry operations.
     *
     * <p>The array is shared by callers on the same thread. Consume its contents
     * before requesting another scratch buffer of this type.
     *
     * @param minCapacity minimum required capacity
     * @return reusable double array (may be larger than requested)
     */
    public static double[] getDoubleBuffer(int minCapacity) {
        return BUFFER_POOL.getDoubleBuffer(minCapacity);
    }

    /**
     * Gets a reusable int array for pixel operations on the current thread.
     * The next caller on this thread may overwrite its contents.
     *
     * @param minCapacity minimum required capacity
     * @return reusable int array
     */
    public static int[] getIntBuffer(int minCapacity) {
        return BUFFER_POOL.getIntBuffer(minCapacity);
    }

    /**
     * Gets a reusable float array for coordinate operations on the current thread.
     * The next caller on this thread may overwrite its contents.
     *
     * @param minCapacity minimum required capacity
     * @return reusable float array
     */
    public static float[] getFloatBuffer(int minCapacity) {
        return BUFFER_POOL.getFloatBuffer(minCapacity);
    }

    /**
     * Creates a snapshot of stroke and color cache counters for the current thread.
     *
     * @return stats object with hit/miss ratios
     */
    public static AllocationStats getStats() {
        return new AllocationStats(
            STROKE_CACHE.get().getHitCount(),
            STROKE_CACHE.get().getMissCount(),
            COLOR_CACHE.get().getHitCount(),
            COLOR_CACHE.get().getMissCount()
        );
    }

    /**
     * Discards caches and scratch buffers owned by the current thread.
     *
     * <p>Existing object references remain valid, but subsequent lookups create
     * new entries. Use during initialization or tests, outside timed render loops.
     */
    public static void reset() {
        STROKE_CACHE.remove();
        COLOR_CACHE.remove();
        BUFFER_POOL.reset();
    }
}

/**
 * Thread-local cache of reusable AWT strokes.
 */
class StrokeCache {
    private final ConcurrentHashMap<StrokeKey, BasicStroke> cache = new ConcurrentHashMap<>();
    private long hitCount = 0;
    private long missCount = 0;

    BasicStroke getStroke(float width) {
        StrokeKey key = new StrokeKey(width);
        BasicStroke stroke = cache.get(key);

        if (stroke == null) {
            stroke = new BasicStroke(width);
            cache.put(key, stroke);
            missCount++;
        } else {
            hitCount++;
        }

        return stroke;
    }

    BasicStroke getStroke(float width, float[] dashPattern) {
        StrokeKey key = new StrokeKey(width, dashPattern);
        BasicStroke stroke = cache.get(key);

        if (stroke == null) {
            stroke = new BasicStroke(width, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f);
            cache.put(key, stroke);
            missCount++;
        } else {
            hitCount++;
        }

        return stroke;
    }

    long getHitCount() { return hitCount; }
    long getMissCount() { return missCount; }

    private static class StrokeKey {
        final float width;
        final float[] dash;
        final int hash;

        StrokeKey(float width) {
            this(width, null);
        }

        StrokeKey(float width, float[] dash) {
            this.width = width;
            this.dash = dash;
            this.hash = Float.hashCode(width) * 31 + (dash != null ? java.util.Arrays.hashCode(dash) : 0);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StrokeKey)) return false;
            StrokeKey k = (StrokeKey) o;
            return width == k.width && java.util.Arrays.equals(dash, k.dash);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}

/**
 * Thread-local cache of reusable AWT colors.
 */
class ColorCache {
    private final ConcurrentHashMap<Integer, Color> cache = new ConcurrentHashMap<>();
    private long hitCount = 0;
    private long missCount = 0;

    Color getColor(int r, int g, int b) {
        int key = (r << 16) | (g << 8) | b;
        Color color = cache.get(key);

        if (color == null) {
            color = new Color(r, g, b);
            cache.put(key, color);
            missCount++;
        } else {
            hitCount++;
        }

        return color;
    }

    Color getColor(int r, int g, int b, int a) {
        int key = (a << 24) | (r << 16) | (g << 8) | b;
        Color color = cache.get(key);

        if (color == null) {
            color = new Color(r, g, b, a);
            cache.put(key, color);
            missCount++;
        } else {
            hitCount++;
        }

        return color;
    }

    long getHitCount() { return hitCount; }
    long getMissCount() { return missCount; }
}

/**
 * Reusable buffer pool for geometry operations.
 */
class BufferPool {
    private static final int INITIAL_CAPACITY = 10000;

    private final ThreadLocal<double[]> doubleBuffer =
        ThreadLocal.withInitial(() -> new double[INITIAL_CAPACITY]);

    private final ThreadLocal<int[]> intBuffer =
        ThreadLocal.withInitial(() -> new int[INITIAL_CAPACITY]);

    private final ThreadLocal<float[]> floatBuffer =
        ThreadLocal.withInitial(() -> new float[INITIAL_CAPACITY]);

    double[] getDoubleBuffer(int minCapacity) {
        double[] buffer = doubleBuffer.get();
        if (buffer.length < minCapacity) {
            buffer = new double[minCapacity];
            doubleBuffer.set(buffer);
        }
        return buffer;
    }

    int[] getIntBuffer(int minCapacity) {
        int[] buffer = intBuffer.get();
        if (buffer.length < minCapacity) {
            buffer = new int[minCapacity];
            intBuffer.set(buffer);
        }
        return buffer;
    }

    float[] getFloatBuffer(int minCapacity) {
        float[] buffer = floatBuffer.get();
        if (buffer.length < minCapacity) {
            buffer = new float[minCapacity];
            floatBuffer.set(buffer);
        }
        return buffer;
    }

    void reset() {
        doubleBuffer.remove();
        intBuffer.remove();
        floatBuffer.remove();
    }
}

