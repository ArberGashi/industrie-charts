package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.engine.allocation.ZeroAllocPool;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;

/**
 * Graphics2D-backed ArberCanvas implementation (Swing/legacy bridge).
 *
 * <p><strong>ZERO-GC Implementation:</strong> This adapter uses thread-local
 * caches and ZeroAllocPool to eliminate all allocations in the hot path.
 *
 * @since 2.0.0
 */
public final class AwtCanvasAdapter implements ArberCanvas {

    /** Maximum polyline/polygon size for pre-allocated buffers. */
    private static final int MAX_POLY_POINTS = 65536;

    /** Thread-local int buffer for polyline X coordinates. */
    private static final ThreadLocal<int[]> INT_BUF_X =
            ThreadLocal.withInitial(() -> new int[MAX_POLY_POINTS]);

    /** Thread-local int buffer for polyline Y coordinates. */
    private static final ThreadLocal<int[]> INT_BUF_Y =
            ThreadLocal.withInitial(() -> new int[MAX_POLY_POINTS]);

    /** Thread-local reusable Path2D for polygon fills. */
    private static final ThreadLocal<Path2D.Float> PATH_CACHE =
            ThreadLocal.withInitial(() -> new Path2D.Float(Path2D.WIND_NON_ZERO, 1024));

    /** Thread-local reusable Rectangle2D for clipping. */
    private static final ThreadLocal<Rectangle2D.Float> CLIP_RECT_CACHE =
            ThreadLocal.withInitial(Rectangle2D.Float::new);

    private final Graphics2D g2;
    private float strokeWidth = 1f;
    private ArberRect clip;
    private float lastX;
    private float lastY;

    /**
     * Creates a new adapter wrapping the given Graphics2D context.
     *
     * @param g2 the AWT graphics context (non-null)
     */
    public AwtCanvasAdapter(Graphics2D g2) {
        this.g2 = g2;
    }

    @Override
    public void setColor(ArberColor color) {
        int argb = color.argb();
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        g2.setColor(ZeroAllocPool.getColor(r, g, b, a));
    }

    @Override
    public void setStroke(float width) {
        strokeWidth = width;
        g2.setStroke(ZeroAllocPool.getStroke(width));
    }

    @Override
    public void moveTo(float x, float y) {
        lastX = x;
        lastY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        g2.drawLine(Math.round(lastX), Math.round(lastY), Math.round(x), Math.round(y));
        lastX = x;
        lastY = y;
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        n = Math.min(n, MAX_POLY_POINTS);

        // Use thread-local pre-allocated buffers - ZERO ALLOCATION
        int[] xi = INT_BUF_X.get();
        int[] yi = INT_BUF_Y.get();

        for (int i = 0; i < n; i++) {
            xi[i] = Math.round(xs[i]);
            yi[i] = Math.round(ys[i]);
        }
        g2.drawPolyline(xi, yi, n);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        g2.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        g2.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 2) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));

        // Use thread-local reusable Path2D - ZERO ALLOCATION
        Path2D.Float path = PATH_CACHE.get();
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            path.lineTo(xs[i], ys[i]);
        }
        path.closePath();
        g2.fill(path);
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        if (buffer == null) return;
        float[] xs = buffer.x();
        float[] ys = buffer.y();
        int[] colors = buffer.argb();
        int n = buffer.count();
        int size = Math.max(1, Math.round(strokeWidth));

        for (int i = 0; i < n; i++) {
            if (colors != null && i < colors.length) {
                int argb = colors[i];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int gb = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                g2.setColor(ZeroAllocPool.getColor(r, gb, b, a));
            }
            g2.fillRect(Math.round(xs[i]), Math.round(ys[i]), size, size);
        }
    }

    @Override
    public ArberMatrix getTransform() {
        return ArberMatrices.identity();
    }

    @Override
    public void setClip(ArberRect clip) {
        this.clip = clip;
        if (clip == null) {
            g2.setClip(null);
        } else {
            // Use thread-local reusable Rectangle2D - ZERO ALLOCATION
            Rectangle2D.Float rect = CLIP_RECT_CACHE.get();
            rect.setRect((float) clip.x(), (float) clip.y(),
                    (float) clip.width(), (float) clip.height());
            g2.setClip(rect);
        }
    }

    @Override
    public ArberRect getClip() {
        return clip;
    }
}
