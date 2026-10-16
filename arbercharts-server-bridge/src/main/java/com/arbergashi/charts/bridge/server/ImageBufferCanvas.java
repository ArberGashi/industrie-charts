package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Headless canvas backed by a non-premultiplied ARGB pixel buffer.
 *
 * <p>Java2D supplies clipping, stroked paths and source-over compositing. Paths
 * and bounded style caches are reused between draw calls. Instances belong to
 * one rendering session and must not be used concurrently.</p>
 */
public final class ImageBufferCanvas implements ArberCanvas, AutoCloseable {
    private static final int STYLE_CACHE_SIZE = 256;
    private static final Font DEFAULT_TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    private final int width;
    private final int height;
    private final int[] pixels;
    private final Graphics2D graphics;
    private final Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 1024);
    private final Rectangle2D.Float clipRect = new Rectangle2D.Float();
    private final Color[] colors = new Color[STYLE_CACHE_SIZE];
    private final BasicStroke[] strokes = new BasicStroke[STYLE_CACHE_SIZE];
    private int color;
    private float stroke;
    private float lastX;
    private float lastY;
    private ArberRect clip;
    private boolean closed;
    private ArberFont cachedFontDescriptor;
    private Font cachedTextFont;

    /**
     * Creates a transparent canvas; each dimension is at least one pixel.
     *
     * @param width requested pixel width
     * @param height requested pixel height
     */
    public ImageBufferCanvas(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        this.graphics = image.createGraphics();
        reset();
    }

    /**
     * Returns the live ARGB pixel buffer, which remains readable after close.
     *
     * @return pixels in row-major order, with non-premultiplied color channels
     */
    public int[] pixels() { return pixels; }

    /** @return canvas width in pixels */
    public int width() { return width; }

    /** @return canvas height in pixels */
    public int height() { return height; }

    /**
     * Clears the buffer and restores black, a one-pixel stroke, no clip and the
     * origin as the current point for a new frame. Text uses a plain, 12-pixel
     * sans-serif font after reset.
     *
     * @throws IllegalStateException if the canvas is closed
     */
    public void reset() {
        ensureOpen();
        Arrays.fill(pixels, 0);
        graphics.setComposite(AlphaComposite.SrcOver);
        setClip(null);
        color = 0xFF000000;
        graphics.setColor(cachedColor(color));
        setStroke(1f);
        setFont(null);
        lastX = 0f;
        lastY = 0f;
        path.reset();
    }

    @Override
    public void setColor(ArberColor color) {
        ensureOpen();
        this.color = color.argb();
        graphics.setColor(cachedColor(this.color));
    }

    @Override
    public void setStroke(float width) {
        ensureOpen();
        BasicStroke cached = cachedStroke(width);
        stroke = width;
        graphics.setStroke(cached);
    }

    @Override
    public void setFont(ArberFont font) {
        ensureOpen();
        ArberCanvas.super.setFont(font);
        if (font == null) {
            graphics.setFont(DEFAULT_TEXT_FONT);
            return;
        }
        if (!font.equals(cachedFontDescriptor)) {
            String name = font.name() == null || font.name().isBlank() ? Font.SANS_SERIF : font.name();
            cachedTextFont = new Font(name, font.style(), 1).deriveFont(font.size());
            cachedFontDescriptor = font;
        }
        graphics.setFont(cachedTextFont);
    }

    @Override
    public void drawText(float x, float y, String text) {
        ensureOpen();
        if (text == null || text.isEmpty() || !Float.isFinite(x) || !Float.isFinite(y)) return;
        graphics.drawString(text, x, y);
    }

    @Override
    public float measureTextWidth(String text) {
        ensureOpen();
        return text == null || text.isEmpty() ? 0f : graphics.getFontMetrics().stringWidth(text);
    }

    @Override
    public float getTextLineHeight() {
        ensureOpen();
        return graphics.getFontMetrics().getHeight();
    }

    @Override
    public float getTextAscent() {
        ensureOpen();
        return graphics.getFontMetrics().getAscent();
    }

    @Override
    public void moveTo(float x, float y) {
        ensureOpen();
        lastX = x;
        lastY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        ensureOpen();
        if (fitsInteger(lastX) && fitsInteger(lastY) && fitsInteger(x) && fitsInteger(y)
                && Math.abs((double) x - lastX) <= Integer.MAX_VALUE
                && Math.abs((double) y - lastY) <= Integer.MAX_VALUE) {
            graphics.drawLine(Math.round(lastX), Math.round(lastY), Math.round(x), Math.round(y));
        } else {
            // Keep large coordinates intact until the path rasterizer clips them.
            path.reset();
            path.moveTo(roundedCoordinate(lastX), roundedCoordinate(lastY));
            path.lineTo(roundedCoordinate(x), roundedCoordinate(y));
            graphics.draw(path);
        }
        lastX = x;
        lastY = y;
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        ensureOpen();
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 1) return;
        path.reset();
        path.moveTo(roundedCoordinate(xs[0]), roundedCoordinate(ys[0]));
        for (int i = 1; i < n; i++) {
            path.lineTo(roundedCoordinate(xs[i]), roundedCoordinate(ys[i]));
        }
        graphics.draw(path);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        ensureOpen();
        if (fitsIntegerRectangle(x, y, w, h)) {
            graphics.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        } else if (roundedCoordinate(w) >= 0 && roundedCoordinate(h) >= 0) {
            graphics.draw(rectanglePath(x, y, w, h));
        }
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        ensureOpen();
        if (fitsIntegerRectangle(x, y, w, h)) {
            graphics.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        } else if (roundedCoordinate(w) > 0 && roundedCoordinate(h) > 0) {
            graphics.fill(rectanglePath(x, y, w, h));
        }
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        ensureOpen();
        if (xs == null || ys == null || count <= 2) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 2) return;
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            path.lineTo(xs[i], ys[i]);
        }
        path.closePath();
        graphics.fill(path);
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        ensureOpen();
        if (buffer == null) return;
        float[] xs = buffer.x();
        float[] ys = buffer.y();
        if (xs == null || ys == null) return;
        int[] argb = buffer.argb();
        int n = Math.min(buffer.count(), Math.min(xs.length, ys.length));
        int size = Math.max(1, Math.round(stroke));
        try {
            for (int i = 0; i < n; i++) {
                graphics.setColor(cachedColor(argb != null && i < argb.length ? argb[i] : color));
                graphics.fillRect(Math.round(xs[i]), Math.round(ys[i]), size, size);
            }
        } finally {
            graphics.setColor(cachedColor(color));
        }
    }

    @Override
    public ArberMatrix getTransform() {
        return ArberMatrices.identity();
    }

    @Override
    public void setClip(ArberRect clip) {
        ensureOpen();
        this.clip = clip;
        if (clip == null) {
            graphics.setClip(null);
        } else {
            clipRect.setRect(clip.x(), clip.y(), clip.width(), clip.height());
            graphics.setClip(clipRect);
        }
    }

    @Override
    public ArberRect getClip() {
        return clip;
    }

    /** Releases the drawing context. Repeated calls have no effect. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            graphics.dispose();
        }
    }

    private Color cachedColor(int argb) {
        int slot = styleSlot(argb);
        Color cached = colors[slot];
        if (cached == null || cached.getRGB() != argb) {
            cached = new Color(argb, true);
            colors[slot] = cached;
        }
        return cached;
    }

    private BasicStroke cachedStroke(float width) {
        int bits = Float.floatToIntBits(width);
        int slot = styleSlot(bits);
        BasicStroke cached = strokes[slot];
        if (cached == null || cached.getLineWidth() != width) {
            cached = new BasicStroke(width);
            strokes[slot] = cached;
        }
        return cached;
    }

    private static int styleSlot(int bits) {
        int mixed = (bits ^ (bits >>> 16)) * 0x7FEB352D;
        return (mixed ^ (mixed >>> 15)) & (STYLE_CACHE_SIZE - 1);
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

    private Path2D.Float rectanglePath(float x, float y, float w, float h) {
        double left = roundedCoordinate(x);
        double top = roundedCoordinate(y);
        double right = left + roundedCoordinate(w);
        double bottom = top + roundedCoordinate(h);
        path.reset();
        path.moveTo(left, top);
        path.lineTo(right, top);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.closePath();
        return path;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Canvas is closed");
        }
    }
}
