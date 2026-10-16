package com.arbergashi.charts.platform.ui;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.platform.swing.util.SwingAssets;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.util.function.LongSupplier;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * High precision crosshair overlay with subtle animation and axis labels.
 *
 * <p><strong>Zero-GC:</strong> This component reuses all geometric objects and fonts
 * to minimize allocations during rendering.</p>
 *
 * @author Arber Gashi
 * @since 2026-01-01
 */
public final class HighPrecisionCrosshair extends JComponent {

    @Override
    public boolean contains(int x, int y) {
        return false;
    }
    private static final String STYLE_CLASS_PROPERTY = "FlatLaf.styleClass";

    // Professional crosshair proportions - readable baseline for production demos.
    private static final float LINE_WIDTH = 0.6f;
    private static final float DASH_LENGTH = 2.0f;
    private static final float DASH_SPACE = 2.0f;
    private static final int LABEL_PADDING_H = 6;
    private static final int LABEL_PADDING_V = 3;
    private static final int LABEL_ARC = 6;
    private static final float LABEL_FONT_SIZE = 10.0f;
    private static final float MARKER_SIZE = 8.0f;
    private static final float MARKER_RING_STROKE = 1.6f;
    private static final int FRAME_DELAY_MS = 16;
    private static final long FADE_DURATION_NANOS = 120_000_000L;

    private final Timer animationTimer;
    private final LongSupplier animationClock;
    private boolean animationsEnabled = !Boolean.getBoolean("arbercharts.reduceMotion");
    private long fadeStart;
    private float fadeFrom;

    // Zero-GC: Reusable geometric objects
    private final Line2D.Float verticalLine = new Line2D.Float();
    private final Line2D.Float horizontalLine = new Line2D.Float();
    private final RoundRectangle2D.Float xLabelBox = new RoundRectangle2D.Float();
    private final RoundRectangle2D.Float yLabelBox = new RoundRectangle2D.Float();
    private final BasicStroke dashedStroke;
    private final BasicStroke solidStroke;
    private final BasicStroke markerRingStroke;

    // Zero-GC: Cached font instance
    private transient Font cachedLabelFont;
    private transient float cachedFontScale = -1f;
    private transient float cachedFontBaseSize = -1f;

    private float currentX;
    private float currentY;
    private float opacity;
    private float targetOpacity;

    private String xLabel = "";
    private String yLabel = "";

    private Color accentColor;
    private boolean customAccent;

    private transient int themeKey;
    private transient Color lineColor;
    private transient Color labelBg;
    private transient Color labelFg;

    private ChartTheme theme = ChartThemes.getDarkTheme();

    public HighPrecisionCrosshair() {
        this(System::nanoTime);
    }

