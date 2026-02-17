package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import com.arbergashi.charts.render.analysis.VectorFieldRenderer;
import com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer;
import com.arbergashi.charts.render.financial.PredictiveCandleRenderer;
import com.arbergashi.charts.render.forensic.PlaybackStatusRenderer;
import com.arbergashi.charts.render.grid.MedicalGridLayer;
import com.arbergashi.charts.render.grid.SmithChartGridLayer;
import com.arbergashi.charts.render.predictive.AnomalyGapRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.util.LatencyTracker;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Toolkit;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ArberCharts Demo Application - Enterprise-grade showcase for all 157 renderers.
 *
 * <p>This application demonstrates the full capabilities of the ArberCharts framework,
 * including all renderer categories, theme switching, export functionality, and
 * real-time data streaming. Built with modern Swing using FlatLaf themes.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>157 production renderers across 11 categories</li>
 *   <li>Modern Dark/Light themes with Inter font</li>
 *   <li>Real-time data streaming and updates</li>
 *   <li>Export to PNG/SVG/PDF/CSV</li>
 *   <li>Zero-GC rendering architecture</li>
 *   <li>High-DPI/Retina display support</li>
 * </ul>
 *
 * <h2>Keyboard Shortcuts</h2>
 * <ul>
 *   <li>{@code Cmd/Ctrl+E} - Export current chart to PNG</li>
 *   <li>{@code Cmd/Ctrl+R} - Reload current renderer</li>
 *   <li>{@code Cmd/Ctrl+T} - Toggle dark/light theme</li>
 *   <li>{@code Cmd/Ctrl+F} - Focus search field</li>
 *   <li>{@code Cmd/Ctrl+B} - Run benchmark on current renderer</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * java --add-modules jdk.incubator.vector -jar arbercharts-demo.jar
 * }</pre>
 *
 * <p><strong>License:</strong> ArberCharts binaries are licensed under the MIT License.
 * See {@code META-INF/LICENSE} for full license text.
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2.0.0
 */
public final class DemoApplication {

    /** Application version. */
    private static final String VERSION = "2.0.0";

    /** Application name for display. */
    private static final String APP_NAME = "ArberCharts Demo";

    /** Copyright notice. */
    private static final String COPYRIGHT = "© 2026 Arber Gashi";

