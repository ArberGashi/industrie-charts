package com.arbergashi.charts.platform.swing.util;

import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.util.ChartScale;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;

/**
 * Applies render hints to Swing Graphics2D based on core hints.
 */
public final class SwingRenderHints {
    private SwingRenderHints() {
    }

    public static void apply(ChartRenderHints hints, Graphics2D g2) {
        if (g2 == null || hints == null) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                hints.isAntialiasing() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        Float width = hints.getStrokeWidth();
        if (width != null && Float.isFinite(width) && width > 0f) {
            g2.setStroke(new BasicStroke(ChartScale.scale(width)));
        }
    }
}
