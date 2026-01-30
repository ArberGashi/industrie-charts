package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;
import com.arbergashi.charts.util.PredictiveMath;

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
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class GaugeBandsRenderer extends BaseRenderer {

    private static final double START = 225.0;
    private static final double SWEEP = 270.0;
    private static final String KEY_JITTER = "Chart.circular.gauge.needleJitter";
    private static final String KEY_ALERT_PULSE = "Chart.circular.gauge.alertPulse";
    private static final String KEY_THICKNESS = "Chart.circular.gauge.thickness";
    private static final String KEY_GHOST_ENABLED = "Chart.circular.ghost.enabled";
    private static final String KEY_GHOST_ALPHA = "Chart.circular.ghost.alpha";
    private static final String KEY_LOOKAHEAD = "Chart.predictive.global.lookahead";
    private static final String KEY_SMOOTHING = "Chart.predictive.global.smoothing";

    // hit-test cache
    private double lastCx;
    private double lastCy;
    private double lastOuter;
    private double lastInner;
    private double lastValue = Double.NaN;
    private double jitterEnergy = 0.0;
    private double smoothedDelta = 0.0;

    public GaugeBandsRenderer() {
        super("gaugeBands");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect b = context.getPlotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double value = model.getY(0);

        double min = context.getMinY();
        double max = context.getMaxY();
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

        double range = Math.max(1e-9, max - min);
        if (Double.isFinite(lastValue)) {
            double jump = Math.min(1.0, Math.abs(value - lastValue) / range);
            jitterEnergy = Math.max(jitterEnergy * 0.9, jump);
            double smoothing = ChartAssets.getFloat(KEY_SMOOTHING, 0.85f);
            smoothedDelta = PredictiveMath.smoothDelta(smoothedDelta, value - lastValue, smoothing);
        }
        lastValue = value;

        double rawT = (value - min) / range;
        boolean alert = ChartAssets.getBoolean(KEY_ALERT_PULSE, true) && (rawT < 0.0 || rawT > 1.0);
        double t = Double.isFinite(rawT) ? MathUtils.clamp(rawT, 0.0, 1.0) : 0.0;

        double size = Math.min(b.getWidth(), b.getHeight()) * 0.85;
        if (!(size > 1)) return;
        double cx = b.centerX();
        double cy = b.centerY() + size * 0.08;

        double outer = size * 0.5;
        float thickness = ChartAssets.getFloat(KEY_THICKNESS, 0.15f);
        thickness = (float) MathUtils.clamp(thickness, 0.08, 0.4);
        double inner = outer * (1.0 - thickness);

        // cache for hit-tests
        lastCx = cx;
        lastCy = cy;
        lastOuter = outer;
        lastInner = inner;

        float ringW = (float) ChartScale.scale(outer - inner);
        ringW = Math.max((float) ChartScale.scale(1.0), ringW);

        // Draw bands under value arc.
        drawBands(canvas, cx, cy, outer, ringW, bandSpec, min, max, getResolvedTheme(context));

        // Draw value arc.
        ArberColor accent = (model.getColor() != null) ? model.getColor() : getResolvedTheme(context).getAccentColor();
        if (alert) {
            double seconds = System.nanoTime() * 1.0e-9;
            float pulse = (float) (0.55 + 0.45 * (0.5 + 0.5 * Math.sin(seconds * 6.0)));
            accent = ColorRegistry.applyAlpha(accent, pulse);
        }
        canvas.setStroke(ringW);
        canvas.setColor(ColorRegistry.applyAlpha(accent, 0.95f));
        drawArcPolyline(canvas, cx, cy, outer, START, -SWEEP * t);

        // Ghost needle (predictive)
        if (ChartAssets.getBoolean(KEY_GHOST_ENABLED, true) && Double.isFinite(smoothedDelta)) {
            int lookahead = ChartAssets.getInt(KEY_LOOKAHEAD, 32);
            double ghostValue = PredictiveMath.extrapolate(value, smoothedDelta, lookahead);
            double ghostT = MathUtils.clamp((ghostValue - min) / range, 0.0, 1.0);
            double ghostAngle = START - SWEEP * ghostT;
            double gRad = Math.toRadians(ghostAngle);
            double gLen = inner * 0.95;
            double gx = cx + Math.cos(gRad) * gLen;
            double gy = cy - Math.sin(gRad) * gLen;
            float ghostAlpha = ChartAssets.getFloat(KEY_GHOST_ALPHA, 0.25f);
            ArberColor ghostColor = ColorRegistry.applyAlpha(getResolvedTheme(context).getAccentColor(), ghostAlpha);
            canvas.setStroke((float) ChartScale.scale(2.0));
            canvas.setColor(ghostColor);
            drawLine(canvas, cx, cy, gx, gy);
        }

        // Needle and hub (like GaugeRenderer)
        double angle = START - SWEEP * t;
        if (ChartAssets.getBoolean(KEY_JITTER, true) && jitterEnergy > 0.02) {
            double seconds = System.nanoTime() * 1.0e-9;
            angle += Math.sin(seconds * 18.0) * (1.6 * jitterEnergy);
            jitterEnergy *= 0.96;
        }
        double needleLen = inner * 0.95;
        double rad = Math.toRadians(angle);
        double nx = cx + Math.cos(rad) * needleLen;
        double ny = cy - Math.sin(rad) * needleLen;

        ArberColor fg = getResolvedTheme(context).getForeground();
        canvas.setStroke((float) ChartScale.scale(2.0));
        canvas.setColor(ColorRegistry.applyAlpha(fg, 0.75f));
        drawLine(canvas, cx, cy, nx, ny);

        double hub = ChartScale.scale(8.0);
        canvas.setColor(ColorRegistry.applyAlpha(fg, 0.25f));
        drawCircleFill(canvas, cx, cy, hub);
        canvas.setColor(ColorRegistry.applyAlpha(fg, 0.55f));
        drawCircleFill(canvas, cx, cy, hub * 0.6);
    }

    private void drawBands(ArberCanvas canvas, double cx, double cy, double radius, float ringW,
                           String bandSpec, double min, double max, ChartTheme theme) {
        canvas.setStroke(ringW);
        String[] bands = bandSpec.split(";");
        for (String band : bands) {
            String[] parts = band.trim().split(",");
            if (parts.length < 3) continue;
            double bStart = parseDouble(parts[0], Double.NaN);
            double bEnd = parseDouble(parts[1], Double.NaN);
            if (!Double.isFinite(bStart) || !Double.isFinite(bEnd)) continue;
            ArberColor c = parseColor(parts[2].trim(), theme.getAccentColor());

            double bandStartT = MathUtils.clamp((bStart - min) / (max - min), 0, 1);
            double bandEndT = MathUtils.clamp((bEnd - min) / (max - min), 0, 1);
            double bandStartAngle = START - SWEEP * bandStartT;
            double bandSweep = -SWEEP * (bandEndT - bandStartT);

            canvas.setColor(c);
            drawArcPolyline(canvas, cx, cy, radius, bandStartAngle, bandSweep);
        }
    }

    private static ArberColor parseColor(String token, ArberColor fallback) {
        try {
            String t = token;
            if (t.startsWith("#")) t = t.substring(1);
            if (t.length() == 6) {
                int rgb = Integer.parseInt(t, 16);
                return ColorRegistry.of((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
            }
        } catch (Exception ignore) {
        }
        return fallback;
    }

    private static double[] parseBandRange(String bandSpec) {
        if (bandSpec == null || bandSpec.isBlank()) return null;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        String[] bands = bandSpec.split(";");
        for (String band : bands) {
            String[] parts = band.trim().split(",");
            if (parts.length < 2) continue;
            double bStart = parseDouble(parts[0], Double.NaN);
            double bEnd = parseDouble(parts[1], Double.NaN);
            if (!Double.isFinite(bStart) || !Double.isFinite(bEnd)) continue;
            min = Math.min(min, Math.min(bStart, bEnd));
            max = Math.max(max, Math.max(bStart, bEnd));
        }
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) return null;
        return new double[]{min, max};
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception ignore) {
            return fallback;
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = new float[2];
        float[] ys = new float[2];
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = Math.max(24, (int) (Math.PI * r / 6.0));
        float[] xs = new float[segments];
        float[] ys = new float[segments];
        for (int i = 0; i < segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

    private void drawArcPolyline(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double sweepDeg) {
        int segments = Math.max(12, (int) (Math.abs(sweepDeg) / 4.0));
        float[] xs = new float[segments + 1];
        float[] ys = new float[segments + 1];
        double step = sweepDeg / segments;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + step * i);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy - Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }
}
