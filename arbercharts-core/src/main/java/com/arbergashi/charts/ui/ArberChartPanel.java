package com.arbergashi.charts.ui;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ChartModel.ChartModelListener;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.circular.SunburstRenderer;
import com.arbergashi.charts.ui.grid.DefaultGridLayer;
import com.arbergashi.charts.ui.grid.GridLayer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartI18N;
import com.arbergashi.charts.util.FormatUtils;
import com.arbergashi.charts.util.NiceScale;
import com.arbergashi.charts.util.AnimationUtils;
import com.arbergashi.charts.ui.legend.DockedLegendPanel;
import com.arbergashi.charts.ui.legend.InteractiveLegendOverlay;
import com.arbergashi.charts.ui.legend.LegendChartContext;
import com.arbergashi.charts.ui.legend.LegendConfig;
import com.arbergashi.charts.ui.legend.LegendDockSide;
import com.arbergashi.charts.ui.legend.LegendPlacement;
import com.arbergashi.charts.ui.legend.LegendPosition;
import com.arbergashi.charts.ui.legend.LayerVisibilityModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <h1>ArberChartPanel</h1>
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
 * @version 1.0.0
 * @since 2024-06-01
 */
public class ArberChartPanel extends JPanel {


    // --- Architecture: Layers ---
    
    /**
     * Internal container for a renderable layer.
     */
    private static class RenderLayer {
        final ChartModel model;
        final ChartRenderer renderer;
        final ChartModelListener listener;

        RenderLayer(ChartModel model, ChartRenderer renderer, ChartModelListener listener) {
            this.model = model;
            this.renderer = renderer;
            this.listener = listener;
        }
    }

    private final List<RenderLayer> layers = new CopyOnWriteArrayList<>();

    // --- Layout & Caching ---
    private final Insets padding = new Insets(40, 60, 40, 40);
    private final Rectangle2D.Double plotBoundsCache = new Rectangle2D.Double();
    
    // Viewport State (Camera)
    private double viewMinX = Double.NaN;
    private double viewMaxX = Double.NaN;
    private double viewMinY = Double.NaN;
    private double viewMaxY = Double.NaN;

    // Caching for Zero-Allocation Axis Rendering
    private final NiceScale cachedScaleX = new NiceScale(0, 1);
    private final NiceScale cachedScaleY = new NiceScale(0, 1);
    private final double[] axisMapBuffer = new double[2];
    private transient PlotContext contextCache;
    private transient boolean cacheDirty = true;
    private transient long lastModelStamp = -1;

    // --- Components ---
    private GridLayer gridLayer;
    private ChartTooltip chartTooltip;
    private HighPrecisionCrosshair crosshair;
    private LegendChartContext legendContext;
    private JPopupMenu contextMenu;
    // add:
    private InteractiveLegendOverlay interactiveLegendOverlay;
    private DockedLegendPanel dockedLegendPanel;
    private LegendConfig legendConfig = LegendConfig.DEFAULT;
    private JComponent overlayCanvas;
    private final LayerVisibilityModel layerVisibility = new LayerVisibilityModel();
    private ChartExportHandler exportHandler;
    private JMenuItem exportPngItem;
    private JMenuItem exportSvgItem;
    private JMenuItem exportPdfItem;

    // Configuration
    private boolean tooltipsEnabled = true;
    private boolean crosshairEnabled = true;
    private boolean legendVisible = true;
    private boolean animationsEnabled = true;
    private AxisConfig xAxisConfig = new AxisConfig();
    private AxisConfig yAxisConfig = new AxisConfig();
    private ChartRenderHints renderHints = new ChartRenderHints();

    // --- Animation State ---
    private Timer zoomAnimationTimer;
    private long zoomAnimationStart = -1L;
    private static final long ZOOM_ANIMATION_DURATION_MS = 250L;
    private double animFromMinX, animFromMaxX, animToMinX, animToMaxX;

    /**
     * Theme for this panel. This is the single source of truth for UI chrome (grid/axes/legend/overlays).
     */
    private ChartTheme theme = ChartThemes.defaultDark();

