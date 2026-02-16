package com.arbergashi.charts.platform.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.platform.swing.util.SwingAssets;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight tooltip overlay styled like the legend (eckig, business-friendly).
 */
public final class TooltipOverlay extends JComponent {
    private ChartTheme theme = ChartThemes.getDarkTheme();
    private final List<String> lines = new ArrayList<>();
    private Dimension preferred = new Dimension(0, 0);
    private Color accent;

    public TooltipOverlay() {
        setOpaque(false);
        setVisible(false);
    }

    public void setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.getDarkTheme();
        repaint();
    }

    public void setLines(List<String> values) {
        lines.clear();
        if (values != null) {
            for (String line : values) {
                if (line != null && !line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        updatePreferredSize();
    }

    public void setAccentColor(Color accent) {
        this.accent = accent;
        repaint();
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public Dimension getPreferredSize() {
        return preferred;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (lines.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            Font font = resolveFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int padding = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.legend.padding", 6f)));
            int rowGap = Math.round(ChartScale.scale(2f));
            int width = preferred.width;
            int height = preferred.height;

            float bgAlpha = ChartAssets.getFloat("Chart.legend.background.alpha", 0.72f);
            float borderAlpha = ChartAssets.getFloat("Chart.legend.border.alpha", 0.18f);
            float brightness = ChartAssets.getFloat("Chart.legend.background.brightness", 0.98f);

            ArberColor baseBg = theme.getBackground();
            ArberColor adjustedBg = ColorUtils.adjustBrightness(baseBg, brightness);
            Color bg = SwingAssets.toAwtColor(ColorUtils.applyAlpha(adjustedBg, bgAlpha));
            Color border = SwingAssets.toAwtColor(ColorUtils.applyAlpha(theme.getAxisLabelColor(), borderAlpha));
            if (accent != null) {
                Color candidate = withAlpha(accent, borderAlpha);
                border = ensureContrast(candidate, bg, SwingAssets.toAwtColor(theme.getAxisLabelColor()));
            }
            Color text = SwingAssets.toAwtColor(theme.getForeground());

            g2.setColor(bg);
            g2.fillRect(0, 0, width, height);
            g2.setColor(border);
            g2.drawRect(0, 0, width - 1, height - 1);

            g2.setColor(text);
            int y = padding + fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, padding, y);
                y += fm.getHeight() + rowGap;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    private void updatePreferredSize() {
        Font font = resolveFont();
        FontMetrics fm = getFontMetrics(font);
        int padding = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.legend.padding", 6f)));
        int rowGap = Math.round(ChartScale.scale(2f));
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(line));
        }
        int height = lines.isEmpty() ? 0 : (lines.size() * fm.getHeight()) + ((lines.size() - 1) * rowGap) + padding * 2;
        int width = maxWidth + padding * 2;
        preferred = new Dimension(Math.max(0, width), Math.max(0, height));
        setSize(preferred);
        revalidate();
        repaint();
    }

    private Font resolveFont() {
        Font base = SwingAssets.toAwtFont(theme.getBaseFont());
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        float fontScale = ChartAssets.getFloat("Chart.legend.fontScale", 1.0f);
        float size = ChartScale.font(base.getSize2D()) * fontScale;
        return base.deriveFont(Font.PLAIN, Math.max(9f, size));
    }

    private static Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(alpha * 255)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private static Color ensureContrast(Color candidate, Color bg, Color fallback) {
        if (candidate == null || bg == null) return candidate != null ? candidate : fallback;
        double ratio = contrastRatio(candidate, bg);
        if (ratio >= 3.0) {
            return candidate;
        }
        // If too low, choose the fallback (theme axis label color).
        return fallback != null ? fallback : candidate;
    }

    private static double contrastRatio(Color c1, Color c2) {
        double l1 = luminance(c1) + 0.05;
        double l2 = luminance(c2) + 0.05;
        return Math.max(l1, l2) / Math.min(l1, l2);
    }

    private static double luminance(Color color) {
        double r = srgbToLinear(color.getRed() / 255.0);
        double g = srgbToLinear(color.getGreen() / 255.0);
        double b = srgbToLinear(color.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double srgbToLinear(double c) {
        return (c <= 0.04045) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
