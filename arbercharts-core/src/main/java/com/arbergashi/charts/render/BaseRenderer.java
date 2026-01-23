package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for all chart renderers, providing common functionality and utility methods.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public abstract class BaseRenderer implements com.arbergashi.charts.render.ChartRenderer {

    // Thread-local pixel buffers for zero-allocation coordinate mapping
    public static final ThreadLocal<double[]> PIXEL_BUF = ThreadLocal.withInitial(() -> new double[2]);
    public static final ThreadLocal<double[]> PIXEL_BUF4 = ThreadLocal.withInitial(() -> new double[4]);
    private final String id;
    // Reusable objects (Thread-confinement is guaranteed by Swing EDT)
    private final Path2D.Double pathCache = new Path2D.Double();
    private final Line2D.Double lineCache = new Line2D.Double();
    private final Rectangle2D.Double rectCache = new Rectangle2D.Double();
    private final Ellipse2D.Double ellipseCache = new Ellipse2D.Double();
    private final Arc2D.Double arcCache = new Arc2D.Double();
    private final RoundRectangle2D.Float roundRectCache = new RoundRectangle2D.Float();
    private final java.awt.geom.CubicCurve2D.Double cubicCache = new java.awt.geom.CubicCurve2D.Double();
    // Stroke Cache
    private final Map<Float, BasicStroke> strokeCache = new HashMap<>();
    private final Map<String, BasicStroke> complexStrokeCache = new HashMap<>();
    // Font cache for allocation-free label rendering
    private final Map<String, Font> fontCache = new HashMap<>();

    // Theme for this renderer
    private ChartTheme theme;
    private transient PlotContext activeContext;

    // Layer index for multi-layer charts (0-based)
    private int layerIndex = 0;
    private boolean multiColor;

    protected BaseRenderer(String id) {
        this.id = id;
    }

    /**
     * Returns the renderer ID used for registry/legend wiring.
     *
     * @return renderer ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the theme for this renderer.
     * <p>
     * <b>Framework Note:</b> This method is automatically called by {@link com.arbergashi.charts.ui.ArberChartPanel}
     * when adding layers. Framework users should ensure that the panel has a theme set before rendering.
     * </p>
     *
     * @param theme The chart theme to apply. May be {@code null}, in which case fallbacks are used.
     */
    public void setTheme(ChartTheme theme) {
        this.theme = theme;
    }

    /**
     * Sets the layer index for this renderer.
     * <p>
     * <b>Framework Note:</b> This is automatically called by {@link com.arbergashi.charts.ui.ArberChartPanel}
     * when adding layers. The index determines which color from the theme palette is used.
     * </p>
     *
     * @param index The 0-based layer index (0 = first color, 1 = second color, etc.)
     */
    public void setLayerIndex(int index) {
        this.layerIndex = index;
    }

    /**
     * Returns the current layer index used for theme palette selection.
     *
     * @return 0-based layer index
     */
    protected int getLayerIndex() {
        return layerIndex;
    }

    /**
     * Enables or disables multi-color rendering for supported renderers.
     *
     * @param enabled true to enable multi-color mode
     * @return this renderer for chaining
     */
    public BaseRenderer setMultiColor(boolean enabled) {
        this.multiColor = enabled;
        return this;
    }

    /**
     * Returns whether multi-color mode is enabled.
     *
     * @return true when the renderer should use per-series palette colors
     */
    protected boolean isMultiColor() {
        return multiColor;
    }

    /**
     * Resolves the theme for the current render call.
     *
     * <p><b>Framework contract (Option 1):</b> {@link PlotContext#theme()} must be non-null
     * during rendering. Failing fast here prevents silent dark/light inconsistencies.</p>
     */
    protected ChartTheme resolveTheme(PlotContext context) {
        if (theme != null) return theme;
        ChartTheme ctxTheme = (context != null) ? context.theme() : null;
        return Objects.requireNonNull(ctxTheme, "ChartTheme must not be null during rendering");
    }

    protected Color themeForeground(PlotContext context) {
        return resolveTheme(context).getForeground();
    }

    protected Color themeBackground(PlotContext context) {
        return resolveTheme(context).getBackground();
    }

    protected Color themeAxisLabel(PlotContext context) {
        return resolveTheme(context).getAxisLabelColor();
    }

    protected Color themeGrid(PlotContext context) {
        return resolveTheme(context).getGridColor();
    }

    protected Color themeAccent(PlotContext context) {
        return resolveTheme(context).getAccentColor();
    }

    protected Color themeSeries(PlotContext context, int index) {
        return resolveTheme(context).getSeriesColor(index);
    }

    protected Color themeBullish(PlotContext context) {
        return resolveTheme(context).getBullishColor();
    }

    protected Color themeBearish(PlotContext context) {
        return resolveTheme(context).getBearishColor();
    }

    protected Color seriesOrBase(ChartModel model, PlotContext context, int index) {
        if (!isMultiColor()) {
            return getSeriesColor(model);
        }
        Color c = themeSeries(context, index);
        return c != null ? c : getSeriesColor(model);
    }

    /**
     * Gets the current theme for this renderer.
     *
     * @return The current theme, or {@code null} if not set
     */
    protected ChartTheme getTheme() {
        return theme;
    }

    /**
     * Main render method.
     *
     * @param g2 graphics context
     * @param model chart data model
     * @param context plot context
     */
    public void render(Graphics2D g2, ChartModel model, PlotContext context) {
        if (model == null || model.isEmpty()) return;

        // Reset path cache before each frame
        pathCache.reset();

        activeContext = context;
        try {
            drawData(g2, model, context);
        } finally {
            activeContext = null;
        }
    }

    /**
     * Implementation-specific drawing logic.
     * Must follow Zero-Allocation rules.
     */
    protected abstract void drawData(Graphics2D g2, ChartModel model, PlotContext context);

    /**
     * Returns the data index nearest to the given pixel coordinate, if supported.
     */
    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        return Optional.empty();
    }

    /**
     * Returns tooltip text for the given data index, or null when not supported.
     */
    @Override
    public String getTooltipText(int index, ChartModel model) {
        return null;
    }

    /**
     * Returns the renderer name used by legends and UI labels.
     */
    @Override
    public String getName() {
        return id;
    }

    /**
     * Indicates whether this renderer should appear in the legend.
     */
    public boolean isLegendRequired() {
        return true;
    }

    // --- Caching Helpers ---

    /**
     * Returns the shared Path2D.Double instance for building shapes.
     * NOTE: This is a shared, reusable object and is reset at frame start.
     * Do not retain a reference to this object across frames.
     */
    protected Path2D.Double getPathCache() {
        return pathCache;
    }

    /**
     * Return a small allocation-free buffer for mapToPixel results (length=2)
     */
    protected double[] pBuffer() {
        return PIXEL_BUF.get();
    }

    /**
     * Return a small allocation-free buffer used for multi-value mapping (length=4)
     */
    protected double[] pBuffer4() {
        return PIXEL_BUF4.get();
    }

    protected BasicStroke getCachedStroke(float width) {
        return strokeCache.computeIfAbsent(width, w ->
                new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    protected BasicStroke getCachedStroke(float width, int cap, int join) {
        String key = width + ":" + cap + ":" + join;
        return complexStrokeCache.computeIfAbsent(key, k -> new BasicStroke(width, cap, join));
    }

    protected BasicStroke getCachedStroke(float width, int cap, int join, float miterlimit, float[] dash, float dash_phase) {
        // For complex strokes (Dashed), local caching in the renderer is recommended (see MovingAverageRenderer)
        return new BasicStroke(width, cap, join, miterlimit, dash, dash_phase);
    }

    protected Color getSeriesColor(ChartModel model) {
        ChartTheme t = theme;
        if (t == null) {
            // In framework mode, theme is expected to come from the PlotContext.
            // Keep this method usable for legacy call sites by falling back to a stable core default.
            t = ChartThemes.defaultDark();
        }
        return t.getSeriesColor(layerIndex);
    }

    /**
     * Returns the color for the legend.
     *
     * @param model The current data model
     * @return color for legend swatches
     */
    public Color getLegendColor(ChartModel model) {
        return getSeriesColor(model);
    }

    protected Stroke getSeriesStroke() {
        float base = 1.5f;
        if (activeContext != null && activeContext.renderHints() != null) {
            Float hinted = activeContext.renderHints().getStrokeWidth();
            if (hinted != null && Float.isFinite(hinted) && hinted > 0f) {
                base = hinted;
            }
        }
        return getCachedStroke(ChartScale.scale(base));
    }

    // --- Shape Helpers (Zero-Allocation via Shared Cache) ---
    // NOTE: These methods always return the same object!
    // Use only for immediate drawing (g2.draw).

    protected Line2D getLine(double x1, double y1, double x2, double y2) {
        lineCache.setLine(x1, y1, x2, y2);
        return lineCache;
    }

    protected Rectangle2D getRect(double x, double y, double w, double h) {
        rectCache.setRect(x, y, w, h);
        return rectCache;
    }

    protected Ellipse2D getEllipse(double x, double y, double w, double h) {
        ellipseCache.setFrame(x, y, w, h);
        return ellipseCache;
    }

    protected Arc2D getArc(double x, double y, double w, double h, double start, double extent, int type) {
        arcCache.setArc(x, y, w, h, start, extent, type);
        return arcCache;
    }

    protected RoundRectangle2D getRoundRectangle(double x, double y, double w, double h, float arcw, float arch) {
        roundRectCache.setRoundRect((float) x, (float) y, (float) w, (float) h, arcw, arch);
        return roundRectCache;
    }

    protected java.awt.Shape getCubicCurve(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2) {
        cubicCache.setCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
        return cubicCache;
    }

    protected void drawHighlightPoint(Graphics2D g2, java.awt.geom.Point2D p, Color c) {
        double r = ChartScale.scale(3.0);
        g2.setColor(c);
        g2.fill(getEllipse(p.getX() - r, p.getY() - r, r * 2, r * 2));
    }

    protected void drawLabel(Graphics2D g2, String text, Font font, Color color, float x, float y) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    /**
     * Draws a localized label. The key is resolved via ChartI18N and then drawn using
     * {@link #drawLabel(Graphics2D, String, Font, Color, float, float)}.
     */
    protected void drawI18nLabel(Graphics2D g2, String key, Font font, Color color, float x, float y) {
        String text = com.arbergashi.charts.util.ChartI18N.getString(key);
        drawLabel(g2, text, font, color, x, y);
    }

    protected Paint getCachedGradient(Color base, float height) {
        // Simple gradient cache could be added here
        return new GradientPaint(0, 0, ColorUtils.withAlpha(base, 0.1f), 0, height, ColorUtils.withAlpha(base, 0.8f));
    }

    /**
     * Returns a cached Font for the requested base size and style.
     * This avoids deriving/allocating fonts each frame in draw loops.
     */
    protected Font getCachedFont(float baseSize, int style) {
        final int safeStyle = style & (Font.BOLD | Font.ITALIC);
        String key = safeStyle + ":" + Math.round(baseSize);
        return fontCache.computeIfAbsent(key, _k -> {
            Font base = UIManager.getFont("Label.font");
            if (base == null) base = new Font(Font.SANS_SERIF, safeStyle, Math.round(baseSize));
            return base.deriveFont(safeStyle, ChartScale.uiFontSize(base, baseSize));
        });
    }

    /**
     * Apply high-quality rendering hints for anti-aliasing / stroke control.
     * Renderers that need pixel-perfect quality should call this at frame start.
     */
    protected void setupQualityHints(Graphics2D g2) {
        // Use ChartEngine helper to keep hints consistent across renderers
        try {
            com.arbergashi.charts.util.ChartEngine.prepareGraphics(g2, true);
        } catch (Exception ignored) {
            // Best-effort - do not fail render if hints can't be applied
        }
    }
}
