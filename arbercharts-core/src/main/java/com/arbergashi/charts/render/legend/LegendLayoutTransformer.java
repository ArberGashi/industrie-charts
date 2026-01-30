package com.arbergashi.charts.render.legend;

import com.arbergashi.charts.core.geometry.ArberInsets;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.geometry.ArberSize;
import com.arbergashi.charts.core.geometry.TextAnchor;
import com.arbergashi.charts.domain.legend.LegendPosition;
/**
 * Computes legend placement rectangles based on {@link LegendPosition}.
 *
 * Implementation note: part of the ArberCharts High-Integrity Architecture Doctrine.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class LegendLayoutTransformer {

    private LegendLayoutTransformer() {}

    /**
     * Computes a legend rectangle anchored within the given bounds.
     *
     * <p>The returned rectangle is clamped so it always stays fully inside {@code bounds}.</p>
     *
     * @param position the anchor position
     * @param bounds the available area (typically the component bounds)
     * @param size the preferred legend size
     * @param padding padding from the edges (applied relative to the anchor)
     * @return legend rectangle, clamped to bounds
     */
    public static ArberRect place(LegendPosition position, ArberRect bounds, ArberSize size, ArberInsets padding) {
        LegendPosition pos = (position != null) ? position : LegendPosition.TOP_LEFT;
        ArberRect safeBounds = (bounds != null) ? bounds : new ArberRect(0, 0, 0, 0);
        ArberSize safeSize = (size != null) ? size : new ArberSize(0, 0);
        ArberInsets safePadding = (padding != null) ? padding : new ArberInsets(0, 0, 0, 0);

        double w = Math.max(0, Math.min(safeSize.width(), safeBounds.width()));
        double h = Math.max(0, Math.min(safeSize.height(), safeBounds.height()));

        double x;
        double y;

        // horizontal
        switch (pos) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> x = safeBounds.x() + (safeBounds.width() - w) / 2.0;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> x = safeBounds.x() + safeBounds.width() - w;
            default -> x = safeBounds.x();
        }

        // vertical
        switch (pos) {
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> y = safeBounds.y() + (safeBounds.height() - h) / 2.0;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> y = safeBounds.y() + safeBounds.height() - h;
            default -> y = safeBounds.y();
        }

        // apply padding relative to the anchor direction
        boolean isLeft = (pos == LegendPosition.TOP_LEFT || pos == LegendPosition.MIDDLE_LEFT || pos == LegendPosition.BOTTOM_LEFT);
        boolean isRight = (pos == LegendPosition.TOP_RIGHT || pos == LegendPosition.MIDDLE_RIGHT || pos == LegendPosition.BOTTOM_RIGHT);
        boolean isTop = (pos == LegendPosition.TOP_LEFT || pos == LegendPosition.TOP_CENTER || pos == LegendPosition.TOP_RIGHT);
        boolean isBottom = (pos == LegendPosition.BOTTOM_LEFT || pos == LegendPosition.BOTTOM_CENTER || pos == LegendPosition.BOTTOM_RIGHT);

        if (isLeft) x += safePadding.left();
        if (isRight) x -= safePadding.right();
        if (!isLeft && !isRight) x += (safePadding.left() - safePadding.right()) / 2.0;

        if (isTop) y += safePadding.top();
        if (isBottom) y -= safePadding.bottom();
        if (!isTop && !isBottom) y += (safePadding.top() - safePadding.bottom()) / 2.0;

        // clamp
        x = Math.max(safeBounds.x(), Math.min(x, safeBounds.x() + safeBounds.width() - w));
        y = Math.max(safeBounds.y(), Math.min(y, safeBounds.y() + safeBounds.height() - h));

        return new ArberRect(x, y, w, h);
    }

    /**
     * Returns a text anchor that matches the legend position intent.
     */
    public static TextAnchor anchorFor(LegendPosition position) {
        LegendPosition pos = (position != null) ? position : LegendPosition.TOP_LEFT;
        return switch (pos) {
            case TOP_LEFT -> TextAnchor.TOP_LEFT;
            case TOP_CENTER -> TextAnchor.TOP_CENTER;
            case TOP_RIGHT -> TextAnchor.TOP_RIGHT;
            case MIDDLE_LEFT -> TextAnchor.MIDDLE_LEFT;
            case CENTER -> TextAnchor.CENTER;
            case MIDDLE_RIGHT -> TextAnchor.MIDDLE_RIGHT;
            case BOTTOM_LEFT -> TextAnchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> TextAnchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> TextAnchor.BOTTOM_RIGHT;
        };
    }
}
