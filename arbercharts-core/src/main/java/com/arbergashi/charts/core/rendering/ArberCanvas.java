package com.arbergashi.charts.core.rendering;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.core.geometry.ArberRect;

/**
 * Drawing contract shared by platform-specific rendering backends.
 *
 * <p>Coordinates and stroke widths are expressed in the canvas coordinate
 * system. Raster backends composite colors using source-over alpha blending;
 * their edge rasterization and antialiasing need not be pixel-identical.
 * A canvas belongs to one rendering session and is not required to be
 * thread-safe. The host owns its lifecycle, device scaling and frame reset.</p>
 * @author Arber Gashi
 * @since 2026-01-30
 */
public interface ArberCanvas {
    /**
     * Sets the color for subsequent drawing operations.
     *
     * @param color non-null color with non-premultiplied ARGB channels
     */
    void setColor(ArberColor color);

    /**
     * Sets the width used for stroked geometry.
     *
     * @param width positive, finite width in canvas units
     */
    void setStroke(float width);

    /**
     * Moves the current point without drawing.
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    void moveTo(float x, float y);

    /**
     * Draws a segment from the current point and updates that point.
     * @param x end-point horizontal coordinate
     * @param y end-point vertical coordinate
     */
    void lineTo(float x, float y);

    /**
     * Draws connected segments through the supplied points.
     * Missing arrays or fewer than two available points produce no output.
     * @param xs horizontal coordinates
     * @param ys vertical coordinates
     * @param count maximum number of points to consume, bounded by both arrays
     */
    void drawPolyline(float[] xs, float[] ys, int count);

    /**
     * Strokes the outline of a rectangle using the current color and width.
     * @param x left coordinate
     * @param y top coordinate
     * @param w rectangle width
     * @param h rectangle height
     */
    void drawRect(float x, float y, float w, float h);

    /**
     * Fills a rectangle. Non-positive dimensions produce no output.
     * @param x left coordinate
     * @param y top coordinate
     * @param w rectangle width
     * @param h rectangle height
     */
    void fillRect(float x, float y, float w, float h);

    /**
     * Fills a closed polygon using the current color.
     * Missing arrays or fewer than three available points produce no output.
     * @param xs horizontal coordinates
     * @param ys vertical coordinates
     * @param count maximum number of vertices to consume, bounded by both arrays
     */
    void fillPolygon(float[] xs, float[] ys, int count);

    /**
     * Draws projected voxels as squares with a size derived from the stroke width.
     * Per-voxel colors are temporary; missing colors use the current canvas color,
     * which is preserved after drawing. Missing coordinates produce no output.
     * @param buffer coordinates, optional ARGB colors and a count bounded by the coordinate arrays
     */
    void drawVoxelField(VoxelBuffer buffer);

    /**
     * Returns the backend's transform metadata.
     * @return the transform exposed by this backend
     */
    ArberMatrix getTransform();

    /**
     * Replaces the clipping rectangle for all subsequent draw calls.
     *
     * <p>{@code null} clears the canvas clip. A caller may save the current
     * rectangle with {@link #getClip()} and restore it after a nested render pass.</p>
     *
     * @param clip clip bounds or {@code null} to clear
     */
    void setClip(ArberRect clip);

    /**
     * Returns the current clipping rectangle, if any.
     * @return the rectangle last set, or {@code null} when the canvas clip is cleared
     */
    ArberRect getClip();

    /**
     * Selects the font for subsequent text drawing and measurement.
     *
     * <p>A {@code null} descriptor selects a plain, 12-unit sans-serif font.
     * Text-capable backends use the selected font for both drawing and metrics.
     * Backends without text support may ignore a valid descriptor.</p>
     *
     * @param font core-safe font descriptor, or {@code null} for the default
     * @throws IllegalArgumentException if the requested size is not positive and finite
     */
    default void setFont(ArberFont font) {
        if (font != null && (!Float.isFinite(font.size()) || font.size() <= 0f)) {
            throw new IllegalArgumentException("Font size must be positive and finite");
        }
    }

    /**
     * Draws a single line of text from its left baseline using the current color
     * and font. The current clip and transform also apply to text.
     *
     * <p>Null or empty text and non-finite coordinates produce no output.
     * Backends without text support may ignore this call. The version-one native
     * command stream can encode text but leaves font selection and metrics to
     * its consumer. Drawing text does not move the current
     * geometry point or change the color, stroke or clip.</p>
     *
     * @param x left baseline coordinate
     * @param y vertical baseline coordinate
     * @param text single-line text to draw, or {@code null} for no output
     */
    default void drawText(float x, float y, String text) {
    }

    /**
     * Measures the horizontal advance of text in the selected font.
     *
     * <p>Text-capable backends return their actual font metrics in canvas units.
     * The default is only a layout estimate of seven units per Unicode code
     * point for a 12-unit font; it does not imply that the backend can draw text.
     * Use the same font for measurement and drawing.</p>
     *
     * @param text single-line text to measure, or {@code null}
     * @return horizontal advance, or zero for null or empty text
     */
    default float measureTextWidth(String text) {
        return text == null ? 0f : text.codePointCount(0, text.length()) * 7f;
    }

    /**
     * Returns the selected font's baseline-to-baseline line spacing.
     *
     * <p>The default is a 14-unit estimate for backends without font metrics.</p>
     *
     * @return positive line spacing in canvas units
     */
    default float getTextLineHeight() {
        return 14f;
    }

    /**
     * Returns the distance from the top of the font's line box to its baseline.
     * Add this value to a top-aligned position before calling {@link #drawText}.
     *
     * <p>The default is an 11-unit estimate for backends without font metrics.</p>
     *
     * @return positive ascent in canvas units
     */
    default float getTextAscent() {
        return 11f;
    }

    /**
     * Draws a line and leaves the current point at its end.
     * @param x1 start-point horizontal coordinate
     * @param y1 start-point vertical coordinate
     * @param x2 end-point horizontal coordinate
     * @param y2 end-point vertical coordinate
     */
    default void drawLine(float x1, float y1, float x2, float y2) {
        moveTo(x1, y1);
        lineTo(x2, y2);
    }

    /**
     * Strokes the outline of the supplied rectangle.
     * @param rect rectangle to draw, or {@code null} for no output
     */
    default void drawRect(ArberRect rect) {
        if (rect == null) return;
        drawRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
    }

    /**
     * Fills the supplied rectangle.
     * @param rect rectangle to fill, or {@code null} for no output
     */
    default void fillRect(ArberRect rect) {
        if (rect == null) return;
        fillRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
    }
}
