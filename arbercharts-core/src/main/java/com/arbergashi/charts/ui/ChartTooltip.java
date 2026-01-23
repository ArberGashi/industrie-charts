package com.arbergashi.charts.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A simple, theme-aware tooltip component for charts.
 * It now accepts a pre-formatted string.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class ChartTooltip extends JComponent {

    private final JLabel label;
    private ChartTheme theme = ChartThemes.defaultDark();

    /**
     * Creates a theme-aware tooltip component.
     */
    public ChartTooltip() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setVisible(false);

        label = new JLabel();
        label.setBorder(new EmptyBorder(Math.round(ChartScale.scale(5)), Math.round(ChartScale.scale(8)), Math.round(ChartScale.scale(5)), Math.round(ChartScale.scale(8))));
        add(label, BorderLayout.CENTER);

        updateTheme();
    }

    /**
     * Sets the text to be displayed in the tooltip.
     * The text can contain HTML for multi-line formatting.
     *
     * @param text The tooltip text.
     */
    public void setText(String text) {
        label.setText(text);
        setSize(getPreferredSize());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateTheme();
    }

    /**
     * Sets the theme for tooltip background and text colors.
     *
     * @param theme chart theme to apply; falls back to a default when null
     */
    public void setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.defaultDark();
        updateTheme();
    }

    private void updateTheme() {
        if (label == null) return;

        Font font = UIManager.getFont("ToolTip.font");
        if (font == null) font = new Font("SansSerif", Font.PLAIN, 12);
        label.setFont(font);

        Color bg = (theme != null) ? theme.getBackground() : null;
        Color fg = (theme != null) ? theme.getForeground() : null;
        Color border = (theme != null) ? theme.getAxisLabelColor() : null;

        if (bg == null) bg = UIManager.getColor("ToolTip.background");
        if (fg == null) fg = UIManager.getColor("ToolTip.foreground");
        if (border == null) border = UIManager.getColor("ToolTip.border");

        Color resolvedBg = bg != null ? bg : new Color(255, 255, 220);
        Color resolvedFg = fg != null ? fg : Color.BLACK;
        Color resolvedBorder = border != null ? border : Color.GRAY;

        setBackground(ColorUtils.withAlpha(resolvedBg, 0.95f));
        label.setForeground(resolvedFg);
        setBorder(BorderFactory.createLineBorder(ColorUtils.withAlpha(resolvedBorder, 0.7f)));
    }
}
