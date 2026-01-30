package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.VoxelBuffer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;

/**
 * Graphics2D-backed ArberCanvas implementation (Swing/legacy bridge).
 */
public final class AwtCanvasAdapter implements ArberCanvas {
    private final Graphics2D g2;
    private float strokeWidth = 1f;
    private ArberRect clip;

    public AwtCanvasAdapter(Graphics2D g2) {
        this.g2 = g2;
    }

    @Override
    public void setColor(ArberColor color) {
        g2.setColor(new Color(color.argb(), true));
    }

    @Override
    public void setStroke(float width) {
        strokeWidth = width;
        g2.setStroke(new BasicStroke(width));
    }

    @Override
    public void moveTo(float x, float y) {
        // no-op for immediate mode; use lineTo or drawPolyline
    }

    @Override
    public void lineTo(float x, float y) {
        g2.drawLine(Math.round(x), Math.round(y), Math.round(x), Math.round(y));
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        int[] xi = new int[n];
        int[] yi = new int[n];
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
        Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, n);
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            path.lineTo(xs[i], ys[i]);
        }
        path.closePath();
        g2.fill(path);
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        // Swing bridge draws voxels as points for now.
        if (buffer == null) return;
        float[] xs = buffer.x();
        float[] ys = buffer.y();
        int[] colors = buffer.argb();
        int n = buffer.count();
        for (int i = 0; i < n; i++) {
            if (colors != null && i < colors.length) {
                g2.setColor(new Color(colors[i], true));
            }
            g2.fillRect(Math.round(xs[i]), Math.round(ys[i]), Math.max(1, Math.round(strokeWidth)), Math.max(1, Math.round(strokeWidth)));
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
            g2.setClip(new Rectangle2D.Float((float) clip.x(), (float) clip.y(),
                    (float) clip.width(), (float) clip.height()));
        }
    }

    @Override
    public ArberRect getClip() {
        return clip;
    }
}
