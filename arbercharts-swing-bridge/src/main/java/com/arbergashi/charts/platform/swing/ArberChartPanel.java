package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.AnomalyEvent;
import com.arbergashi.charts.api.AnomalyListener;
import com.arbergashi.charts.api.AnimationProfile;
import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.ChartFocus;
import com.arbergashi.charts.domain.render.AxisRole;
import com.arbergashi.charts.render.legend.LayerLegendContext;
import com.arbergashi.charts.api.AxisGapModel;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.TextAnchor;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ChartModel.ChartModelListener;
import com.arbergashi.charts.render.AbstractSpatialLayer;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.render.circular.SunburstRenderer;
import com.arbergashi.charts.render.grid.DefaultGridLayer;
import com.arbergashi.charts.render.grid.GridLayer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartI18N;
import com.arbergashi.charts.util.FormatUtils;
import com.arbergashi.charts.util.NiceScale;
import com.arbergashi.charts.util.AnimationUtils;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;
import com.arbergashi.charts.platform.swing.util.PhysicalScaleResolver;
import com.arbergashi.charts.platform.swing.util.SwingAssets;
import com.arbergashi.charts.platform.swing.util.SwingRenderHints;
import com.arbergashi.charts.platform.render.StrokeCache;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.platform.swing.spatial.SwingSpatialBatchConsumer;
import com.arbergashi.charts.platform.swing.legend.DockedLegendPanel;
import com.arbergashi.charts.render.legend.InteractiveLegendOverlay;
import com.arbergashi.charts.render.legend.LegendChartContext;
import com.arbergashi.charts.domain.legend.LegendConfig;
import com.arbergashi.charts.domain.legend.LegendDockSide;
import com.arbergashi.charts.domain.legend.LegendPlacement;
import com.arbergashi.charts.domain.legend.LegendPosition;
import com.arbergashi.charts.domain.legend.LayerVisibilityModel;
import com.arbergashi.charts.domain.legend.LegendActionListener;
import com.arbergashi.charts.platform.export.ChartExportHandler;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.arbergashi.charts.platform.ui.HighPrecisionCrosshair;
import com.arbergashi.charts.platform.ui.TooltipOverlay;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
/**
 * <h2>ArberChartPanel</h2>
 * <p>
 * High-performance Swing panel for rendering charts with multiple layers.
 * This panel is the primary UI component of the framework.
 * </p>
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Multi-Layer Architecture:</b> Supports overlays and multiple series (e.g., candles + indicators).</li>
 *     <li><b>Zero-Allocation Rendering:</b> Optimized paint loop to minimize GC pressure.</li>
 *     <li><b>Smart Hit-Testing:</b> Snapping and multi-series tooltips.</li>
 *     <li><b>Theming:</b> Integrated with {@link com.arbergashi.charts.api.ChartTheme} for dark/light mode support.</li>
 *     <li><b>Interactive Navigation:</b> High-precision zooming and panning.</li>
 *     <li><b>Export &amp; Tools:</b> Context menu for exporting to PNG/SVG/PDF and resetting view.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @since 2024-06-01
 */
public class ArberChartPanel extends JPanel {


    // --- Architecture: Layers ---
    
    /**
     * Internal container for a renderable layer.
     */
    private static class RenderLayer {
        final String id;
        final ChartModel model;
        final ChartRenderer renderer;
        final ChartModelListener listener;

        RenderLayer(String id, ChartModel model, ChartRenderer renderer, ChartModelListener listener) {
            this.id = id;
            this.model = model;
            this.renderer = renderer;
            this.listener = listener;
        }
    }

    private final List<RenderLayer> layers = new CopyOnWriteArrayList<>();
    private final List<RenderLayer> preDataLayers = new CopyOnWriteArrayList<>();

    // --- Layout & Caching ---
    private final Insets padding = new Insets(32, 48, 36, 24);
    private final Rectangle2D.Double plotBoundsCache = new Rectangle2D.Double();
    
    // Viewport State (Camera)
    private double viewMinX = Double.NaN;
    private double viewMaxX = Double.NaN;
    private double viewMinY = Double.NaN;
    private double viewMaxY = Double.NaN;

    // Caching for Zero-Allocation Axis Rendering
    private final NiceScale cachedScaleX = new NiceScale(0, 1);
    private final NiceScale cachedScaleY = new NiceScale(0, 1);
    private final NiceScale cachedScaleSecondaryY = new NiceScale(0, 1);
    private final NiceScale measurementScale = new NiceScale(0, 1);
    private final double[] secondaryRangeBuffer = new double[2];
    private final double[] axisMapBuffer = new double[2];
    private transient PlotContext contextCache;
    private transient PlotContext secondaryContextCache;
    private int contextWidth = -1;
    private int contextHeight = -1;
    private double logicalPixelsPerMm = PhysicalScaleResolver.logicalPixelsPerMillimeter(null);
    private transient boolean cacheDirty = true;
    private transient long lastModelStamp = -1;
    private final SpatialPathBatchBuilder spatialPathBatchBuilder = new SpatialPathBatchBuilder();
    private final SwingSpatialBatchConsumer spatialBatchConsumer = new SwingSpatialBatchConsumer(spatialPathBatchBuilder);

    // --- Components ---
    private GridLayer gridLayer;
    private TooltipOverlay tooltipOverlay;
    private HighPrecisionCrosshair crosshair;
    private LegendChartContext legendContext;
    private JPopupMenu contextMenu;
    // add:
    private InteractiveLegendOverlay interactiveLegendOverlay;
    private DockedLegendPanel dockedLegendPanel;
    private LegendConfig legendConfig = LegendConfig.overlay(LegendPosition.TOP_LEFT);
    private JComponent overlayCanvas;
    private final LayerVisibilityModel layerVisibility = new LayerVisibilityModel();
    private ChartExportHandler exportHandler;
    private JMenuItem exportPngItem;
    private JMenuItem exportSvgItem;
    private JMenuItem exportPdfItem;
    private final List<AnomalyListener> anomalyListeners = new CopyOnWriteArrayList<>();

    // Configuration
    private boolean tooltipsEnabled = true;
    private Point lastCrosshairPoint;
    private RenderLayer lastSnapLayer;
    private int lastSnapIndex = -1;
    private Point lastSnapPoint;
    private boolean externalRenderSurface = false;
    private boolean crosshairEnabled = true;
    private boolean legendVisible = true;
    private boolean axisLabelsVisible = true;
    private boolean animationsEnabled = !Boolean.getBoolean("arbercharts.reduceMotion");
    private boolean modelListenersAttached = true;
    private Locale locale = Locale.getDefault();
    private AxisConfig xAxisConfig = new AxisConfig();
    private AxisConfig yAxisConfig = new AxisConfig();
    private AxisConfig yAxisSecondaryConfig = new AxisConfig();
    private AxisGapModel gapModel;
    private AnimationProfile animationProfile = AnimationProfile.ACADEMIC;
    private com.arbergashi.charts.api.CoordinateTransformer coordinateTransformer;
    private ChartRenderHints renderHints = new ChartRenderHints();
    private transient Composite soloComposite;
    private transient float soloCompositeAlpha = Float.NaN;
    private final LatencyTracker latencyTracker = new LatencyTracker(600);
    private final Map<Class<?>, Boolean> canvasSupportCache = new ConcurrentHashMap<>();
    private volatile double physicalPixelsPerMm = Double.NaN;
    private boolean frozen;
    private double freezeScrub;
    private final JLabel freezeMeasurementLabel = new JLabel() {
        @Override
        public boolean contains(int x, int y) { return false; }
    };
    private boolean freezeMeasureArmed;
    private double freezeMeasureStartX;
    private final double[] freezeMeasureScratch = new double[2];
    private com.arbergashi.charts.api.ViewportAuditTrail auditTrail;
    private com.arbergashi.charts.api.forensic.StreamPlaybackDrive streamPlaybackDrive;

    // --- Animation State ---
    private final Timer zoomAnimationTimer;
    private final LongSupplier animationClock;
    private long zoomAnimationStart;
    private static final long ZOOM_ANIMATION_DURATION_NANOS = 180_000_000L;
    private double animFromMinX, animFromMaxX, animToMinX, animToMaxX;

    /**
     * Theme for this panel. This is the single source of truth for UI chrome (grid/axes/legend/overlays).
     */
    private ChartTheme theme = ChartThemes.getDarkTheme();

    /**
     * Creates a new high-performance chart panel with an initial dataset.
     *
     * @param initialModel    The primary data model to display.
     * @param initialRenderer The renderer responsible for visualizing the data.
     */
    public ArberChartPanel(ChartModel initialModel, ChartRenderer initialRenderer) {
        this(initialModel, initialRenderer, System::nanoTime);
    }

