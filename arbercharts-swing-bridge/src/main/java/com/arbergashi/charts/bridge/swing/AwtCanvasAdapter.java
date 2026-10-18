package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.engine.allocation.ZeroAllocPool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;

/**
 * Graphics2D-backed ArberCanvas implementation (Swing/legacy bridge).
 *
 * <p>The adapter reuses thread-local coordinate and path buffers and drawing
 * styles from {@link ZeroAllocPool}. Initialization, path-buffer growth and
 * style-cache access may allocate.</p>
 *
 * @since 2.0.0
 */
public final class AwtCanvasAdapter implements ArberCanvas {
    private static final Font DEFAULT_TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    /** Capacity of the reusable integer buffers for ordinary polylines. */
    private static final int POLYLINE_BUFFER_CAPACITY = 65536;

    /** Thread-local int buffer for polyline X coordinates. */
    private static final ThreadLocal<int[]> INT_BUF_X =
            ThreadLocal.withInitial(() -> new int[POLYLINE_BUFFER_CAPACITY]);

    /** Thread-local int buffer for polyline Y coordinates. */
    private static final ThreadLocal<int[]> INT_BUF_Y =
            ThreadLocal.withInitial(() -> new int[POLYLINE_BUFFER_CAPACITY]);

    /** Reusable path for fills and extended polylines; capacity grows as needed. */
    private static final ThreadLocal<Path2D.Float> PATH_CACHE =
            ThreadLocal.withInitial(() -> new Path2D.Float(Path2D.WIND_NON_ZERO, 1024));

    /** Thread-local reusable Rectangle2D for clipping. */
    private static final ThreadLocal<Rectangle2D.Float> CLIP_RECT_CACHE =
            ThreadLocal.withInitial(Rectangle2D.Float::new);

    private final Graphics2D g2;
    private float strokeWidth = 1f;
    private float lastX;
    private float lastY;
    private ArberFont cachedFontDescriptor;
    private Font cachedTextFont;

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
    public void setFont(ArberFont font) {
        ArberCanvas.super.setFont(font);
        if (font == null) {
            g2.setFont(DEFAULT_TEXT_FONT);
            return;
        }
        if (!font.equals(cachedFontDescriptor)) {
            String name = font.name() == null || font.name().isBlank() ? Font.SANS_SERIF : font.name();
            cachedTextFont = new Font(name, font.style(), 1).deriveFont(font.size());
            cachedFontDescriptor = font;
        }
        g2.setFont(cachedTextFont);
    }

    @Override
    public void drawText(float x, float y, String text) {
        if (text == null || text.isEmpty() || !Float.isFinite(x) || !Float.isFinite(y)) return;
        g2.drawString(text, x, y);
    }

    @Override
    public float measureTextWidth(String text) {
        return text == null || text.isEmpty() ? 0f : g2.getFontMetrics().stringWidth(text);
    }

    @Override
    public float getTextLineHeight() {
        return g2.getFontMetrics().getHeight();
    }

    @Override
    public float getTextAscent() {
        return g2.getFontMetrics().getAscent();
    }

    @Override
    public void moveTo(float x, float y) {
        lastX = x;
        lastY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        if (fitsInteger(lastX) && fitsInteger(lastY) && fitsInteger(x) && fitsInteger(y)
                && Math.abs((double) x - lastX) <= Integer.MAX_VALUE
                && Math.abs((double) y - lastY) <= Integer.MAX_VALUE) {
            g2.drawLine(Math.round(lastX), Math.round(lastY), Math.round(x), Math.round(y));
        } else {
            // The integer fast path can overflow before Java2D clips the line.
            Path2D.Float path = PATH_CACHE.get();
            path.reset();
            path.moveTo(roundedCoordinate(lastX), roundedCoordinate(lastY));
            path.lineTo(roundedCoordinate(x), roundedCoordinate(y));
            g2.draw(path);
        }
        lastX = x;
        lastY = y;
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 1) return;
        if (n > POLYLINE_BUFFER_CAPACITY) {
            drawPolylinePath(xs, ys, n);
            return;
        }

        // Reuse fixed-size coordinate buffers for the ordinary integer path.
        int[] xi = INT_BUF_X.get();
        int[] yi = INT_BUF_Y.get();

