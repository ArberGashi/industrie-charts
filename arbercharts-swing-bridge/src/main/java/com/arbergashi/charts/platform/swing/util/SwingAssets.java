package com.arbergashi.charts.platform.swing.util;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.core.geometry.ArberRect;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

/**
 * Swing bridge utilities for converting core types to AWT equivalents.
 */
public final class SwingAssets {
    private SwingAssets() {
    }

    public static Color toAwtColor(ArberColor color) {
        if (color == null) {
            return new Color(0, 0, 0, 0);
        }
        int argb = color.argb();
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    public static Font toAwtFont(ArberFont font) {
        if (font == null) {
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        int style = Font.PLAIN;
        if (font.style() == ArberFont.BOLD) {
            style = Font.BOLD;
        } else if (font.style() == ArberFont.ITALIC) {
            style = Font.ITALIC;
        }
        return new Font(font.name(), style, Math.round(font.size()));
    }

    public static Rectangle2D toAwtRect(ArberRect rect) {
        if (rect == null) {
            return new Rectangle2D.Double();
        }
        return new Rectangle2D.Double(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