    /**
     * Creates a new high-performance chart panel with an initial dataset.
     *
     * @param initialModel    The primary data model to display.
     * @param initialRenderer The renderer responsible for visualizing the data.
     */
    public ArberChartPanel(ChartModel initialModel, ChartRenderer initialRenderer) {
        // Use absolute layout for precise overlay positioning
        setLayout(new BorderLayout());
        setOpaque(false);

        // Create a dedicated canvas for the chart + overlays.
        overlayCanvas = new JPanel(null);
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
            addLayer(initialModel, initialRenderer);
        }
    }

    // --- Layer Management (Multi-Series Support) ---

    /**
     * Adds a new data series to the chart.
     * <p>
     * The chart supports stacking multiple layers. Each layer consists of a data model and a renderer.
     * Layers are drawn in the order they are added (Painter's Algorithm).
     * </p>
     *
     * @param model    The data model containing the series data.
     * @param renderer The renderer used to visualize this specific series.
     * @implNote This method is safe to call on the EDT only; it triggers repainting.
     */
    public void addLayer(ChartModel model, ChartRenderer renderer) {
        if (model == null || renderer == null) return;

        // Set layer index for color differentiation in multi-layer charts
        if (renderer instanceof BaseRenderer) {
            ((BaseRenderer) renderer).setLayerIndex(layers.size());
        }

        ChartModelListener listener = () -> {
            cacheDirty = true;
            repaint();
        };
        model.addChangeListener(listener);
        layers.add(new RenderLayer(model, renderer, listener));

        // Auto-scale if it's the first layer or view is not set
        if (Double.isNaN(viewMinX)) {
            autoScale();
        }

        cacheDirty = true;
        repaint();
    }

    /**
     * Adds an overlay renderer that uses the PRIMARY model (the first one added).
     * <p>
     * This is useful for technical indicators (e.g., Moving Averages, Regression Lines) that
     * are derived from the main dataset but require a separate rendering pass.
     * </p>
     *
     * @param renderer The overlay renderer.
     * @implNote Overlay renderers share the primary model and are drawn as additional layers.
     */
    public void addOverlay(ChartRenderer renderer) {
        if (layers.isEmpty()) return;
        addLayer(layers.getFirst().model, renderer);
    }

    /**
     * Removes all layers and clears the chart.
     *
     * @implNote Listeners are removed and the panel is repainted.
     */
    public void clearLayers() {
        for (RenderLayer layer : layers) {
            layer.model.removeChangeListener(layer.listener);
        }
        layers.clear();
        cacheDirty = true;
        repaint();
    }

    /**
     * Enables or disables multi-color mode on renderers that support it.
     *
     * @param enabled true to enable multi-color rendering
     */
    public void setMultiColorEnabled(boolean enabled) {
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof com.arbergashi.charts.render.BaseRenderer br) {
                br.setMultiColor(enabled);
            }
        }
        cacheDirty = true;
        repaint();
    }

    // --- Initialization ---

    private void initializeOverlays() {
        // 1. Tooltip (Topmost)
        chartTooltip = new ChartTooltip();
        chartTooltip.setTheme(theme);
        overlayCanvas.add(chartTooltip);

        // 2. Crosshair
        crosshair = new HighPrecisionCrosshair();
        crosshair.setVisible(true);
        crosshair.setEnabled(true);
        overlayCanvas.add(crosshair);

        // 3. Legend (overlay by default; can be docked)
        // Provide a minimal data view so legend components can query model/renderers.
        legendContext = new LegendChartContext() {
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
        interactiveLegendOverlay.setVisible(legendVisible);
        overlayCanvas.add(interactiveLegendOverlay);

        // Z-order: tooltip on top
        overlayCanvas.setComponentZOrder(chartTooltip, 0);
        overlayCanvas.setComponentZOrder(crosshair, 1);
        overlayCanvas.setComponentZOrder(interactiveLegendOverlay, 2);

        applyThemeToOverlays();
        applyLegendPlacement();
    }

    private void applyLegendPlacement() {
        if (interactiveLegendOverlay == null) return;

        if (!legendVisible) {
            if (dockedLegendPanel != null) dockedLegendPanel.setVisible(false);
            interactiveLegendOverlay.setVisible(false);
            return;
        }

        if (legendConfig.placement() == LegendPlacement.DOCKED) {
            // create dock panel lazily
            if (dockedLegendPanel == null) {
                dockedLegendPanel = new DockedLegendPanel(legendContext, theme);
                dockedLegendPanel.setVisibilityResolver(layerVisibility::isVisible);
                dockedLegendPanel.setLegendActionListener(new com.arbergashi.charts.ui.legend.LegendActionListener() {
                    @Override
                    public void toggleSeries(String seriesId) {
                        layerVisibility.toggle(seriesId);
                        repaint();
                        if (interactiveLegendOverlay != null) {
                            interactiveLegendOverlay.repaint();
                        }
                        if (dockedLegendPanel != null) {
                            dockedLegendPanel.repaint();
                        }
                    }

                    @Override
                    public void openSeriesSettings(String seriesId) {
                        // Consumers can attach their own UI.
                    }

                    @Override
                    public void soloSeries(String seriesId) {
                        boolean currentlySolo = isOnlySeriesVisible(seriesId);
                        if (currentlySolo) {
                            showAllSeries();
                        } else {
                            soloSeriesInternal(seriesId);
                        }
                        repaint();
                        if (interactiveLegendOverlay != null) {
                            interactiveLegendOverlay.repaint();
                        }
                        if (dockedLegendPanel != null) {
                            dockedLegendPanel.repaint();
                        }
                    }
                });
            }
            dockedLegendPanel.setVisible(true);

            // Hide overlay legend inside chart canvas.
            interactiveLegendOverlay.setVisible(false);

            // Ensure the dock panel reflects current side sizing policy.
            dockedLegendPanel.setDockSide(legendConfig.dockSide());

            // Ensure dock panel is added to border layout if possible.
            ensureDockedLegendAdded();
        } else {
            // Overlay mode
            if (dockedLegendPanel != null) dockedLegendPanel.setVisible(false);
            interactiveLegendOverlay.setVisible(true);
            // Configure overlay position via ChartAssets as the current overlay uses that property.
            // Keep it consistent with the config.
            com.arbergashi.charts.util.ChartAssets.setProperty("Chart.legend.position", legendConfig.overlayPosition().name());
        }
    }

    private void ensureDockedLegendAdded() {
        if (dockedLegendPanel == null) return;

        // Ensure the dock panel reflects current side sizing policy.
        dockedLegendPanel.setDockSide(legendConfig.dockSide());

        String constraint = switch (legendConfig.dockSide()) {
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
        if (chartTooltip != null) {
            chartTooltip.setTheme(theme);
        }
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setTheme(theme);
            interactiveLegendOverlay.setVisible(legendVisible);
        }
        if (dockedLegendPanel != null) {
            dockedLegendPanel.setTheme(theme);
            dockedLegendPanel.setVisible(legendVisible && legendConfig.placement() == LegendPlacement.DOCKED);
        }
        // legacy
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
        updateExportMenuState();
    }

    private void initializeInputController() {
        MouseAdapter controller = new MouseAdapter() {
            private Point lastDragPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    lastDragPoint = e.getPoint();
                    
                    // Handle special clicks for renderers
                    for (RenderLayer layer : layers) {
                        if (layer.renderer instanceof SunburstRenderer sunburst) {
                            sunburst.handleClick(e.getPoint(), getOrBuildContext());
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

                double dx = e.getX() - lastDragPoint.getX();
                double width = plotBoundsCache.getWidth();

                if (width > 0) {
                    PlotContext ctx = getOrBuildContext();
                    double rangeX = ctx.maxX() - ctx.minX();
                    // Sensitivity factor 1.0 = 1:1 movement
                    double shift = -(dx / width) * rangeX;

                    if (Double.isNaN(viewMinX)) {
                        viewMinX = ctx.minX();
                        viewMaxX = ctx.maxX();
                    }
                    viewMinX += shift;
                    viewMaxX += shift;

                    cacheDirty = true;
                    repaint();
                }
                lastDragPoint = e.getPoint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                PlotContext ctx = getOrBuildContext();
                double zoomFactor = 1.1;
                double currentRange = ctx.maxX() - ctx.minX();
                
                // Zoom towards mouse pointer
                double mouseX = e.getX();
                double plotX = ctx.plotBounds().getX();
                double plotW = ctx.plotBounds().getWidth();
                
                // Relative position (0.0 to 1.0)
                double relX = (mouseX - plotX) / plotW;
                relX = Math.max(0, Math.min(1, relX)); // Clamp

                double focusPoint = ctx.minX() + currentRange * relX;

                double newRange;
                if (e.getWheelRotation() < 0) { // Zoom In
                    newRange = currentRange / zoomFactor;
                } else { // Zoom Out
                    newRange = currentRange * zoomFactor;
                }

                // Calculate new min/max keeping focusPoint stable
                double targetMinX = focusPoint - newRange * relX;
                double targetMaxX = focusPoint + newRange * (1 - relX);

                // Use animation if enabled, otherwise immediate
                if (animationsEnabled) {
                    startZoomAnimation(targetMinX, targetMaxX);
                } else {
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

                if (chartTooltip != null) chartTooltip.setVisible(false);
                if (crosshair != null) crosshair.hideCrosshair();
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

        if (chartTooltip != null) {
            chartTooltip.addMouseListener(controller);
            chartTooltip.addMouseMotionListener(controller);
            chartTooltip.addMouseWheelListener(controller);
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

    // --- Animation Support ---

    /**
     * Starts a smooth zoom animation from current view to target bounds.
     */
    private void startZoomAnimation(double targetMinX, double targetMaxX) {
        // Stop any existing animation
        if (zoomAnimationTimer != null && zoomAnimationTimer.isRunning()) {
            zoomAnimationTimer.stop();
        }

        // Setup animation
        PlotContext ctx = getOrBuildContext();
        animFromMinX = Double.isNaN(viewMinX) ? ctx.minX() : viewMinX;
        animFromMaxX = Double.isNaN(viewMaxX) ? ctx.maxX() : viewMaxX;
        animToMinX = targetMinX;
        animToMaxX = targetMaxX;
        zoomAnimationStart = AnimationUtils.nowNanos();

        // Create animation timer (60 FPS)
        zoomAnimationTimer = new Timer(16, _ -> {
            double progress = AnimationUtils.getEasedProgressNanos(
                zoomAnimationStart, ZOOM_ANIMATION_DURATION_MS);

            if (progress >= 1.0) {
                // Animation complete
                zoomAnimationTimer.stop();
                viewMinX = animToMinX;
                viewMaxX = animToMaxX;
            } else {
                // Interpolate
                viewMinX = AnimationUtils.lerp(animFromMinX, animToMinX, progress);
                viewMaxX = AnimationUtils.lerp(animFromMaxX, animToMaxX, progress);
            }

            cacheDirty = true;
            repaint();
        });
        zoomAnimationTimer.start();
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

        if (chartTooltip != null) {
            chartTooltip.setBounds(0, 0, chartTooltip.getWidth(), chartTooltip.getHeight());
        }
        if (crosshair != null) {
            crosshair.setBounds(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        }
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setBounds(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
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
                renderHints.applyTo(g2);
            }
            
            // 2. Build Context (View Matrix)
            PlotContext ctx = getOrBuildContext();
            
            // 3. Render Grid (Background)
            boolean showGrid = (xAxisConfig == null || xAxisConfig.isShowGrid())
                    || (yAxisConfig == null || yAxisConfig.isShowGrid());
            if (gridLayer != null && showGrid) {
                gridLayer.renderGrid(g2, ctx);
            }

            // 4. Render Layers (Z-Order: First added is bottom)
            // We iterate normally so first layer is drawn first, others on top.
            for (RenderLayer layer : layers) {
                if (layer.renderer instanceof BaseRenderer br) {
                    if (!layerVisibility.isVisible(br.getId())) {
                        continue;
                    }
                }
                if (layer.model == null || layer.model.isEmpty()) {
                    if (layer.renderer != null && layer.renderer.supportsEmptyState()) {
                        Shape oldClip = g2.getClip();
                        g2.clip(ctx.plotBounds());
                        layer.renderer.renderEmptyState(g2, layer.model, ctx);
                        g2.setClip(oldClip);
                    }
                    continue;
                }
                // Clip to plot bounds to prevent drawing outside
                Shape oldClip = g2.getClip();
                g2.clip(ctx.plotBounds());
                layer.renderer.render(g2, layer.model, ctx);
                g2.setClip(oldClip);
            }

            // 5. Render Axes (Foreground)
            drawAxisLabels(g2, ctx);

            // 6. Render Legend (Overlay)
            // Legend is rendered by InteractiveLegendOverlay (single source of truth).

        } finally {
            g2.dispose();
        }
    }

    // --- Optimized Axis Rendering (Zero-Allocation) ---

    private void drawAxisLabels(Graphics2D g2, PlotContext ctx) {
        ChartTheme t = ctx.theme() != null ? ctx.theme() : theme;

        // Use theme font as the single source of truth for chart typography.
        // Fall back to UI defaults only if the theme does not provide a font.
        Font base = t.getBaseFont();
        if (base == null) base = UIManager.getFont("Chart.font");
        if (base == null) base = UIManager.getFont("Label.font");
        if (base == null) base = g2.getFont();

        Font axisFont = base.deriveFont(Font.BOLD, ChartScale.uiFontSize(base, 11.0f));
        g2.setFont(axisFont);
        g2.setColor(t.getAxisLabelColor());
        FontMetrics fm = g2.getFontMetrics();

        // Y-Axis
        AxisConfig yCfg = (yAxisConfig != null) ? yAxisConfig : new AxisConfig();
        cachedScaleY.setRange(ctx.minY(), ctx.maxY());
        cachedScaleY.setMaxTicks(Math.max(2, yCfg.getRequestedTickCount()));
        // calculateTicks() is called internally by getTicks()
        for (double val : cachedScaleY.getTicks()) {
            ctx.mapToPixel(ctx.minX(), val, axisMapBuffer);
            String label = yCfg.formatValue(val);
            float tx = (float) (ctx.plotBounds().getX() - fm.stringWidth(label) - ChartScale.scale(10));
            float ty = (float) (axisMapBuffer[1] + fm.getAscent() / 2.0 - 2);
            g2.drawString(label, tx, ty);
        }

        // X-Axis
        AxisConfig xCfg = (xAxisConfig != null) ? xAxisConfig : new AxisConfig();
        cachedScaleX.setRange(ctx.minX(), ctx.maxX());
        cachedScaleX.setMaxTicks(Math.max(2, xCfg.getRequestedTickCount()));
        for (double val : cachedScaleX.getTicks()) {
            ctx.mapToPixel(val, ctx.minY(), axisMapBuffer);
            String label = xCfg.formatValue(val);
            float tx = (float) (axisMapBuffer[0] - fm.stringWidth(label) / 2.0);
            float ty = (float) (ctx.plotBounds().getY() + ctx.plotBounds().getHeight() + fm.getAscent() + ChartScale.scale(10));
            g2.drawString(label, tx, ty);
        }
    }

    // --- Interaction & Hit Testing ---

    private void handleMouseMove(MouseEvent e) {
        if (crosshairEnabled && crosshair != null) {
            updateCrosshair(e.getPoint());
        }
        if (tooltipsEnabled && chartTooltip != null) {
            updateTooltip(e);
        }
    }

    private void updateTooltip(MouseEvent e) {
        if (layers.isEmpty()) {
            chartTooltip.setVisible(false);
            return;
        }

        PlotContext ctx = getOrBuildContext();
        Point p = e.getPoint();

        // Find the "best" hit across all layers
        StringBuilder bestTooltip = null;

        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer br) {
                if (!layerVisibility.isVisible(br.getId())) {
                    continue;
                }
            }

            Optional<Integer> idxOpt = layer.renderer.getPointAt(p, layer.model, ctx);

            // FALLBACK: If renderer doesn't implement hit-testing (e.g. LineRenderer), do it ourselves
            if (idxOpt.isEmpty() && layer.model.getPointCount() > 0) {
                idxOpt = findNearestPoint(p, layer.model, ctx);
            }

            if (idxOpt.isPresent()) {
                String text = layer.renderer.getTooltipText(idxOpt.get(), layer.model);
                // Fallback text if renderer returns null
                if (text == null) {
                    double xVal = layer.model.getX(idxOpt.get());
                    double yVal = layer.model.getY(idxOpt.get());
                    String xLabel = formatAxisValueX(xVal);
                    String yLabel = formatAxisValueY(yVal);
                    text = String.format("X: %s<br>Y: %s", xLabel, yLabel);
                }

                if (bestTooltip == null) {
                    bestTooltip = new StringBuilder(128);
                    bestTooltip.append("<html>");
                } else {
                    bestTooltip.append("<br>");
                }

                if (layers.size() > 1) {
                    String name = layer.model.getName();
                    if (name != null) bestTooltip.append("<b>").append(name).append(":</b> ");
                }
                bestTooltip.append(text);
            }
        }

        if (bestTooltip != null) {
            bestTooltip.append("</html>");
            chartTooltip.setText(bestTooltip.toString());

            // Smart Positioning
            int tx = p.x + (int) ChartScale.scale(15);
            int ty = p.y + (int) ChartScale.scale(15);
            if (tx + chartTooltip.getWidth() > getWidth()) tx = p.x - chartTooltip.getWidth() - 10;
            if (ty + chartTooltip.getHeight() > getHeight()) ty = p.y - chartTooltip.getHeight() - 10;

            chartTooltip.setLocation(tx, ty);
            chartTooltip.setVisible(true);
        } else {
            chartTooltip.setVisible(false);
        }
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

    private void updateCrosshair(Point position) {
        if (crosshair == null) return;
        PlotContext context = getOrBuildContext();
        if (!context.plotBounds().contains(position)) {
            crosshair.hideCrosshair();
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

        if (snapEnabled) {
            for (RenderLayer layer : layers) {
                if (layer.renderer instanceof BaseRenderer br) {
                    if (!layerVisibility.isVisible(br.getId())) {
                        continue;
                    }
                }

                Optional<Integer> idxOpt = layer.renderer.getPointAt(position, layer.model, context);
                if (idxOpt.isEmpty() && layer.model.getPointCount() > 0) {
                    idxOpt = findNearestPoint(position, layer.model, context);
                }

                if (idxOpt.isPresent()) {
                    int idx = idxOpt.get();
                    double xVal = layer.model.getX(idx);
                    double yVal = layer.model.getY(idx);
                    double[] pix = axisMapBuffer;
                    context.mapToPixel(xVal, yVal, pix);
                    double dx = pix[0] - position.x;
                    double dy = pix[1] - position.y;
                    double distSq = dx * dx + dy * dy;
                    if (distSq < bestDistSq && distSq <= snapThresholdSq) {
                        bestDistSq = distSq;
                        bestX = (int) Math.round(pix[0]);
                        bestY = (int) Math.round(pix[1]);
                        bestDataX = xVal;
                        bestDataY = yVal;
                        snapped = true;
                    }
                }
            }
        }

        if (snapped) {
            String labelX = formatAxisValueX(bestDataX);
            String labelY = formatAxisValueY(bestDataY);
            crosshair.updatePosition(bestX, bestY, labelX, labelY);
            return;
        }

        // Map pixel to data
        context.mapToData(position.x, position.y, axisMapBuffer);
        String labelX = formatAxisValueX(axisMapBuffer[0]);
        String labelY = formatAxisValueY(axisMapBuffer[1]);

        crosshair.updatePosition(position.x, position.y, labelX, labelY);
    }

    // --- Context & Bounds ---

    private PlotContext getOrBuildContext() {
        // Check if any model changed
        long combinedStamp = 0;
        for (RenderLayer layer : layers) {
            combinedStamp += layer.model.getUpdateStamp();
        }

        if (!cacheDirty && contextCache != null && combinedStamp == lastModelStamp) {
            return contextCache;
        }

        Rectangle2D.Double plotBounds = calculatePlotBounds();
        
        // Auto-Scale if view is invalid
        if (Double.isNaN(viewMinX) || Double.isNaN(viewMaxX)) {
            autoScale();
        }

        // Create Context
        // Note: We use the first layer's model as the "primary" model for the context,
        // but the context bounds (minX/maxX) are what matters most for rendering.
        ChartModel primaryModel = layers.isEmpty() ? null : layers.getFirst().model;

        applyAxisOverrides(plotBounds);
        boolean invertX = xAxisConfig != null && xAxisConfig.isInverted();
        boolean invertY = yAxisConfig != null && yAxisConfig.isInverted();
        contextCache = new DefaultPlotContext(plotBounds, viewMinX, viewMaxX, viewMinY, viewMaxY, false, invertX, invertY,
                com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR, com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR,
                theme, renderHints);

        lastModelStamp = combinedStamp;
        cacheDirty = false;
        return contextCache;
    }

    private void autoScale() {
        if (layers.isEmpty()) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean hasData = false;

        for (RenderLayer layer : layers) {
            if (layer.model.isEmpty()) continue;

            double[] x = layer.model.getXData();
            double[] y = layer.model.getYData();
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
                if (x[i] < minX) minX = x[i];
                if (x[i] > maxX) maxX = x[i];
                if (!hasOverride && y != null && i < y.length) {
                    if (y[i] < minY) minY = y[i];
                    if (y[i] > maxY) maxY = y[i];
                }
            }

            if (hasOverride) {
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

    void applyAxisOverrides(Rectangle2D plotBounds) {
        if (plotBounds == null) return;

        if (xAxisConfig != null) {
            if (xAxisConfig.hasFixedRange()) {
                viewMinX = xAxisConfig.getFixedMin();
                viewMaxX = xAxisConfig.getFixedMax();
            } else if (xAxisConfig.getUnitsPerPixel() != null && Double.isFinite(xAxisConfig.getUnitsPerPixel())) {
                double range = plotBounds.getWidth() * xAxisConfig.getUnitsPerPixel();
                double center = (viewMinX + viewMaxX) * 0.5;
                if (!Double.isFinite(center)) {
                    center = 0.0;
                }
                viewMinX = center - range / 2.0;
                viewMaxX = center + range / 2.0;
            }
        }

        if (yAxisConfig != null) {
            if (yAxisConfig.hasFixedRange()) {
                viewMinY = yAxisConfig.getFixedMin();
                viewMaxY = yAxisConfig.getFixedMax();
            } else if (yAxisConfig.getUnitsPerPixel() != null && Double.isFinite(yAxisConfig.getUnitsPerPixel())) {
                double range = plotBounds.getHeight() * yAxisConfig.getUnitsPerPixel();
                double center = (viewMinY + viewMaxY) * 0.5;
                if (!Double.isFinite(center)) {
                    center = 0.0;
                }
                viewMinY = center - range / 2.0;
                viewMaxY = center + range / 2.0;
            }
        }
    }

    private Rectangle2D.Double calculatePlotBounds() {
        double left = ChartScale.scale(padding.left), top = ChartScale.scale(padding.top);
        double right = ChartScale.scale(padding.right), bottom = ChartScale.scale(padding.bottom);
        plotBoundsCache.setRect(left, top, Math.max(1, getWidth() - left - right), Math.max(1, getHeight() - top - bottom));
        return plotBoundsCache;
    }

    private void drawEmptyState(Graphics2D g2) {
        ChartTheme t = theme;
        g2.setColor(t.getAxisLabelColor());
        FontMetrics fm = g2.getFontMetrics();
        String s = "No data";
        g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, (getHeight() - fm.getAscent()) / 2 + fm.getAscent());
    }

    // --- Fluent API ---

    /**
     * Sets the grid layer for this chart.
     *
     * @param gridLayer The grid layer to use (e.g., {@link DefaultGridLayer} or {@link com.arbergashi.charts.ui.grid.MedicalGridLayer}).
     * @return This panel for chaining.
     */
    public ArberChartPanel withGridLayer(GridLayer gridLayer) {
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
    public ArberChartPanel setRenderHints(ChartRenderHints renderHints) {
        this.renderHints = renderHints;
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
     * @return This panel for chaining.
     * @throws NullPointerException if {@code theme} is {@code null}.
     */
    public ArberChartPanel withTheme(ChartTheme theme) {
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
     *
     * @param locale locale to apply (null resets to JVM default)
     * @return this panel for chaining
     */
    public ArberChartPanel withLocale(Locale locale) {
        setLocale(locale != null ? locale : Locale.getDefault());
        repaint();
        return this;
    }

    /**
     * Enables or disables tooltips.
     *
     * @param enabled True to enable tooltips, false to disable.
     * @return This panel for chaining.
     */
    public ArberChartPanel withTooltips(boolean enabled) {
        this.tooltipsEnabled = enabled;
        if (chartTooltip != null) {
            chartTooltip.setVisible(enabled);
        }
        return this;
    }

    /**
     * Controls the visibility of the chart legend.
     *
     * @param visible True to show the legend, false to hide it.
     * @return This panel for chaining.
     */
    public ArberChartPanel withLegend(boolean visible) {
        this.legendVisible = visible;
        if (interactiveLegendOverlay != null) {
            interactiveLegendOverlay.setVisible(visible && legendConfig.placement() == LegendPlacement.OVERLAY);
        }
        if (dockedLegendPanel != null) {
            dockedLegendPanel.setVisible(visible && legendConfig.placement() == LegendPlacement.DOCKED);
        }
        return this;
    }

    /**
     * Configures legend placement.
     *
     * <p>This is the recommended API for professional applications that need either an overlay legend
     * or a docked legend outside of the plot area.</p>
     *
     * @param config legend configuration (null uses defaults)
     * @return this panel for chaining
     */
    public ArberChartPanel withLegendConfig(LegendConfig config) {
        this.legendConfig = config != null ? config : LegendConfig.DEFAULT;
        applyLegendPlacement();
        return this;
    }

    /**
     * Convenience helper to dock the legend to a side.
     *
     * @param side dock side
     * @return this panel for chaining
     */
    public ArberChartPanel withDockedLegend(LegendDockSide side) {
        return withLegendConfig(LegendConfig.docked(side));
    }

    /**
     * Convenience helper to use overlay legend at a position.
     *
     * @param pos overlay position
     * @return this panel for chaining
     */
    public ArberChartPanel withOverlayLegend(LegendPosition pos) {
        return withLegendConfig(LegendConfig.overlay(pos));
    }

    /**
     * Enables or disables smooth animations for zoom and pan operations.
     * <p>
     * When enabled, zoom operations will use {@link AnimationUtils} for smooth eased transitions.
     * Default is {@code true}.
     * </p>
     *
     * @param enabled {@code true} to enable animations, {@code false} for instant updates.
     * @return This panel for chaining.
     */
    public ArberChartPanel withAnimations(boolean enabled) {
        this.animationsEnabled = enabled;
        return this;
    }

    /**
     * Configures the X-axis behavior and formatting.
     *
     * @param config axis configuration (null uses defaults)
     * @return this panel for chaining
     */
    public ArberChartPanel withXAxisConfig(AxisConfig config) {
        this.xAxisConfig = (config != null) ? config : new AxisConfig();
        repaint();
        return this;
    }

    /**
     * Configures the Y-axis behavior and formatting.
     *
     * @param config axis configuration (null uses defaults)
     * @return this panel for chaining
     */
    public ArberChartPanel withYAxisConfig(AxisConfig config) {
        this.yAxisConfig = (config != null) ? config : new AxisConfig();
        repaint();
        return this;
    }

    // --- Public Actions ---
    
    /**
     * Resets the zoom to fit all data.
     */
    public void resetZoom() {
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
    
    private void updateExportMenuState() {
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
     * <p>An export handler must be configured via {@link #withExportHandler(ChartExportHandler)}
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
     * @return this panel for chaining
     */
    public ArberChartPanel withExportHandler(ChartExportHandler handler) {
        this.exportHandler = handler;
        updateExportMenuState();
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Detect DPI scaling (single point of policy in ChartScale).
        ChartScale.autoDetect(getGraphicsConfiguration());
    }

    /**
     * Returns the visibility model used by the interactive legend.
     *
     * @return layer visibility model
     */
    public LayerVisibilityModel getLayerVisibilityModel() {
        return layerVisibility;
    }

    // Package-private helpers for UI tests.
    PlotContext getDebugContext() {
        return getOrBuildContext();
    }

    JComponent getOverlayCanvasForTesting() {
        return overlayCanvas;
    }

    private boolean isOnlySeriesVisible(String seriesId) {
        if (seriesId == null) return false;
        boolean anyVisible = false;

        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer br) {
                boolean v = layerVisibility.isVisible(br.getId());
                if (v) {
                    anyVisible = true;
                    if (!br.getId().equals(seriesId)) return false;
                }
            }
        }
        return anyVisible;
    }

    private void showAllSeries() {
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer br) {
                layerVisibility.setVisible(br.getId(), true);
            }
        }
    }

    private void soloSeriesInternal(String seriesId) {
        for (RenderLayer layer : layers) {
            if (layer.renderer instanceof BaseRenderer br) {
                layerVisibility.setVisible(br.getId(), br.getId().equals(seriesId));
            }
        }
    }

    private String formatAxisValueX(double value) {
        if (xAxisConfig != null) {
            return xAxisConfig.formatValue(value);
        }
        return FormatUtils.formatAxisLabel(value, getLocale());
    }

    private String formatAxisValueY(double value) {
        if (yAxisConfig != null) {
            return yAxisConfig.formatValue(value);
        }
        return FormatUtils.formatAxisLabel(value, getLocale());
    }
}
