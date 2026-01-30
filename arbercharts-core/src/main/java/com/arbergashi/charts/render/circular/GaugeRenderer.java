package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.AnimationProfile;
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

import java.util.List;

/**
 * <h1>Modern Gauge Renderer</h1>
 * <p>
 * Draws a professional, highly configurable radial gauge, adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Configurable Bands:</b> Define colored ranges (e.g., green, yellow, red).</li>
 *     <li><b>Custom Range:</b> Explicitly set the min/max values for the gauge scale.</li>
 *     <li><b>Modern Needle:</b> A sleek, animated needle design.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class GaugeRenderer extends BaseRenderer {
    private static final String KEY_JITTER = "Chart.circular.gauge.needleJitter";
    private static final String KEY_ALERT_PULSE = "Chart.circular.gauge.alertPulse";
    private static final String KEY_THICKNESS = "Chart.circular.gauge.thickness";
    private static final String KEY_GHOST_ENABLED = "Chart.circular.ghost.enabled";
    private static final String KEY_GHOST_ALPHA = "Chart.circular.ghost.alpha";
    private static final String KEY_LOOKAHEAD = "Chart.predictive.global.lookahead";
    private static final String KEY_SMOOTHING = "Chart.predictive.global.smoothing";

    /**
     * Defines a colored value band on the gauge scale.
     */
    public static final class Band {
        private double from;
        private double to;
        private ArberColor color;

        /**
         * Creates a band for the given range and color.
         *
         * @param from start value (inclusive)
         * @param to end value (inclusive)
         * @param color band color
         */
        public Band(double from, double to, ArberColor color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }

        public double getFrom() {
            return from;
        }

        public Band setFrom(double from) {
            this.from = from;
            return this;
        }

        public double getTo() {
            return to;
        }

        public Band setTo(double to) {
            this.to = to;
            return this;
        }

        public ArberColor getColor() {
            return color;
        }

        public Band setColor(ArberColor color) {
            this.color = color;
            return this;
        }
    }

    private double minValue = 0.0;
    private double maxValue = 100.0;
    private double value = 0.0;
    private AnimationProfile animationProfile = AnimationProfile.ACADEMIC;
    private List<Band> bands = List.of();

    private double animatedValue = 0.0;
    private double jitterEnergy = 0.0;
    private double lastValue = Double.NaN;
    private double smoothedDelta = 0.0;

    public GaugeRenderer() {
        super("gauge");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (model != null && model.getPointCount() > 0) {
            setValue(model.getY(0));
        }

        ArberRect b = context.getPlotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double size = Math.min(b.getWidth(), b.getHeight());
        double cx = b.centerX();
        double cy = b.centerY() + size * 0.15;
        double radius = size * 0.4;

        ChartTheme theme = getResolvedTheme(context);

        drawBands(canvas, cx, cy, radius, theme);
        drawGhostNeedle(canvas, cx, cy, radius, theme);
        drawNeedle(canvas, cx, cy, radius, theme);
        drawHub(canvas, cx, cy, theme);
    }

    private void drawBands(ArberCanvas canvas, double cx, double cy, double radius, ChartTheme theme) {
        double startAngle = 225;
        double sweepAngle = 270;
        float thickness = ChartAssets.getFloat(KEY_THICKNESS, 0.15f);
        thickness = (float) MathUtils.clamp(thickness, 0.08, 0.4);
        float bandWidth = (float) (radius * thickness);
        canvas.setStroke(bandWidth);

        if (bands == null || bands.isEmpty()) {
            double lowT = 0.0;
            double midT = 0.60;
            double highT = 0.85;

            double lowAngle = startAngle - sweepAngle * lowT;
            double midAngle = startAngle - sweepAngle * midT;
            double highAngle = startAngle - sweepAngle * highT;

            canvas.setColor(theme.getBearishColor());
            drawArcPolyline(canvas, cx, cy, radius, lowAngle, -(midT - lowT) * sweepAngle);

            canvas.setColor(theme.getAccentColor());
            drawArcPolyline(canvas, cx, cy, radius, midAngle, -(highT - midT) * sweepAngle);

            canvas.setColor(theme.getBullishColor());
            drawArcPolyline(canvas, cx, cy, radius, highAngle, -(1.0 - highT) * sweepAngle);
            return;
        }

        for (Band band : bands) {
            double bandStartT = MathUtils.clamp((band.from - minValue) / (maxValue - minValue), 0, 1);
            double bandEndT = MathUtils.clamp((band.to - minValue) / (maxValue - minValue), 0, 1);

            double bandStartAngle = startAngle - sweepAngle * bandStartT;
            double bandSweep = -sweepAngle * (bandEndT - bandStartT);

            canvas.setColor(band.color != null ? band.color : themeAccent(null));
            drawArcPolyline(canvas, cx, cy, radius, bandStartAngle, bandSweep);
        }
    }

    private void drawNeedle(ArberCanvas canvas, double cx, double cy, double radius, ChartTheme theme) {
        double range = maxValue - minValue;
        if (range <= 0) return;

        double rawT = (animatedValue - minValue) / range;
        boolean alert = ChartAssets.getBoolean(KEY_ALERT_PULSE, true) && (rawT < 0.0 || rawT > 1.0);
        double t = MathUtils.clamp(rawT, 0, 1);

        double angleDeg = 225 - 270 * t;
        if (ChartAssets.getBoolean(KEY_JITTER, true) && jitterEnergy > 0.02) {
            double seconds = System.nanoTime() * 1.0e-9;
            double jitterDeg = Math.sin(seconds * 18.0) * (1.6 * jitterEnergy);
            angleDeg += jitterDeg;
            jitterEnergy *= 0.96;
        }

        ArberColor needleColor = theme.getAccentColor();
        if (alert) {
            double seconds = System.nanoTime() * 1.0e-9;
            float pulse = (float) (0.55 + 0.45 * (0.5 + 0.5 * Math.sin(seconds * 6.0)));
            needleColor = ColorRegistry.applyAlpha(needleColor, pulse);
        }
        canvas.setColor(needleColor);
        canvas.setStroke((float) ChartScale.scale(2.0));

        double rad = Math.toRadians(angleDeg);
        double nx = cx + Math.cos(rad) * (radius * 0.95);
        double ny = cy - Math.sin(rad) * (radius * 0.95);
        drawLine(canvas, cx, cy, nx, ny);
    }

    private void drawGhostNeedle(ArberCanvas canvas, double cx, double cy, double radius, ChartTheme theme) {
        if (!ChartAssets.getBoolean(KEY_GHOST_ENABLED, true)) return;
        double range = maxValue - minValue;
        if (range <= 0) return;
        if (!Double.isFinite(smoothedDelta)) return;

        int lookahead = ChartAssets.getInt(KEY_LOOKAHEAD, 32);
        double ghostValue = PredictiveMath.extrapolate(value, smoothedDelta, lookahead);
        double t = MathUtils.clamp((ghostValue - minValue) / range, 0, 1);

        double angleDeg = 225 - 270 * t;
        double rad = Math.toRadians(angleDeg);
        double nx = cx + Math.cos(rad) * (radius * 0.95);
        double ny = cy - Math.sin(rad) * (radius * 0.95);

        float ghostAlpha = ChartAssets.getFloat(KEY_GHOST_ALPHA, 0.25f);
        ArberColor needleColor = ColorRegistry.applyAlpha(theme.getAccentColor(), ghostAlpha);
        canvas.setColor(needleColor);
        canvas.setStroke((float) ChartScale.scale(2.0));
        drawLine(canvas, cx, cy, nx, ny);
    }

    private void drawHub(ArberCanvas canvas, double cx, double cy, ChartTheme theme) {
        double hubRadius = ChartScale.scale(8.0);
        ArberColor fg = theme.getForeground();
        canvas.setColor(ColorRegistry.applyAlpha(fg, 0.25f));
        drawCircleFill(canvas, cx, cy, hubRadius);
        canvas.setColor(ColorRegistry.applyAlpha(fg, 0.55f));
        drawCircleFill(canvas, cx, cy, hubRadius * 0.6);
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

    // --- Public API ---

    public GaugeRenderer setRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    public GaugeRenderer setValue(double value) {
        if (Math.abs(this.value - value) > 1e-6) {
            this.value = value;
            animatedValue = value;
        }
        return this;
    }

    public GaugeRenderer setAnimationProfile(AnimationProfile profile) {
        this.animationProfile = (profile != null) ? profile : AnimationProfile.ACADEMIC;
        if (!this.animationProfile.animatesData()) {
            animatedValue = value;
        }
        return this;
    }

    public GaugeRenderer setBands(List<Band> bands) {
        this.bands = bands;
        return this;
    }
}
