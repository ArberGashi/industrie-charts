package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * GaugeBandsRenderer renders a gauge with colored bands (green/yellow/red), suitable for business KPIs.
 *
 * <p>Contract:
 * <ul>
 *   <li>Uses first data point as value.</li>
 *   <li>Range uses PlotContext y-range (preferred) and falls back to ChartPoint min/max or 0..100.</li>
 *   <li>Bands are configured via ChartAssets key {@code chart.gaugeBands.bands}.</li>
 * </ul>
 *
 * <p>Band format (UTF-8 properties safe):
 * <pre>
 *  start,end,color; start,end,color; ...
 *  example: 0,60,#22c55e;60,85,#f59e0b;85,100,#ef4444
 * </pre>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class GaugeBandsRenderer extends BaseRenderer {

    private static final double START = 225.0;
    private static final double SWEEP = 270.0;

    // hit-test cache
    private double lastCx;
    private double lastCy;
    private double lastOuter;
    private double lastInner;
    private final java.text.NumberFormat valueFormat = java.text.NumberFormat.getInstance();
    private final Font valueFont;
    private final Font labelFont;

    public GaugeBandsRenderer() {
        super("gaugeBands");
        valueFormat.setMaximumFractionDigits(1);
        valueFont = getCachedFont(20f, Font.BOLD);
        labelFont = getCachedFont(11f, Font.PLAIN);
    }

    private static Color parseColor(String token, Color fallback) {
        try {
            String t = token;
            if (t.startsWith("#")) t = t.substring(1);
            if (t.length() == 6) {
                int rgb = Integer.parseInt(t, 16);
                return com.arbergashi.charts.util.ColorRegistry.of((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
            }
        } catch (Exception ignore) {
        }
        return fallback;
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        Rectangle2D b = context.plotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double value = model.getY(0);

        double min = context.minY();
        double max = context.maxY();
        if (!(Double.isFinite(min) && Double.isFinite(max) && max > min)) {
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < count; i++) {
                double v = model.getY(i);
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        String bandSpec = ChartAssets.getString("chart.gaugeBands.bands", "0,60,#22c55e;60,85,#f59e0b;85,100,#ef4444");
        if (!(Double.isFinite(min) && Double.isFinite(max) && max > min)) {
            double[] range = parseBandRange(bandSpec);
            if (range != null) {
                min = range[0];
                max = range[1];
            } else {
                min = 0.0;
                max = 100.0;
            }
        }
        if (Math.abs(max - min) < 1e-6) {
            double[] range = parseBandRange(bandSpec);
            if (range != null) {
                min = range[0];
                max = range[1];
            } else {
                min = 0.0;
                max = 100.0;
            }
        }

        double t = (value - min) / (max - min);
        if (!Double.isFinite(t)) t = 0.0;
        t = MathUtils.clamp(t, 0.0, 1.0);

        double size = Math.min(b.getWidth(), b.getHeight()) * 0.85;
        if (!(size > 1)) return;
        double cx = b.getCenterX();
        double cy = b.getCenterY() + size * 0.08;

        double outer = size * 0.5;
        double inner = outer * 0.72;

        // cache for hit-tests
        lastCx = cx;
        lastCy = cy;
        lastOuter = outer;
        lastInner = inner;

        float ringW = (float) ChartScale.scale(outer - inner);
        ringW = Math.max((float) ChartScale.scale(1.0), ringW);

        // Draw bands under value arc.
        drawBands(g2, cx, cy, outer, ringW, bandSpec, min, max, resolveTheme(context));

        // Draw value arc.
        Color accent = (model.getColor() != null) ? model.getColor() : resolveTheme(context).getAccentColor();
        g2.setStroke(getCachedStroke(ringW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ColorUtils.withAlpha(accent, 0.95f));
        g2.draw(getArc(cx - outer, cy - outer, outer * 2, outer * 2, START, -SWEEP * t, Arc2D.OPEN));

        // Needle and hub (like GaugeRenderer)
        double angle = START - SWEEP * t;
        double needleLen = inner * 0.95;
        double rad = Math.toRadians(angle);
        double nx = cx + Math.cos(rad) * needleLen;
        double ny = cy - Math.sin(rad) * needleLen;

        Color fg = resolveTheme(context).getForeground();
        g2.setStroke(getCachedStroke((float) ChartScale.scale(2.0)));
        g2.setColor(ColorUtils.withAlpha(fg, 0.75f));
        g2.draw(getLine(cx, cy, nx, ny));

        double hub = ChartScale.scale(8.0);
        g2.setColor(ColorUtils.withAlpha(fg, 0.25f));
        g2.fill(getEllipse(cx - hub, cy - hub, hub * 2, hub * 2));
        g2.setColor(ColorUtils.withAlpha(fg, 0.55f));
        g2.fill(getEllipse(cx - hub * 0.6, cy - hub * 0.6, hub * 1.2, hub * 1.2));

        drawCenterText(g2, cx, cy, resolveTheme(context), value, min, max, model.getLabel(0));
    }

    private void drawCenterText(Graphics2D g2, double cx, double cy, ChartTheme theme,
                                double value, double min, double max, String label) {
        String unit = ChartAssets.getString("chart.gaugeBands.unit", "");
        if ((unit == null || unit.isBlank()) && min >= 0.0 && max <= 100.0) {
            unit = "%";
        }
        String valueText = valueFormat.format(value) + unit;

        g2.setFont(valueFont);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(theme.getForeground());
        float valueY = (float) (cy + ChartScale.scale(22));
        g2.drawString(valueText, (float) (cx - fm.stringWidth(valueText) / 2.0), valueY);

        if (label != null && !label.isBlank()) {
            g2.setFont(labelFont);
            fm = g2.getFontMetrics();
            g2.setColor(theme.getAxisLabelColor());
            float labelY = valueY + fm.getHeight();
            g2.drawString(label, (float) (cx - fm.stringWidth(label) / 2.0), labelY);
        }
    }

    private static double[] parseBandRange(String spec) {
        if (spec == null || spec.isBlank()) return null;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        String[] parts = spec.split(";");
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            String[] tok = s.split(",");
            if (tok.length < 2) continue;
            try {
                double a = Double.parseDouble(tok[0].trim());
                double b = Double.parseDouble(tok[1].trim());
                if (a < min) min = a;
                if (b < min) min = b;
                if (a > max) max = a;
                if (b > max) max = b;
            } catch (Exception ignore) {
            }
        }
        if (min == Double.POSITIVE_INFINITY || max == Double.NEGATIVE_INFINITY || max <= min) return null;
        return new double[]{min, max};
    }

    private void drawBands(Graphics2D g2, double cx, double cy, double outer, float ringW,
                           String spec, double min, double max, ChartTheme theme) {
        g2.setStroke(getCachedStroke(ringW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Color fallbackBg = ColorUtils.withAlpha(theme.getForeground(), 0.12f);
        if (spec == null || spec.isBlank()) {
            g2.setColor(fallbackBg);
            g2.draw(getArc(cx - outer, cy - outer, outer * 2, outer * 2, START, -SWEEP, Arc2D.OPEN));
            return;
        }

        // Very small parser to keep it dependency-free.
        // Format: a,b,#rrggbb;...
        String[] parts = spec.split(";");
        boolean drewAny = false;
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            String[] tok = s.split(",");
            if (tok.length < 3) continue;

            double a;
            double b;
            try {
                a = Double.parseDouble(tok[0].trim());
                b = Double.parseDouble(tok[1].trim());
            } catch (Exception ignore) {
                continue;
            }

            Color c = parseColor(tok[2].trim(), fallbackBg);

            double ta = (a - min) / (max - min);
            double tb = (b - min) / (max - min);
            if (!Double.isFinite(ta) || !Double.isFinite(tb)) continue;
            ta = MathUtils.clamp(ta, 0.0, 1.0);
            tb = MathUtils.clamp(tb, 0.0, 1.0);
            if (tb <= ta) continue;

            double angA = -SWEEP * ta;
            double angB = -SWEEP * tb;
            double extent = (angB - angA);

            g2.setColor(c);
            g2.draw(getArc(cx - outer, cy - outer, outer * 2, outer * 2, START + angA, extent, Arc2D.OPEN));
            drewAny = true;
        }

        if (!drewAny) {
            g2.setColor(fallbackBg);
            g2.draw(getArc(cx - outer, cy - outer, outer * 2, outer * 2, START, -SWEEP, Arc2D.OPEN));
        }
    }


    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        if (!(lastOuter > 0)) return Optional.empty();

        double dx = pixel.getX() - lastCx;
        double dy = pixel.getY() - lastCy;
        double d2 = dx * dx + dy * dy;

        double outer2 = lastOuter * lastOuter;
        if (d2 > outer2) return Optional.empty();

        double inner2 = lastInner * lastInner;
        if (d2 < inner2) {
            double hub = ChartScale.scale(10.0);
            if (d2 <= hub * hub) return Optional.of(0);
            return Optional.empty();
        }

        // check if within sweep
        double a = Math.toDegrees(Math.atan2(-dy, dx));
        if (a < 0) a += 360.0;

        // For START=225 and SWEEP=270, the covered arc always wraps across 0°: [end..360) U [0..start]
        double normStart = ((START % 360.0) + 360.0) % 360.0;
        double end = ((normStart - SWEEP) % 360.0 + 360.0) % 360.0;
        boolean inSweep = (a >= end || a <= normStart);
        if (!inSweep) return Optional.empty();

        // ring thickness tolerance
        double ringMid = (lastOuter + lastInner) * 0.5;
        double tol = ChartScale.scale(10.0);
        double r = Math.sqrt(d2);
        if (Math.abs(r - ringMid) <= tol) return Optional.of(0);

        return Optional.empty();
    }
}
