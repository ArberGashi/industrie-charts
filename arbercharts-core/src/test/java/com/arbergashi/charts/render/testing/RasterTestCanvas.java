package com.arbergashi.charts.render.testing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Test-only Java2D reference canvas with actual fills, clipping and source-over
 * compositing. Production Core remains independent of java.desktop.
 */
public final class RasterTestCanvas implements ArberCanvas, AutoCloseable {
    private final BufferedImage image;
    private final Graphics2D graphics;
    private final Path2D.Float path = new Path2D.Float();
    private float lastX;
    private float lastY;
    private ArberRect clip;

    public RasterTestCanvas(int width, int height) {
        image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    public int[] pixels() { return ((DataBufferInt) image.getRaster().getDataBuffer()).getData(); }
    public int width() { return image.getWidth(); }
    public int height() { return image.getHeight(); }

    @Override
    public void setColor(ArberColor color) {
        if (color != null) graphics.setColor(new Color(color.argb(), true));
    }

    @Override
    public void setStroke(float width) {
        if (Float.isFinite(width) && width > 0) graphics.setStroke(new BasicStroke(width));
    }

    @Override
    public void moveTo(float x, float y) { lastX = x; lastY = y; }

    @Override
    public void lineTo(float x, float y) {
        path.reset();
        path.moveTo(lastX, lastY);
        path.lineTo(x, y);
        graphics.draw(path);
        moveTo(x, y);
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        int n = available(xs, ys, count);
        if (n < 2) return;
        buildPath(xs, ys, n);
        graphics.draw(path);
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        if (width < 0 || height < 0) return;
        graphics.draw(new Rectangle2D.Float(x, y, width, height));
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        if (!(width > 0) || !(height > 0)) return;
        graphics.fill(new Rectangle2D.Float(x, y, width, height));
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        int n = available(xs, ys, count);
        if (n < 3) return;
        buildPath(xs, ys, n);
        path.closePath();
        graphics.fill(path);
    }

    private static int available(float[] xs, float[] ys, int count) {
        return xs == null || ys == null ? 0 : Math.min(count, Math.min(xs.length, ys.length));
    }

    private void buildPath(float[] xs, float[] ys, int count) {
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) path.lineTo(xs[i], ys[i]);
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        if (buffer == null || buffer.x() == null || buffer.y() == null) return;
        Color original = graphics.getColor();
        float size = ((BasicStroke) graphics.getStroke()).getLineWidth();
        int n = available(buffer.x(), buffer.y(), buffer.count());
        try {
            for (int i = 0; i < n; i++) {
                graphics.setColor(buffer.argb() != null && i < buffer.argb().length
                        ? new Color(buffer.argb()[i], true) : original);
                fillRect(buffer.x()[i] - size / 2, buffer.y()[i] - size / 2, size, size);
            }
        } finally {
            graphics.setColor(original);
        }
    }

    @Override public ArberMatrix getTransform() { return ArberMatrices.identity(); }

    @Override
    public void setClip(ArberRect value) {
        clip = value;
        graphics.setClip(value == null ? null : new Rectangle2D.Double(value.x(), value.y(), value.width(), value.height()));
    }

    @Override public ArberRect getClip() { return clip; }

    @Override
    public void setFont(ArberFont font) {
        if (font != null) graphics.setFont(new Font(font.name(), font.style(), Math.max(1, Math.round(font.size()))));
    }

    @Override public float measureTextWidth(String text) { return graphics.getFontMetrics().stringWidth(text); }
    @Override public float getTextLineHeight() { return graphics.getFontMetrics().getHeight(); }
    @Override public float getTextAscent() { return graphics.getFontMetrics().getAscent(); }

    @Override
    public void drawText(float x, float y, String text) {
        if (text != null && !text.isEmpty() && Float.isFinite(x) && Float.isFinite(y)) graphics.drawString(text, x, y);
    }

    @Override public void close() { graphics.dispose(); }
}
