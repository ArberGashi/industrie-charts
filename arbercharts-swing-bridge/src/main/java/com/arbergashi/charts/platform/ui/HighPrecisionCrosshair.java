package com.arbergashi.charts.platform.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.platform.swing.util.SwingAssets;
import com.arbergashi.charts.util.ColorRegistry;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Simple crosshair overlay for Swing charts.
 */
public final class HighPrecisionCrosshair extends JComponent {
    private ChartTheme theme;
    private boolean active;
    private double x;
    private double y;

    public HighPrecisionCrosshair() {
        setOpaque(false);
    }

    public void setTheme(ChartTheme theme) {
        this.theme = theme;
        repaint();
    }

    public void setPosition(double x, double y, String labelX, String labelY) {
        this.x = x;
        this.y = y;
        this.active = true;
        repaint();
    }

    public void hideCrosshair() {
        this.active = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ArberColor c = (theme != null) ? theme.getAxisLabelColor() : ColorRegistry.of(180, 180, 180, 255);
            Color awt = SwingAssets.toAwtColor(c);
            g2.setColor(awt);
            int xi = (int) Math.round(x);
            int yi = (int) Math.round(y);
            g2.drawLine(0, yi, getWidth(), yi);
            g2.drawLine(xi, 0, xi, getHeight());
        } finally {
            g2.dispose();
        }
    }
}
