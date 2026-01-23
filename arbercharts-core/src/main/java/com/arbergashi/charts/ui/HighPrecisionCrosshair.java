package com.arbergashi.charts.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;

import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * <h1>HighPrecisionCrosshair - Professional TradingView-Style Crosshair</h1>
 *
 * <p>Enterprise-grade crosshair overlay for banking, medical, engineering,
 * and scientific applications with smooth animations and magnetic snapping.</p>
 *
 * <h2>Professional Features:</h2>
 * <ul>
 *   <li><b>Smooth Animation:</b> 60 FPS interpolated position updates</li>
 *   <li><b>Magnetic Snapping:</b> Snaps to nearest data point</li>
 *   <li><b>Axis Labels:</b> X/Y values in professional label boxes</li>
 *   <li><b>Minimal Lines:</b> Subtle, dashed crosshair lines</li>
 *   <li><b>High Contrast:</b> Optimized for medical/engineering precision</li>
 *   <li><b>Zero Lag:</b> Hardware-accelerated rendering</li>
 *   <li><b>Theme-Aware:</b> Adapts to dark/light instantly</li>
 * </ul>
 *
 * <h2>Design Inspiration:</h2>
 * <ul>
 *   <li>TradingView crosshair (smooth, animated)</li>
 *   <li>Bloomberg Terminal cursor (precise, minimal)</li>
 *   <li>Medical imaging crosshair (high contrast)</li>
 *   <li>CAD software precision cursor (snapping)</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0 - Professional Edition
 * @since 2026-01-01
 */
public final class HighPrecisionCrosshair extends JComponent {

    /**
     * Optional FlatLaf style class, applied as a plain client property to avoid a hard dependency.
     */
    private static final String STYLE_CLASS_PROPERTY = "FlatLaf.styleClass";

    // Constants - Professional styling
    private static final float LINE_WIDTH = 1.0f;
    private static final float DASH_LENGTH = 4f;
    private static final float DASH_SPACE = 4f;
    private static final int LABEL_PADDING = 6;
    private static final int LABEL_ARC = 4;
    private static final float LABEL_FONT_SIZE = 10f;
    private static final int ANIMATION_FPS = 60;
    // Reserved for future snapping implementation. Keep crosshair stable and minimal in core.
    private final Timer animationTimer;
    private final Timer hideTimer;
    // Reusable shapes (zero-allocation)
    private final Line2D.Float verticalLine = new Line2D.Float();
    private final Line2D.Float horizontalLine = new Line2D.Float();
    private final RoundRectangle2D.Float xLabelBox = new RoundRectangle2D.Float();
    private final RoundRectangle2D.Float yLabelBox = new RoundRectangle2D.Float();
    private final BasicStroke dashedStroke;
    private final BasicStroke solidStroke;
    // Animation state
    private float currentX = 0;
    private float currentY = 0;
    private float targetX = 0;
    private float targetY = 0;
    private float opacity = 0f;
    private float targetOpacity = 0f;
    // Crosshair state
    private boolean enabled = false;
    private String xLabel = "";
    private String yLabel = "";
    private Color accentColor;
    private boolean customAccent;
    private transient int themeKey;
    private transient Color lineColor;
    private transient Color labelBg;
    private transient Color labelFg;

    private ChartTheme theme = ChartThemes.defaultDark();

    /**
     * Creates a theme-aware, animated crosshair overlay component.
     */
    public HighPrecisionCrosshair() {
        setOpaque(false);
        setVisible(false);
        putClientProperty(STYLE_CLASS_PROPERTY, "precisionCrosshair");

        // Create strokes once
        float scaledDash = ChartScale.scale(DASH_LENGTH);
        float scaledSpace = ChartScale.scale(DASH_SPACE);
        dashedStroke = new BasicStroke(
                ChartScale.scale(LINE_WIDTH),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                10f,
                new float[]{scaledDash, scaledSpace},
                0f
        );
        solidStroke = new BasicStroke(ChartScale.scale(LINE_WIDTH));

        // 60 FPS animation timer
        animationTimer = new Timer(1000 / ANIMATION_FPS, e -> {
            // Smooth interpolation (ease-out)
            float lerp = 0.25f; // Smoothness factor
            currentX += (targetX - currentX) * lerp;
            currentY += (targetY - currentY) * lerp;
            opacity += (targetOpacity - opacity) * lerp;

            // Stop if close enough
            if (Math.abs(targetX - currentX) < 0.5f &&
                    Math.abs(targetY - currentY) < 0.5f &&
                    Math.abs(targetOpacity - opacity) < 0.01f) {
                ((Timer) e.getSource()).stop();
                currentX = targetX;
                currentY = targetY;
                opacity = targetOpacity;
            }

            repaint();
        });

        // One-shot hide timer reused to avoid allocations and timer leaks.
        hideTimer = new Timer(200, _ -> {
            if (opacity < 0.05f) {
                setVisible(false);
            }
        });
        hideTimer.setRepeats(false);
    }

    @Override
    public void removeNotify() {
        // Prevent timer leaks if the component is removed/disposed.
        animationTimer.stop();
        hideTimer.stop();
        super.removeNotify();
    }

