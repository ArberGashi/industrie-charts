package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
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
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
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
        configureMacOSDefaults();
        SwingUtilities.invokeLater(() -> {
            String theme = setupLookAndFeel();
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
    private final JComboBox<String> themeSelector = new JComboBox<>();
    private final JTree tree;
    private JMenuItem aboutMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem benchmarkMenuItem;
    private JFrame mainFrame;
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

        configureMacOSWindow(mainFrame);
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
            e -> searchField.requestFocusInWindow(),
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
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        if (SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported) {
            left.add(Box.createHorizontalStrut(70));
        }
        JLabel title = new JLabel("Renderer Gallery");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        left.add(title);
        left.add(Box.createHorizontalStrut(20));
        countLabel.setText(catalog.entries().size() + " renderers");
        countLabel.setForeground(new Color(110, 118, 128));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 13f));
        left.add(countLabel);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        searchField.setMaximumSize(new Dimension(280, 32));
        searchField.setPreferredSize(new Dimension(280, 32));
        searchField.setToolTipText("Search renderer by name (⌘F)");
        searchField.setFont(searchField.getFont().deriveFont(13f));
        JButton findButton = new JButton("Find");
        findButton.setFont(findButton.getFont().deriveFont(12f));
        findButton.addActionListener(evt -> selectFromSearch());
        JButton reloadButton = new JButton("Reload");
        reloadButton.setFont(reloadButton.getFont().deriveFont(12f));
        reloadButton.setToolTipText("Reload current renderer (⌘R)");
        reloadButton.addActionListener(evt -> refreshCurrent());
        JButton exportButton = new JButton("Export");
        exportButton.setFont(exportButton.getFont().deriveFont(12f));
        exportButton.setToolTipText("Export current chart (⌘E)");
        exportButton.addActionListener(evt -> exportCurrentChart());
        JButton benchmarkButton = new JButton("Benchmark");
        benchmarkButton.setFont(benchmarkButton.getFont().deriveFont(12f));
        benchmarkButton.setToolTipText("Run performance benchmark (⌘B)");
        benchmarkButton.addActionListener(evt -> runBenchmark());

        // Theme selector with all available themes
        for (String themeName : ChartThemes.getAvailableThemes()) {
            themeSelector.addItem(capitalize(themeName));
        }
        themeSelector.setSelectedItem(capitalize(currentThemeName));
        themeSelector.setMaximumSize(new Dimension(130, 30));
        themeSelector.setToolTipText("Select chart theme (⌘T to toggle)");
        themeSelector.addActionListener(evt -> {
            String selected = (String) themeSelector.getSelectedItem();
            if (selected != null) {
                setThemeByName(selected.toLowerCase(Locale.US));
            }
        });

        right.add(new JLabel("Search: "));
        right.add(searchField);
        right.add(Box.createHorizontalStrut(8));
        right.add(findButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(reloadButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(exportButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(benchmarkButton);
        right.add(Box.createHorizontalStrut(12));
        right.add(new JLabel("Theme: "));
        right.add(themeSelector);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 232)));
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 214, 222)));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setForeground(new Color(106, 114, 124));
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));

        metricsLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        metricsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        metricsLabel.setForeground(new Color(106, 114, 124));
        metricsLabel.setFont(metricsLabel.getFont().deriveFont(12f));
        metricsLabel.setText("Renders: 0 | Avg: 0.00ms");

        JLabel versionLabel = new JLabel("v" + VERSION);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        versionLabel.setForeground(new Color(150, 158, 168));
        versionLabel.setFont(versionLabel.getFont().deriveFont(11f));

        JPanel leftPanel = new JPanel(new BorderLayout());
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
        for (String themeName : ChartThemes.getAvailableThemes()) {
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

        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts...");
        shortcutsItem.addActionListener(evt -> showShortcutsDialog(frame));
        helpMenu.add(shortcutsItem);

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

        JLabel detail = new JLabel("<html>Enterprise-grade charting framework for Java 25.<br/>" +
                "158 production renderers • Zero-GC rendering • Medical-grade</html>");
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel features = new JLabel("<html><br/><b>Features:</b><br/>" +
                "• Financial: Candlestick, MACD, Ichimoku, Renko, ...<br/>" +
                "• Medical: ECG, EEG, Spirometry, Capnography, ...<br/>" +
                "• Statistical: BoxPlot, Violin, KDE, Q-Q Plot, ...<br/>" +
                "• Analysis: FFT, Wavelet, Correlation, Peak Detection, ...<br/>" +
                "• Specialized: Smith Chart, Ternary, Sankey, ...</html>");
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        features.setForeground(new Color(100, 108, 118));

        JLabel java = new JLabel("<html><br/>Java: " + System.getProperty("java.version") +
                " (" + System.getProperty("java.vendor") + ")</html>");
        java.setAlignmentX(Component.LEFT_ALIGNMENT);
        java.setForeground(new Color(130, 138, 148));

        JLabel copyright = new JLabel(COPYRIGHT);
        copyright.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(version);
        content.add(Box.createVerticalStrut(12));
        content.add(detail);
        content.add(features);
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
     * Shows keyboard shortcuts dialog.
     */
    private void showShortcutsDialog(JFrame frame) {
        String modifier = SystemInfo.isMacOS ? "⌘" : "Ctrl+";
        String shortcuts = String.format("""
            <html>
            <h3>Keyboard Shortcuts</h3>
            <table cellpadding="4">
            <tr><td><b>%sE</b></td><td>Export chart as PNG</td></tr>
            <tr><td><b>%sR</b></td><td>Reload current renderer</td></tr>
            <tr><td><b>%sT</b></td><td>Toggle dark/light theme</td></tr>
            <tr><td><b>%sF</b></td><td>Focus search field</td></tr>
            <tr><td><b>%sB</b></td><td>Run benchmark</td></tr>
            <tr><td><b>%sQ</b></td><td>Quit application</td></tr>
            <tr><td><b>Escape</b></td><td>Clear speed search</td></tr>
            <tr><td><b>Type</b></td><td>Speed search in tree</td></tr>
            </table>
            </html>
            """, modifier, modifier, modifier, modifier, modifier, modifier);

        JOptionPane.showMessageDialog(frame, shortcuts, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows system information dialog.
     */
    private void showSystemInfo(JFrame frame) {
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        String info = String.format("""
            <html>
            <h3>System Information</h3>
            <table cellpadding="4">
            <tr><td><b>Java Version:</b></td><td>%s</td></tr>
            <tr><td><b>Java Vendor:</b></td><td>%s</td></tr>
            <tr><td><b>OS:</b></td><td>%s %s</td></tr>
            <tr><td><b>Architecture:</b></td><td>%s</td></tr>
            <tr><td><b>Processors:</b></td><td>%d</td></tr>
            <tr><td><b>Memory Used:</b></td><td>%d MB / %d MB</td></tr>
            <tr><td><b>Memory Max:</b></td><td>%d MB</td></tr>
            <tr><td><b>Vector API:</b></td><td>%s</td></tr>
            <tr><td><b>Renderers:</b></td><td>%d</td></tr>
            </table>
            </html>
            """,
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            runtime.availableProcessors(),
            usedMem, totalMem,
            maxMem,
            vectorAvailable ? "Available ✓" : "Not available ✗",
            catalog.entries().size()
        );

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

        // Run benchmark in background
        new Thread(() -> {
            try {
                int iterations = 100;
                long totalNs = 0;
                long minNs = Long.MAX_VALUE;
                long maxNs = 0;

                // Warm up
                for (int i = 0; i < 10; i++) {
                    currentChartPanel.repaint();
                    currentChartPanel.paintImmediately(0, 0,
                            currentChartPanel.getWidth(), currentChartPanel.getHeight());
                }

                // Measure
                for (int i = 0; i < iterations; i++) {
                    long start = System.nanoTime();
                    currentChartPanel.paintImmediately(0, 0,
                            currentChartPanel.getWidth(), currentChartPanel.getHeight());
                    long elapsed = System.nanoTime() - start;
                    totalNs += elapsed;
                    minNs = Math.min(minNs, elapsed);
                    maxNs = Math.max(maxNs, elapsed);
                }

                double avgMs = (totalNs / iterations) / 1_000_000.0;
                double minMs = minNs / 1_000_000.0;
                double maxMs = maxNs / 1_000_000.0;

                String result = String.format(
                        "Benchmark: %s | %d iterations | Avg: %.2fms | Min: %.2fms | Max: %.2fms",
                        entry.simpleName(), iterations, avgMs, minMs, maxMs
                );

                SwingUtilities.invokeLater(() -> {
                    updateStatus(result);
                    JOptionPane.showMessageDialog(mainFrame,
                            String.format("""
                                    <html>
                                    <h3>Benchmark Results</h3>
                                    <table cellpadding="4">
                                    <tr><td><b>Renderer:</b></td><td>%s</td></tr>
                                    <tr><td><b>Iterations:</b></td><td>%d</td></tr>
                                    <tr><td><b>Average:</b></td><td>%.2f ms</td></tr>
                                    <tr><td><b>Minimum:</b></td><td>%.2f ms</td></tr>
                                    <tr><td><b>Maximum:</b></td><td>%.2f ms</td></tr>
                                    <tr><td><b>FPS (avg):</b></td><td>%.0f</td></tr>
                                    </table>
                                    </html>
                                    """,
                                    entry.simpleName(), iterations, avgMs, minMs, maxMs, 1000.0 / avgMs),
                            "Benchmark Results",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        updateStatus("Benchmark failed: " + ex.getMessage()));
            }
        }).start();
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
        if (themeName.equals(currentThemeName)) {
            return;
        }
        currentThemeName = themeName;

        // Update Look and Feel for dark/light base
        if ("light".equals(themeName) || "solarized-light".equals(themeName)) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }

        FlatLaf.updateUI();

        // Update chart themes
        ChartTheme theme = ChartThemes.getTheme(themeName);
        applyThemeToCharts(detailHost, theme);

        // Update selector
        themeSelector.setSelectedItem(capitalize(themeName));

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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header with renderer name and class
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel title = new JLabel(entry.simpleName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel(entry.className());
        subtitle.setForeground(new Color(110, 118, 128));
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        // Chart container with proper spacing
        JPanel chartHost = new JPanel(new BorderLayout());
        chartHost.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Metadata tiles
        JPanel meta = new JPanel(new GridLayout(1, 4, 16, 0));
        meta.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        meta.add(infoTile("Category", capitalize(entry.category())));
        meta.add(infoTile("Theme", capitalize(currentThemeName)));
        meta.add(infoTile("Vector API", vectorAvailable ? "Enabled" : "Disabled"));
        meta.add(infoTile("Source", "Catalog"));

        chartHost.add(buildChart(entry), BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(chartHost, BorderLayout.CENTER);
        panel.add(meta, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildChart(RendererCatalogEntry entry) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 214, 222), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12))
        );
        JLabel status = new JLabel("Rendering...", SwingConstants.CENTER);
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

    private JPanel infoTile(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel top = new JLabel(label);
        top.setForeground(new Color(110, 118, 128));
        top.setFont(top.getFont().deriveFont(11f));
        JLabel bottom = new JLabel(value);
        bottom.setFont(bottom.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(top, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 214, 222), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12))
        );
        return panel;
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
        return ChartThemes.getTheme(currentThemeName);
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("📊");
        icon.setFont(icon.getFont().deriveFont(48f));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("Select a renderer from the left panel");
        label.setForeground(new Color(110, 118, 128));
        label.setFont(label.getFont().deriveFont(16f));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Or use ⌘F to search by name");
        hint.setForeground(new Color(140, 148, 158));
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

    private static String setupLookAndFeel() {
        // Install Inter font from FlatLaf fonts package BEFORE L&F setup
        try {
            Class<?> interFontClass = Class.forName("com.formdev.flatlaf.fonts.inter.FlatInterFont");
            java.lang.reflect.Method installMethod = interFontClass.getMethod("installLazy");
            installMethod.invoke(null);
        } catch (Exception e) {
            // Inter font not available - will use system fonts
        }

        // Register custom theme properties BEFORE setup
        FlatLaf.registerCustomDefaultsSource("themes");

        String theme = System.getProperty("demo.theme", "dark").toLowerCase(Locale.US);
        if ("light".equals(theme)) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
            theme = "dark";
        }

        // Set Inter as the default font for chart elements
        java.awt.Font interFont = com.arbergashi.charts.platform.swing.util.ChartFonts.getBaseFont();
        UIManager.put("Chart.font", interFont.deriveFont(java.awt.Font.PLAIN, 11f));
        UIManager.put("defaultFont", interFont);

        configureAssets();
        return theme;
    }

    private static void configureMacOSDefaults() {
        if (!SystemInfo.isMacOS) {
            return;
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "ArberCharts Demo");
        System.setProperty("apple.awt.application.appearance", "system");
    }

    private static void configureMacOSWindow(JFrame frame) {
        if (!SystemInfo.isMacOS) {
            return;
        }
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
    }

    private static void configureAssets() {
        // Performance & audit
        ChartAssets.setProperty("Chart.performance.audit.enabled", "true");
        ChartAssets.setProperty("Chart.circular.performance.enabled", "true");
        ChartAssets.setProperty("Chart.playback.status.enabled", "true");
        ChartAssets.setProperty("Chart.calibration.enabled", "true");
        ChartAssets.setProperty("Chart.calibration.pixelsPerMm", "3.78");
        ChartAssets.setProperty("Chart.scale.physical.enabled", "true");
        ChartAssets.setProperty("Chart.scale.pixelsPerMm", "3.78");
        ChartAssets.setProperty("Chart.audit.enabled", "true");

        // Predictive features
        ChartAssets.setProperty("Chart.predictive.enabled", "true");
        ChartAssets.setProperty("Chart.predictive.anomaly.enabled", "true");
        ChartAssets.setProperty("Chart.predictive.global.lookahead", "28");
        ChartAssets.setProperty("Chart.predictive.lineAlpha", "0.32");

        // Analysis
        ChartAssets.setProperty("Chart.analysis.correlation.enabled", "true");
        ChartAssets.setProperty("Chart.analysis.correlation.window", "128");

        // Crosshair - compact sizing
        ChartAssets.setProperty("Chart.crosshair.label.fontScale", "0.1");
        ChartAssets.setProperty("Chart.crosshair.snap", "true");
        ChartAssets.setProperty("Chart.crosshair.snapDistance", "24");

        // Smith chart
        ChartAssets.setProperty("Chart.smith.vswr.enabled", "true");
        ChartAssets.setProperty("Chart.smith.vswr.levels", "1.5,2.0,3.0,5.0");

        // Reference lines
        ChartAssets.setProperty("chart.render.refline.y", "0");
        ChartAssets.setProperty("chart.render.refline.x", "120");

        // Axis labels - professional sizing (11pt base * 1.0 scale)
        ChartAssets.setProperty("Chart.axis.label.bold", "false");
        ChartAssets.setProperty("Chart.axis.label.fontScale", "1.0");

        // Medical grid - professional settings
        ChartAssets.setProperty("Chart.medicalGrid.stepXMinor", "0.04");
        ChartAssets.setProperty("Chart.medicalGrid.stepXMajor", "0.20");
        ChartAssets.setProperty("Chart.medicalGrid.stepYMinor", "0.10");
        ChartAssets.setProperty("Chart.medicalGrid.stepYMajor", "0.50");
        ChartAssets.setProperty("Chart.medicalGrid.minorAlpha", "0.18");
        ChartAssets.setProperty("Chart.medicalGrid.majorAlpha", "0.50");
        ChartAssets.setProperty("Chart.medicalGrid.minorStrokeWidth", "0.5");
        ChartAssets.setProperty("Chart.medicalGrid.majorStrokeWidth", "1.0");
        ChartAssets.setProperty("Chart.medicalGrid.centerLineAlpha", "0.45");
        ChartAssets.setProperty("Chart.medicalGrid.centerLineStrokeWidth", "1.2");

        // Legend - improved position
        ChartAssets.setProperty("Chart.legend.position", "TOP_RIGHT");
        ChartAssets.setProperty("Chart.legend.fontScale", "1.0");

        // Grid styling
        ChartAssets.setProperty("Chart.grid.minorAlpha", "0.15");
        ChartAssets.setProperty("Chart.grid.majorAlpha", "0.35");
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
        private static final Color CATEGORY_COLOR = new Color(80, 90, 100);

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
                    setForeground(CATEGORY_COLOR);
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
