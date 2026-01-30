package com.arbergashi.charts.core.rendering;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;

/**
 * Core rendering abstraction. No UI framework dependencies.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public interface ArberCanvas {
    void setColor(ArberColor color);
    void setStroke(float width);

    void moveTo(float x, float y);
    void lineTo(float x, float y);
    void drawPolyline(float[] xs, float[] ys, int count);

    void drawRect(float x, float y, float w, float h);
    void fillRect(float x, float y, float w, float h);
    void fillPolygon(float[] xs, float[] ys, int count);

    void drawVoxelField(VoxelBuffer buffer);

    ArberMatrix getTransform();

    /**
     * Sets a clipping rectangle for subsequent draw calls.
     *
     * <p>Backends may treat {@code null} as "no clipping".</p>
     *
     * @param clip clip bounds or {@code null} to clear
     */
    void setClip(ArberRect clip);

    /**
     * Returns the current clipping rectangle, if any.
     */
    ArberRect getClip();

    /**
     * Draws text at the given coordinates using the current style.
     *
     * <p>Backends that do not support text rendering may ignore this call.</p>
     */
    default void drawText(float x, float y, String text) {
    }

    default void drawLine(float x1, float y1, float x2, float y2) {
        moveTo(x1, y1);
        lineTo(x2, y2);
    }

    default void drawRect(ArberRect rect) {
        if (rect == null) return;
        drawRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
    }

    default void fillRect(ArberRect rect) {
        if (rect == null) return;
        fillRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
    }
}