    HighPrecisionCrosshair(LongSupplier animationClock) {
        this.animationClock = animationClock;
        setOpaque(false);
        setVisible(false);
        putClientProperty(STYLE_CLASS_PROPERTY, "precisionCrosshair");

        float dash = ChartScale.scale(DASH_LENGTH);
        float space = ChartScale.scale(DASH_SPACE);
        dashedStroke = new BasicStroke(
                ChartScale.scale(LINE_WIDTH),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                10.0f,
                new float[]{dash, space},
                0.0f
        );
        solidStroke = new BasicStroke(ChartScale.scale(LINE_WIDTH));
        markerRingStroke = new BasicStroke(ChartScale.scale(MARKER_RING_STROKE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        animationTimer = new Timer(FRAME_DELAY_MS, _ -> advanceAnimation(animationClock.getAsLong()));
        animationTimer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                hideImmediately();
            }
        });
    }

    @Override
    public void removeNotify() {
        hideImmediately();
        super.removeNotify();
    }

    public void setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.getDarkTheme();
        if (!customAccent && this.theme != null) {
            accentColor = SwingAssets.toAwtColor(this.theme.getAccentColor());
        }
        themeKey = 0;
        repaint();
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        this.customAccent = true;
        repaint();
    }

    /**
     * Updates the measurement position and its labels together on the EDT.
     * Only opacity is animated: a crosshair must never lag behind the value it reports.
     *
     * @param x horizontal component coordinate
     * @param y vertical component coordinate
     * @param labelX formatted X value, or null
     * @param labelY formatted Y value, or null
     */
    public void updatePosition(int x, int y, String labelX, String labelY) {
        if (!isEnabled()) return;
        currentX = x;
        currentY = y;
        xLabel = labelX != null ? labelX : "";
        yLabel = labelY != null ? labelY : "";
        fadeTo(1.0f);
        repaint();
    }

    /** Hides the crosshair using the current motion setting. Call on the EDT. */
    public void hideCrosshair() {
        fadeTo(0.0f);
    }

    /**
     * Controls opacity transitions without changing measurement positions.
     * Disabling animation settles the current transition immediately. Call on the EDT.
     *
     * @param enabled whether to animate opacity
     */
    public void setAnimationsEnabled(boolean enabled) {
        animationsEnabled = enabled;
        if (!enabled) {
            animationTimer.stop();
            opacity = targetOpacity;
            if (opacity == 0.0f) setVisible(false);
            repaint();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled && animationTimer != null) hideImmediately();
    }

    private void fadeTo(float target) {
        long now = animationClock.getAsLong();
        advanceAnimation(now);
        if (target > 0.0f && !isVisible()) setVisible(true);
        if (targetOpacity == target && (animationTimer.isRunning() || opacity == target)) return;
        fadeFrom = opacity;
        targetOpacity = target;
        fadeStart = now;
        if (!animationsEnabled || !isShowing()) {
            animationTimer.stop();
            opacity = target;
            if (target == 0.0f) setVisible(false);
            repaint();
        } else {
            animationTimer.start();
        }
    }

    private void hideImmediately() {
        animationTimer.stop();
        opacity = targetOpacity = 0.0f;
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isEnabled() || opacity < 0.01f) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            int w = getWidth();
            int h = getHeight();
            ensureThemeCache();

            verticalLine.setLine(currentX, 0, currentX, h);
            horizontalLine.setLine(0, currentY, w, currentY);

            g2.setColor(lineColor);
            g2.setStroke(dashedStroke);
            g2.draw(verticalLine);
            g2.draw(horizontalLine);

            // Draw centered ring marker (more visible than a tiny dot).
            int markerSize = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.crosshair.marker.size", MARKER_SIZE)));
            markerSize = Math.max(6, markerSize);
            int mx = Math.round(currentX) - markerSize / 2;
            int my = Math.round(currentY) - markerSize / 2;
            Color markerColor = accentColor != null ? accentColor : lineColor;
            g2.setColor(withAlpha(markerColor, 0.28f));
            g2.fillOval(mx, my, markerSize, markerSize);
            g2.setColor(markerColor);
            g2.setStroke(markerRingStroke);
            g2.drawOval(mx, my, markerSize, markerSize);


            drawAxisLabels(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawAxisLabels(Graphics2D g2) {
        if ((xLabel == null || xLabel.isEmpty()) && (yLabel == null || yLabel.isEmpty())) {
            return;
        }
        ensureThemeCache();

        // Zero-GC: Cache font instance, only recreate when scale changes
        // Use Inter font from ChartFonts for professional crosshair labels
        float scale = ChartAssets.getFloat("Chart.crosshair.label.fontScale", 1.0f);
        if (scale < 0.3f || scale > 3.0f) scale = 1.0f;
        float baseSize = ChartAssets.getFloat("Chart.crosshair.label.baseSize", LABEL_FONT_SIZE);
        if (baseSize < 8f || baseSize > 20f) baseSize = LABEL_FONT_SIZE;

        if (cachedLabelFont == null || cachedFontScale != scale || cachedFontBaseSize != baseSize) {
            com.arbergashi.charts.platform.swing.util.ChartFonts.initialize();
            cachedLabelFont = com.arbergashi.charts.platform.swing.util.ChartFonts.getCrosshairFont(
                    ChartScale.font(baseSize * scale));
            cachedFontScale = scale;
            cachedFontBaseSize = baseSize;
        }

        g2.setFont(cachedLabelFont);
        FontMetrics fm = g2.getFontMetrics();

        int padH = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.crosshair.label.paddingH", LABEL_PADDING_H)));
        int padV = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.crosshair.label.paddingV", LABEL_PADDING_V)));
        int arc = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.crosshair.label.arc", LABEL_ARC)));

        // X-Axis label (bottom center)
        if (xLabel != null && !xLabel.isEmpty()) {
            int textWidth = fm.stringWidth(xLabel);
            int bw = textWidth + padH * 2;
            int bh = fm.getHeight() + padV * 2;
            int bx = Math.round(currentX) - bw / 2;
            int by = getHeight() - bh - Math.round(ChartScale.scale(8f));

            // Clamp to viewport
            bx = Math.max(2, Math.min(bx, getWidth() - bw - 2));
            by = Math.max(2, Math.min(by, getHeight() - bh - 2));

            xLabelBox.setRoundRect(bx, by, bw, bh, arc, arc);
            g2.setColor(labelBg);
            g2.fill(xLabelBox);
            g2.setColor(accentColor != null ? accentColor : lineColor);
            g2.setStroke(solidStroke);
            g2.draw(xLabelBox);
            g2.setColor(labelFg);
            g2.drawString(xLabel, bx + padH, by + fm.getAscent() + padV);
        }

        // Y-Axis label (left side)
        if (yLabel != null && !yLabel.isEmpty()) {
            int textWidth = fm.stringWidth(yLabel);
            int bw = textWidth + padH * 2;
            int bh = fm.getHeight() + padV * 2;
            int bx = Math.round(ChartScale.scale(8f));
            int by = Math.round(currentY) - bh / 2;

            // Clamp to viewport
            by = Math.max(2, Math.min(by, getHeight() - bh - 2));

            yLabelBox.setRoundRect(bx, by, bw, bh, arc, arc);
            g2.setColor(labelBg);
            g2.fill(yLabelBox);
            g2.setColor(accentColor != null ? accentColor : lineColor);
            g2.setStroke(solidStroke);
            g2.draw(yLabelBox);
            g2.setColor(labelFg);
            g2.drawString(yLabel, bx + padH, by + fm.getAscent() + padV);
        }
    }

    private void ensureThemeCache() {
        int key = System.identityHashCode(theme);
        if (key == themeKey && lineColor != null) {
            return;
        }
        themeKey = key;

        Color grid = SwingAssets.toAwtColor(theme != null ? theme.getGridColor() : null);
        if (grid == null || grid.getAlpha() == 0) {
            grid = UIManager.getColor("Separator.foreground");
        }
        if (grid == null) {
            grid = new Color(100, 100, 100);
        }
        lineColor = withAlpha(grid, 0.7f);

        Color bg = SwingAssets.toAwtColor(theme != null ? theme.getBackground() : null);
        if (bg == null || bg.getAlpha() == 0) {
            bg = UIManager.getColor("Panel.background");
        }
        if (bg == null) {
            bg = new Color(30, 30, 30);
        }
        labelBg = withAlpha(bg, 0.95f);

        Color fg = SwingAssets.toAwtColor(theme != null ? theme.getForeground() : null);
        if (fg == null || fg.getAlpha() == 0) {
            fg = UIManager.getColor("Label.foreground");
        }
        if (fg == null) {
            fg = Color.WHITE;
        }
        labelFg = fg;

        if (!customAccent && theme != null) {
            accentColor = SwingAssets.toAwtColor(theme.getAccentColor());
        }
    }

    void advanceAnimation(long now) {
        if (!animationTimer.isRunning()) return;
        double progress = Math.clamp((now - fadeStart) / (double) FADE_DURATION_NANOS, 0.0, 1.0);
        double remaining = 1.0 - progress;
        double eased = 1.0 - remaining * remaining * remaining;
        opacity = (float) (fadeFrom + (targetOpacity - fadeFrom) * eased);
        if (progress >= 1.0) {
            animationTimer.stop();
            opacity = targetOpacity;
            if (opacity == 0.0f) setVisible(false);
        }
        repaint();
    }

    private static Color withAlpha(Color color, float alpha) {
        int a = Math.min(255, Math.max(0, Math.round(alpha * 255)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }
}