        boolean integerSafe = true;
        for (int i = 0; i < n; i++) {
            integerSafe &= fitsInteger(xs[i]) && fitsInteger(ys[i]);
            if (i > 0) {
                integerSafe &= Math.abs((double) xs[i] - xs[i - 1]) <= Integer.MAX_VALUE
                        && Math.abs((double) ys[i] - ys[i - 1]) <= Integer.MAX_VALUE;
            }
            xi[i] = Math.round(xs[i]);
            yi[i] = Math.round(ys[i]);
        }
        if (integerSafe) {
            if (n == 2) {
                g2.drawLine(xi[0], yi[0], xi[1], yi[1]);
            } else {
                g2.drawPolyline(xi, yi, n);
            }
        } else {
            drawPolylinePath(xs, ys, n);
        }
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        if (fitsIntegerRectangle(x, y, w, h)) {
            g2.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        } else if (roundedCoordinate(w) >= 0 && roundedCoordinate(h) >= 0) {
            g2.draw(rectanglePath(x, y, w, h));
        }
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        if (fitsIntegerRectangle(x, y, w, h)) {
            g2.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        } else if (roundedCoordinate(w) > 0 && roundedCoordinate(h) > 0) {
            g2.fill(rectanglePath(x, y, w, h));
        }
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 2) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 2) return;

        // Reuse the path; larger polygons may grow its backing buffers.
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
        if (xs == null || ys == null) return;
        int[] colors = buffer.argb();
        int n = Math.min(buffer.count(), Math.min(xs.length, ys.length));
        int size = Math.max(1, Math.round(strokeWidth));
        Color baseColor = g2.getColor();

        try {
            for (int i = 0; i < n; i++) {
                if (colors != null && i < colors.length) {
                    int argb = colors[i];
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int gb = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    g2.setColor(ZeroAllocPool.getColor(r, gb, b, a));
                } else {
                    g2.setColor(baseColor);
                }
                g2.fillRect(Math.round(xs[i]), Math.round(ys[i]), size, size);
            }
        } finally {
            g2.setColor(baseColor);
        }
    }

    @Override
    public ArberMatrix getTransform() {
        return ArberMatrices.identity();
    }

    @Override
    public void setClip(ArberRect clip) {
        if (clip == null) {
            g2.setClip(null);
        } else {
            // Reuse the clipping rectangle across calls on this thread.
            Rectangle2D.Float rect = CLIP_RECT_CACHE.get();
            rect.setRect((float) clip.x(), (float) clip.y(),
                    (float) clip.width(), (float) clip.height());
            g2.setClip(rect);
        }
    }

    /**
     * Returns the current user-space clip bounds, including rectangular clips
     * applied directly by the Swing host. The canvas clipping contract is rectangular.
     * @return current clip bounds, or {@code null} when no user clip is set
     */
    @Override
    public ArberRect getClip() {
        // The Swing host can update the Graphics2D clip between renderer calls.
        // Read its current user-space bounds instead of retaining stale adapter state.
        Shape current = g2.getClip();
        if (current == null) return null;
        Rectangle2D bounds = current.getBounds2D();
        return new ArberRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    private void drawPolylinePath(float[] xs, float[] ys, int count) {
        Path2D.Float path = PATH_CACHE.get();
        path.reset();
        path.moveTo(roundedCoordinate(xs[0]), roundedCoordinate(ys[0]));
        for (int i = 1; i < count; i++) {
            path.lineTo(roundedCoordinate(xs[i]), roundedCoordinate(ys[i]));
        }
        g2.draw(path);
    }

    private static boolean fitsInteger(float value) {
        return (double) value >= Integer.MIN_VALUE && (double) value <= Integer.MAX_VALUE;
    }

    private static double roundedCoordinate(float value) {
        return Math.floor((double) value + 0.5);
    }

    private static boolean fitsIntegerRectangle(float x, float y, float w, float h) {
        return fitsInteger(x) && fitsInteger(y) && fitsInteger(w) && fitsInteger(h)
                && (long) Math.round(x) + Math.round(w) <= Integer.MAX_VALUE
                && (long) Math.round(y) + Math.round(h) <= Integer.MAX_VALUE;
    }

    private static Path2D.Float rectanglePath(float x, float y, float w, float h) {
        double left = roundedCoordinate(x);
        double top = roundedCoordinate(y);
        double right = left + roundedCoordinate(w);
        double bottom = top + roundedCoordinate(h);
        Path2D.Float path = PATH_CACHE.get();
        path.reset();
        path.moveTo(left, top);
        path.lineTo(right, top);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.closePath();
        return path;
    }
}