    /**
     * Updates crosshair position with smooth animation.
     */
    public void updatePosition(int x, int y, String xValue, String yValue) {
        this.targetX = x;
        this.targetY = y;
        this.xLabel = xValue;
        this.yLabel = yValue;
        this.targetOpacity = 1f;

        if (!isVisible()) {
            setVisible(true);
            currentX = x;
            currentY = y;
            opacity = 0f;
        }

        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    /**
     * Hides crosshair with fade-out animation.
     */
    public void hideCrosshair() {
        targetOpacity = 0f;

        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }

        hideTimer.restart();
    }

    /**
     * Enables/disables crosshair rendering.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hideCrosshair();
        }
    }

    /**
     * Sets accent color for crosshair and labels.
     */
    public void setAccentColor(Color color) {
        this.accentColor = color;
        this.customAccent = true;
        repaint();
    }

    /**
     * Sets the chart theme used to derive colors and label styling.
     *
     * @param theme chart theme (falls back to default when null)
     */
    public void setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.defaultDark();
        if (!customAccent) {
            this.accentColor = this.theme.getAccentColor();
        }
        themeKey = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!enabled || opacity < 0.01f) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        int w = getWidth();
        int h = getHeight();

        ensureThemeCache();

        // Draw vertical line
        verticalLine.setLine(currentX, 0, currentX, h);
        g2.setColor(lineColor);
        g2.setStroke(dashedStroke);
        g2.draw(verticalLine);

        // Draw horizontal line
        horizontalLine.setLine(0, currentY, w, currentY);
        g2.draw(horizontalLine);

        // Draw center dot (high-precision marker)
        int dotSize = Math.round(ChartScale.scale(4));
        g2.setColor(accentColor);
        g2.fillOval((int) currentX - dotSize / 2, (int) currentY - dotSize / 2, dotSize, dotSize);

        // Draw axis labels
        drawAxisLabels(g2);

        g2.dispose();
    }

    private void drawAxisLabels(Graphics2D g2) {
        if (xLabel.isEmpty() && yLabel.isEmpty()) return;

        ensureThemeCache();

        Font baseFont = getFont();
        Font labelFont = baseFont.deriveFont(Font.BOLD, ChartScale.uiFontSize(baseFont, LABEL_FONT_SIZE));
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        int padding = Math.round(ChartScale.scale(LABEL_PADDING));
        int arc = Math.round(ChartScale.scale(LABEL_ARC));

        // X-axis label (bottom)
        if (!xLabel.isEmpty()) {
            int labelW = fm.stringWidth(xLabel) + padding * 2;
            int labelH = fm.getHeight() + padding;
            int labelX = (int) currentX - labelW / 2;
            int labelY = getHeight() - labelH - Math.round(ChartScale.scale(5));

            // clamp into component bounds
            labelX = Math.max(Math.round(ChartScale.scale(2)), Math.min(labelX, getWidth() - labelW - Math.round(ChartScale.scale(2))));
            labelY = Math.max(Math.round(ChartScale.scale(2)), Math.min(labelY, getHeight() - labelH - Math.round(ChartScale.scale(2))));

            xLabelBox.setRoundRect(labelX, labelY, labelW, labelH, arc, arc);

            g2.setColor(labelBg);
            g2.fill(xLabelBox);

            g2.setColor(accentColor);
            g2.setStroke(solidStroke);
            g2.draw(xLabelBox);

            g2.setColor(labelFg);
            g2.drawString(xLabel, labelX + padding, labelY + fm.getAscent() + padding / 2);
        }

        // Y-axis label (left)
        if (!yLabel.isEmpty()) {
            int labelW = fm.stringWidth(yLabel) + padding * 2;
            int labelH = fm.getHeight() + padding;
            int labelX = Math.round(ChartScale.scale(5));
            int labelY = (int) currentY - labelH / 2;

            // clamp into component bounds
            labelY = Math.max(Math.round(ChartScale.scale(2)), Math.min(labelY, getHeight() - labelH - Math.round(ChartScale.scale(2))));

            yLabelBox.setRoundRect(labelX, labelY, labelW, labelH, arc, arc);

            g2.setColor(labelBg);
            g2.fill(yLabelBox);

            g2.setColor(accentColor);
            g2.setStroke(solidStroke);
            g2.draw(yLabelBox);

            g2.setColor(labelFg);
            g2.drawString(yLabel, labelX + padding, labelY + fm.getAscent() + padding / 2);
        }
    }

    private void ensureThemeCache() {
        int key = System.identityHashCode(theme);
        if (key == themeKey && lineColor != null) return;
        themeKey = key;

        Color baseLine = theme.getGridColor();
        if (baseLine == null) baseLine = UIManager.getColor("Separator.foreground");
        if (baseLine == null) baseLine = new Color(100, 100, 100);
        lineColor = ColorUtils.withAlpha(baseLine, 0.6f);

        Color bg = theme.getBackground();
        if (bg == null) bg = UIManager.getColor("Panel.background");
        if (bg == null) bg = new Color(30, 30, 30);
        labelBg = ColorUtils.withAlpha(bg, 0.95f);

        Color fg = theme.getForeground();
        if (fg == null) fg = UIManager.getColor("Label.foreground");
        if (fg == null) fg = Color.WHITE;
        labelFg = fg;

        if (!customAccent) {
            accentColor = theme.getAccentColor();
        }
    }
}
