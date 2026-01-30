package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.domain.render.AxisRole;
import com.arbergashi.charts.api.ArberThreadException;
import com.arbergashi.charts.render.medical.CalibrationRenderer;
import com.arbergashi.charts.render.financial.PredictiveCandleRenderer;
import com.arbergashi.charts.render.financial.LiquidityHeatmapRenderer;
import com.arbergashi.charts.render.predictive.AnomalyGapRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.render.analysis.MovingCorrelationRenderer;
import com.arbergashi.charts.render.statistical.LiveDistributionOverlayRenderer;
import com.arbergashi.charts.render.statistical.QuantileBandRenderer;
import com.arbergashi.charts.render.common.PhysicalScaleRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.standard.AreaRenderer;
import com.arbergashi.charts.render.standard.ScatterRenderer;
import com.arbergashi.charts.render.standard.PredictivePathRenderer;
import com.arbergashi.charts.render.standard.StepRenderer;
import com.arbergashi.charts.render.standard.StepAreaRenderer;
import com.arbergashi.charts.render.standard.BarRenderer;
import com.arbergashi.charts.render.standard.GroupedBarRenderer;
import com.arbergashi.charts.render.standard.StackedBarRenderer;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer;
import com.arbergashi.charts.render.specialized.RadarGlowRenderer;
import com.arbergashi.charts.render.specialized.SmithChartRenderer;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.grid.GridLayer;
import com.arbergashi.charts.render.grid.SmithChartGridLayer;
import com.arbergashi.charts.domain.legend.LegendConfig;
import com.arbergashi.charts.render.specialized.SmithChartTransform;
import com.arbergashi.charts.render.specialized.VSWRCircleRenderer;
import com.arbergashi.charts.util.ChartAssets;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
/**
 * Fluent builder for quick chart panel configuration.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ArberChartBuilder {

    private final ArberChartPanel panel;
    private final AxisConfig xAxis = new AxisConfig();
    private final AxisConfig yAxis = new AxisConfig();
    private final AxisConfig yAxisSecondary = new AxisConfig();
    private final ChartRenderHints hints = new ChartRenderHints();
    private boolean xAxisConfigured;
    private boolean yAxisConfigured;
    private boolean yAxisSecondaryConfigured;
    private boolean hintsConfigured;
    private ChartTheme theme;

    private ArberChartBuilder(ArberChartPanel panel) {
        this.panel = panel;
    }

    public static ArberChartBuilder of() {
        return new ArberChartBuilder(new ArberChartPanel(null, null));
    }

    public static ArberChartBuilder of(ChartModel model, ChartRenderer renderer) {
        return new ArberChartBuilder(new ArberChartPanel(model, renderer));
    }

    public ArberChartBuilder setTitle(String title) {
        panel.setName(title);
        panel.putClientProperty("chart.title", title);
        return this;
    }

    public ArberChartBuilder setTheme(ChartTheme theme) {
        this.theme = theme;
        return this;
    }

    public ArberChartBuilder setDarkMode() {
        setTheme(ChartThemes.getDarkTheme());
        return this;
    }

    public ArberChartBuilder setGridLayer(GridLayer gridLayer) {
        panel.setGridLayer(gridLayer);
        return this;
    }

    /**
     * Adds a predictive shadow layer that renders after the grid but before data.
     */
    public ArberChartBuilder setPredictiveShadow(PredictionModel predictor) {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        setPredictiveLayers(primary, predictor);
        return this;
    }

    /**
     * Adds predictive layers plus a financial ghost-candle overlay.
     */
    public ArberChartBuilder setPredictiveFinancials(PredictionModel predictor) {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        PredictiveShadowRenderer shadow = setPredictiveLayers(primary, predictor);
        if (shadow == null) {
            return this;
        }
        if (ChartAssets.getBoolean("Chart.financial.ghost.enabled", true)) {
            panel.setPreDataLayer(primary, new PredictiveCandleRenderer(shadow));
        }
        return this;
    }

    /**
     * Adds a liquidity heatmap overlay for level-2 style depth visualization.
     */
    public ArberChartBuilder setLiquidityHeatmap() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        if (ChartAssets.getBoolean("Chart.financial.liquidityHeatmap.enabled", true)) {
            panel.setPreDataLayer(primary, new LiquidityHeatmapRenderer());
        }
        return this;
    }

    /**
     * Adds an audit/provenance overlay for non-original ticks.
     */
    public ArberChartBuilder setAuditTrailOverlay() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        if (ChartAssets.getBoolean("Chart.audit.enabled", true)) {
            panel.setPreDataLayer(primary, new com.arbergashi.charts.render.financial.AuditTrailRenderer());
        }
        return this;
    }

    /**
     * Adds statistical overlays (quantile band + live distribution histogram).
     */
    public ArberChartBuilder setStatisticalOverlays() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        panel.setPreDataLayer(primary, new QuantileBandRenderer());
        panel.setPreDataLayer(primary, new LiveDistributionOverlayRenderer());
        return this;
    }

    /**
     * Adds a rolling correlation indicator band as a pre-data overlay.
     */
    public ArberChartBuilder setCorrelationOverlay() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        panel.setPreDataLayer(primary, new MovingCorrelationRenderer());
        return this;
    }

    /**
     * Adds a signal-integrity overlay that switches to predictive-only mode when data is stale.
     */
    public ArberChartBuilder setSignalIntegrityGuard() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        if (ChartAssets.getBoolean("Chart.signal.integrity.enabled", true)) {
            panel.setPreDataLayer(primary, new com.arbergashi.charts.render.military.SignalIntegrityLayer());
        }
        return this;
    }

    /**
     * Adds a predictive dashed extension for standard line/area/scatter charts.
     */
    public ArberChartBuilder setPredictiveFuture() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        ChartRenderer renderer = panel.getPrimaryRenderer();
        PredictivePathRenderer.Style style = PredictivePathRenderer.Style.LINEAR;
        if (renderer instanceof StepRenderer || renderer instanceof StepAreaRenderer) {
            style = PredictivePathRenderer.Style.STEP;
        } else if (renderer instanceof BarRenderer || renderer instanceof GroupedBarRenderer || renderer instanceof StackedBarRenderer) {
            style = PredictivePathRenderer.Style.BAR;
        }
        panel.setPreDataLayer(primary, new PredictivePathRenderer(style));
        return this;
    }

    private PredictiveShadowRenderer setPredictiveLayers(ChartModel primary, PredictionModel predictor) {
        PredictiveShadowRenderer shadow = new PredictiveShadowRenderer(predictor);
        AnomalyGapRenderer anomaly = new AnomalyGapRenderer(shadow);
        panel.setPreDataLayer(primary, shadow);
        panel.setPreDataLayer(primary, anomaly);
        return shadow;
    }

    public ArberChartBuilder setCoordinateTransform(CoordinateTransformer transformer) {
        panel.setCoordinateTransformer(transformer);
        return this;
    }

    public ArberChartBuilder setSmithChartMode() {
        panel.setGridLayer(new SmithChartGridLayer());
        panel.setCoordinateTransformer(new SmithChartTransform());
        return this;
    }

    public ArberChartBuilder setTooltips(boolean enabled) {
        panel.setTooltips(enabled);
        return this;
    }

    public ArberChartBuilder setLegend(boolean visible) {
        panel.setLegend(visible);
        return this;
    }

    public ArberChartBuilder setLegendConfig(LegendConfig config) {
        panel.setLegendConfig(config);
        return this;
    }

    /**
     * Adds a circular latency report overlay (p99/p99.9) for dashboard validation.
     */
    public ArberChartBuilder setCircularPerformanceReport() {
        ChartModel primary = panel.getPrimaryModel();
        if (primary == null) {
            return this;
        }
        panel.setLayer(primary, new CircularLatencyOverlayRenderer(panel.getLatencyTracker()));
        return this;
    }

    /**
     * Enables the global performance audit overlay (p99/p99.9).
     */
    public ArberChartBuilder setPerformanceAudit() {
        ChartAssets.setProperty("Chart.performance.audit.enabled", "true");
        if (shouldAddCircularPerfOverlay()) {
            setCircularPerformanceReport();
        }
        return this;
        
    }

    /**
     * Enables realtime watchdog monitoring for playback/stream integrity.
     */
    public ArberChartBuilder setRealtimeGuard() {
        ChartAssets.setProperty("Chart.watchdog.enabled", "true");
        ChartAssets.setProperty("Chart.playback.status.enabled", "true");
        ChartAssets.setProperty("Chart.watchdog.warn.p999", "1.0");
        ChartAssets.setProperty("Chart.watchdog.crit.p999", "3.0");
        ChartAssets.setProperty("Chart.watchdog.warn.dropRate", "0.005");
        ChartAssets.setProperty("Chart.watchdog.crit.dropRate", "0.02");
        ChartAssets.setProperty("Chart.watchdog.drop.window", "1000");
        return this;
    }

    private boolean shouldAddCircularPerfOverlay() {
        ChartRenderer renderer = panel.getPrimaryRenderer();
        if (renderer == null) return false;
        return renderer.getClass().getName().startsWith("com.arbergashi.charts.render.circular.");
    }

    /**
     * Applies a tactical configuration preset via ChartAssets properties.
     */
    public ArberChartBuilder setTacticalProfile() {
        ChartAssets.setProperty("Chart.tactical.nightvision.enabled", "true");
        ChartAssets.setProperty("Chart.signal.integrity.enabled", "true");
        ChartAssets.setProperty("Chart.playback.status.enabled", "true");
        ChartAssets.setProperty("Chart.circular.dynamic.zones.enabled", "true");
        return this;
    }

    /**
     * Applies a medical configuration preset via ChartAssets properties.
     */
    public ArberChartBuilder setMedicalProfile() {
        ChartAssets.setProperty("Chart.medical.alert.enabled", "true");
        ChartAssets.setProperty("Chart.scale.physical.enabled", "true");
        ChartAssets.setProperty("Chart.scale.physical.ratio", "25");
        ChartAssets.setProperty("Chart.scale.physical.y.enabled", "true");
        ChartAssets.setProperty("Chart.scale.physical.y.ratio", "10");
        return this;
    }

    public ArberChartBuilder setLocale(Locale locale) {
        if (locale != null) {
            panel.setLocale(locale);
        }
        return this;
    }

    public ArberChartBuilder setLayer(ChartModel model, ChartRenderer renderer) {
        panel.setLayer(model, renderer);
        return this;
    }

    public ArberChartBuilder setLayerSecondary(ChartModel model, ChartRenderer renderer) {
        if (renderer instanceof com.arbergashi.charts.render.BaseRenderer br) {
            br.setYAxisRole(AxisRole.SECONDARY);
        }
        panel.setLayer(model, renderer);
        return this;
    }

    public ArberChartBuilder setLineSeries(String name, double[] x, double[] y) {
        DefaultChartModel model = new DefaultChartModel(name);
        if (x != null && y != null) {
            model.setXYArrays(x, y);
        }
        panel.setLayer(model, new LineRenderer());
        return this;
    }

    public <T> ArberChartBuilder setLineSeries(String name, List<T> data, ToDoubleFunction<T> xFn, ToDoubleFunction<T> yFn) {
        DefaultChartModel model = new DefaultChartModel(name);
        if (data != null && xFn != null && yFn != null) {
            for (T item : data) {
                model.setXY(xFn.applyAsDouble(item), yFn.applyAsDouble(item));
            }
        }
        panel.setLayer(model, new LineRenderer());
        return this;
    }

    public ArberChartBuilder setXAxis(Consumer<AxisConfig> config) {
        if (config != null) {
            config.accept(xAxis);
            xAxisConfigured = true;
        }
        return this;
    }

    public ArberChartBuilder setYAxis(Consumer<AxisConfig> config) {
        if (config != null) {
            config.accept(yAxis);
            yAxisConfigured = true;
        }
        return this;
    }

    public ArberChartBuilder setYAxisSecondary(Consumer<AxisConfig> config) {
        if (config != null) {
            config.accept(yAxisSecondary);
            yAxisSecondaryConfigured = true;
        }
        return this;
    }

    public ArberChartBuilder setRenderHints(Consumer<ChartRenderHints> config) {
        if (config != null) {
            config.accept(hints);
            hintsConfigured = true;
        }
        return this;
    }

    public ArberChartPanel build() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new ArberThreadException("ArberChartBuilder must be invoked on EDT to ensure Theme-Integrity.");
        }
        if (panel.getLayerCount() == 0) {
            throw new IllegalStateException("No chart layers configured");
        }
        if (xAxisConfigured) {
            panel.setXAxisConfig(xAxis);
        }
        if (yAxisConfigured) {
            panel.setYAxisConfig(yAxis);
        }
        if (yAxisSecondaryConfigured) {
            panel.setYAxisSecondaryConfig(yAxisSecondary);
        }
        if (hintsConfigured) {
            panel.setRenderHints(hints);
        }
        if (theme != null) {
            panel.setTheme(theme);
        }
        ChartModel primary = panel.getPrimaryModel();
        if (primary != null) {
            ChartRenderer primaryRenderer = panel.getPrimaryRenderer();
            if (primaryRenderer instanceof RadarRenderer
                    && ChartAssets.getBoolean("Chart.radar.glow.enabled", true)) {
                panel.setPreDataLayer(primary, new RadarGlowRenderer());
            }
            if (primaryRenderer instanceof SmithChartRenderer
                    && ChartAssets.getBoolean("Chart.smith.vswr.enabled", true)) {
                panel.setPreDataLayer(primary, new VSWRCircleRenderer());
            }
            if (ChartAssets.getBoolean("Chart.calibration.enabled", false)) {
                CalibrationRenderer calibration = new CalibrationRenderer();
                panel.setPreDataLayer(primary, calibration);
            }
            if (ChartAssets.getBoolean("Chart.scale.physical.enabled", false)) {
                PhysicalScaleRenderer scaleRenderer = new PhysicalScaleRenderer();
                panel.setPreDataLayer(primary, scaleRenderer);
            }
        }
        return panel;
    }
}
