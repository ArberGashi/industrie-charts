package com.arbergashi.charts.rendererpanels;

import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Polished presentation wrapper for demo charts.
 */
public final class DemoPresentationPanel extends JPanel implements ChartHost {

    private final ArberChartPanel chartPanel;
    private boolean initialZoomDone;

    public DemoPresentationPanel(ArberChartPanel chartPanel, DemoPanelUtils.PresentationMeta meta) {
        this(chartPanel, chartPanel, meta);
    }

    public DemoPresentationPanel(JComponent content, ArberChartPanel chartPanel, DemoPanelUtils.PresentationMeta meta) {
        super(new BorderLayout());
        this.chartPanel = chartPanel;
        setOpaque(false);

        JPanel header = buildHeader(meta);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(scale(4), scale(14), scale(10), scale(14)));

        CardPanel card = new CardPanel();
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(scale(10), scale(10), scale(10), scale(10)));
        card.add(content, BorderLayout.CENTER);
        chartPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (initialZoomDone) return;
                if (chartPanel.getWidth() > 0 && chartPanel.getHeight() > 0) {
                    initialZoomDone = true;
                    chartPanel.resetZoom();
                }
            }
        });

        body.add(card, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public ArberChartPanel getChartPanel() {
        return chartPanel;
    }

    private JPanel buildHeader(DemoPanelUtils.PresentationMeta meta) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(scale(8), scale(14), scale(5), scale(14)));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        Font titleFont = base.deriveFont(Font.BOLD, ChartScale.uiFontSize(base, 15.5f));
        Font subtitleFont = base.deriveFont(Font.PLAIN, ChartScale.uiFontSize(base, 10.5f));

        JLabel title = new JLabel(meta.title());
        title.setFont(titleFont);
        title.setForeground(UIManager.getColor("Label.foreground"));

        JPanel accent = new JPanel();
        accent.setOpaque(true);
        accent.setBackground(resolveAccentColor());
        accent.setPreferredSize(new Dimension(scale(22), scale(2)));

        JLabel subtitle = new JLabel(meta.subtitle());
        subtitle.setFont(subtitleFont);
        subtitle.setForeground(mutedColor());

        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(scale(3)));
        textBlock.add(accent);
        textBlock.add(Box.createVerticalStrut(scale(3)));
        textBlock.add(subtitle);

        JPanel tags = buildTagRow(meta.tags());
        JPanel metrics = buildMetricRow(meta.metrics());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(tags);
        right.add(Box.createVerticalStrut(scale(3)));
        right.add(metrics);

        header.add(textBlock, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JSeparator separator = new JSeparator();
        separator.setForeground(borderColor());
        header.add(separator, BorderLayout.SOUTH);

        return header;
    }

    private JPanel buildTagRow(String[] tags) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, scale(6), 0));
        row.setOpaque(false);
        if (tags == null) return row;
        for (String tag : tags) {
            row.add(makeChip(tag));
        }
        return row;
    }

    private JPanel buildMetricRow(String[] metrics) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        if (metrics == null) return row;
        String text = String.join("  |  ", metrics);
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, ChartScale.uiFontSize(label.getFont(), 11f)));
        label.setForeground(mutedColor());
        row.add(label);
        return row;
    }

    private Chip makeChip(String text) {
        return new Chip(text);
    }

    private Color borderColor() {
        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) {
            throw new IllegalStateException("Missing Component.borderColor in FlatLaf theme properties.");
        }
        return ColorUtils.withAlpha(border, 0.6f);
    }

    private Color mutedColor() {
        Color fg = UIManager.getColor("Chart.foreground");
        if (fg == null) {
            throw new IllegalStateException("Missing Chart.foreground in FlatLaf theme properties.");
        }
        return ColorUtils.withAlpha(fg, 0.7f);
    }

    private Color chipBackground() {
        Color bg = UIManager.getColor("Chart.background");
        if (bg == null) {
            throw new IllegalStateException("Missing Chart.background in FlatLaf theme properties.");
        }
        return ColorUtils.withAlpha(bg, 0.85f);
    }

    private Color chipForeground() {
        Color fg = UIManager.getColor("Chart.foreground");
        if (fg == null) {
            throw new IllegalStateException("Missing Chart.foreground in FlatLaf theme properties.");
        }
        return fg;
    }

    private Color resolveAccentColor() {
        Color accent = UIManager.getColor("Chart.accent.blue");
        if (accent == null) {
            throw new IllegalStateException("Missing Chart.accent.blue in FlatLaf theme properties.");
        }
        return accent;
    }

    private int scale(int value) {
        return Math.round(ChartScale.scale(value));
    }

    private final class CardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = Math.round(ChartScale.scale(16));

                Color base = UIManager.getColor("Chart.background");
                if (base == null) {
                    throw new IllegalStateException("Missing Chart.background in FlatLaf theme properties.");
                }
                Color fill = isDark(base) ? ColorUtils.adjustBrightness(base, 1.08) : ColorUtils.adjustBrightness(base, 0.96);
                Color border = UIManager.getColor("Component.borderColor");
                if (border == null) {
                    throw new IllegalStateException("Missing Component.borderColor in FlatLaf theme properties.");
                }

                Color shadow = ColorUtils.withAlpha(Color.BLACK, 0.08f);
                g2.setColor(shadow);
                g2.fillRoundRect(scale(1), scale(2), w - scale(2), h - scale(2), arc, arc);

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w - scale(2), h - scale(2), arc, arc);

                g2.setColor(ColorUtils.withAlpha(border, 0.6f));
                g2.drawRoundRect(0, 0, w - scale(2), h - scale(2), arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        private static boolean isDark(Color color) {
            double lum = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
            return lum < 0.5;
        }

        private static int scale(int value) {
            return Math.round(ChartScale.scale(value));
        }
    }

    private final class Chip extends JPanel {
        private final String text;

        private Chip(String text) {
            this.text = text;
            setOpaque(false);
            setBorder(new EmptyBorder(scale(2), scale(8), scale(2), scale(8)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = Math.round(ChartScale.scale(10));
                g2.setColor(chipBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(ColorUtils.withAlpha(borderColor(), 0.6f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = getFont().deriveFont(Font.PLAIN, ChartScale.uiFontSize(getFont(), 11f));
            FontMetrics fm = getFontMetrics(font);
            int w = fm.stringWidth(text) + scale(16);
            int h = fm.getHeight() + scale(4);
            return new Dimension(w, h);
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font font = getFont().deriveFont(Font.PLAIN, ChartScale.uiFontSize(getFont(), 11f));
                g2.setFont(font);
                g2.setColor(chipForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = scale(8);
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
            } finally {
                g2.dispose();
            }
        }
    }
}