    /**
     * Main entry point for the demo application.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        configurePlatformDefaults();
        DemoThemeSupport.bootstrapThemeResources();
        SwingUtilities.invokeLater(() -> {
            String theme = DemoThemeSupport.setupLookAndFeel();
            DemoApplication app = new DemoApplication(theme);
            app.show();
        });
    }

    private final RendererCatalog catalog = RendererCatalog.load();
    private final Map<String, RendererCatalogEntry> byName = new HashMap<>();
    private final Map<String, JPanel> rendererPanels = new HashMap<>();
    private final CardLayout detailLayout = new CardLayout();
    private final JPanel detailHost = new JPanel(detailLayout);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel countLabel = new JLabel();
    private final JLabel metricsLabel = new JLabel();
    private final JTextField searchField = new JTextField();
    private final JTree tree;
    private JMenuItem aboutMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem benchmarkMenuItem;
    private JFrame mainFrame;
    private JPanel headerPanel;
    private JPanel footerPanel;
    private JLabel headerTitleLabel;
    private JLabel versionLabel;
    private ArberChartPanel currentChartPanel;
    private final boolean vectorAvailable = isVectorAvailable();
    private final StringBuilder speedSearch = new StringBuilder();
    private Timer speedSearchTimer;
    private Timer metricsTimer;
    private String currentRendererKey;
    private String currentThemeName = "dark";
    private final AtomicLong totalRenderTimeNs = new AtomicLong(0);
    private final AtomicLong renderCount = new AtomicLong(0);

    /**
     * Creates a new demo application instance.
     *
     * @param initialTheme the initial theme name (dark or light)
     */
    public DemoApplication(String initialTheme) {
        this.currentThemeName = initialTheme;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Renderers");
        Map<String, DefaultMutableTreeNode> categories = new HashMap<>();
        for (RendererCatalogEntry entry : catalog.entries()) {
            categories.computeIfAbsent(entry.category(), key -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
                root.add(node);
                return node;
            }).add(new DefaultMutableTreeNode(entry));
            byName.put(entry.simpleName().toLowerCase(Locale.US), entry);
        }
        tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setCellRenderer(new RendererTreeCell());
        tree.addTreeSelectionListener(new RendererSelectionListener());
        installSpeedSearch();
    }

    private void show() {
        mainFrame = new JFrame(APP_NAME);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(1280, 820));
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setJMenuBar(buildMenuBar(mainFrame));

        JPanel header = buildHeader();
        mainFrame.add(header, BorderLayout.NORTH);

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());

        detailHost.add(buildEmptyDetail(), "empty");
        detailLayout.show(detailHost, "empty");
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailHost);
        split.setDividerLocation(280);
        split.setResizeWeight(0.15);
        split.setDividerSize(1);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        mainFrame.add(split, BorderLayout.CENTER);
        mainFrame.add(buildFooter(), BorderLayout.SOUTH);

        configurePlatformWindow(mainFrame);
        configureMacOSMenuHandlers(mainFrame);
        setupKeyboardShortcuts();
        startMetricsTimer();

        expandAll();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        if (!vectorAvailable) {
            showVectorWarning();
        }
        if (catalog.entries().isEmpty()) {
            statusLabel.setText("Renderer catalog is empty.");
        }

        updateStatus("Ready - " + catalog.entries().size() + " renderers available");
        applyDemoPalette();
    }

    /**
     * Sets up keyboard shortcuts for the application.
     */
    private void setupKeyboardShortcuts() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Cmd/Ctrl+E - Export
        mainFrame.getRootPane().registerKeyboardAction(
            e -> exportCurrentChart(),
            KeyStroke.getKeyStroke(KeyEvent.VK_E, mask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Cmd/Ctrl+R - Reload
        mainFrame.getRootPane().registerKeyboardAction(
            e -> refreshCurrent(),
            KeyStroke.getKeyStroke(KeyEvent.VK_R, mask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Cmd/Ctrl+T - Toggle Theme
        mainFrame.getRootPane().registerKeyboardAction(
            e -> toggleTheme(),
            KeyStroke.getKeyStroke(KeyEvent.VK_T, mask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Cmd/Ctrl+F - Focus Search
        mainFrame.getRootPane().registerKeyboardAction(
            e -> showSearchEverywhereDialog(),
            KeyStroke.getKeyStroke(KeyEvent.VK_F, mask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Cmd/Ctrl+B - Benchmark
        mainFrame.getRootPane().registerKeyboardAction(
            e -> runBenchmark(),
            KeyStroke.getKeyStroke(KeyEvent.VK_B, mask),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /**
     * Starts the metrics update timer.
     */
    private void startMetricsTimer() {
        metricsTimer = new Timer(1000, e -> updateMetrics());
        metricsTimer.start();
    }

    /**
     * Updates the metrics display in the footer.
     */
    private void updateMetrics() {
        long count = renderCount.get();
        long totalNs = totalRenderTimeNs.get();
        if (count > 0) {
            double avgMs = (totalNs / count) / 1_000_000.0;
            metricsLabel.setText(String.format("Renders: %d | Avg: %.2fms", count, avgMs));
        }
    }

    private JPanel buildHeader() {
        DemoPalette palette = currentPalette();
        JPanel header = new JPanel(new BorderLayout());
        this.headerPanel = header;
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        header.setBackground(palette.windowBackground());

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setBackground(palette.windowBackground());
        if (SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported) {
            left.add(Box.createHorizontalStrut(70));
        }
        JLabel title = new JLabel("Renderer Gallery");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(palette.foreground());
        this.headerTitleLabel = title;
        left.add(title);
        left.add(Box.createHorizontalStrut(20));
        countLabel.setText(catalog.entries().size() + " renderers");
        countLabel.setForeground(palette.muted());
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 13f));
        left.add(countLabel);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setBackground(palette.windowBackground());
        JButton searchEverywhereButton = new JButton("Search Everywhere", new SearchEverywhereIcon(palette.muted()));
        searchEverywhereButton.setFont(searchEverywhereButton.getFont().deriveFont(12f));
        searchEverywhereButton.setToolTipText("Search renderer by name (⌘F)");
        searchEverywhereButton.addActionListener(evt -> showSearchEverywhereDialog());

        JButton themeSwitchButton = new JButton("Theme Switch", new ThemeSwitchIcon(palette.muted()));
        themeSwitchButton.setFont(themeSwitchButton.getFont().deriveFont(12f));
        themeSwitchButton.setToolTipText("Toggle dark/light theme (⌘T)");
        themeSwitchButton.addActionListener(evt -> toggleTheme());

        right.add(searchEverywhereButton);
        right.add(Box.createHorizontalStrut(10));
        right.add(themeSwitchButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, palette.border()));
        return header;
    }

    private JPanel buildFooter() {
        DemoPalette palette = currentPalette();
        JPanel footer = new JPanel(new BorderLayout());
        this.footerPanel = footer;
        footer.setBackground(palette.windowBackground());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, palette.border()));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setForeground(palette.muted());
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));

        metricsLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        metricsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        metricsLabel.setForeground(palette.muted());
        metricsLabel.setFont(metricsLabel.getFont().deriveFont(12f));
        metricsLabel.setText("Renders: 0 | Avg: 0.00ms");

        JLabel versionLabel = new JLabel("v" + VERSION);
        this.versionLabel = versionLabel;
        versionLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        versionLabel.setForeground(palette.softMuted());
        versionLabel.setFont(versionLabel.getFont().deriveFont(11f));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(statusLabel, BorderLayout.WEST);
        leftPanel.add(versionLabel, BorderLayout.EAST);

        footer.add(leftPanel, BorderLayout.WEST);
        footer.add(metricsLabel, BorderLayout.EAST);
        return footer;
    }

    private JMenuBar buildMenuBar(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // File menu
        JMenu fileMenu = new JMenu("File");

        exportMenuItem = new JMenuItem("Export Chart...");
        exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, mask));
        exportMenuItem.addActionListener(evt -> exportCurrentChart());

        JMenuItem exportPdfItem = new JMenuItem("Export as PDF...");
        exportPdfItem.addActionListener(evt -> exportCurrentChartAsPdf());

        JMenuItem exportSvgItem = new JMenuItem("Export as SVG...");
        exportSvgItem.addActionListener(evt -> exportCurrentChartAsSvg());

        fileMenu.add(exportMenuItem);
        fileMenu.add(exportPdfItem);
        fileMenu.add(exportSvgItem);
        fileMenu.addSeparator();

        exitMenuItem = new JMenuItem("Quit");
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask));
        exitMenuItem.addActionListener(evt -> frame.dispose());
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        // View menu
        JMenu viewMenu = new JMenu("View");

        JMenuItem reloadItem = new JMenuItem("Reload Renderer");
        reloadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));
        reloadItem.addActionListener(evt -> refreshCurrent());
        viewMenu.add(reloadItem);

        viewMenu.addSeparator();

        // Theme submenu
        JMenu themeMenu = new JMenu("Theme");
        for (String themeName : new String[]{"dark", "light"}) {
            JMenuItem themeItem = new JMenuItem(capitalize(themeName));
            themeItem.addActionListener(evt -> setThemeByName(themeName));
            themeMenu.add(themeItem);
        }
        viewMenu.add(themeMenu);

        JMenuItem toggleThemeItem = new JMenuItem("Toggle Dark/Light");
        toggleThemeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, mask));
        toggleThemeItem.addActionListener(evt -> toggleTheme());
        viewMenu.add(toggleThemeItem);

        menuBar.add(viewMenu);

        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");

        benchmarkMenuItem = new JMenuItem("Run Benchmark");
        benchmarkMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, mask));
        benchmarkMenuItem.addActionListener(evt -> runBenchmark());
        toolsMenu.add(benchmarkMenuItem);

        JMenuItem resetMetricsItem = new JMenuItem("Reset Metrics");
        resetMetricsItem.addActionListener(evt -> resetMetrics());
        toolsMenu.add(resetMetricsItem);

        toolsMenu.addSeparator();

        JMenuItem systemInfoItem = new JMenuItem("System Info...");
        systemInfoItem.addActionListener(evt -> showSystemInfo(frame));
        toolsMenu.add(systemInfoItem);

        menuBar.add(toolsMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");

        aboutMenuItem = new JMenuItem("About " + APP_NAME);
        aboutMenuItem.addActionListener(evt -> showAboutDialog(frame));
        helpMenu.add(aboutMenuItem);

        preferencesMenuItem = new JMenuItem("Preferences");
        preferencesMenuItem.addActionListener(evt -> updateStatus("Preferences not available."));
        helpMenu.add(preferencesMenuItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private void configureMacOSMenuHandlers(JFrame frame) {
        if (!SystemInfo.isMacOS) {
            return;
        }
        if (aboutMenuItem != null) {
            aboutMenuItem.setVisible(false);
        }
        if (preferencesMenuItem != null) {
            preferencesMenuItem.setVisible(false);
        }
        if (exitMenuItem != null) {
            exitMenuItem.setVisible(false);
        }
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(evt -> showAboutDialog(frame));
        }
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler(evt -> statusLabel.setText("Preferences not available."));
        }
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((evt, response) -> {
                response.performQuit();
            });
        }
    }

    private void showAboutDialog(JFrame frame) {
        JDialog dialog = new JDialog(frame, "About " + APP_NAME, true);
        dialog.setLayout(new BorderLayout());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel(APP_NAME);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel version = new JLabel("Version " + VERSION);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detail = new JLabel("Enterprise-grade charting framework for Java 25.");
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel features = new JPanel();
        features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
        features.setOpaque(false);
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        Color muted = DemoThemeSupport.uiColor("Component.grayForeground", "Label.disabledForeground");

        JLabel featuresHeader = new JLabel("Features:");
        featuresHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        featuresHeader.setForeground(muted);
        featuresHeader.setFont(featuresHeader.getFont().deriveFont(Font.BOLD));
        features.add(featuresHeader);
        features.add(Box.createVerticalStrut(4));

        JLabel f1 = new JLabel("• Financial: Candlestick, MACD, Ichimoku, Renko, ...");
        f1.setAlignmentX(Component.LEFT_ALIGNMENT);
        f1.setForeground(muted);
        features.add(f1);

        JLabel f2 = new JLabel("• Medical: ECG, EEG, Spirometry, Capnography, ...");
        f2.setAlignmentX(Component.LEFT_ALIGNMENT);
        f2.setForeground(muted);
        features.add(f2);

        JLabel f3 = new JLabel("• Statistical: BoxPlot, Violin, KDE, Q-Q Plot, ...");
        f3.setAlignmentX(Component.LEFT_ALIGNMENT);
        f3.setForeground(muted);
        features.add(f3);

        JLabel f4 = new JLabel("• Analysis: FFT, Wavelet, Correlation, Peak Detection, ...");
        f4.setAlignmentX(Component.LEFT_ALIGNMENT);
        f4.setForeground(muted);
        features.add(f4);

        JLabel f5 = new JLabel("• Specialized: Smith Chart, Ternary, Sankey, ...");
        f5.setAlignmentX(Component.LEFT_ALIGNMENT);
        f5.setForeground(muted);
        features.add(f5);

        JLabel java = new JLabel("Java: " + System.getProperty("java.version") +
                " (" + System.getProperty("java.vendor") + ")");
        java.setAlignmentX(Component.LEFT_ALIGNMENT);
        java.setForeground(muted);

        JLabel copyright = new JLabel(COPYRIGHT);
        copyright.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(version);
        content.add(Box.createVerticalStrut(12));
        content.add(detail);
        content.add(Box.createVerticalStrut(8));
        content.add(features);
        content.add(Box.createVerticalStrut(8));
        content.add(java);
        content.add(Box.createVerticalStrut(16));
        content.add(copyright);

        JButton close = new JButton("Close");
        close.addActionListener(evt -> dialog.dispose());
        JPanel footer = new JPanel();
        footer.add(close);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * Shows system information dialog.
     */
    private void showSystemInfo(JFrame frame) {
        String info = DemoSystemInfoFormatter.format(vectorAvailable, catalog.entries().size());
        JOptionPane.showMessageDialog(frame, info, "System Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Exports the current chart to PNG.
     */
    private void exportCurrentChart() {
        if (currentChartPanel == null) {
            updateStatus("No chart to export. Select a renderer first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Chart as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".png"));

        if (chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ChartExportService.exportPng(currentChartPanel, file);
                updateStatus("Exported to: " + file.getName());
            } catch (Exception ex) {
                updateStatus("Export failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(mainFrame, "Export failed: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exports the current chart to PDF.
     */
    private void exportCurrentChartAsPdf() {
        if (currentChartPanel == null) {
            updateStatus("No chart to export. Select a renderer first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Chart as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".pdf"));

        if (chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            try {
                ChartExportService.exportPdf(currentChartPanel, file);
                updateStatus("Exported PDF to: " + file.getName());
            } catch (Exception ex) {
                updateStatus("PDF export failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Exports the current chart to SVG.
     */
    private void exportCurrentChartAsSvg() {
        if (currentChartPanel == null) {
            updateStatus("No chart to export. Select a renderer first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Chart as SVG");
        chooser.setFileFilter(new FileNameExtensionFilter("SVG Images", "svg"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".svg"));

        if (chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".svg")) {
                file = new File(file.getAbsolutePath() + ".svg");
            }
            try {
                ChartExportService.exportSvg(currentChartPanel, file);
                updateStatus("Exported SVG to: " + file.getName());
            } catch (Exception ex) {
                updateStatus("SVG export failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Runs a performance benchmark on the current renderer.
     */
    private void runBenchmark() {
        if (currentChartPanel == null || currentRendererKey == null) {
            updateStatus("No renderer selected for benchmark.");
            return;
        }

        RendererCatalogEntry entry = catalog.getRequired(currentRendererKey);
        updateStatus("Running benchmark for " + entry.simpleName() + "...");
        DemoBenchmarkRunner.run(entry, currentChartPanel, this::updateStatus);
    }

    /**
     * Resets the metrics counters.
     */
    private void resetMetrics() {
        totalRenderTimeNs.set(0);
        renderCount.set(0);
        updateMetrics();
        updateStatus("Metrics reset");
    }

    /**
     * Toggles between dark and light theme.
     */
    private void toggleTheme() {
        if ("dark".equals(currentThemeName)) {
            setThemeByName("light");
        } else {
            setThemeByName("dark");
        }
    }

    /**
     * Sets the theme by name.
     *
     * @param themeName the theme name
     */
    private void setThemeByName(String themeName) {
        themeName = DemoThemeSupport.normalizeTheme(themeName);
        if (themeName.equals(currentThemeName)) {
            return;
        }
        currentThemeName = themeName;

        DemoThemeSupport.applyLookAndFeel(themeName);
        applyDemoPalette();

        DemoThemeSupport.clearAssetCache();
        ChartTheme theme = getActiveTheme();
        applyThemeToCharts(detailHost, theme);
        rebuildRendererPanelsForTheme();

        updateStatus("Theme: " + capitalize(themeName));
    }

    /**
     * Updates the status bar.
     *
     * @param message the status message
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param input the input string
     * @return the capitalized string
     */
    private static String capitalize(String input) {
        if (input == null || input.isBlank()) return "";
        if (input.length() == 1) return input.toUpperCase(Locale.US);
        return input.substring(0, 1).toUpperCase(Locale.US) + input.substring(1);
    }

    private void selectFromSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            return;
        }
        RendererCatalogEntry entry = byName.get(query.trim().toLowerCase(Locale.US));
        if (entry == null) {
            statusLabel.setText("No renderer named: " + query);
            return;
        }
        TreePath path = findTreePath(entry);
        if (path != null) {
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            statusLabel.setText("Selected " + entry.simpleName());
        }
    }

    private void showSearchEverywhereDialog() {
        String query = JOptionPane.showInputDialog(
                mainFrame,
                "Search renderer:",
                "Search Everywhere",
                JOptionPane.PLAIN_MESSAGE
        );
        if (query == null || query.isBlank()) {
            return;
        }
        searchField.setText(query.trim());
        selectFromSearch();
    }

    private TreePath findTreePath(RendererCatalogEntry entry) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode category = (DefaultMutableTreeNode) root.getChildAt(i);
            for (int j = 0; j < category.getChildCount(); j++) {
                DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) category.getChildAt(j);
                Object user = leaf.getUserObject();
                if (user instanceof RendererCatalogEntry item && item.className().equals(entry.className())) {
                    return new TreePath(new Object[]{root, category, leaf});
                }
            }
        }
        return null;
    }

    private TreePath findTreePathByPrefix(String prefix) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode category = (DefaultMutableTreeNode) root.getChildAt(i);
            for (int j = 0; j < category.getChildCount(); j++) {
                DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) category.getChildAt(j);
                Object user = leaf.getUserObject();
                if (user instanceof RendererCatalogEntry item) {
                    String name = item.simpleName().toLowerCase(Locale.US);
                    if (name.startsWith(prefix)) {
                        return new TreePath(new Object[]{root, category, leaf});
                    }
                }
            }
        }
        return null;
    }

    private void installSpeedSearch() {
        speedSearchTimer = new Timer(900, evt -> {
            speedSearch.setLength(0);
            statusLabel.setText("Ready");
        });
        speedSearchTimer.setRepeats(false);
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    speedSearch.setLength(0);
                    statusLabel.setText("Ready");
                    return;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.isAltDown() || e.isControlDown() || e.isMetaDown()) {
                    return;
                }
                char ch = e.getKeyChar();
                if (ch == '\b') {
                    if (speedSearch.length() > 0) {
                        speedSearch.deleteCharAt(speedSearch.length() - 1);
                        updateSpeedSearch();
                    }
                    return;
                }
                if (Character.isISOControl(ch)) {
                    return;
                }
                if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-' && ch != ' ') {
                    return;
                }
                speedSearch.append(Character.toLowerCase(ch));
                updateSpeedSearch();
            }
        });
    }

    private void updateSpeedSearch() {
        if (speedSearchTimer != null) {
            speedSearchTimer.restart();
        }
        String query = speedSearch.toString().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Ready");
            return;
        }
        TreePath path = findTreePathByPrefix(query.toLowerCase(Locale.US));
        if (path != null) {
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            statusLabel.setText("Search: " + query);
        } else {
            statusLabel.setText("No match: " + query);
        }
    }

    private void openRenderer(RendererCatalogEntry entry) {
        if (entry == null) return;
        String key = entry.className();
        JPanel panel = rendererPanels.get(key);
        if (panel == null) {
            panel = buildRendererPanel(entry);
            rendererPanels.put(key, panel);
            detailHost.add(panel, key);
        }
        detailLayout.show(detailHost, key);
        currentRendererKey = key;
    }

    private JPanel buildRendererPanel(RendererCatalogEntry entry) {
        DemoPalette palette = currentPalette();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(palette.contentBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header with renderer name and class
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel title = new JLabel(entry.simpleName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(palette.foreground());
        JLabel subtitle = new JLabel(entry.className());
        subtitle.setForeground(palette.muted());
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        // Chart container with proper spacing
        JPanel chartHost = new JPanel(new BorderLayout());
        chartHost.setBackground(palette.contentBackground());
        chartHost.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        chartHost.add(buildChart(entry), BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(chartHost, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChart(RendererCatalogEntry entry) {
        DemoPalette palette = currentPalette();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(palette.surfaceBackground());
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.border(), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12))
        );
        JLabel status = new JLabel("Rendering...", SwingConstants.CENTER);
        status.setForeground(palette.muted());
        status.setFont(status.getFont().deriveFont(13f));
        wrapper.add(status, BorderLayout.CENTER);

        Timer timer = new Timer(80, evt -> {
            ((Timer) evt.getSource()).stop();
            long startTime = System.nanoTime();
            try {
                if (!vectorAvailable) {
                    status.setText("<html>Vector API missing.<br/>Run with --add-modules jdk.incubator.vector</html>");
                    return;
                }
                String className = entry.className();
                ChartModel model = RendererDemoDataFactory.build(entry.category(), className);
                ChartRenderer renderer = instantiateRenderer(className);
                ArberChartPanel chartPanel = new ArberChartPanel(model, renderer);
                if ("standard".equals(entry.category())) {
                    java.util.List<ChartModel> series = RendererDemoDataFactory.standardSeries(3);
                    if (!series.isEmpty()) {
                        chartPanel.clearLayers();
                        ChartRenderer first = instantiateRenderer(className, true);
                        chartPanel.setLayer(series.getFirst(), first);
                        for (int i = 1; i < series.size(); i++) {
                            chartPanel.setLayer(series.get(i), instantiateRenderer(className, false));
                        }
                        chartPanel.setMultiColorEnabled(true);
                    }
                }
                chartPanel.setTheme(getActiveTheme());
                chartPanel.setPreferredSize(new Dimension(1100, 680));
                chartPanel.setMinimumSize(new Dimension(800, 500));
                configureChart(entry, chartPanel);
                wrapper.removeAll();
                wrapper.add(chartPanel, BorderLayout.CENTER);
                wrapper.revalidate();
                wrapper.repaint();

                // Track metrics
                long elapsed = System.nanoTime() - startTime;
                totalRenderTimeNs.addAndGet(elapsed);
                renderCount.incrementAndGet();

                // Store current chart panel for export
                currentChartPanel = chartPanel;

                updateStatus(String.format("Rendered %s in %.2fms",
                        entry.simpleName(), elapsed / 1_000_000.0));
            } catch (RuntimeException ex) {
                String message = buildErrorMessage(ex);
                status.setText("<html>" + message.replace("\n", "<br/>") + "</html>");
                updateStatus("Failed to render " + entry.simpleName());
                currentChartPanel = null;
            }
        });
        timer.setRepeats(false);
        timer.start();
        return wrapper;
    }

    private void configureChart(RendererCatalogEntry entry, ArberChartPanel panel) {
        // Configure grid based on category
        if ("medical".equals(entry.category())) {
            panel.setGridLayer(new MedicalGridLayer());
        } else {
            String simple = entry.simpleName();
            if ("SmithChartRenderer".equals(simple) || "VSWRCircleRenderer".equals(simple)) {
                panel.setGridLayer(new SmithChartGridLayer());
            }
        }

        // Configure axis for optimal presentation
        configureAxisForCategory(entry.category(), panel);

        // Enable crosshair and animations for optimal user experience
        panel.setAnimationsEnabled(true);
    }

    /**
     * Configures axis settings based on chart category for optimal visualization.
     */
    private void configureAxisForCategory(String category, ArberChartPanel panel) {
        com.arbergashi.charts.api.AxisConfig xCfg = new com.arbergashi.charts.api.AxisConfig();
        com.arbergashi.charts.api.AxisConfig yCfg = new com.arbergashi.charts.api.AxisConfig();

        // Default tick count for good readability
        xCfg.setRequestedTickCount(8);
        yCfg.setRequestedTickCount(6);

        switch (category) {
            case "financial" -> {
                xCfg.setRequestedTickCount(10);
                yCfg.setRequestedTickCount(8);
            }
            case "medical" -> {
                // Physical scale for medical charts
                double pixelsPerMm = resolvePixelsPerMm();
                if (Double.isFinite(pixelsPerMm) && pixelsPerMm > 0.0) {
                    xCfg.setUnitsPerPixel((1.0 / 25.0) / pixelsPerMm);
                    yCfg.setUnitsPerPixel((1.0 / 10.0) / pixelsPerMm);
                }
                xCfg.setRequestedTickCount(12);
                yCfg.setRequestedTickCount(8);
            }
            case "statistical" -> {
                xCfg.setRequestedTickCount(6);
                yCfg.setRequestedTickCount(8);
            }
            case "circular" -> {
                // Circular charts need less axis clutter
                xCfg.setRequestedTickCount(4);
                yCfg.setRequestedTickCount(4);
            }
            default -> {
                // Standard configuration
                xCfg.setRequestedTickCount(8);
                yCfg.setRequestedTickCount(6);
            }
        }

        panel.setXAxisConfig(xCfg);
        panel.setYAxisConfig(yCfg);
    }


    private double resolvePixelsPerMm() {
        try {
            int ppi = Toolkit.getDefaultToolkit().getScreenResolution();
            if (ppi <= 0) return Double.NaN;
            return ppi / 25.4;
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    private void refreshCurrent() {
        if (currentRendererKey == null || currentRendererKey.isBlank()) {
            return;
        }
        RendererCatalogEntry entry = catalog.getRequired(currentRendererKey);
        JPanel existing = rendererPanels.remove(currentRendererKey);
        if (existing != null) {
            detailHost.remove(existing);
        }
        JPanel panel = buildRendererPanel(entry);
        rendererPanels.put(currentRendererKey, panel);
        detailHost.add(panel, currentRendererKey);
        detailLayout.show(detailHost, currentRendererKey);
        detailHost.revalidate();
        detailHost.repaint();
        statusLabel.setText("Reloaded " + entry.simpleName());
    }

    private ChartTheme getActiveTheme() {
        return DemoThemeSupport.buildChartTheme(currentThemeName);
    }


    private void applyThemeToCharts(Container container, ChartTheme theme) {
        for (Component child : container.getComponents()) {
            if (child instanceof ArberChartPanel panel) {
                panel.setTheme(theme);
                panel.repaint();
            } else if (child instanceof Container nested) {
                applyThemeToCharts(nested, theme);
            }
        }
    }

    private JPanel buildEmptyDetail() {
        DemoPalette palette = currentPalette();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(palette.contentBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel icon = new JLabel("📊");
        icon.setFont(icon.getFont().deriveFont(48f));
        icon.setForeground(palette.softMuted());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("Select a renderer from the left panel");
        label.setForeground(palette.muted());
        label.setFont(label.getFont().deriveFont(16f));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Or use ⌘F to search by name");
        hint.setForeground(palette.softMuted());
        hint.setFont(hint.getFont().deriveFont(13f));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(icon);
        center.add(Box.createVerticalStrut(16));
        center.add(label);
        center.add(Box.createVerticalStrut(8));
        center.add(hint);
        center.add(Box.createVerticalGlue());

        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private ChartRenderer instantiateRenderer(String className) {
        return instantiateRenderer(className, true);
    }

    private ChartRenderer instantiateRenderer(String className, boolean resetRegistry) {
        try {
            if (resetRegistry) {
                RendererRegistryReset.reset();
            }
            ChartRenderer special = buildSpecialRenderer(className);
            if (special != null) {
                enableMultiColor(special);
                return special;
            }
            String resolved = resolveRendererClass(className);
            Class<?> type = Class.forName(resolved);
            if (!ChartRenderer.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Not a ChartRenderer: " + resolved);
            }
            ChartRenderer renderer = (ChartRenderer) type.getDeclaredConstructor().newInstance();
            enableMultiColor(renderer);
            return renderer;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to construct renderer: " + className, ex);
        }
    }

    private void enableMultiColor(ChartRenderer renderer) {
        if (renderer instanceof com.arbergashi.charts.render.BaseRenderer base) {
            base.setMultiColor(true);
        }
    }

    private ChartRenderer buildSpecialRenderer(String className) {
        if (isSpecializedVectorField(className)) {
            className = "com.arbergashi.charts.render.analysis.VectorFieldRenderer";
        }
        return switch (className) {
            case "com.arbergashi.charts.render.specialized.CandlestickHollowRenderer" ->
                    new com.arbergashi.charts.render.financial.CandlestickHollowRenderer();
            case "com.arbergashi.charts.render.financial.PredictiveCandleRenderer" ->
                    new PredictiveCandleRenderer(new PredictiveShadowRenderer());
            case "com.arbergashi.charts.render.predictive.AnomalyGapRenderer" ->
                    new AnomalyGapRenderer(new PredictiveShadowRenderer());
            case "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer" ->
                    new AdaptiveFunctionRenderer(x -> Math.sin(x * 0.12) * 32 + Math.cos(x * 0.05) * 18);
            case "com.arbergashi.charts.render.analysis.VectorFieldRenderer" ->
                    new VectorFieldRenderer((x, y, out) -> {
                        out[0] = Math.cos(x * 0.08) * 0.8;
                        out[1] = Math.sin(y * 0.08) * 0.8;
                        return true;
                    });
            case "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer" ->
                    new CircularLatencyOverlayRenderer(sampleLatencyTracker());
            case "com.arbergashi.charts.render.forensic.PlaybackStatusRenderer" ->
                    new PlaybackStatusRenderer(new DemoPlaybackController());
            case "com.arbergashi.charts.render.common.PerformanceAuditRenderer" ->
                    new PerformanceAuditRendererAdapter(sampleLatencyTracker());
            case "com.arbergashi.charts.render.common.PhysicalScaleRenderer" ->
                    new PhysicalScaleRendererAdapter();
            default -> null;
        };
    }

    private static String resolveRendererClass(String className) {
        if (isSpecializedVectorField(className)) {
            return "com.arbergashi.charts.render.analysis.VectorFieldRenderer";
        }
        if ("com.arbergashi.charts.render.specialized.CandlestickHollowRenderer".equals(className)) {
            return "com.arbergashi.charts.render.financial.CandlestickHollowRenderer";
        }
        return className;
    }

    private static boolean isSpecializedVectorField(String className) {
        return "com.arbergashi.charts.render.specialized.VectorFieldRenderer".equals(className);
    }

    private static LatencyTracker sampleLatencyTracker() {
        LatencyTracker tracker = new LatencyTracker(256);
        for (int i = 0; i < 200; i++) {
            tracker.record(400_000 + (i % 12) * 35_000L);
        }
        return tracker;
    }

    private static void configurePlatformDefaults() {
        if (!SystemInfo.isMacOS) {
            return;
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "ArberCharts Demo");
        System.setProperty("apple.awt.application.appearance", "system");
    }

    private static void configurePlatformWindow(JFrame frame) {
        if (SystemInfo.isMacOS) {
            if (SystemInfo.isMacFullWindowContentSupported) {
                frame.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
                frame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
                if (SystemInfo.isJava_17_orLater) {
                    frame.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
                } else {
                    frame.setTitle(null);
                }
            }
            if (!SystemInfo.isJava_11_orLater) {
                frame.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            }
            return;
        }

        // Cross-platform FlatLaf title bar hints (Windows/Linux).
        frame.getRootPane().putClientProperty("JRootPane.titleBarShowIcon", Boolean.TRUE);
        frame.getRootPane().putClientProperty("JRootPane.titleBarShowTitle", Boolean.TRUE);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground",
                DemoThemeSupport.uiColor("TitlePane.background", "Panel.background"));
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground",
                DemoThemeSupport.uiColor("TitlePane.foreground", "Label.foreground"));
    }

    private void rebuildRendererPanelsForTheme() {
        String selectedKey = currentRendererKey;
        detailHost.removeAll();
        rendererPanels.clear();
        detailHost.add(buildEmptyDetail(), "empty");
        detailLayout.show(detailHost, "empty");
        if (selectedKey != null && !selectedKey.isBlank()) {
            RendererCatalogEntry selected = catalog.getRequired(selectedKey);
            openRenderer(selected);
        }
        detailHost.revalidate();
        detailHost.repaint();
    }

    private void applyDemoPalette() {
        DemoPalette palette = currentPalette();
        if (headerPanel != null) {
            headerPanel.setBackground(palette.windowBackground());
            headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, palette.border()));
        }
        if (footerPanel != null) {
            footerPanel.setBackground(palette.windowBackground());
            footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, palette.border()));
        }
        if (headerTitleLabel != null) {
            headerTitleLabel.setForeground(palette.foreground());
        }
        if (versionLabel != null) {
            versionLabel.setForeground(palette.softMuted());
        }
        countLabel.setForeground(palette.muted());
        statusLabel.setForeground(palette.muted());
        metricsLabel.setForeground(palette.muted());
        tree.setBackground(palette.sidebarBackground());
        tree.setForeground(palette.foreground());
        detailHost.setBackground(palette.contentBackground());
    }

    private DemoPalette currentPalette() {
        return DemoThemeSupport.currentPalette();
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void showVectorWarning() {
        statusLabel.setText("Vector API missing: run with --add-modules jdk.incubator.vector");
    }

    private static String buildErrorMessage(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        boolean vectorMissing = false;
        while (current != null && depth < 5) {
            if (depth > 0) {
                sb.append("\nCaused by: ");
            }
            String msg = current.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = current.getClass().getSimpleName();
            }
            sb.append(msg);
            if (current instanceof NoClassDefFoundError && msg.contains("jdk/incubator/vector")) {
                vectorMissing = true;
            }
            if (msg.contains("VectorIntrinsics")) {
                vectorMissing = true;
            }
            current = current.getCause();
            depth++;
        }
        if (vectorMissing) {
            sb.append("\nHint: enable Vector API with --add-modules jdk.incubator.vector");
        }
        return sb.toString();
    }

    private static boolean isVectorAvailable() {
        try {
            Class.forName("jdk.incubator.vector.DoubleVector");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private final class RendererSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object user = node.getUserObject();
            if (user instanceof RendererCatalogEntry entry) {
                openRenderer(entry);
            }
        }
    }

    private static final class RendererTreeCell extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object user = node.getUserObject();

            // Apply consistent font sizing
            Font baseFont = tree.getFont();
            if (baseFont == null) baseFont = comp.getFont();

            if (user instanceof RendererCatalogEntry entry) {
                setText(entry.simpleName());
                setToolTipText(entry.className());
                setFont(baseFont.deriveFont(Font.PLAIN, 13f));
            } else if (user instanceof String label) {
                setText(capitalize(label) + " (" + node.getChildCount() + ")");
                setToolTipText(null);
                setFont(baseFont.deriveFont(Font.BOLD, 13f));
                if (!sel) {
                    Color categoryColor = UIManager.getColor("Component.grayForeground");
                    if (categoryColor == null) {
                        categoryColor = tree.getForeground();
                    }
                    setForeground(categoryColor);
                }
            }
            return comp;
        }

        private String capitalize(String input) {
            if (input == null || input.isBlank()) return "";
            if (input.length() == 1) return input.toUpperCase(Locale.US);
            return input.substring(0, 1).toUpperCase(Locale.US) + input.substring(1);
        }
    }

    private static final class SearchEverywhereIcon implements Icon {
        private final Color color;

        private SearchEverywhereIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(x + 1, y + 1, 9, 9);
                g2.drawLine(x + 9, y + 9, x + 13, y + 13);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    private static final class ThemeSwitchIcon implements Icon {
        private final Color color;

        private ThemeSwitchIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(x + 1, y + 1, 12, 12);
                g2.fillArc(x + 2, y + 2, 10, 10, 90, 180);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    private static final class DemoPlaybackController implements com.arbergashi.charts.api.forensic.PlaybackController {
        private boolean active = true;
        private boolean deterministic = true;
        private double speed = 1.0;
        private long lastTick = System.nanoTime();
        private final LatencyTracker tracker = sampleLatencyTracker();

        @Override
        public com.arbergashi.charts.api.forensic.PlaybackController setDeterministic(boolean enabled) {
            deterministic = enabled;
            return this;
        }

        @Override
        public boolean isDeterministic() {
            return deterministic;
        }

        @Override
        public com.arbergashi.charts.api.forensic.PlaybackController setSpeed(double speed) {
            this.speed = speed;
            return this;
        }

        @Override
        public double getSpeed() {
            return speed;
        }

        @Override
        public void reset(long firstTimestampNanos) {
            lastTick = firstTimestampNanos;
            active = true;
        }

        @Override
        public void stepTo(long tickTimestampNanos) {
            lastTick = tickTimestampNanos;
        }

        @Override
        public void advanceByNanos(long deltaNanos) {
            lastTick += deltaNanos;
        }

        @Override
        public boolean isPlaybackActive() {
            return active;
        }

        @Override
        public long getLastTickNanos() {
            return lastTick;
        }

        @Override
        public long getPlaybackElapsedNanos() {
            return 2_000_000_000L;
        }

        @Override
        public long getResolvedNowNanos(long fallbackNowNanos) {
            return lastTick > 0 ? lastTick : fallbackNowNanos;
        }

        @Override
        public void appendStatus(StringBuilder sb) {
            sb.append("PLAYBACK 1.0x");
        }

        @Override
        public LatencyTracker getLatencyTracker() {
            return tracker;
        }
    }
}