    ArberChartPanel(ChartModel initialModel, ChartRenderer initialRenderer, LongSupplier animationClock) {
        this.animationClock = animationClock;
        zoomAnimationTimer = new Timer(16, _ -> advanceZoomAnimation(animationClock.getAsLong()));
        zoomAnimationTimer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                stopZoomAnimation(true);
                if (crosshair != null) crosshair.hideCrosshair();
            }
        });
        // Use absolute layout for precise overlay positioning
        setLayout(new BorderLayout());
        setOpaque(false);

        // Create a dedicated canvas for the chart + overlays.
        overlayCanvas = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!externalRenderSurface) renderChart((Graphics2D) graphics.create());
            }
        };
        overlayCanvas.setOpaque(false);
        add(overlayCanvas, BorderLayout.CENTER);

        // Accessibility defaults (framework-grade baseline)
        getAccessibleContext().setAccessibleName("Arber Chart View");
        getAccessibleContext().setAccessibleDescription(
                "An interactive chart component with zooming, panning, tooltips, and crosshair support."
        );

        // Default Grid
        this.gridLayer = new DefaultGridLayer();

        // Initialize UI Components
        initializeOverlays();
        initializeContextMenu();
        initializeInputController();

        // Ensure overlays pick up the current theme immediately.
        applyThemeToOverlays();

        // Add primary layer
        if (initialModel != null && initialRenderer != null) {
            setLayer(initialModel, initialRenderer);
        }
    }

    private static Color toAwt(ArberColor color) {
        return SwingAssets.toAwtColor(color);
    }

    // --- Layer Management (Multi-Series Support) ---

    /**
     * Adds a new data series to the chart.
     * <p>
     * The chart supports stacking multiple layers. Each layer consists of a data model and a renderer.
     * Layers are drawn in the order they are added (Painter's Algorithm).
     * </p>
     * <p>This method is safe to call on the EDT only; it triggers repainting.</p>
     *
     * @param model    The data model containing the series data.
     * @param renderer The renderer used to visualize this specific series.
     */
    public void setLayer(ChartModel model, ChartRenderer renderer) {
        if (model == null || renderer == null) return;

        // Set layer index for color differentiation in multi-layer charts
        if (renderer instanceof BaseRenderer) {
            ((BaseRenderer) renderer).setLayerIndex(layers.size());
            ((BaseRenderer) renderer).setTheme(theme);
        }

        ChartModelListener listener = () -> {
            cacheDirty = true;
            repaint();
        };
        if (modelListenersAttached) model.setChangeListener(listener);
        layers.add(new RenderLayer(nextLayerId(renderer), model, renderer, listener));

        // Auto-scale if it's the first layer or view is not set
        if (Double.isNaN(viewMinX)) {
            autoScale();
        }

        cacheDirty = true;
        repaint();
    }

    /**
     * Adds a pre-data layer that is rendered before the primary data layers.
     *
     * <p>This is used for predictive and analytical overlays that should appear beneath
     * the live data while still respecting the primary model's coordinate system.</p>
     *
     * @param model    The data model to use for alignment.
     * @param renderer The renderer used for the pre-data overlay.
     */
    public void setPreDataLayer(ChartModel model, ChartRenderer renderer) {
        if (model == null || renderer == null) return;
        ChartModelListener listener = () -> {
            cacheDirty = true;
            repaint();
        };
        if (modelListenersAttached) model.setChangeListener(listener);
        preDataLayers.add(new RenderLayer(nextLayerId(renderer), model, renderer, listener));
    }

    /**
     * Returns the primary model (first registered layer) or {@code null} if none exists.
     */
    public ChartModel getPrimaryModel() {
        return layers.isEmpty() ? null : layers.getFirst().model;
    }

    /**
     * Returns the primary renderer (first registered layer) or {@code null} if none exists.
     */
    public ChartRenderer getPrimaryRenderer() {
        return layers.isEmpty() ? null : layers.getFirst().renderer;
    }

    /**
     * Sets the coordinate transformer for non-linear plot mappings.
     */
    public ArberChartPanel setCoordinateTransformer(com.arbergashi.charts.api.CoordinateTransformer transformer) {
        this.coordinateTransformer = transformer;
        cacheDirty = true;
        repaint();
        return this;
    }

    /**
     * Returns the current latency tracker used by performance overlays.
     */
    public LatencyTracker getLatencyTracker() {
        return latencyTracker;
    }

    /**
     * Overrides the secondary Y-axis configuration.
     */
    public ArberChartPanel setYAxisSecondaryConfig(AxisConfig config) {
        this.yAxisSecondaryConfig = (config != null) ? config : new AxisConfig();
        cacheDirty = true;
        repaint();
        return this;
    }

    /**
     * Returns the most recent device-pixel density measurement.
     * Plot coordinates use the logical density returned by {@link PlotContext#getPixelsPerMillimeter()}.
     */
    public double getPhysicalPixelsPerMillimeter() {
        return physicalPixelsPerMm;
    }

    /**
     * Adds an overlay renderer that uses the PRIMARY model (the first one added).
     * <p>
     * This is useful for technical indicators (e.g., Moving Averages, Regression Lines) that
     * are derived from the main dataset but require a separate rendering pass.
     * </p>
     * <p>Overlay renderers share the primary model and are drawn as additional layers.</p>
     *
     * @param renderer The overlay renderer.
     */
    public void setOverlay(ChartRenderer renderer) {
        if (layers.isEmpty()) return;
        setLayer(layers.getFirst().model, renderer);
    }

    /**
     * Removes all layers and clears the chart.
     *
     * <p>Listeners are removed and the panel is repainted.</p>
     */
    public void clearLayers() {
        stopZoomAnimation(false);
        if (modelListenersAttached) {
            for (RenderLayer layer : layers) layer.model.removeChangeListener(layer.listener);
            for (RenderLayer layer : preDataLayers) layer.model.removeChangeListener(layer.listener);
        }
        layers.clear();
        preDataLayers.clear();
        layerVisibility.clear();
        cacheDirty = true;
        repaint();
    }

    /**
     * Enables or disables multi-color mode on renderers that support it.
     *
     * @param enabled true to enable multi-color rendering
     */
    public ArberChartPanel setMultiColorEnabled(boolean enabled) {
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof com.arbergashi.charts.render.BaseRenderer br) {
                br.setMultiColor(enabled);
            }
        }
        cacheDirty = true;
        repaint();
        return this;
    }

    private String nextLayerId(ChartRenderer renderer) {
        String base = renderer instanceof BaseRenderer br ? br.getId() : renderer.getClass().getSimpleName();
        String id = base;
        int suffix = 2;
        while (hasLayerId(id)) id = base + "#" + suffix++;
        return id;
    }

    private boolean hasLayerId(String id) {
        for (RenderLayer layer : layers) if (layer.id.equals(id)) return true;
        for (RenderLayer layer : preDataLayers) if (layer.id.equals(id)) return true;
        return false;
    }

    private void layoutMeasurementLabel() {
        if (overlayCanvas == null || freezeMeasurementLabel == null) return;
        Dimension size = freezeMeasurementLabel.getPreferredSize();
        int width = Math.min(Math.max(0, overlayCanvas.getWidth() - 16), size.width + 12);
        freezeMeasurementLabel.setBounds(Math.max(0, overlayCanvas.getWidth() - width - 8),
                6, width, size.height + 6);
    }

    // --- Initialization ---

    private void initializeOverlays() {
        javax.swing.ToolTipManager.sharedInstance().unregisterComponent(this);
        javax.swing.ToolTipManager.sharedInstance().unregisterComponent(overlayCanvas);
        // 1. Tooltip (topmost)
        tooltipOverlay = new TooltipOverlay();
        tooltipOverlay.setTheme(theme);
        overlayCanvas.add(tooltipOverlay);

        // 2. Crosshair
        crosshair = new HighPrecisionCrosshair();
        crosshair.setAnimationsEnabled(animationsEnabled);
        crosshair.setVisible(true);
        crosshair.setEnabled(true);
        overlayCanvas.add(crosshair);

        // 3. Legend (overlay by default; can be docked)
        // Provide a minimal data view so legend components can query model/renderers.
        legendContext = new LayerLegendContext() {
            @Override
            public List<Entry> getEntries() {
                java.util.List<Entry> entries = new java.util.ArrayList<>();
                for (RenderLayer layer : layers) {
                    if (layer.renderer instanceof BaseRenderer renderer) {
                        String name = layer.model.getName();
                        if (name == null || name.isBlank()) name = renderer.getName();
                        entries.add(new Entry(layer.id, name, layer.model, renderer));
                    }
                }
                return entries;
            }

            @Override
            public PlotContext getContext(Entry entry) {
                return getLayerContext(entry.renderer());
            }

            @Override
            public ChartModel getModel() {
                return layers.isEmpty() ? null : layers.getFirst().model;
            }

            @Override
            public java.util.List<BaseRenderer> getRenderers() {
                java.util.List<BaseRenderer> renderers = new java.util.ArrayList<>();
                for (RenderLayer layer : layers) {
                    if (layer.renderer instanceof BaseRenderer br) {
                        renderers.add(br);
                    }
                }
                return renderers;
            }
        };

        // Replace legacy legend overlay with the interactive legend.
        interactiveLegendOverlay = new InteractiveLegendOverlay(legendContext, theme);
        interactiveLegendOverlay.setVisibilityResolver(layerVisibility::isVisible);
        interactiveLegendOverlay.setLegendActionListener(createLegendActions());
        interactiveLegendOverlay.setVisible(legendVisible);
        overlayCanvas.add(interactiveLegendOverlay);

        // Z-order: tooltip on top
        overlayCanvas.setComponentZOrder(tooltipOverlay, 0);
        overlayCanvas.setComponentZOrder(interactiveLegendOverlay, 1);
        overlayCanvas.setComponentZOrder(crosshair, 2);
        freezeMeasurementLabel.setVisible(false);
        freezeMeasurementLabel.setOpaque(true);
        overlayCanvas.add(freezeMeasurementLabel);
        overlayCanvas.setComponentZOrder(freezeMeasurementLabel, 0);

        applyThemeToOverlays();
        applyLegendPlacement();
    }

    private LegendActionListener createLegendActions() {
        return new LegendActionListener() {
            @Override
            public void toggleSeries(String seriesId) {
                layerVisibility.toggle(seriesId);
                repaint();
            }

            @Override
            public void openSeriesSettings(String seriesId) {
                // Consumers can attach their own UI.
            }

            @Override
            public void soloSeries(String seriesId) {
                if (isSoloSeries(seriesId)) showAllSeries();
                else soloSeriesInternal(seriesId);
                repaint();
            }
        };
    }

    private void applyLegendPlacement() {
        if (interactiveLegendOverlay == null) return;

        if (!legendVisible) {
            if (dockedLegendPanel != null) dockedLegendPanel.setVisible(false);
            interactiveLegendOverlay.setVisible(false);
            return;
        }

        if (legendConfig.getPlacement() == LegendPlacement.DOCKED) {
            // create dock panel lazily
            if (dockedLegendPanel == null) {
                dockedLegendPanel = new DockedLegendPanel(legendContext, theme);
                dockedLegendPanel.setVisibilityResolver(layerVisibility::isVisible);
                dockedLegendPanel.setLegendActionListener(createLegendActions());
            }
            dockedLegendPanel.setVisible(true);

            // Hide overlay legend inside chart canvas.
            interactiveLegendOverlay.setVisible(false);

            // Ensure the dock panel reflects current side sizing policy.
            dockedLegendPanel.setDockSide(legendConfig.getDockSide());

            // Ensure dock panel is added to border layout if possible.
            ensureDockedLegendAdded();
        } else {
            // Overlay mode
            if (dockedLegendPanel != null) dockedLegendPanel.setVisible(false);
            interactiveLegendOverlay.setVisible(true);
            // If the config is still default, honor the ChartAssets preference.
            if (legendConfig.getOverlayPosition() == LegendPosition.TOP_LEFT) {
                String pref = com.arbergashi.charts.util.ChartAssets.getString("Chart.legend.position", "TOP_LEFT");
                LegendPosition parsed = LegendPosition.parse(pref, LegendPosition.TOP_LEFT);
                if (parsed != null) {
                    legendConfig.setOverlayPosition(parsed);
                }
            }
            // Configure overlay position via ChartAssets as the current overlay uses that property.
            // Keep it consistent with the config.
            interactiveLegendOverlay.setPosition(legendConfig.getOverlayPosition());
        }
    }

    private void ensureDockedLegendAdded() {
        if (dockedLegendPanel == null) return;

        // Ensure the dock panel reflects current side sizing policy.
        dockedLegendPanel.setDockSide(legendConfig.getDockSide());

        String constraint = switch (legendConfig.getDockSide()) {
            case LEFT -> BorderLayout.WEST;
            case RIGHT -> BorderLayout.EAST;
            case TOP -> BorderLayout.NORTH;
            case BOTTOM -> BorderLayout.SOUTH;
        };

        remove(dockedLegendPanel);
        add(dockedLegendPanel, constraint);
        revalidate();
        repaint();
    }

    private void applyThemeToOverlays() {
        if (crosshair != null) {
            crosshair.setTheme(theme);
        }
        if (tooltipOverlay != null) {
            tooltipOverlay.setTheme(theme);
        }
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setTheme(theme);
            interactiveLegendOverlay.setVisible(legendVisible && legendConfig.getPlacement() == LegendPlacement.OVERLAY);
        }
        if (dockedLegendPanel != null) {
            dockedLegendPanel.setTheme(theme);
            dockedLegendPanel.setVisible(legendVisible && legendConfig.getPlacement() == LegendPlacement.DOCKED);
        }
        if (freezeMeasurementLabel != null && theme != null) {
            freezeMeasurementLabel.setForeground(toAwt(theme.getForeground()));
            freezeMeasurementLabel.setBackground(toAwt(theme.getBackground()));
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // When LAF switches (FlatDark/FlatLight), re-apply theme to overlays.
        applyThemeToOverlays();
    }

    private void initializeContextMenu() {
        contextMenu = new JPopupMenu();
        
        JMenuItem resetItem = new JMenuItem(ChartI18N.getString("chart.menu.reset_zoom"));
        resetItem.addActionListener(_ -> resetZoom());
        contextMenu.add(resetItem);
        
        contextMenu.addSeparator();
        
        JMenu exportMenu = new JMenu(ChartI18N.getString("chart.export.title"));
        
        exportPngItem = new JMenuItem(ChartI18N.getString("chart.export.png"));
        exportPngItem.addActionListener(_ -> exportAs("png"));
        exportMenu.add(exportPngItem);
        
        exportSvgItem = new JMenuItem(ChartI18N.getString("chart.export.svg"));
        exportSvgItem.addActionListener(_ -> exportAs("svg"));
        exportMenu.add(exportSvgItem);
        
        exportPdfItem = new JMenuItem(ChartI18N.getString("chart.export.pdf"));
        exportPdfItem.addActionListener(_ -> exportAs("pdf"));
        exportMenu.add(exportPdfItem);
        
        contextMenu.add(exportMenu);
        setExportMenuState();
    }

    private void initializeInputController() {
        MouseAdapter controller = new MouseAdapter() {
            private Point lastDragPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getComponent() == interactiveLegendOverlay) return;
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    advanceZoomAnimation(animationClock.getAsLong());
                    stopZoomAnimation(false);
                    handleFreezeMeasurementClick(e);
                    lastDragPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), overlayCanvas);
                    
                    // Handle special clicks for renderers
                    for (RenderLayer layer : layers) {
                        if (layer.renderer instanceof SunburstRenderer sunburst) {
                            ArberPoint click = new ArberPoint(e.getX(), e.getY());
                            sunburst.handleClick(click, getOrBuildContext());
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                lastDragPoint = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDragPoint == null || !SwingUtilities.isLeftMouseButton(e)) return;

                Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), overlayCanvas);
                double dx = point.getX() - lastDragPoint.getX();
                double width = plotBoundsCache.getWidth();

                if (width > 0) {
                    PlotContext ctx = getOrBuildContext();
                    double rangeX = ctx.getMaxX() - ctx.getMinX();
                    // Sensitivity factor 1.0 = 1:1 movement
                    double shift = (ctx.isInvertedX() ? dx : -dx) / width * rangeX;

                    if (Double.isNaN(viewMinX)) {
                        viewMinX = ctx.getMinX();
                        viewMaxX = ctx.getMaxX();
                    }
                    viewMinX += shift;
                    viewMaxX += shift;

                    cacheDirty = true;
                    repaint();
                }
                lastDragPoint = point;
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getComponent() == interactiveLegendOverlay) return;
                double rotation = e.getPreciseWheelRotation();
                if (!Double.isFinite(rotation) || rotation == 0.0) return;
                long now = animationClock.getAsLong();
                advanceZoomAnimation(now);
                PlotContext ctx = getOrBuildContext();
                double plotWidth = ctx.getPlotBounds().getWidth();
                if (plotWidth <= 0) return;

                Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), overlayCanvas);
                double relativeX = Math.clamp((point.getX() - ctx.getPlotBounds().getX()) / plotWidth, 0.0, 1.0);
                if (ctx.isInvertedX()) relativeX = 1.0 - relativeX;

                // Accumulate every wheel impulse against the pending destination, including
                // fractional trackpad deltas. Coordinates stay in the viewport's base domain.
                double baseMin = zoomAnimationTimer.isRunning() ? animToMinX : ctx.getMinX();
                double baseMax = zoomAnimationTimer.isRunning() ? animToMaxX : ctx.getMaxX();
                double baseRange = baseMax - baseMin;
                double newRange = baseRange * Math.pow(1.1, rotation);
                double focus = Math.fma(ctx.getMaxX() - ctx.getMinX(), relativeX, ctx.getMinX());
                double targetMinX = focus - newRange * relativeX;
                double targetMaxX = focus + newRange * (1.0 - relativeX);
                if (!Double.isFinite(targetMinX) || !Double.isFinite(targetMaxX)
                        || !(targetMaxX > targetMinX)) return;

                if (animationsEnabled && isShowing()) {
                    startZoomAnimation(targetMinX, targetMaxX, now);
                } else {
                    stopZoomAnimation(false);
                    viewMinX = targetMinX;
                    viewMaxX = targetMaxX;
                    cacheDirty = true;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // CRITICAL FIX: Prevent flickering when mouse moves over child components (overlays)
                // Only hide if the mouse actually leaves the ArberChartPanel bounds
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ArberChartPanel.this);
                if (ArberChartPanel.this.contains(p)) {
                    return; // Still inside the panel (e.g. over crosshair)
                }

                hideTooltip();
                if (crosshair != null) crosshair.hideCrosshair();
                updateLegendFocus(ChartFocus.EMPTY);
            }
        };

        // Listen on the canvas, not the outer container (because docking adds siblings).
        overlayCanvas.addMouseListener(controller);
        overlayCanvas.addMouseMotionListener(controller);
        overlayCanvas.addMouseWheelListener(controller);

        // Forward events from overlays (Crosshair blocks events if visible!)
        if (crosshair != null) {
            crosshair.addMouseListener(controller);
            crosshair.addMouseMotionListener(controller);
            crosshair.addMouseWheelListener(controller);
        }

        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.addMouseListener(controller);
            interactiveLegendOverlay.addMouseMotionListener(controller);
            interactiveLegendOverlay.addMouseWheelListener(controller);
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                cacheDirty = true;
                repaint();
                if (crosshair != null) {
                    crosshair.setBounds(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
                }
            }
        });
    }
    
    private void showContextMenu(MouseEvent e) {
        if (contextMenu != null) {
            contextMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void handleFreezeMeasurementClick(MouseEvent e) {
        if (!frozen || !SwingUtilities.isLeftMouseButton(e)) return;

        PlotContext ctx = getOrBuildContext();
        if (ctx == null) return;

        Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), overlayCanvas);
        if (!ctx.getPlotBounds().contains(new ArberPoint(point.x, point.y))) return;
        ctx.mapToData(point.x, point.y, freezeMeasureScratch);
        double dataX = freezeMeasureScratch[0];

        if (!freezeMeasureArmed) {
            freezeMeasureArmed = true;
            freezeMeasureStartX = dataX;
            return;
        }

        freezeMeasureArmed = false;
        double deltaX = Math.abs(dataX - freezeMeasureStartX);
        if (deltaX <= 0.0) return;

        double bpm = 60.0 / deltaX;
        double low = ChartAssets.getUIFloat("Chart.medical.bpm.low", ChartAssets.getFloat("Chart.medical.bpm.low", 40f));
        double high = ChartAssets.getUIFloat("Chart.medical.bpm.high", ChartAssets.getFloat("Chart.medical.bpm.high", 160f));
        boolean alarmEnabled = ChartAssets.getUIBoolean("Chart.medical.bpm.alarmEnabled", false)
                || ChartAssets.getBoolean("Chart.medical.bpm.alarmEnabled", false);
        boolean alarm = alarmEnabled && (bpm < low || bpm > high);

        String text = String.format(getLocale(), "BPM: %.1f%s", bpm, alarm ? " !" : "");
        freezeMeasurementLabel.setText(text);
        freezeMeasurementLabel.setVisible(true);
        layoutMeasurementLabel();
        repaint();
    }

    // --- Animation Support ---

    private void startZoomAnimation(double targetMinX, double targetMaxX, long now) {
        PlotContext context = getOrBuildContext();
        animFromMinX = context.getMinX();
        animFromMaxX = context.getMaxX();
        animToMinX = targetMinX;
        animToMaxX = targetMaxX;
        zoomAnimationStart = now;
        // Reuse one coalescing timer; retargeting never queues another animation.
        if (!zoomAnimationTimer.isRunning()) zoomAnimationTimer.start();
    }

    void advanceZoomAnimation(long now) {
        if (!zoomAnimationTimer.isRunning()) return;
        double progress = Math.clamp((now - zoomAnimationStart) / (double) ZOOM_ANIMATION_DURATION_NANOS, 0.0, 1.0);
        // Ease out starts responding immediately even during a stream of wheel events.
        double remaining = 1.0 - progress;
        double eased = 1.0 - remaining * remaining * remaining;
        viewMinX = AnimationUtils.lerp(animFromMinX, animToMinX, eased);
        viewMaxX = AnimationUtils.lerp(animFromMaxX, animToMaxX, eased);
        if (progress >= 1.0) {
            zoomAnimationTimer.stop();
            viewMinX = animToMinX;
            viewMaxX = animToMaxX;
        }
        cacheDirty = true;
        repaint();
    }

    private void stopZoomAnimation(boolean finish) {
        if (!zoomAnimationTimer.isRunning()) return;
        zoomAnimationTimer.stop();
        if (finish) {
            viewMinX = animToMinX;
            viewMaxX = animToMaxX;
            cacheDirty = true;
            repaint();
        }
    }

    // --- Rendering Loop (The Engine) ---
    
    @Override
    public boolean isOptimizedDrawingEnabled() {
        // Essential for overlapping components (overlays) to repaint correctly
        return false;
    }
    
    @Override
    public void doLayout() {
        super.doLayout();

        if (overlayCanvas == null) return;
        if (contextWidth != overlayCanvas.getWidth() || contextHeight != overlayCanvas.getHeight()) cacheDirty = true;
        layoutMeasurementLabel();

        if (crosshair != null) {
            crosshair.setBounds(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        }
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setBounds(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        }
    }

    /**
     * Render order: background fill -&gt; grid layer -&gt; data layers -&gt; overlays.
     */
    @Override/**
 * @since 1.5.0
 */
    protected void paintComponent(Graphics g) {
        if (externalRenderSurface) return;
        super.paintComponent(g);
        if (overlayCanvas.getWidth() <= 0 || overlayCanvas.getHeight() <= 0) doLayout();
        if (theme != null) {
            g.setColor(toAwt(theme.getBackground()));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Renders chart content onto an external surface using this panel's coordinate system.
     * A docked legend's offset is applied to the chart canvas; child overlays are painted by Swing.
     */
    public void renderExternal(Graphics2D g2) {
        if (g2 == null) return;
        Graphics2D copy = (Graphics2D) g2.create();
        copy.translate(overlayCanvas.getX(), overlayCanvas.getY());
        renderChart(copy);
    }

    private void renderChart(Graphics2D g2) {
        long frameStart = System.nanoTime();
        try {
            if (layers.isEmpty()) {
                drawEmptyState(g2);
                return;
            }

            // 1. Setup Graphics (Anti-aliasing, etc.)
            // FORCE High Quality Hints (Gold Standard for Scientific Charts)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            if (renderHints != null) {
                SwingRenderHints.apply(renderHints, g2);
            }
            
            physicalPixelsPerMm = PhysicalScaleResolver.pixelsPerMillimeter(g2);
            double density = PhysicalScaleResolver.logicalPixelsPerMillimeter(g2);
            if (density != logicalPixelsPerMm) {
                logicalPixelsPerMm = density;
                cacheDirty = true;
            }
            // 2. Build Context (View Matrix)
            PlotContext ctx = getOrBuildContext();
            ArberCanvas canvas = tryCreateCanvasAdapter(g2);

            // 2.5 Fill panel background from theme (ensures consistent theming for transparent renderers)
            ChartTheme bgTheme = ctx.getTheme() != null ? ctx.getTheme() : theme;
            if (bgTheme != null && bgTheme.getBackground() != null) {
                g2.setColor(toAwt(bgTheme.getBackground()));
                g2.fillRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
            }

            
            // 3. Render Grid (Background)
            boolean showGrid = (xAxisConfig == null || xAxisConfig.isShowGrid())
                    || (yAxisConfig == null || yAxisConfig.isShowGrid());
            if (gridLayer != null && showGrid && canvas != null) {
                gridLayer.renderGrid(canvas, ctx);
            }

            // 3.5 Render Pre-Data Layers (predictive/analysis overlays below data)
            for (RenderLayer layer : preDataLayers) {
                PlotContext layerContext = getLayerContext(layer.renderer);
                if (layer.renderer instanceof BaseRenderer br) {
                    if (!layerVisibility.isVisible(layer.id)) {
                        continue;
                    }
                }
                if (layer.model == null || layer.model.isEmpty()) {
                    if (layer.renderer != null && layer.renderer.supportsEmptyState()) {
                        Shape oldClip = g2.getClip();
                        g2.clip(SwingAssets.toAwtRect(ctx.getPlotBounds()));
                        if (canvas != null) {
                            layer.renderer.renderEmptyState(canvas, layer.model, layerContext);
                        }
                        g2.setClip(oldClip);
                    }
                    continue;
                }
                Shape oldClip = g2.getClip();
                Composite oldComposite = g2.getComposite();
                g2.clip(SwingAssets.toAwtRect(ctx.getPlotBounds()));
                if (layer.renderer instanceof BaseRenderer br && layerVisibility.isDimmed(layer.id)) {
                    g2.setComposite(getSoloComposite());
                }
                if (layer.renderer instanceof SpatialChunkRenderer spatialRenderer) {
                    renderSpatialLayer(spatialRenderer, layer.model, layerContext, g2, layers.indexOf(layer));
                } else {
                    if (canvas != null) {
                        layer.renderer.render(canvas, layer.model, layerContext);
                    }
                }
                g2.setComposite(oldComposite);
                g2.setClip(oldClip);
            }

            // 4. Render Layers (Z-Order: First added is bottom)
            // We iterate normally so first layer is drawn first, others on top.
            for (RenderLayer layer : layers) {
                PlotContext layerContext = getLayerContext(layer.renderer);
                if (layer.renderer instanceof BaseRenderer br) {
                    if (!layerVisibility.isVisible(layer.id)) {
                        continue;
                    }
                }
                if (layer.model == null || layer.model.isEmpty()) {
                    if (layer.renderer != null && layer.renderer.supportsEmptyState()) {
                        Shape oldClip = g2.getClip();
                        g2.clip(SwingAssets.toAwtRect(ctx.getPlotBounds()));
                        if (canvas != null) {
                            layer.renderer.renderEmptyState(canvas, layer.model, layerContext);
                        }
                        g2.setClip(oldClip);
                    }
                    continue;
                }
                // Clip to plot bounds to prevent drawing outside
                Shape oldClip = g2.getClip();
                Composite oldComposite = g2.getComposite();
                g2.clip(SwingAssets.toAwtRect(ctx.getPlotBounds()));
                if (layer.renderer instanceof BaseRenderer br && layerVisibility.isDimmed(layer.id)) {
                    g2.setComposite(getSoloComposite());
                }
                if (layer.renderer instanceof SpatialChunkRenderer spatialRenderer) {
                    renderSpatialLayer(spatialRenderer, layer.model, layerContext, g2, layers.indexOf(layer));
                } else {
                    if (canvas != null) {
                        layer.renderer.render(canvas, layer.model, layerContext);
                    }
                }
                g2.setComposite(oldComposite);
                g2.setClip(oldClip);
            }

            // 5. Render Axes (Foreground)
            drawAxisLabels(g2, ctx);

            // 6. Render Legend (Overlay)
            // Legend is rendered by InteractiveLegendOverlay (single source of truth).

        } finally {
            latencyTracker.record(System.nanoTime() - frameStart);
            long frameMicros = (System.nanoTime() - frameStart) / 1_000L;
            if (frameMicros > Integer.MAX_VALUE) frameMicros = Integer.MAX_VALUE;
            com.arbergashi.charts.util.ChartAssets.setProperty("Chart.render.lastFrameMicros",
                    Long.toString(frameMicros));
            g2.dispose();
        }
    }

    // --- Optimized Axis Rendering (Zero-Allocation) ---

    private void renderSpatialLayer(SpatialChunkRenderer renderer, ChartModel model, PlotContext ctx, Graphics2D g2, int seriesIndex) {
        if (renderer == null || model == null) {
            return;
        }
        SpatialPathBatchBuilder builder = (renderer instanceof AbstractSpatialLayer asl)
                ? asl.getSpatialPathBatchBuilder()
                : spatialPathBatchBuilder;
        ChartTheme t = ctx.getTheme() != null ? ctx.getTheme() : theme;
        Color series = (t != null) ? toAwt(t.getSeriesColor(seriesIndex)) : g2.getColor();
        long styleKey = SpatialStyleDescriptor.pack(series.getRGB(), 1.0f, 0, 0);
        builder.setStyleKey(styleKey);
        spatialBatchConsumer
                .setBuilder(builder)
                .setGraphics(g2)
                .setColor(series)
                .setStroke(StrokeCache.get(1.0f))
                .reset();
        renderer.renderSpatial(model, ctx, spatialBatchConsumer);
        spatialBatchConsumer.flush();
    }

    private Font axisFont() {
        // Use Inter font from ChartFonts for professional axis labels (ZERO-GC cached)
        com.arbergashi.charts.platform.swing.util.ChartFonts.initialize();

        // Professional axis font sizing with readable baseline for production dashboards.
        float baseAxisSize = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.axis.label.baseSize", 11.5f);
        if (baseAxisSize < 8.0f || baseAxisSize > 22.0f) {
            baseAxisSize = 11.5f;
        }
        float axisScale = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.axis.label.fontScale", 1.08f);
        if (axisScale < 0.3f || axisScale > 3.0f) axisScale = 1.0f;
        float axisSize = ChartScale.font(baseAxisSize * axisScale);
        axisSize = Math.max(10.0f, axisSize);

        // Keep axis labels at least as large as HighPrecisionCrosshair labels.
        float crosshairScale = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.crosshair.label.fontScale", 1.08f);
        if (crosshairScale < 0.3f || crosshairScale > 3.0f) crosshairScale = 1.0f;
        float crosshairBase = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.crosshair.label.baseSize", 11.5f);
        if (crosshairBase < 8.0f || crosshairBase > 22.0f) crosshairBase = 11.5f;
        float crosshairSize = ChartScale.font(crosshairBase * crosshairScale);
        axisSize = Math.max(axisSize, crosshairSize);

        return com.arbergashi.charts.platform.swing.util.ChartFonts.getAxisFont(axisSize);
    }

    private double measuredAxisWidth(FontMetrics metrics, AxisConfig config, double min, double max) {
        NiceScale scale = measurementScale.setRange(min, max);
        scale.setMaxTicks(Math.max(2, Math.min(1000, config.getRequestedTickCount())));
        double[] ticks = scale.getTicks();
        double width = 0;
        for (int i = 0; i < scale.getTickCount(); i++) {
            if (!visibleTick(ticks[i], min, max)) continue;
            width = Math.max(width, metrics.stringWidth(config.getFormattedValue(ticks[i], getLocale(), scale.getTickSpacing())));
        }
        return width;
    }

    private static boolean visibleTick(double value, double min, double max) {
        double epsilon = Math.abs(max - min) * 1e-9;
        return Double.isFinite(value) && value >= min - epsilon && value <= max + epsilon;
    }

    void drawAxisLabels(Graphics2D g2, PlotContext ctx) {
        if (!axisLabelsVisible) return;
        ChartTheme activeTheme = ctx.getTheme() != null ? ctx.getTheme() : theme;
        g2.setFont(axisFont());
        g2.setColor(toAwt(activeTheme.getAxisLabelColor()));
        drawYAxis(g2, ctx, yAxisConfig, cachedScaleY, false);
        if (hasSecondaryAxis()) {
            for (RenderLayer layer : layers) {
                if (layer.renderer instanceof BaseRenderer br && br.getYAxisRole() == AxisRole.SECONDARY) {
                    drawYAxis(g2, getLayerContext(layer.renderer), yAxisSecondaryConfig, cachedScaleSecondaryY, true);
                    break;
                }
            }
        }

        cachedScaleX.setRange(ctx.getMinX(), ctx.getMaxX());
        cachedScaleX.setMaxTicks(ctx.getRequestedTickCountX());
        TextAnchor anchor = xAxisConfig.getLabelAnchorOrDefault(TextAnchor.TOP_CENTER);
        double[] ticks = cachedScaleX.getTicks();
        double y = ctx.getPlotBounds().getY() + ctx.getPlotBounds().getHeight() + ChartScale.scale(14);
        FontMetrics metrics = g2.getFontMetrics();
        double previousX = Double.NaN;
        double previousWidth = 0;
        for (int i = 0; i < cachedScaleX.getTickCount(); i++) {
            double value = ticks[i];
            if (!visibleTick(value, ctx.getMinX(), ctx.getMaxX())) continue;
            ctx.mapToPixel(value, ctx.getMinY(), axisMapBuffer);
            if (!Double.isFinite(axisMapBuffer[0])) continue;
            String label = xAxisConfig.getFormattedValue(value, getLocale(), cachedScaleX.getTickSpacing());
            double width = metrics.stringWidth(label);
            if (Double.isFinite(previousX) && Math.abs(axisMapBuffer[0] - previousX)
                    < (previousWidth + width) * 0.5 + ChartScale.scale(6)) continue;
            drawAnchoredString(g2, label, axisMapBuffer[0], y, anchor);
            previousX = axisMapBuffer[0];
            previousWidth = width;
        }
    }

    private void drawYAxis(Graphics2D g2, PlotContext ctx, AxisConfig config, NiceScale scale, boolean secondary) {
        scale.setRange(ctx.getMinY(), ctx.getMaxY());
        scale.setMaxTicks(ctx.getRequestedTickCountY());
        TextAnchor anchor = config.getLabelAnchorOrDefault(secondary ? TextAnchor.MIDDLE_LEFT : TextAnchor.MIDDLE_RIGHT);
        double[] ticks = scale.getTicks();
        double x = secondary ? ctx.getPlotBounds().getMaxX() + ChartScale.scale(8)
                : ctx.getPlotBounds().getX() - ChartScale.scale(8);
        double previousY = Double.NaN;
        double separation = g2.getFontMetrics().getHeight() + ChartScale.scale(2);
        for (int i = 0; i < scale.getTickCount(); i++) {
            double value = ticks[i];
            if (!visibleTick(value, ctx.getMinY(), ctx.getMaxY())) continue;
            ctx.mapToPixel(ctx.getMinX(), value, axisMapBuffer);
            if (!Double.isFinite(axisMapBuffer[1]) || (Double.isFinite(previousY)
                    && Math.abs(axisMapBuffer[1] - previousY) < separation)) continue;
            String label = config.getFormattedValue(value, getLocale(), scale.getTickSpacing());
            drawAnchoredString(g2, label, x, axisMapBuffer[1], anchor);
            previousY = axisMapBuffer[1];
        }
    }

    private void drawAnchoredString(Graphics2D g2, String text, double x, double y, TextAnchor anchor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        TextAnchor a = (anchor != null) ? anchor : TextAnchor.TOP_LEFT;
        FontMetrics fm = g2.getFontMetrics();
        double width = fm.stringWidth(text);
        double ascent = fm.getAscent();
        double descent = fm.getDescent();

        double tx;
        double ty;

        switch (a) {
            case TOP_CENTER, CENTER, BOTTOM_CENTER, BASELINE_CENTER -> tx = x - width / 2.0;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT, BASELINE_RIGHT -> tx = x - width;
            default -> tx = x;
        }

        switch (a) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> ty = y + ascent;
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> ty = y + (ascent - descent) / 2.0;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> ty = y - descent;
            case BASELINE_LEFT, BASELINE_CENTER, BASELINE_RIGHT -> ty = y;
            default -> ty = y;
        }

        tx = Math.max(0, Math.min(tx, overlayCanvas.getWidth() - width));
        ty = Math.max(ascent, Math.min(ty, overlayCanvas.getHeight() - descent));
        g2.drawString(text, (float) tx, (float) ty);
    }

    // --- Interaction & Hit Testing ---

    private void handleMouseMove(MouseEvent e) {
        if (overlayCanvas == null) return;
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), overlayCanvas);
        handleMouseMove(p);
    }

    private void handleMouseMove(Point p) {
        if (crosshairEnabled && crosshair != null) {
            setCrosshair(p);
        }
        if (!tooltipsEnabled || tooltipOverlay == null) {
            return;
        }
        if (lastSnapLayer != null && lastSnapIndex >= 0 && lastSnapPoint != null) {
            showTooltipAtSnap();
        } else {
            hideTooltip();
        }
    }

    private void showTooltipAtSnap() {
        if (tooltipOverlay == null || lastSnapLayer == null || lastSnapIndex < 0 || lastSnapPoint == null) {
            hideTooltip();
            return;
        }

        String text = lastSnapLayer.renderer.getTooltipText(lastSnapIndex, lastSnapLayer.model);
        if (text == null) {
            double xVal = lastSnapLayer.model.getX(lastSnapIndex);
            double yVal = lastSnapLayer.model.getY(lastSnapIndex);
            String xLabel = formatAxisValueX(xVal);
            String yLabel = formatAxisValueY(yVal, lastSnapLayer);
            text = "X: " + xLabel + "  Y: " + yLabel;
        }

        java.util.List<String> lines = splitTooltipText(text);
        if (lines.isEmpty()) {
            hideTooltip();
            return;
        }

        com.arbergashi.charts.api.types.ArberColor legendColor = null;
        if (lastSnapLayer.renderer instanceof BaseRenderer br) {
            legendColor = br.getLegendColor(lastSnapLayer.model);
        }
        java.awt.Color accent = SwingAssets.toAwtColor(legendColor);
        if (accent == null && theme != null) {
            accent = SwingAssets.toAwtColor(theme.getAccentColor());
        }

        tooltipOverlay.setLines(lines);
        tooltipOverlay.setAccentColor(accent);
        Dimension pref = tooltipOverlay.getPreferredSize();
        Point tip = resolveTooltipPosition(lastSnapPoint, pref);

        Rectangle legendBounds = (interactiveLegendOverlay != null && interactiveLegendOverlay.isVisible())
                ? interactiveLegendOverlay.getLegendBounds()
                : null;
        if (legendBounds != null) {
            Rectangle tipRect = new Rectangle(tip.x, tip.y, pref.width, pref.height);
            if (tipRect.intersects(legendBounds)) {
                hideTooltip();
                return;
            }
        }

        Rectangle oldBounds = tooltipOverlay.getBounds();
        tooltipOverlay.setBounds(tip.x, tip.y, pref.width, pref.height);
        tooltipOverlay.setVisible(true);
        Rectangle newBounds = tooltipOverlay.getBounds();
        Rectangle repaint = oldBounds.isEmpty() ? newBounds : oldBounds.union(newBounds);
        overlayCanvas.repaint(repaint.x - 5, repaint.y - 5, repaint.width + 10, repaint.height + 10);
    }

    private void hideTooltip() {
        if (tooltipOverlay == null) return;
        if (!tooltipOverlay.isVisible()) return;
        Rectangle oldBounds = tooltipOverlay.getBounds();
        tooltipOverlay.setVisible(false);
        if (oldBounds.width > 0 && oldBounds.height > 0) {
            repaint(oldBounds.x - 2, oldBounds.y - 2, oldBounds.width + 4, oldBounds.height + 4);
        }
    }

    private static java.util.List<String> splitTooltipText(String text) {
        if (text == null || text.isBlank()) return java.util.List.of();
        String cleaned = text.trim()
                .replace("<html>", "")
                .replace("</html>", "")
                .replace("<br/>", "\n")
                .replace("<br>", "\n");
        cleaned = cleaned.replaceAll("<[^>]+>", "");
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String line : cleaned.split("\n")) {
            String value = line.trim();
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
        return lines;
    }

    private Point resolveTooltipPosition(Point anchor, Dimension size) {
        int w = size.width;
        int h = size.height;
        int maxX = overlayCanvas.getWidth();
        int maxY = overlayCanvas.getHeight();

        int x = anchor.x;
        int y = anchor.y;

        if (x + w > maxX) {
            x = Math.max(0, anchor.x - w);
        }
        if (y + h > maxY) {
            y = Math.max(0, anchor.y - h);
        }
        return new Point(x, y);
    }


    /**
     * Generic fallback hit-testing for renderers that don't implement getPointAt.
     */
    private Optional<Integer> findNearestPoint(Point p, ChartModel model, PlotContext ctx) {
        double[] xData = model.getXData();
        if (xData == null || xData.length == 0) return Optional.empty();

        int count = model.getPointCount();
        if (count <= 0) return Optional.empty();

        // Guard against inconsistent models (count may be > returned array length)
        count = Math.min(count, xData.length);

        // Map pixel X back to data X
        double[] dataPos = new double[2];
        ctx.mapToData(p.x, p.y, dataPos);
        double targetX = dataPos[0];

        int bestIdx = -1;
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            double dist = Math.abs(xData[i] - targetX);
            if (dist < minDist) {
                minDist = dist;
                bestIdx = i;
            }
        }

        if (bestIdx != -1) {
            double[] pixelPos = new double[2];
            ctx.mapToPixel(xData[bestIdx], model.getY(bestIdx), pixelPos);
            double pixelDist = Math.abs(pixelPos[0] - p.x);

            if (pixelDist < ChartScale.scale(20)) {
                return Optional.of(bestIdx);
            }
        }
        return Optional.empty();
    }

    private void setCrosshair(Point position) {
        if (crosshair == null) return;
        PlotContext context = getOrBuildContext();
        if (!context.getPlotBounds().contains(new ArberPoint(position.x, position.y))) {
            crosshair.hideCrosshair();
            lastCrosshairPoint = null;
            lastSnapLayer = null;
            lastSnapIndex = -1;
            lastSnapPoint = null;
            updateLegendFocus(ChartFocus.EMPTY);
            return;
        }

        boolean snapEnabled = com.arbergashi.charts.util.ChartAssets.getBoolean("Chart.crosshair.snap", true);
        double snapThreshold = ChartScale.scale(20);
        double snapThresholdSq = snapThreshold * snapThreshold;

        boolean snapped = false;
        double bestDistSq = Double.MAX_VALUE;
        int bestX = position.x;
        int bestY = position.y;
        double bestDataX = Double.NaN;
        double bestDataY = Double.NaN;
        RenderLayer bestLayer = null;
        int bestIndex = -1;

        if (snapEnabled) {
            for (RenderLayer layer : layers) {
                if (layer.renderer instanceof BaseRenderer br) {
                    if (!layerVisibility.isVisible(layer.id)) {
                        continue;
                    }
                }

                PlotContext layerContext = getLayerContext(layer.renderer);
                Optional<Integer> idxOpt = layer.renderer.getPointAt(new ArberPoint(position.x, position.y), layer.model, layerContext);
                if (idxOpt.isEmpty() && layer.model.getPointCount() > 0) {
                    idxOpt = findNearestPoint(position, layer.model, layerContext);
                }

                if (idxOpt.isPresent()) {
                    int idx = idxOpt.get();
                    if (!isIndexValid(layer.model, idx)) {
                        continue;
                    }
                    double xVal = layer.model.getX(idx);
                    double yVal = layer.model.getY(idx);
                    double[] pix = axisMapBuffer;
                    layerContext.mapToPixel(xVal, yVal, pix);
                    double dx = pix[0] - position.x;
                    double dy = pix[1] - position.y;
                    double distSq = dx * dx + dy * dy;
                    if (distSq < bestDistSq && distSq <= snapThresholdSq) {
                        bestDistSq = distSq;
                        bestX = (int) Math.round(pix[0]);
                        bestY = (int) Math.round(pix[1]);
                        bestDataX = xVal;
                        bestDataY = yVal;
                        bestLayer = layer;
                        bestIndex = idx;
                        snapped = true;
                    }
                }
            }
        }

        if (snapped) {
            String labelX = formatAxisValueX(bestDataX);
            String labelY = formatAxisValueY(bestDataY, bestLayer);
            crosshair.updatePosition(bestX, bestY, labelX, labelY);
            lastCrosshairPoint = new Point(bestX, bestY);
            lastSnapLayer = bestLayer;
            lastSnapIndex = bestIndex;
            lastSnapPoint = new Point(bestX, bestY);
            updateLegendFocus(new ChartFocus(new ArberPoint(bestX, bestY), bestDataX, bestDataY,
                    bestIndex, bestLayer.model, null, Map.of()));
            return;
        }

        // Map pixel to data
        context.mapToData(position.x, position.y, axisMapBuffer);
        String labelX = formatAxisValueX(axisMapBuffer[0]);
        String labelY = formatAxisValueY(axisMapBuffer[1]);

        crosshair.updatePosition(position.x, position.y, labelX, labelY);
        lastCrosshairPoint = new Point(position.x, position.y);
        lastSnapLayer = null;
        lastSnapIndex = -1;
        lastSnapPoint = null;
        updateLegendFocus(new ChartFocus(new ArberPoint(position.x, position.y), axisMapBuffer[0], axisMapBuffer[1],
                -1, null, null, Map.of()));
    }

    private void updateLegendFocus(ChartFocus focus) {
        if (interactiveLegendOverlay != null) interactiveLegendOverlay.setFocus(focus);
        if (dockedLegendPanel != null) dockedLegendPanel.setFocus(focus);
    }

    private static boolean isIndexValid(com.arbergashi.charts.model.ChartModel model, int idx) {
        if (model == null) return false;
        int count = model.getPointCount();
        return idx >= 0 && idx < count;
    }

    private ArberCanvas tryCreateCanvasAdapter(Graphics2D g2) {
        try {
            Class<?> cls = Class.forName("com.arbergashi.charts.bridge.swing.AwtCanvasAdapter");
            Constructor<?> ctor = cls.getConstructor(Graphics2D.class);
            return (ArberCanvas) ctor.newInstance(g2);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean supportsArberCanvas(ChartRenderer renderer) {
        if (!(renderer instanceof BaseRenderer)) return false;
        return canvasSupportCache.computeIfAbsent(renderer.getClass(), cls -> {
            try {
                Method m = cls.getDeclaredMethod("drawData", ArberCanvas.class, ChartModel.class, PlotContext.class);
                return m.getDeclaringClass() != BaseRenderer.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        });
    }

    // --- Context & Bounds ---

    private PlotContext getOrBuildContext() {
        if (overlayCanvas.getWidth() <= 0 && getWidth() > 0) doLayout();
        long combinedStamp = 0;
        for (RenderLayer layer : layers) combinedStamp += layer.model.getUpdateStamp();
        int width = overlayCanvas.getWidth();
        int height = overlayCanvas.getHeight();
        if (!cacheDirty && contextCache != null && combinedStamp == lastModelStamp
                && width == contextWidth && height == contextHeight) return contextCache;

        if (!Double.isFinite(viewMinX) || !Double.isFinite(viewMaxX)) autoScale();
        Rectangle2D.Double plotBounds = getCalculatedPlotBounds();
        applyAxisOverridesInternal(plotBounds);
        contextCache = createContext(plotBounds, viewMinY, viewMaxY, yAxisConfig);
        secondaryContextCache = null;
        contextWidth = width;
        contextHeight = height;
        lastModelStamp = combinedStamp;
        cacheDirty = false;
        return contextCache;
    }

    private PlotContext createContext(Rectangle2D plotBounds, double minY, double maxY, AxisConfig yConfig) {
        com.arbergashi.charts.core.geometry.ArberRect bounds = new com.arbergashi.charts.core.geometry.ArberRect(
                plotBounds.getX(), plotBounds.getY(), plotBounds.getWidth(), plotBounds.getHeight());
        PlotContext base = new DefaultPlotContext(bounds, viewMinX, viewMaxX, minY, maxY, false,
                xAxisConfig.isInverted(), yConfig.isInverted(), NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR,
                theme, renderHints, gapModel, animationsEnabled ? animationProfile : AnimationProfile.TACTICAL)
                .setRequestedTickCountX(Math.max(2, Math.min(1000, xAxisConfig.getRequestedTickCount())))
                .setRequestedTickCountY(Math.max(2, Math.min(1000, yConfig.getRequestedTickCount())));
        return new HostPlotContext(base, coordinateTransformer, xAxisConfig.isShowGrid(), yConfig.isShowGrid(),
                logicalPixelsPerMm);
    }

    private PlotContext getLayerContext(ChartRenderer renderer) {
        PlotContext primary = getOrBuildContext();
        if (!(renderer instanceof BaseRenderer br) || br.getYAxisRole() != AxisRole.SECONDARY) return primary;
        if (secondaryContextCache == null) {
            double[] range = secondaryRange();
            Double units = yAxisSecondaryConfig.getResolvedUnitsPerPixel(logicalPixelsPerMm);
            if (units != null && Double.isFinite(units) && units > 0.0 && !yAxisSecondaryConfig.isFixedRange()) {
                double center = (range[0] + range[1]) * 0.5;
                double extent = primary.getPlotBounds().height() * units * 0.5;
                range[0] = center - extent;
                range[1] = center + extent;
            }
            secondaryContextCache = createContext(plotBoundsCache, range[0], range[1], yAxisSecondaryConfig);
        }
        return secondaryContextCache;
    }

    private double[] secondaryRange() {
        if (yAxisSecondaryConfig.isFixedRange()) {
            secondaryRangeBuffer[0] = yAxisSecondaryConfig.getFixedMin();
            secondaryRangeBuffer[1] = yAxisSecondaryConfig.getFixedMax();
            return secondaryRangeBuffer;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (RenderLayer layer : layers) {
            if (!(layer.renderer instanceof BaseRenderer br) || br.getYAxisRole() != AxisRole.SECONDARY) continue;
            double[] preferred = layer.renderer.getPreferredYRange(layer.model);
            if (validRange(preferred)) {
                min = Math.min(min, preferred[0]);
                max = Math.max(max, preferred[1]);
            } else {
                for (int i = 0; i < layer.model.getPointCount(); i++) {
                    double y = layer.model.getY(i);
                    if (!Double.isFinite(y)) continue;
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                }
            }
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            secondaryRangeBuffer[0] = 0;
            secondaryRangeBuffer[1] = 1;
            return secondaryRangeBuffer;
        }
        double padding = Math.max(max - min, Math.ulp(Math.max(Math.abs(min), Math.abs(max)))) * 0.05;
        if (max == min) padding = Math.max(0.05, Math.abs(min) * 0.05);
        secondaryRangeBuffer[0] = min - padding;
        secondaryRangeBuffer[1] = max + padding;
        return secondaryRangeBuffer;
    }

    private static boolean validRange(double[] range) {
        return range != null && range.length >= 2 && Double.isFinite(range[0]) && Double.isFinite(range[1]);
    }

    private boolean hasSecondaryAxis() {
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer br && br.getYAxisRole() == AxisRole.SECONDARY) return true;
        }
        return false;
    }

    private void autoScale() {
        if (layers.isEmpty()) {
            viewMinX = viewMinY = 0;
            viewMaxX = viewMaxY = 1;
            return;
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean hasData = false;

        for (RenderLayer layer : layers) {
            if (layer.model.isEmpty()) continue;

            double[] x = layer.model.getXData();
            double[] y = layer.model.getYData();
            double[] xOverride = layer.renderer.getPreferredXRange(layer.model);
            boolean secondary = layer.renderer instanceof BaseRenderer br && br.getYAxisRole() == AxisRole.SECONDARY;
            double[] yOverride = (layer.renderer != null) ? layer.renderer.getPreferredYRange(layer.model) : null;
            boolean hasOverride = yOverride != null && yOverride.length >= 2
                    && Double.isFinite(yOverride[0]) && Double.isFinite(yOverride[1]);
            if (x == null || x.length == 0 || (!hasOverride && (y == null || y.length == 0))) {
                continue;
            }

            hasData = true;

            int count = layer.model.getPointCount();
            count = Math.min(count, x.length);
            if (count <= 0) continue;

            for (int i = 0; i < count; i++) {
                if (!validRange(xOverride)) {
                    if (x[i] < minX) minX = x[i];
                    if (x[i] > maxX) maxX = x[i];
                }
                if (!secondary && !hasOverride && y != null && i < y.length) {
                    if (y[i] < minY) minY = y[i];
                    if (y[i] > maxY) maxY = y[i];
                }
            }

            if (validRange(xOverride)) {
                minX = Math.min(minX, xOverride[0]);
                maxX = Math.max(maxX, xOverride[1]);
            }
            if (!secondary && hasOverride) {
                if (yOverride[0] < minY) minY = yOverride[0];
                if (yOverride[1] > maxY) maxY = yOverride[1];
            }
        }

        if (!hasData) {
            viewMinX = 0;
            viewMaxX = 10;
            viewMinY = 0;
            viewMaxY = 10;
            return;
        }

        if (minY == Double.MAX_VALUE || maxY == -Double.MAX_VALUE) { minY = 0; maxY = 1; }
        if (minX == Double.MAX_VALUE || maxX == -Double.MAX_VALUE) { minX = 0; maxX = 1; }
        // Add Padding
        double rangeY = maxY - minY;
        if (rangeY == 0) rangeY = 1.0;
        this.viewMinY = minY - rangeY * 0.05;
        this.viewMaxY = maxY + rangeY * 0.05;

        double rangeX = maxX - minX;
        if (rangeX == 0) rangeX = 1.0;
        this.viewMinX = minX - rangeX * 0.05;
        this.viewMaxX = maxX + rangeX * 0.05;
    }

    void applyAxisOverridesInternal(Rectangle2D plotBounds) {
        if (plotBounds == null) return;

        if (xAxisConfig != null) {
            Double units = xAxisConfig.getResolvedUnitsPerPixel(logicalPixelsPerMm);
            if (xAxisConfig.isFixedRange()) {
                viewMinX = xAxisConfig.getFixedMin();
                viewMaxX = xAxisConfig.getFixedMax();
            } else if (units != null && Double.isFinite(units) && units > 0.0) {
                double range = plotBounds.getWidth() * units;
                double center = (viewMinX + viewMaxX) * 0.5;
                if (!Double.isFinite(center)) {
                    center = 0.0;
                }
                viewMinX = center - range / 2.0;
                viewMaxX = center + range / 2.0;
            }
        }

        if (yAxisConfig != null) {
            Double units = yAxisConfig.getResolvedUnitsPerPixel(logicalPixelsPerMm);
            if (yAxisConfig.isFixedRange()) {
                viewMinY = yAxisConfig.getFixedMin();
                viewMaxY = yAxisConfig.getFixedMax();
            } else if (units != null && Double.isFinite(units) && units > 0.0) {
                double range = plotBounds.getHeight() * units;
                double center = (viewMinY + viewMaxY) * 0.5;
                if (!Double.isFinite(center)) {
                    center = 0.0;
                }
                viewMinY = center - range / 2.0;
                viewMaxY = center + range / 2.0;
            }
        }
    }

    private Rectangle2D.Double getCalculatedPlotBounds() {
        double left = ChartScale.scale(padding.left), top = ChartScale.scale(padding.top);
        double right = ChartScale.scale(padding.right), bottom = ChartScale.scale(padding.bottom);
        if (axisLabelsVisible) {
            FontMetrics metrics = getFontMetrics(axisFont());
            double minY = yAxisConfig.isFixedRange() ? yAxisConfig.getFixedMin() : viewMinY;
            double maxY = yAxisConfig.isFixedRange() ? yAxisConfig.getFixedMax() : viewMaxY;
            left = Math.max(left, measuredAxisWidth(metrics, yAxisConfig, minY, maxY) + ChartScale.scale(12));
            if (hasSecondaryAxis()) {
                double[] range = secondaryRange();
                right = Math.max(right, measuredAxisWidth(metrics, yAxisSecondaryConfig, range[0], range[1]) + ChartScale.scale(12));
            }
            double minX = xAxisConfig.isFixedRange() ? xAxisConfig.getFixedMin() : viewMinX;
            double maxX = xAxisConfig.isFixedRange() ? xAxisConfig.getFixedMax() : viewMaxX;
            double endLabelMargin = measuredAxisWidth(metrics, xAxisConfig, minX, maxX) * 0.5 + ChartScale.scale(4);
            left = Math.max(left, endLabelMargin);
            right = Math.max(right, endLabelMargin);
            bottom = Math.max(bottom, metrics.getHeight() + ChartScale.scale(18));
        }
        int width = Math.max(1, overlayCanvas.getWidth());
        int height = Math.max(1, overlayCanvas.getHeight());
        left = Math.min(left, width * 0.4);
        right = Math.min(right, width * 0.4);
        plotBoundsCache.setRect(left, top, Math.max(1, width - left - right), Math.max(1, height - top - bottom));
        return plotBoundsCache;
    }

    private void drawEmptyState(Graphics2D g2) {
        ChartTheme t = theme;
        g2.setColor(toAwt(t.getAxisLabelColor()));
        FontMetrics fm = g2.getFontMetrics();
        String s = "No data";
        drawAnchoredString(g2, s, overlayCanvas.getWidth() / 2.0, overlayCanvas.getHeight() / 2.0, TextAnchor.CENTER);
    }

    // --- Fluent API ---

    /**
     * Sets the grid layer for this chart.
     *
     * @param gridLayer The grid layer to use (e.g., {@link DefaultGridLayer} or {@link com.arbergashi.charts.render.grid.MedicalGridLayer}).
     * @return This panel for chaining.
     */
    public ArberChartPanel setGridLayer(GridLayer gridLayer){
        this.gridLayer = gridLayer;
        repaint();
        return this;
        
    }

    /**
     * Returns the current theme of this chart panel.
     */
    public ChartTheme getTheme() {
        return theme;
    }

    /**
     * Sets rendering hints for this panel.
     */
    public ArberChartPanel setRenderHints(ChartRenderHints renderHints){
        this.renderHints = renderHints;
        cacheDirty = true;
        repaint();
        return this;
        
    }

    /**
     * Sets the theme for this chart panel and propagates it to all existing renderers.
     * <p>
     * <b>Runtime Theme Switching:</b> This method can be called at any time to change the visual appearance
     * dynamically (e.g., Dark ↔ Light mode). All layers will be updated immediately, and the panel will repaint.
     * </p>
     *
     * @param theme The theme to apply. Must not be {@code null}.
     * @throws NullPointerException if {@code theme} is {@code null}.
     */
    public ArberChartPanel setTheme(ChartTheme theme) {
        if (theme == null) {
            throw new NullPointerException("Theme must not be null");
        }

        this.theme = theme;

        // Propagate theme to all renderers
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer) {
                ((BaseRenderer) layer.renderer).setTheme(theme);
            }
        }

        applyThemeToOverlays();

        cacheDirty = true;
        repaint();
        return this;
    }

    /**
     * Sets the locale used for formatting axis labels and tooltips.
     * An explicit {@link AxisConfig#setLocale(Locale) axis locale} overrides this host default.
     *
     * @param locale locale to apply (null resets to JVM default)
     */
    @Override
    public void setLocale(Locale locale) {
        this.locale = (locale != null ? locale : Locale.getDefault());
        // Keep the AWT component locale in sync, otherwise Component.getLocale() keeps
        // answering with the JVM default (CI on Linux: "en" instead of "en_US").
        super.setLocale(this.locale);
        cacheDirty = true;
        repaint();
    }

    /**
     * Returns the locale used for formatting axis labels and tooltips.
     *
     * @return the configured locale, never null
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Enables or disables tooltips.
     *
     * @param enabled True to enable tooltips, false to disable.
     */
    public ArberChartPanel setTooltips(boolean enabled) {
        this.tooltipsEnabled = enabled;
        if (!enabled) {
            hideTooltip();
        }
        return this;
    }

    /**
     * When enabled, the panel skips internal chart painting and only renders child overlays.
     * Intended for external active-rendering surfaces.
     */
    public ArberChartPanel setExternalRenderSurface(boolean externalRenderSurface) {
        this.externalRenderSurface = externalRenderSurface;
        repaint();
        return this;
    }

    public boolean isExternalRenderSurface() {
        return externalRenderSurface;
    }

    /**
     * Controls the visibility of the chart legend.
     *
     * @param visible True to show the legend, false to hide it.
     */
    public ArberChartPanel setLegend(boolean visible) {
        this.legendVisible = visible;
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setVisible(visible && legendConfig.getPlacement() == LegendPlacement.OVERLAY);
        }
        if (dockedLegendPanel != null) {
            dockedLegendPanel.setVisible(visible && legendConfig.getPlacement() == LegendPlacement.DOCKED);
        }
        cacheDirty = true;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Configures legend placement.
     *
     * <p>This is the recommended API for professional applications that need either an overlay legend
     * or a docked legend outside of the plot area.</p>
     *
     * @param config legend configuration (null uses defaults)
     */
    public ArberChartPanel setLegendConfig(LegendConfig config) {
        this.legendConfig = config != null ? config : LegendConfig.overlay(LegendPosition.TOP_LEFT);
        applyLegendPlacement();
        cacheDirty = true;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Convenience helper to dock the legend to a side.
     *
     * @param side dock side
     */
    public ArberChartPanel setDockedLegend(LegendDockSide side) {
        setLegendConfig(LegendConfig.docked(side));
        return this;
    }

    /**
     * Convenience helper to use overlay legend at a position.
     *
     * @param pos overlay position
     */
    public ArberChartPanel setOverlayLegend(LegendPosition pos) {
        setLegendConfig(LegendConfig.overlay(pos));
        return this;
    }

    /**
     * Enables or disables viewport and overlay animation on the EDT.
     * Disabling motion completes any pending zoom immediately and removes crosshair fading.
     * The default is enabled unless {@code arbercharts.reduceMotion=true} is set at construction.
     *
     * @param enabled whether to animate interaction feedback
     * @return this panel
     */
    public ArberChartPanel setAnimationsEnabled(boolean enabled) {
        boolean previous = animationsEnabled;
        animationsEnabled = enabled;
        if (!enabled) stopZoomAnimation(true);
        if (crosshair != null) crosshair.setAnimationsEnabled(enabled);
        cacheDirty = true;
        firePropertyChange("animationsEnabled", previous, enabled);
        repaint();
        return this;
    }

    /**
     * Returns whether interaction animations are enabled.
     *
     * @return the animation setting
     */
    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    /**
     * Returns whether the X-axis and both Y-axis tick labels are visible.
     *
     * @return {@code true} when axis labels are visible; the default is {@code true}
     * @since 2.5.0
     */
    public boolean isAxisLabelsVisible() {
        return axisLabelsVisible;
    }

    /**
     * Shows or hides all axis tick labels, including secondary Y-axis labels.
     * Hidden labels do not reserve additional measured space, while the panel's
     * base padding remains in place. Axis configuration and grid visibility are unchanged;
     * use {@link AxisConfig#setShowGrid(boolean)} to configure grid lines separately.
     *
     * @param visible {@code true} to show axis labels
     * @return this panel for chaining
     * @since 2.5.0
     */
    public ArberChartPanel setAxisLabelsVisible(boolean visible) {
        if (axisLabelsVisible != visible) {
            axisLabelsVisible = visible;
            cacheDirty = true;
            repaint();
        }
        return this;
    }

    /**
     * Configures the X-axis behavior and formatting.
     *
     * @param config axis configuration (null uses defaults)
     */
    public ArberChartPanel setXAxisConfig(AxisConfig config) {
        stopZoomAnimation(false);
        this.xAxisConfig = (config != null) ? config : new AxisConfig();
        cacheDirty = true;
        repaint();
        return this;
    }

    /**
     * Configures the Y-axis behavior and formatting.
     *
     * @param config axis configuration (null uses defaults)
     */
    public ArberChartPanel setYAxisConfig(AxisConfig config) {
        this.yAxisConfig = (config != null) ? config : new AxisConfig();
        cacheDirty = true;
        repaint();
        return this;
    }

    // --- Public Actions ---
    
    /**
     * Resets the zoom to fit all data.
     */
    public void resetZoom() {
        stopZoomAnimation(false);
        autoScale();
        cacheDirty = true;
        repaint();
    }

    /**
     * Exports the current chart view to a file.
     *
     * @param file The output file.
     */
    public void export(File file) {
        ChartExportService.exportPng(this, file);
    }

    /**
     * Registers an anomaly listener.
     */
    public void setAnomalyListener(AnomalyListener listener) {
        if (listener != null) {
            anomalyListeners.add(listener);
        }
    }

    /**
     * Notifies anomaly listeners (used by predictive overlays).
     */
    public void notifyAnomaly(AnomalyEvent event) {
        if (event == null) return;
        for (AnomalyListener l : anomalyListeners) {
            l.onAnomaly(event);
        }
    }

    /**
     * Returns the last-built plot context for tests and diagnostics.
     */
    public PlotContext getDebugContext() {
        return getOrBuildContext();
    }

    /**
     * Returns the overlay canvas for tests.
     */
    public JComponent getOverlayCanvasForTesting() {
        return overlayCanvas;
    }

    /**
     * Enables or disables freeze mode (diagnostic tooling).
     */
    public ArberChartPanel setFreeze(boolean freeze) {
        this.frozen = freeze;
        if (!freeze) {
            freezeMeasureArmed = false;
            freezeMeasurementLabel.setVisible(false);
        }
        repaint();
        return this;
    }

    /**
     * Sets the freeze scrub position (0..1 range expected).
     */
    public ArberChartPanel setFreezeScrub(double scrub) {
        this.freezeScrub = scrub;
        return this;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public JLabel getFreezeMeasurementLabel() {
        return freezeMeasurementLabel;
    }

    public ArberChartPanel setAuditLogger(com.arbergashi.charts.api.ViewportAuditTrail trail) {
        this.auditTrail = trail;
        return this;
    }

    public ArberChartPanel setStreamPlaybackDrive(com.arbergashi.charts.api.forensic.StreamPlaybackDrive drive) {
        this.streamPlaybackDrive = drive;
        return this;
    }

    /**
     * Public wrapper to allow tests to trigger axis override logic.
     */
    public void applyAxisOverrides(Rectangle2D plotBounds) {
        applyAxisOverridesInternal(plotBounds);
    }
    
    private void setExportMenuState() {
        boolean enabled = exportHandler != null;
        if (exportPngItem != null) exportPngItem.setEnabled(enabled);
        if (exportSvgItem != null) exportSvgItem.setEnabled(enabled);
        if (exportPdfItem != null) exportPdfItem.setEnabled(enabled);
    }

    /**
     * Returns the number of render layers in this chart.
     *
     * @return the number of layers
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * Returns the stable series identifier used by the legend and visibility model.
     * The first occurrence retains the renderer identifier; repeated types receive a numeric suffix.
     *
     * @param index zero-based layer index
     * @return identifier for this layer
     * @throws IndexOutOfBoundsException if the index does not identify a layer
     */
    public String getLayerId(int index) {
        return layers.get(index).id;
    }

    /**
     * Checks if the legend is currently visible.
     *
     * @return true if the legend is visible, false otherwise
     */
    public boolean isLegendVisible() {
        return legendVisible;
    }

    /**
     * Exports the chart to a file in the specified format via the configured export handler.
     * Supported formats: PNG, SVG, PDF.
     *
     * <p>An export handler must be configured via {@link #setExportHandler(ChartExportHandler)}
     * before calling this method.</p>
     *
     * @param format the export format ("png", "svg", or "pdf")
     * @throws IllegalStateException if no export handler is configured
     */
    public void exportAs(String format) {
        if (exportHandler == null) {
            throw new IllegalStateException("No export handler configured for ArberChartPanel.");
        }
        exportHandler.export(this, format);
    }

    /**
     * Sets the export handler used by export actions.
     *
     * @param handler export handler (may be null to disable exports)
     */
    public ArberChartPanel setExportHandler(ChartExportHandler handler) {
        this.exportHandler = handler;
        setExportMenuState();
        return this;
    }

    @Override
    public void removeNotify() {
        stopZoomAnimation(true);
        if (modelListenersAttached) {
            for (RenderLayer layer : layers) layer.model.removeChangeListener(layer.listener);
            for (RenderLayer layer : preDataLayers) layer.model.removeChangeListener(layer.listener);
            modelListenersAttached = false;
        }
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!modelListenersAttached) {
            for (RenderLayer layer : layers) layer.model.setChangeListener(layer.listener);
            for (RenderLayer layer : preDataLayers) layer.model.setChangeListener(layer.listener);
            modelListenersAttached = true;
            cacheDirty = true;
        }
        // On Java 9+, Swing handles HiDPI scaling itself. Keep framework scale at 1x unless forced.
        float scale = 1.0f;
        boolean forceGraphicsScale = Boolean.parseBoolean(System.getProperty("arbercharts.hidpi.autodetect", "false"));
        boolean modernJdk = Runtime.version().feature() >= 9;
        if (forceGraphicsScale || !modernJdk) {
            if (getGraphicsConfiguration() != null && getGraphicsConfiguration().getDefaultTransform() != null) {
                scale = (float) getGraphicsConfiguration().getDefaultTransform().getScaleX();
            }
        }
        ChartScale.autoDetect(scale);
    }

    /**
     * Returns the visibility model used by the interactive legend.
     * Use {@link #getLayerId(int)} to address independent series, including repeated renderer types.
     *
     * @return layer visibility model
     */
    public LayerVisibilityModel getLayerVisibilityModel() {
        return layerVisibility;
    }

    private boolean isSoloSeries(String seriesId) {
        return seriesId != null && layerVisibility.isSoloActive() && !layerVisibility.isDimmed(seriesId);
    }

    private void showAllSeries() {
        layerVisibility.clearSolo();
    }

    private void soloSeriesInternal(String seriesId) {
        layerVisibility.setSolo(seriesId);
    }

    private Composite getSoloComposite() {
        float alpha = ChartAssets.getFloat("Chart.legend.soloDimAlpha", 0.05f);
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;
        if (soloComposite == null || soloCompositeAlpha != alpha) {
            soloCompositeAlpha = alpha;
            soloComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        }
        return soloComposite;
    }

    private String formatAxisValueX(double value) {
        if (xAxisConfig != null) {
            return xAxisConfig.getFormattedValue(value, getLocale(), cachedScaleX.getTickSpacing());
        }
        return FormatUtils.formatAxisLabel(value, getLocale());
    }

    private String formatAxisValueY(double value) {
        if (yAxisConfig != null) {
            return yAxisConfig.getFormattedValue(value, getLocale(), cachedScaleY.getTickSpacing());
        }
        return FormatUtils.formatAxisLabel(value, getLocale());
    }

    private String formatAxisValueY(double value, RenderLayer layer) {
        boolean secondary = layer.renderer instanceof BaseRenderer br && br.getYAxisRole() == AxisRole.SECONDARY;
        AxisConfig config = secondary ? yAxisSecondaryConfig : yAxisConfig;
        NiceScale scale = secondary ? cachedScaleSecondaryY : cachedScaleY;
        PlotContext context = getLayerContext(layer.renderer);
        scale.setRange(context.getMinY(), context.getMaxY());
        scale.setMaxTicks(context.getRequestedTickCountY());
        return config.getFormattedValue(value, getLocale(), scale.getTickSpacing());
    }
}
