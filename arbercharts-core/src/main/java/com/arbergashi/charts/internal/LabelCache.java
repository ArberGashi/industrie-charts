package com.arbergashi.charts.internal;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight LRU cache for pre-rendered labels.
 * Provides a single-entry drawing API used by BaseRenderer.
 * The cache keys are derived from text + font + color to ensure visuals stay correct across themes.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class LabelCache {
    private static final int MAX_ENTRIES = 512;
    private final Map<Key, BufferedImage> cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public LabelCache() {
    }

    /**
     * Draws a label at the given position, using a cached pre-rendered image when available.
     */
    public synchronized void drawLabel(Graphics2D g2, String text, Font font, Color color, float x, float y) {
        if (text == null || text.isEmpty()) return;

        FontRenderContext frc = g2.getFontRenderContext();
        int ascent = (int) Math.ceil(font.getLineMetrics(text, frc).getAscent());

        Key key = new Key(text, font, color, ascent);
        BufferedImage img = cache.get(key);
        if (img == null) {
            img = renderToImage(text, font, color, frc);
            cache.put(key, img);
        }

        // draw image aligning baseline to cached ascent
        g2.drawImage(img, Math.round(x), Math.round(y - ascent), null);
    }

    private BufferedImage renderToImage(String text, Font font, Color color, FontRenderContext frc) {
        // measure
        java.awt.font.TextLayout layout = new java.awt.font.TextLayout(text, font, frc);
        Rectangle bounds = layout.getPixelBounds(null, 0, 0);
        int w = Math.max(1, bounds.width);
        int h = Math.max(1, bounds.height);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(font);
            g.setColor(color);
            // draw at baseline
            int ascent = (int) Math.ceil(font.getLineMetrics(text, frc).getAscent());
            g.drawString(text, 0, ascent);
        } finally {
            g.dispose();
        }
        return img;
    }

    private record Key(String text, Font font, Color color, int ascent) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return ascent == k.ascent && text.equals(k.text) && font.equals(k.font) && color.equals(k.color);
        }

    }
}
