package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.geometry.TextAnchor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.domain.render.AxisRole;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartScale;

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
    public static final ThreadLocal<double[]> PIXEL_BUF = new ThreadLocal<>() {
        @Override
        protected double[] initialValue() {
            return new double[2];
        }
    };
    public static final ThreadLocal<double[]> PIXEL_BUF4 = new ThreadLocal<>() {
        @Override
        protected double[] initialValue() {
            return new double[4];
        }
    };
    private final String id;
    // Theme for this renderer
    private ChartTheme theme;
    private transient PlotContext activeContext;

    // Layer index for multi-layer charts (0-based)
    private int layerIndex = 0;
    private boolean multiColor;
    private AxisRole yAxisRole = AxisRole.PRIMARY;

    protected BaseRenderer(String id) {
        this.id = id;
    }

    /**
     * Core render entry using ArberCanvas (headless/bridge).
     */
    public void render(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (canvas == null) return;
        drawData(canvas, model, context);
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
     * <b>Framework Note:</b> This method is typically called by a platform-specific
     * chart layer manager when attaching renderers to a view.
     * </p>
     *
     * @param theme The chart theme to apply. May be {@code null}, in which case fallbacks are used.
     */
    public BaseRenderer setTheme(ChartTheme theme) {
        this.theme = theme;
        return this;
    }

    /**
     * Sets the layer index for this renderer.
     * <p>
     * <b>Framework Note:</b> This is typically called by a platform-specific
     * chart layer manager when attaching renderers. The index determines which
     * color from the theme palette is used.
     * </p>
     *
     * @param index The 0-based layer index (0 = first color, 1 = second color, etc.)
     */
    public BaseRenderer setLayerIndex(int index) {
        this.layerIndex = index;
        return this;
    }

    /**
     * Assigns the renderer to the primary or secondary Y axis.
     *
     * @param role axis role (defaults to PRIMARY)
     */
    public BaseRenderer setYAxisRole(AxisRole role) {
        this.yAxisRole = (role != null) ? role : AxisRole.PRIMARY;
        return this;
    }

    /**
     * Returns the current Y axis role.
     */
    public AxisRole getYAxisRole() {
        return yAxisRole;
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
     * Computes an anchor point inside the given bounds.
     *
     * <p>Used by label placement logic in a platform-agnostic way. Rendering backends
     * apply font metrics and alignment rules.</p>
     */
    protected ArberPoint anchorPoint(ArberRect bounds, TextAnchor anchor, double offsetX, double offsetY, ArberPoint out) {
        ArberRect b = (bounds != null) ? bounds : new ArberRect(0, 0, 0, 0);
        TextAnchor a = (anchor != null) ? anchor : TextAnchor.TOP_LEFT;
        ArberPoint target = (out != null) ? out : new ArberPoint();

        double x;
        double y;
        switch (a) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER, BASELINE_CENTER -> x = b.x() + b.width() * 0.5;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT, BASELINE_RIGHT -> x = b.x() + b.width();
            default -> x = b.x();
        }

        switch (a) {
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> y = b.y() + b.height() * 0.5;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> y = b.y() + b.height();
            case BASELINE_LEFT, BASELINE_CENTER, BASELINE_RIGHT -> y = b.y() + b.height();
            default -> y = b.y();
        }

        target.setLocation(x + offsetX, y + offsetY);
        return target;
    }

    protected ArberPoint anchorPoint(ArberRect bounds, TextAnchor anchor, ArberPoint out) {
        return anchorPoint(bounds, anchor, 0.0, 0.0, out);
    }

    /**
     * Enables or disables multi-color rendering for supported renderers.
     *
     * @param enabled true to enable multi-color mode
     */
    public BaseRenderer setMultiColor(boolean enabled){
        this.multiColor = enabled;
        return this;
        
    }

    /**
     * Returns whether multi-color mode is enabled.
     *
     * @since 1.7.0
     */
    public boolean isMultiColorEnabled() {
        return multiColor;
    }
    /**
     * Returns whether multi-color mode is enabled.
     *
     * @return true when the renderer should use per-series palette colors
     * @since 1.5.0
     */
    protected boolean isMultiColor() {
        return multiColor;
    }
    /**
         * Resolves the theme for the current render call.
         *
         * <p><b>Framework contract (Option 1):</b> {@link PlotContext#getTheme()} must be non-null
         * during rendering. Failing fast here prevents silent dark/light inconsistencies.</p>
         *//**
     * @since 1.5.0
    */
    protected ChartTheme getResolvedTheme(PlotContext context) {
        if (theme != null) return theme;
        ChartTheme ctxTheme = (context != null) ? context.getTheme() : null;
        return Objects.requireNonNull(ctxTheme, "ChartTheme must not be null during rendering");
    }

    protected ArberColor themeForeground(PlotContext context) {
        return getResolvedTheme(context).getForeground();
    }

    protected ArberColor themeBackground(PlotContext context) {
        return getResolvedTheme(context).getBackground();
    }

    protected ArberColor themeAxisLabel(PlotContext context) {
        return getResolvedTheme(context).getAxisLabelColor();
    }

    protected ArberColor themeGrid(PlotContext context) {
        return getResolvedTheme(context).getGridColor();
    }

    protected ArberColor themeAccent(PlotContext context) {
        return getResolvedTheme(context).getAccentColor();
    }

    protected ArberColor themeSeries(PlotContext context, int index) {
        return getResolvedTheme(context).getSeriesColor(index);
    }

    protected ArberColor themeBullish(PlotContext context) {
        return getResolvedTheme(context).getBullishColor();
    }

    protected ArberColor themeBearish(PlotContext context) {
        return getResolvedTheme(context).getBearishColor();
    }
    /**
     * @since 1.5.0
    */

    protected ArberColor seriesOrBase(ChartModel model, PlotContext context, int index) {
        if (!isMultiColor()) {
            return getSeriesColor(model);
        }
        ArberColor c = themeSeries(context, index);
        return c != null ? c : getSeriesColor(model);
    }
    /**
         * Gets the current theme for this renderer.
         *
         * @return The current theme, or {@code null} if not set
         *//**
     * @since 1.5.0
    */
    protected ChartTheme getTheme() {
        return theme;
    }

    /**
     * ArberCanvas rendering hook. Default no-op for non-migrated renderers.
     */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
    }

    protected static ArberColor toArberColor(ArberColor color) {
        if (color == null) return ArberColor.TRANSPARENT;
        return color;
    }

    /**
     * Returns the active PlotContext for the current render pass.
     */
    protected PlotContext getActiveContext() {
        return activeContext;
    }

    /**
     * Returns the data index nearest to the given pixel coordinate, if supported.
     */
    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
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
         * Return a small allocation-free buffer for mapToPixel results (length=2)
         *//**
     * @since 1.5.0
    */
    protected double[] pBuffer() {
        return PIXEL_BUF.get();
    }
    /**
         * Return a small allocation-free buffer used for multi-value mapping (length=4)
         *//**
     * @since 1.5.0
    */
    protected double[] pBuffer4() {
        return PIXEL_BUF4.get();
    }
    /**
     * @since 1.5.0
    */

    /**
     * @since 1.5.0
    */
    protected ArberColor getSeriesColor(ChartModel model) {
        ChartTheme t = theme;
        if (t == null) {
            // In framework mode, theme is expected to come from the PlotContext.
            // Keep this method usable for legacy call sites by falling back to a stable core default.
            t = (activeContext != null) ? activeContext.getTheme() : null;
            if (t == null) {
                return ArberColor.TRANSPARENT;
            }
        }
        return t.getSeriesColor(layerIndex);
    }

    /**
     * Returns the color for the legend.
     *
     * @param model The current data model
     * @return color for legend swatches
     */
    public ArberColor getLegendColor(ChartModel model) {
        return getSeriesColor(model);
    }

    /**
     * @since 1.5.0
    */
    protected float getSeriesStrokeWidth() {
        float base = 1.5f;
        if (activeContext != null && activeContext.getRenderHints() != null) {
            Float hinted = activeContext.getRenderHints().getStrokeWidth();
            if (hinted != null && Float.isFinite(hinted) && hinted > 0f) {
                base = hinted;
            }
        }
        return ChartScale.scale(base);
    }

    // AWT shape/label/gradient helpers removed from core. Use ArberCanvas primitives or bridge-specific UI.
}
