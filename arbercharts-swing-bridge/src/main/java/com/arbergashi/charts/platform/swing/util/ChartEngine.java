package com.arbergashi.charts.platform.swing.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Swing rendering helpers.
 */
public final class ChartEngine {
    private ChartEngine() {
    }

    public static void prepareGraphics(Graphics2D g2, boolean enableTextAntialias) {
        if (g2 == null) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (enableTextAntialias) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }
}
