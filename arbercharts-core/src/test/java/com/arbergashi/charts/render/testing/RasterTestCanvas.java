package com.arbergashi.charts.render.testing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;

/**
 * Minimal raster canvas for renderer regression tests.
 */
public final class RasterTestCanvas implements ArberCanvas {
    private final int width;
    private final int height;
    private final int[] pixels;
    private int color = 0xFF000000;
    private float stroke = 1f;
    private float lastX;
    private float lastY;
    private ArberRect clip;
    private boolean clipEnabled;
    private int clipX0;
    private int clipY0;
    private int clipX1;
    private int clipY1;

    public RasterTestCanvas(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.pixels = new int[this.width * this.height];
    }

    public int[] pixels() {
        return pixels;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void setColor(ArberColor color) {
        if (color != null) {
            this.color = color.argb();
        }
    }

    @Override
    public void setStroke(float width) {
        this.stroke = Math.max(1f, width);
    }

    @Override
    public void moveTo(float x, float y) {
        lastX = x;
        lastY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        drawLine(Math.round(lastX), Math.round(lastY), Math.round(x), Math.round(y));
        lastX = x;
        lastY = y;
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        int x0 = Math.round(xs[0]);
        int y0 = Math.round(ys[0]);
        for (int i = 1; i < n; i++) {
            int x1 = Math.round(xs[i]);
            int y1 = Math.round(ys[i]);
            drawLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        int x0 = Math.round(x);
        int y0 = Math.round(y);
        int x1 = Math.round(x + w);
        int y1 = Math.round(y + h);
        drawLine(x0, y0, x1, y0);
        drawLine(x1, y0, x1, y1);
        drawLine(x1, y1, x0, y1);
        drawLine(x0, y1, x0, y0);
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        int x0 = Math.max(0, Math.round(x));
        int y0 = Math.max(0, Math.round(y));
        int x1 = Math.min(width - 1, Math.round(x + w));
        int y1 = Math.min(height - 1, Math.round(y + h));
        if (clipEnabled) {
            x0 = Math.max(x0, clipX0);
            y0 = Math.max(y0, clipY0);
            x1 = Math.min(x1, clipX1);
            y1 = Math.min(y1, clipY1);
        }
        for (int yy = y0; yy <= y1; yy++) {
            int row = yy * width;
            for (int xx = x0; xx <= x1; xx++) {
                pixels[row + xx] = color;
            }
        }
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        // Fast outline-only fill for regression tests (keeps determinism, avoids heavy scan fills).
        drawPolyline(xs, ys, count);
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        if (buffer == null) return;
        float[] xs = buffer.x();
        float[] ys = buffer.y();
        int[] argb = buffer.argb();
        int n = buffer.count();
        for (int i = 0; i < n; i++) {
            int x = Math.round(xs[i]);
            int y = Math.round(ys[i]);
            if (x < 0 || y < 0 || x >= width || y >= height) continue;
            if (clipEnabled && (x < clipX0 || x > clipX1 || y < clipY0 || y > clipY1)) continue;
            pixels[y * width + x] = (argb != null && i < argb.length) ? argb[i] : color;
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
            clipEnabled = false;
            return;
        }
        clipEnabled = true;
        clipX0 = (int) Math.floor(clip.x());
        clipY0 = (int) Math.floor(clip.y());
        clipX1 = (int) Math.ceil(clip.x() + clip.width()) - 1;
        clipY1 = (int) Math.ceil(clip.y() + clip.height()) - 1;
    }

    @Override
    public ArberRect getClip() {
        return clip;
    }

    private void drawLine(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        int maxSteps = Math.max(dx, dy) + 2;
        for (int i = 0; i < maxSteps; i++) {
            if (x >= 0 && y >= 0 && x < width && y < height) {
                if (!clipEnabled || (x >= clipX0 && x <= clipX1 && y >= clipY0 && y <= clipY1)) {
                    pixels[y * width + x] = color;
                }
            }
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    // pointInPolygon removed for performance; outline-only is sufficient for deterministic hashes.
}
