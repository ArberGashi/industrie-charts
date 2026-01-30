package com.arbergashi.charts.platform.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.platform.swing.util.SwingAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * Lightweight Swing tooltip for chart overlays.
 */
public final class ChartTooltip extends JComponent {
    private static final int PADDING = 6;
    private static final RoundRectangle2D.Float BOX = new RoundRectangle2D.Float();

    private String text = "";
    private ChartTheme theme;
    private Locale locale = Locale.getDefault();

    public ChartTooltip() {
        setOpaque(false);
        setVisible(false);
    }

    public void setTheme(ChartTheme theme) {
        this.theme = theme;
        repaint();
    }

    public void setText(String text) {
        this.text = (text != null) ? text : "";
        revalidate();
        repaint();
    }

    public void setLocale(Locale locale) {
        this.locale = (locale != null) ? locale : Locale.getDefault();
    }

    @Override
    public java.awt.Dimension getPreferredSize() {
        Font font = resolveFont();
        FontMetrics fm = getFontMetrics(font);
        int w = fm.stringWidth(text) + PADDING * 2;
        int h = fm.getHeight() + PADDING * 2;
        return new java.awt.Dimension(w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (text == null || text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font font = resolveFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int w = getWidth();
            int h = getHeight();

            ArberColor bg = (theme != null) ? theme.getBackground() : ColorRegistry.of(20, 20, 20, 230);
            ArberColor fg = (theme != null) ? theme.getForeground() : ColorRegistry.of(240, 240, 240, 255);
            Color bgAwt = SwingAssets.toAwtColor(bg);
            Color fgAwt = SwingAssets.toAwtColor(fg);

            BOX.setRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g2.setColor(bgAwt);
            g2.fill(BOX);

            g2.setColor(fgAwt);
            int tx = PADDING;
            int ty = PADDING + fm.getAscent();
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }

    private Font resolveFont() {
        ArberFont base = (theme != null) ? theme.getBaseFont() : null;
        Font font = SwingAssets.toAwtFont(base);
        float size = (base != null) ? base.size() : 12f;
        return font.deriveFont(ChartScale.font(size));
    }
}
