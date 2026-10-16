package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.AnimationProfile;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import com.arbergashi.charts.render.financial.PredictiveCandleRenderer;
import com.arbergashi.charts.render.forensic.PlaybackStatusRenderer;
import com.arbergashi.charts.render.grid.MedicalGridLayer;
import com.arbergashi.charts.render.grid.AnalysisGridLayer;
import com.arbergashi.charts.render.grid.FinancialGridLayer;
import com.arbergashi.charts.render.grid.IsometricGridLayer;
import com.arbergashi.charts.render.grid.PolarGridLayer;
import com.arbergashi.charts.render.grid.SmithChartGridLayer;
import com.arbergashi.charts.render.grid.StatisticalGridLayer;
import com.arbergashi.charts.render.grid.TernaryGridLayer;
import com.arbergashi.charts.render.predictive.AnomalyGapRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.util.LatencyTracker;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
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
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Swing workbench for exploring the Industrie Charts renderer catalog.
 *
 * <p>This application demonstrates the capabilities of the Industrie Charts framework,
 * including all renderer categories, theme switching, export functionality, and
 * real-time data streaming. Built with modern Swing using FlatLaf themes.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Searchable renderer catalog with category navigation</li>
 *   <li>Modern Dark/Light themes with Inter font</li>
 *   <li>Real-time data streaming and updates</li>
 *   <li>Chart export to PNG and optional SVG/PDF backends</li>
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
 * @since 2.0.0
 */
public final class DemoApplication {

    /** Application version. */
    private static final String VERSION = resolveVersion();

    /** Application name for display. */
    private static final String APP_NAME = "Industrie Charts";

    /** Copyright notice. */
    private static final String COPYRIGHT = "© 2026 Arber Gashi";
    private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();

    /**
     * Main entry point for the demo application.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        if (ensureRuntimeFlags(args)) {
            return;
        }
        configurePlatformDefaults();
        DemoThemeSupport.bootstrapThemeResources();
        SwingUtilities.invokeLater(() -> {
            String theme = DemoThemeSupport.setupLookAndFeel();
            DemoApplication app = new DemoApplication(theme);
            app.show();
        });
    }

    /**
     * Ensures that required runtime flags are active when started as executable JAR.
     * If flags are missing, the process is re-launched once with proper arguments.
     *
     * @return true if current process should exit because a re-launch was started
     */
    private static boolean ensureRuntimeFlags(String[] args) {
        if (isVectorAvailable()) {
            return false;
        }

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> inputArgs = runtime.getInputArguments();
        boolean hasVectorModule = inputArgs.stream()
                .anyMatch(arg -> arg.contains("jdk.incubator.vector"));
        if (hasVectorModule) {
            return false;
        }

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classPath = System.getProperty("java.class.path", "");
        if (!classPath.endsWith(".jar")) {
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.addAll(inputArgs);
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("--add-modules");
        command.add("jdk.incubator.vector");
        command.add("-jar");
        command.add(classPath);
        for (String arg : args) {
            command.add(arg);
        }

        try {
            new ProcessBuilder(command).inheritIO().start();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private final RendererCatalog catalog = RendererCatalog.load();
    private final Map<String, JPanel> rendererPanels = new HashMap<>();
    private final Map<String, ArberChartPanel> chartPanels = new HashMap<>();
    private final CardLayout detailLayout = new CardLayout();
    private final JPanel detailHost = new JPanel(detailLayout);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel countLabel = new JLabel();
    private final JLabel metricsLabel = new JLabel();
    private final JTree tree;
    private JMenuItem aboutMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem benchmarkMenuItem;
    private JFrame mainFrame;
    private JPanel headerPanel;
    private JPanel footerPanel;
    private JPanel sidebarPanel;
    private JPanel editorToolbar;
    private JLabel activeRendererLabel;
    private JLabel categoryLabel;
    private JPanel catalogBody;
    private JTextField catalogFilter;
    private JButton themeSwitchButton;
    private JButton searchEverywhereButton;
    private JButton reloadButton;
    private JButton exportButton;
    private JLabel headerTitleLabel;
    private JLabel headerMarkLabel;
    private JLabel versionLabel;
    private JDialog searchEverywhereDialog;
    private JTextField searchEverywhereField;
    private JList<RendererCatalogEntry> searchEverywhereResults;
    private DefaultListModel<RendererCatalogEntry> searchEverywhereModel;
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
        Map<String, DefaultMutableTreeNode> categories = new LinkedHashMap<>();
        for (RendererCatalogEntry entry : catalog.entries()) {
            categories.computeIfAbsent(entry.category(), key -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
                root.add(node);
                return node;
            }).add(new DefaultMutableTreeNode(entry));
        }
        tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 6));
        tree.getAccessibleContext().setAccessibleName("Renderer catalog");
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new RendererTreeCell());
        tree.addTreeSelectionListener(new RendererSelectionListener());
        installSpeedSearch();
    }

    private void show() {
        mainFrame = new JFrame(APP_NAME);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                disposeDemoResources();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                disposeDemoResources();
            }
        });
        mainFrame.setMinimumSize(new Dimension(1000, 680));
        mainFrame.setSize(1360, 880);
        mainFrame.setJMenuBar(buildMenuBar(mainFrame));
        mainFrame.setContentPane(buildWorkspace());

        configurePlatformWindow(mainFrame);
        configureMacOSMenuHandlers(mainFrame);
        setupKeyboardShortcuts();
        startMetricsTimer();
        applyApplicationIcons(mainFrame);

        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        if (!vectorAvailable) {
            showVectorWarning();
        }
        if (catalog.entries().isEmpty()) {
            statusLabel.setText("Renderer catalog is empty.");
        }

    }

    /** Builds the same Swing content for the desktop window and offscreen visual checks. */
    JPanel buildWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.add(buildHeader(), BorderLayout.NORTH);
        detailHost.add(buildEmptyDetail(), "empty");
        detailLayout.show(detailHost, "empty");
        detailHost.setMinimumSize(new Dimension(420, 320));

        JPanel editor = new JPanel(new BorderLayout());
        editor.add(buildEditorToolbar(), BorderLayout.NORTH);
        editor.add(detailHost, BorderLayout.CENTER);
        editor.setMinimumSize(new Dimension(420, 320));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), editor);
        split.setDividerLocation(292);
        split.setResizeWeight(0.0);
        split.setDividerSize(1);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        workspace.add(split, BorderLayout.CENTER);
        workspace.add(buildFooter(), BorderLayout.SOUTH);

        catalog.entries().stream()
                .filter(entry -> "LineRenderer".equals(entry.simpleName()))
                .findFirst()
                .or(() -> catalog.entries().stream().findFirst())
                .ifPresent(this::selectRenderer);
        SwingUtilities.invokeLater(() -> tree.scrollRowToVisible(0));
        applyDemoPalette();
        return workspace;
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
        double avgMs = count == 0 ? 0.0 : (totalNs / count) / 1_000_000.0;
        metricsLabel.setText(String.format(Locale.ROOT, "Charts opened: %d   ·   Setup: %.1f ms", count, avgMs));
        metricsLabel.setToolTipText("Average chart construction time; excludes frame rendering");
    }

    private JPanel buildHeader() {
        DemoPalette palette = currentPalette();
        JPanel header = new JPanel(new BorderLayout(24, 0));
        this.headerPanel = header;
        header.setPreferredSize(new Dimension(0, 48));

        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.X_AXIS));
        identity.setOpaque(false);
        if (SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported) {
            identity.add(Box.createHorizontalStrut(76));
        }
        JLabel mark = new JLabel(loadTablerOutlineIcon("chart-line.svg", 20,
                UIManager.getIcon("FileView.fileIcon"), palette.accent()));
        headerMarkLabel = mark;
        mark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        headerTitleLabel = new JLabel(APP_NAME);
        headerTitleLabel.setFont(headerTitleLabel.getFont().deriveFont(Font.BOLD, 13f));
        identity.add(mark);
        identity.add(headerTitleLabel);
        JLabel context = new JLabel("  /  Swing demo");
        context.putClientProperty("demo.muted", true);
        identity.add(context);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setOpaque(false);
        searchEverywhereButton = new JButton("Find renderer");
        searchEverywhereButton.putClientProperty("JButton.buttonType", "toolBarButton");
        searchEverywhereButton.setToolTipText("Find renderer (" + shortcutLabel("F") + ")");
        searchEverywhereButton.addActionListener(evt -> showSearchEverywhereDialog());
        searchEverywhereButton.getAccessibleContext().setAccessibleName("Find renderer");
        actions.add(searchEverywhereButton);
        JLabel shortcut = new JLabel(shortcutLabel("F"));
        shortcut.putClientProperty("demo.muted", true);
        shortcut.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 16));
        shortcut.setFont(shortcut.getFont().deriveFont(11f));
        actions.add(shortcut);

        themeSwitchButton = createTitlebarIconButton(null);
        themeSwitchButton.setToolTipText("Toggle light or dark theme (" + shortcutLabel("T") + ")");
        themeSwitchButton.getAccessibleContext().setAccessibleName("Toggle light or dark theme");
        themeSwitchButton.addActionListener(evt -> toggleTheme());
        actions.add(themeSwitchButton);
        header.add(identity, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        installHeaderWindowGestures(header);
        return header;
    }

    private JButton createTitlebarIconButton(Icon icon) {
        JButton button = new JButton(icon);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("JComponent.minimumWidth", 30);
        button.setPreferredSize(new Dimension(32, 30));
        button.setMinimumSize(new Dimension(32, 30));
        button.setMaximumSize(new Dimension(32, 30));
        return button;
    }

    private JPanel buildSidebar() {
        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setMinimumSize(new Dimension(200, 200));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(13, 16, 10, 14));
        JLabel title = new JLabel("Renderer catalog");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        countLabel.setText(Integer.toString(catalog.entries().size()));
        countLabel.setFont(countLabel.getFont().deriveFont(12f));
        heading.add(title, BorderLayout.WEST);
        heading.add(countLabel, BorderLayout.EAST);

        catalogFilter = new JTextField();
        catalogFilter.putClientProperty("JTextField.placeholderText", "Filter by name or category");
        catalogFilter.putClientProperty("JTextField.showClearButton", true);
        catalogFilter.getAccessibleContext().setAccessibleName("Filter renderer catalog");
        catalogFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                filterCatalog();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                filterCatalog();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                filterCatalog();
            }
        });
        catalogFilter.addActionListener(event -> selectFirstVisibleRenderer());
        catalogFilter.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    catalogFilter.setText("");
                } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectFirstVisibleRenderer();
                    tree.requestFocusInWindow();
                }
            }
        });
        JPanel search = new JPanel(new BorderLayout());
        search.setOpaque(false);
        search.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        search.add(catalogFilter);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(search, BorderLayout.SOUTH);
        sidebarPanel.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        catalogBody = new JPanel(new CardLayout());
        catalogBody.setOpaque(false);
        catalogBody.add(scroll, "catalog");
        JLabel empty = new JLabel("No matching renderers", SwingConstants.CENTER);
        empty.putClientProperty("demo.muted", true);
        empty.setVerticalAlignment(SwingConstants.TOP);
        empty.setBorder(BorderFactory.createEmptyBorder(20, 8, 0, 8));
        catalogBody.add(empty, "empty");
        sidebarPanel.add(catalogBody, BorderLayout.CENTER);

        JLabel hint = new JLabel("Type to filter. Enter to open.");
        hint.putClientProperty("demo.muted", true);
        hint.setBorder(BorderFactory.createEmptyBorder(9, 16, 10, 12));
        hint.setFont(hint.getFont().deriveFont(11f));
        sidebarPanel.add(hint, BorderLayout.SOUTH);
        return sidebarPanel;
    }

    private void filterCatalog() {
        String query = catalogFilter.getText().strip().toLowerCase(Locale.ROOT);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Renderers");
        Map<String, DefaultMutableTreeNode> categories = new LinkedHashMap<>();
        int matches = 0;
        for (RendererCatalogEntry entry : catalog.entries()) {
            if (!matchesQuery(entry, query)) {
                continue;
            }
            categories.computeIfAbsent(entry.category(), category -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
                root.add(node);
                return node;
            }).add(new DefaultMutableTreeNode(entry));
            matches++;
        }
        tree.setModel(new DefaultTreeModel(root));
        countLabel.setText(query.isEmpty() ? Integer.toString(matches) : matches + " / " + catalog.entries().size());
        ((CardLayout) catalogBody.getLayout()).show(catalogBody, matches == 0 ? "empty" : "catalog");
        if (!query.isEmpty()) {
            expandAll();
        }
        if (currentRendererKey != null) {
            TreePath selected = findTreePath(catalog.getRequired(currentRendererKey));
            if (selected != null) {
                tree.setSelectionPath(selected);
                tree.scrollPathToVisible(selected);
            }
        }
    }

    private static boolean matchesQuery(RendererCatalogEntry entry, String query) {
        return query.isBlank()
                || entry.simpleName().toLowerCase(Locale.ROOT).contains(query)
                || entry.className().toLowerCase(Locale.ROOT).contains(query)
                || entry.category().toLowerCase(Locale.ROOT).contains(query);
    }

    private void selectFirstVisibleRenderer() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root.getChildCount() > 0) {
            DefaultMutableTreeNode category = (DefaultMutableTreeNode) root.getFirstChild();
            DefaultMutableTreeNode first = (DefaultMutableTreeNode) category.getFirstChild();
            tree.setSelectionPath(new TreePath(first.getPath()));
        }
    }

    private void selectRenderer(RendererCatalogEntry entry) {
        TreePath path = findTreePath(entry);
        if (path == null && catalogFilter != null) {
            catalogFilter.setText("");
            path = findTreePath(entry);
        }
        if (path != null) {
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        this.footerPanel = footer;
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        metricsLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
        metricsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        metricsLabel.setFont(metricsLabel.getFont().deriveFont(11f));
        updateMetrics();
        versionLabel = new JLabel(VERSION);
        versionLabel.setToolTipText("Demo artifact version");
        versionLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
        versionLabel.setFont(versionLabel.getFont().deriveFont(11f));
        JPanel details = new JPanel(new BorderLayout());
        details.setOpaque(false);
        details.add(metricsLabel, BorderLayout.WEST);
        details.add(versionLabel, BorderLayout.EAST);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(details, BorderLayout.EAST);
        return footer;
    }

    private static String shortcutLabel(String key) {
        return (SystemInfo.isMacOS ? "⌘" : "Ctrl+") + key;
    }

    private static String resolveVersion() {
        try (var input = DemoApplication.class.getResourceAsStream(
                "/META-INF/maven/com.arbergashi/arbercharts-demo/pom.properties")) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version;
                }
            }
        } catch (java.io.IOException exception) {
            System.getLogger(DemoApplication.class.getName()).log(System.Logger.Level.WARNING,
                    "Cannot read demo artifact version", exception);
        }
        String version = DemoApplication.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
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
        DemoPalette palette = currentPalette();
        Color muted = palette.muted();

        JDialog dialog = new JDialog(frame, "About " + APP_NAME, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(palette.border(), 1));
        configurePlatformDialog(dialog);
        int topInset = (SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported) ? 44 : 22;

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(topInset, 22, 14, 22));
        content.setBackground(palette.surfaceBackground());

        JLabel appTitle = new JLabel(APP_NAME);
        appTitle.setFont(appTitle.getFont().deriveFont(Font.BOLD, 20f));
        appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Enterprise charting demo for production-grade Swing applications");
        subtitle.setForeground(muted);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel version = new JLabel("Version " + VERSION);
        version.setForeground(muted);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel renderers = new JLabel("Renderers: " + catalog.entries().size());
        renderers.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel runtime = new JLabel("Java Runtime: " + System.getProperty("java.version") +
                " (" + System.getProperty("java.vendor") + ")");
        runtime.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel license = new JLabel("License: MIT");
        license.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel support = new JLabel("Support: Commercial support, closed-source product delivery");
        support.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel copyright = new JLabel(COPYRIGHT);
        copyright.setForeground(muted);
        copyright.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton close = new JButton("Close");
        close.setAlignmentX(Component.LEFT_ALIGNMENT);
        close.addActionListener(evt -> dialog.dispose());

        content.add(appTitle);
        content.add(Box.createVerticalStrut(6));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(8));
        content.add(version);
        content.add(Box.createVerticalStrut(14));
        content.add(renderers);
        content.add(Box.createVerticalStrut(5));
        content.add(runtime);
        content.add(Box.createVerticalStrut(5));
        content.add(license);
        content.add(Box.createVerticalStrut(5));
        content.add(support);
        content.add(Box.createVerticalStrut(16));
        content.add(copyright);
        content.add(Box.createVerticalStrut(12));
        content.add(close);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setSize(620, 338);
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

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Export Chart as PNG");
        chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("PNG Images", "png"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".png"));

        if (chooser.showSaveDialog(mainFrame) == SystemFileChooser.APPROVE_OPTION) {
            File file = ensureExtension(chooser.getSelectedFile(), ".png");
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

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Export Chart as PDF");
        chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("PDF Documents", "pdf"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".pdf"));

        if (chooser.showSaveDialog(mainFrame) == SystemFileChooser.APPROVE_OPTION) {
            File file = ensureExtension(chooser.getSelectedFile(), ".pdf");
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

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Export Chart as SVG");
        chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("SVG Images", "svg"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setSelectedFile(new File("chart_" + timestamp + ".svg"));

        if (chooser.showSaveDialog(mainFrame) == SystemFileChooser.APPROVE_OPTION) {
            File file = ensureExtension(chooser.getSelectedFile(), ".svg");
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

    private static File ensureExtension(File file, String extension) {
        if (file == null) {
            return null;
        }
        String lowerName = file.getName().toLowerCase(Locale.US);
        if (lowerName.endsWith(extension)) {
            return file;
        }
        return new File(file.getAbsolutePath() + extension);
    }

    private void showSearchEverywhereDialog() {
        ensureSearchEverywhereDialog();
        if (searchEverywhereDialog == null) {
            return;
        }
        searchEverywhereField.setText("");
        updateSearchEverywhereResults("");
        positionSearchEverywhereDialog();
        searchEverywhereDialog.setVisible(true);
        SwingUtilities.invokeLater(() -> searchEverywhereField.requestFocusInWindow());
    }

    private void ensureSearchEverywhereDialog() {
        if (searchEverywhereDialog != null || mainFrame == null) {
            return;
        }

        JDialog dialog = new JDialog(mainFrame, false);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(currentPalette().border(), 1));

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setBackground(currentPalette().surfaceBackground());

        JTextField field = new JTextField();
        field.putClientProperty("JTextField.placeholderText", "Search Everywhere");
        field.addActionListener(evt -> openSelectedFromSearchEverywhere());
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchEverywhereResults(field.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchEverywhereResults(field.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSearchEverywhereResults(field.getText());
            }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                    return;
                }
                if (searchEverywhereResults == null) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int index = searchEverywhereResults.getSelectedIndex();
                    if (index < searchEverywhereModel.getSize() - 1) {
                        searchEverywhereResults.setSelectedIndex(index + 1);
                        searchEverywhereResults.ensureIndexIsVisible(index + 1);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int index = searchEverywhereResults.getSelectedIndex();
                    if (index > 0) {
                        searchEverywhereResults.setSelectedIndex(index - 1);
                        searchEverywhereResults.ensureIndexIsVisible(index - 1);
                    }
                }
            }
        });

        DefaultListModel<RendererCatalogEntry> model = new DefaultListModel<>();
        JList<RendererCatalogEntry> results = new JList<>(model);
        results.setVisibleRowCount(12);
        results.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RendererCatalogEntry entry) {
                    cell.setText(entry.simpleName() + "  [" + entry.category() + "]");
                    Color iconTint = isSelected ? list.getSelectionForeground() : currentPalette().muted();
                    cell.setIcon(rendererIconForCategory(entry.category(), iconTint));
                }
                return cell;
            }
        });
        results.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedFromSearchEverywhere();
                }
            }
        });

        JScrollPane resultsScroll = new JScrollPane(results);
        resultsScroll.setBorder(BorderFactory.createEmptyBorder());

        content.add(field, BorderLayout.NORTH);
        content.add(resultsScroll, BorderLayout.CENTER);
        dialog.add(content, BorderLayout.CENTER);
        dialog.setSize(new Dimension(620, 420));

        this.searchEverywhereDialog = dialog;
        this.searchEverywhereField = field;
        this.searchEverywhereResults = results;
        this.searchEverywhereModel = model;
        updateSearchEverywhereResults("");
    }

    private void updateSearchEverywhereResults(String query) {
        if (searchEverywhereModel == null) {
            return;
        }
        searchEverywhereModel.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<RendererCatalogEntry> matches = catalog.entries().stream()
                .filter(entry -> matchesQuery(entry, normalized))
                .sorted((left, right) -> left.simpleName().compareToIgnoreCase(right.simpleName()))
                .limit(120)
                .toList();
        for (RendererCatalogEntry entry : matches) {
            searchEverywhereModel.addElement(entry);
        }
        if (!searchEverywhereModel.isEmpty() && searchEverywhereResults != null) {
            searchEverywhereResults.setSelectedIndex(0);
        }
    }

    private void openSelectedFromSearchEverywhere() {
        if (searchEverywhereResults == null) {
            return;
        }
        RendererCatalogEntry entry = searchEverywhereResults.getSelectedValue();
        if (entry == null) {
            return;
        }
        selectRenderer(entry);
        updateStatus("Selected " + entry.simpleName());
        if (searchEverywhereDialog != null) {
            searchEverywhereDialog.setVisible(false);
        }
    }

    private void positionSearchEverywhereDialog() {
        if (searchEverywhereDialog == null || mainFrame == null) {
            return;
        }
        int width = Math.min(700, Math.max(520, mainFrame.getWidth() / 2));
        int x = mainFrame.getX() + ((mainFrame.getWidth() - width) / 2);
        int y = mainFrame.getY() + 56;
        searchEverywhereDialog.setSize(new Dimension(width, 420));
        searchEverywhereDialog.setLocation(x, y);
    }

    private void installHeaderWindowGestures(JPanel header) {
        HeaderWindowMouseHandler handler = new HeaderWindowMouseHandler();
        header.addMouseListener(handler);
        header.addMouseMotionListener(handler);
    }

    private final class HeaderWindowMouseHandler extends MouseAdapter {
        private Point dragOffset;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            dragOffset = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragOffset = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mainFrame == null || dragOffset == null || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            if ((mainFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
                return;
            }
            Point screen = e.getLocationOnScreen();
            mainFrame.setLocation(screen.x - dragOffset.x, screen.y - dragOffset.y);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) {
                return;
            }
            toggleWindowMaximizeRestore();
        }
    }

    private void toggleWindowMaximizeRestore() {
        if (mainFrame == null) {
            return;
        }
        int state = mainFrame.getExtendedState();
        if ((state & Frame.MAXIMIZED_BOTH) != 0) {
            mainFrame.setExtendedState(state & ~Frame.MAXIMIZED_BOTH);
            return;
        }
        Rectangle screenBounds = mainFrame.getGraphicsConfiguration().getBounds();
        java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(mainFrame.getGraphicsConfiguration());
        mainFrame.setMaximizedBounds(new Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
        ));
        mainFrame.setExtendedState(state | Frame.MAXIMIZED_BOTH);
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

    private JPanel buildEditorToolbar() {
        editorToolbar = new JPanel(new BorderLayout(16, 0));
        editorToolbar.setPreferredSize(new Dimension(0, 48));
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.X_AXIS));
        identity.setOpaque(false);
        activeRendererLabel = new JLabel("Chart workspace");
        activeRendererLabel.setFont(activeRendererLabel.getFont().deriveFont(Font.BOLD, 13f));
        categoryLabel = new JLabel();
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(12f));
        identity.add(activeRendererLabel);
        identity.add(categoryLabel);
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setOpaque(false);
        reloadButton = new JButton("Reload");
        reloadButton.putClientProperty("JButton.buttonType", "toolBarButton");
        reloadButton.setToolTipText("Reload the selected chart (" + shortcutLabel("R") + ")");
        reloadButton.addActionListener(event -> refreshCurrent());
        exportButton = new JButton("Export PNG");
        exportButton.putClientProperty("JButton.buttonType", "toolBarButton");
        exportButton.setToolTipText("Export the selected chart (" + shortcutLabel("E") + ")");
        exportButton.addActionListener(event -> exportCurrentChart());
        actions.add(reloadButton);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(exportButton);
        editorToolbar.add(identity, BorderLayout.CENTER);
        editorToolbar.add(actions, BorderLayout.EAST);
        updateChartActions();
        return editorToolbar;
    }

    private void openRenderer(RendererCatalogEntry entry) {
        if (entry == null) return;
        String key = entry.className();
        currentRendererKey = key;
        currentChartPanel = chartPanels.get(key);
        JPanel panel = rendererPanels.get(key);
        if (panel == null) {
            panel = buildRendererPanel(entry);
            rendererPanels.put(key, panel);
            detailHost.add(panel, key);
        }
        detailLayout.show(detailHost, key);
        if (activeRendererLabel != null) {
            activeRendererLabel.setText(entry.simpleName());
            activeRendererLabel.setToolTipText(entry.className());
            activeRendererLabel.setIcon(rendererIconForCategory(entry.category(), currentPalette().accent()));
            activeRendererLabel.setIconTextGap(8);
            categoryLabel.setText(capitalize(entry.category()));
        }
        updateChartActions();
        updateStatus(currentChartPanel == null ? "Opening " + entry.simpleName() + "…" : "Ready — " + entry.simpleName());
    }

    private void updateChartActions() {
        if (reloadButton != null) {
            reloadButton.setEnabled(currentRendererKey != null);
        }
        if (exportButton != null) {
            exportButton.setEnabled(currentChartPanel != null);
        }
        if (exportMenuItem != null) {
            exportMenuItem.setEnabled(currentChartPanel != null);
        }
        if (benchmarkMenuItem != null) {
            benchmarkMenuItem.setEnabled(currentChartPanel != null);
        }
    }

    private JPanel buildRendererPanel(RendererCatalogEntry entry) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(currentPalette().contentBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(buildChart(entry), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChart(RendererCatalogEntry entry) {
        DemoPalette palette = currentPalette();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(palette.contentBackground());
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel status = new JLabel("Rendering...", SwingConstants.CENTER);
        status.setForeground(palette.muted());
        status.setFont(status.getFont().deriveFont(13f));
        wrapper.add(status, BorderLayout.CENTER);

        Timer timer = new Timer(80, evt -> {
            ((Timer) evt.getSource()).stop();
            // A theme switch or reload may remove this view before construction starts.
            if (!SwingUtilities.isDescendingFrom(wrapper, detailHost)) {
                return;
            }
            long startTime = System.nanoTime();
            ChartRenderer renderer = null;
            boolean installed = false;
            try {
                String className = entry.className();
                ChartModel model = RendererDemoDataFactory.build(entry.category(), className);
                if (model instanceof com.arbergashi.charts.model.DefaultFinancialChartModel financial) {
                    model = new ShowcaseFinancialModel(financial);
                }
                ChartModel animatedModel = model;
                renderer = instantiateRenderer(className);
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
                        chartPanel.resetZoom();
                        animatedModel = series.getFirst();
                    }
                }
                chartPanel.setTheme(getActiveTheme());
                chartPanel.setPreferredSize(new Dimension(1100, 680));
                chartPanel.setMinimumSize(new Dimension(360, 260));
                configureChart(entry, chartPanel);
                applyShowcasePreset(entry, chartPanel, renderer);
                installShowcaseRendererAnimation(entry, animatedModel, chartPanel);
                wrapper.removeAll();
                wrapper.add(chartPanel, BorderLayout.CENTER);
                wrapper.revalidate();
                wrapper.repaint();

                // Track metrics
                long elapsed = System.nanoTime() - startTime;
                totalRenderTimeNs.addAndGet(elapsed);
                renderCount.incrementAndGet();

                chartPanels.put(entry.className(), chartPanel);
                installed = true;
                if (entry.className().equals(currentRendererKey)) {
                    currentChartPanel = chartPanel;
                    updateChartActions();
                    updateStatus("Ready — " + entry.simpleName());
                }
                updateMetrics();
            } catch (RuntimeException ex) {
                String message = buildErrorMessage(ex);
                status.setText("<html>" + message.replace("\n", "<br/>") + "</html>");
                if (entry.className().equals(currentRendererKey)) {
                    updateStatus("Could not open " + entry.simpleName());
                    currentChartPanel = null;
                    updateChartActions();
                }
            } finally {
                if (!installed) closeDemoRenderer(renderer);
            }
        });
        timer.setRepeats(false);
        timer.start();
        return wrapper;
    }

    private void configureChart(RendererCatalogEntry entry, ArberChartPanel panel) {
        // Use core grid layers by renderer/category so presentation stays consistent.
        String category = entry.category();
        String simple = entry.simpleName();
        if ("medical".equals(category)) {
            panel.setGridLayer(new MedicalGridLayer());
        } else if ("financial".equals(category)) {
            panel.setGridLayer(new FinancialGridLayer());
        } else if ("statistical".equals(category)) {
            panel.setGridLayer(new StatisticalGridLayer());
        } else if ("analysis".equals(category)) {
            panel.setGridLayer(new AnalysisGridLayer());
        } else if ("security".equals(category)
                || "LineSpatialRenderer".equals(simple)
                || "Candlestick3DRenderer".equals(simple)) {
            panel.setGridLayer(new IsometricGridLayer());
        } else if ("SmithChartRenderer".equals(simple) || "VSWRCircleRenderer".equals(simple)) {
            panel.setGridLayer(new SmithChartGridLayer());
        } else if ("PolarRenderer".equals(simple)
                || "PolarLineRenderer".equals(simple)
                || "PolarAdvancedRenderer".equals(simple)
                || "RadarRenderer".equals(simple)
                || "WindRoseRenderer".equals(simple)) {
            panel.setGridLayer(new PolarGridLayer());
        } else if ("TernaryPlotRenderer".equals(simple)
                || "TernaryContourRenderer".equals(simple)
                || "TernaryPhasediagramRenderer".equals(simple)) {
            panel.setGridLayer(new TernaryGridLayer());
        }

        // Configure axis for optimal presentation
        configureAxisForCategory(entry.category(), panel);

    }

    private void applyShowcasePreset(RendererCatalogEntry entry, ArberChartPanel panel, ChartRenderer renderer) {
        if (entry == null || panel == null) {
            return;
        }
        String className = entry.className();
        boolean circular = "circular".equals(entry.category());
        boolean specialized = "specialized".equals(entry.category());
        boolean analysis = "analysis".equals(entry.category());
        boolean predictive = "predictive".equals(entry.category());
        boolean forensic = "forensic".equals(entry.category());
        boolean security = "security".equals(entry.category());
        boolean common = "common".equals(entry.category());

        panel.setTooltips(true);

        if (circular) {
            panel.setLegend(true);
            panel.setOverlayLegend(com.arbergashi.charts.domain.legend.LegendPosition.TOP_RIGHT);
        }
        if (specialized) {
            panel.setLegend(true);
            panel.setOverlayLegend(com.arbergashi.charts.domain.legend.LegendPosition.TOP_RIGHT);
        }
        if (analysis) {
            panel.setLegend(false);
            panel.setOverlayLegend(com.arbergashi.charts.domain.legend.LegendPosition.TOP_RIGHT);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(8).setShowGrid(true);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
            applyAnalysisRendererAxisPreset(className, panel);
        }
        if (predictive) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(8).setShowGrid(true);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }
        if (forensic) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(9).setShowGrid(true);
            y.setRequestedTickCount(7).setShowGrid(true);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }
        if (security) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(7).setShowGrid(true).setFixedRange(-1.0, 1.0);
            y.setRequestedTickCount(7).setShowGrid(true).setFixedRange(-1.0, 1.0);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }
        if (common) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(7).setShowGrid(true);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }

        if (renderer instanceof com.arbergashi.charts.render.circular.GaugeRenderer gaugeRenderer) {
            gaugeRenderer.setAnimationProfile(AnimationProfile.ENTERPRISE);
        }
        if (renderer instanceof com.arbergashi.charts.render.financial.Candlestick3DRenderer candle3d) {
            // Volume-based Z is normalized in the renderer since 2.1.0; defaults are safe.
            candle3d.setZDepth(0.09)
                    .setLodDepthThreshold(2.2)
                    .setSolid(true);
        }

        if ("com.arbergashi.charts.render.circular.GaugeRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.GaugeBandsRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.SemiDonutRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer".equals(className)) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(3).setShowGrid(false);
            y.setRequestedTickCount(5).setShowGrid(false).setFixedRange(0.0, 100.0);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
            return;
        }

        if ("com.arbergashi.charts.render.financial.RSIRenderer".equals(className)) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(6).setShowGrid(true).setFixedRange(0.0, 100.0);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
            return;
        }

        if ("com.arbergashi.charts.render.financial.PointAndFigureAdvancedRenderer".equals(className)) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(8).setShowGrid(false);
            y.setRequestedTickCount(5).setShowGrid(true).setFixedRange(-2.0, 2.0);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
            return;
        }

        if ("com.arbergashi.charts.render.circular.RadarRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.PolarRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.PolarLineRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.PolarAdvancedRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.RadialBarRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.RadialStackedRenderer".equals(className)
                || "com.arbergashi.charts.render.circular.NightingaleRoseRenderer".equals(className)) {
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(6).setShowGrid(false);
            y.setRequestedTickCount(6).setShowGrid(true).setFixedRange(0.0, 100.0);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }

        if ("com.arbergashi.charts.render.specialized.HeatmapRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.HeatmapContourRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.SpectrogramRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.HorizonRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.HorizonChartRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.SparklineRenderer".equals(className)) {
            panel.setLegend(false);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(10).setShowGrid(false);
            y.setRequestedTickCount(8).setShowGrid(false);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
            return;
        }

        if ("com.arbergashi.charts.render.specialized.SankeyRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.SankeyProRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.AlluvialRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.DependencyWheelRenderer".equals(className)
                || "com.arbergashi.charts.render.specialized.ChordFlowRenderer".equals(className)) {
            panel.setLegend(true);
            panel.setDockedLegend(com.arbergashi.charts.domain.legend.LegendDockSide.RIGHT);
            com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
            com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
            x.setRequestedTickCount(4).setShowGrid(false);
            y.setRequestedTickCount(4).setShowGrid(false);
            panel.setXAxisConfig(x);
            panel.setYAxisConfig(y);
        }
    }

    private void installShowcaseRendererAnimation(RendererCatalogEntry entry, ChartModel model, ArberChartPanel panel) {
        if (panel.getPrimaryRenderer() instanceof ShowcaseVectorFieldRenderer vectorField) {
            ShowcaseAnimation.install(panel, (elapsedSeconds, deltaSeconds) -> {
                vectorField.setElapsedSeconds(elapsedSeconds);
                panel.repaint();
            });
            return;
        }
        boolean standard = "standard".equals(entry.category());
        boolean financial = "financial".equals(entry.category());
        boolean statistical = "statistical".equals(entry.category());
        boolean circular = "circular".equals(entry.category());
        boolean specialized = "specialized".equals(entry.category());
        boolean analysis = "analysis".equals(entry.category());
        boolean predictive = "predictive".equals(entry.category());
        boolean forensic = "forensic".equals(entry.category());
        boolean security = "security".equals(entry.category());
        boolean common = "common".equals(entry.category());
        boolean medical = "medical".equals(entry.category());
        if (!standard && !financial && !statistical && !circular && !specialized && !analysis
                && !predictive && !forensic && !security && !common && !medical) {
            return;
        }
        if (medical) {
            if (model instanceof CircularFastMedicalModel medicalModel) {
                installMedicalShowcaseAnimation(medicalModel, panel);
            }
            return;
        }
        if (model instanceof ShowcaseFinancialModel financialModel) {
            installFinancialShowcaseAnimation(financialModel, panel, entry.className());
            return;
        }
        if (!(model instanceof DefaultChartModel defaultModel)) {
            return;
        }
        int count = defaultModel.getPointCount();
        if (count <= 0) {
            return;
        }

        String className = entry.className();
        if ("com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer".equals(className)) {
            return;
        }

        double[] baseX = defaultModel.getXData();
        double[] baseY = defaultModel.getYData();
        double[] baseMin = defaultModel.getMinData();
        double[] baseMax = defaultModel.getMaxData();
        double[] baseW = defaultModel.getWeightData();
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            labels[i] = defaultModel.getLabel(i);
        }

        double speed = showcaseAnimationSpeed(className);
        double amp = showcaseAnimationAmplitude(className);
        if (speed <= 0.0 || amp <= 0.0) {
            return;
        }
        ShowcaseAnimation.install(panel, (elapsedSeconds, deltaSeconds) -> {
            double phase = elapsedSeconds * speed;
            double blend = com.arbergashi.charts.util.AnimationUtils.easeInOut(Math.min(1.0, elapsedSeconds / 0.25));
            defaultModel.clear();
            for (int i = 0; i < count; i++) {
                double x0 = i < baseX.length ? baseX[i] : i;
                double y0 = i < baseY.length ? baseY[i] : 0.0;
                double w0 = i < baseW.length ? baseW[i] : Math.abs(y0);
                String label = labels[i];

                double y;
                double x;
                double weight;

                if (className.endsWith("GaugeRenderer")
                        || className.endsWith("GaugeBandsRenderer")
                        || className.endsWith("SemiDonutRenderer")) {
                    x = x0;
                    y = (i == 0) ? clamp(y0 + Math.sin(phase * 0.9) * (18.0 * amp), 8.0, 96.0) : y0;
                    weight = Math.max(1.0, y);
                } else if (financial) {
                    x = x0;
                    y = y0 * (0.96 + 0.08 * amp * Math.sin(phase * 0.6 + i * 0.18));
                    if (i % 54 == 0) {
                        y += Math.sin(phase * 0.42 + i * 0.05) * (1.8 * amp);
                    }
                    weight = Math.max(0.05, w0 * (0.94 + 0.12 * amp * Math.cos(phase * 0.55 + i * 0.14)));
                } else if (statistical) {
                    x = x0;
                    y = y0 * (0.95 + 0.1 * amp * Math.sin(phase * 0.72 + i * 0.23));
                    weight = Math.max(0.05, w0 * (0.95 + 0.08 * amp * Math.cos(phase * 0.66 + i * 0.2)));
                } else if (standard) {
                    x = x0;
                    y = y0 * (0.94 + 0.11 * amp * Math.sin(phase * 0.78 + i * 0.27));
                    weight = Math.max(0.05, w0 * (0.94 + 0.1 * amp * Math.cos(phase * 0.64 + i * 0.21)));
                } else if (className.endsWith("SmithChartRenderer") || className.endsWith("VSWRCircleRenderer")) {
                    double theta = Math.atan2(y0, x0);
                    double radius = Math.hypot(x0, y0);
                    double spin = phase * 0.35;
                    double ripple = 1.0 + 0.12 * amp * Math.sin(phase * 0.9 + i * 0.18);
                    double r = clamp(radius * ripple, 0.08, 0.98);
                    x = Math.cos(theta + spin) * r;
                    y = Math.sin(theta + spin) * r;
                    weight = Math.max(0.05, r);
                } else if (className.endsWith("PolarRenderer") || className.endsWith("PolarLineRenderer")) {
                    x = x0;
                    y = Math.max(1.0, y0 * (0.82 + (0.24 * amp) * Math.sin(phase + i * 0.55)));
                    weight = Math.max(1.0, w0 * (0.78 + (0.22 * amp) * Math.cos(phase * 0.8 + i * 0.45)));
                } else if (className.endsWith("PolarAdvancedRenderer") || className.endsWith("RadialStackedRenderer")) {
                    x = Math.max(0.0, x0 * (0.9 + (0.12 * amp) * Math.sin(phase * 0.7 + i * 0.4)));
                    y = Math.max(1.0, y0 * (0.8 + (0.25 * amp) * Math.cos(phase * 0.95 + i * 0.6)));
                    weight = Math.max(1.0, w0);
                } else if (className.endsWith("NightingaleRoseRenderer")
                        || className.endsWith("RadarRenderer")
                        || className.endsWith("RadialBarRenderer")) {
                    x = x0;
                    y = Math.max(1.0, y0 * (0.78 + (0.28 * amp) * Math.sin(phase * 0.9 + i * 0.58)));
                    weight = Math.max(1.0, y);
                } else if (analysis) {
                    if (className.endsWith("AdaptiveFunctionRenderer")
                            || className.endsWith("VectorFieldRenderer")
                            || className.endsWith("ReferenceLineRenderer")) {
                        x = x0;
                        y = y0;
                        weight = Math.max(0.05, w0);
                    } else if (className.endsWith("OutlierDetectionRenderer")
                            || className.endsWith("PeakDetectionRenderer")
                            || className.endsWith("ChangePointRenderer")
                            || className.endsWith("ThresholdRenderer")
                            || className.endsWith("MinMaxMarkerRenderer")) {
                        x = x0;
                        y = y0 + Math.sin(phase * 0.8 + i * 0.21) * (Math.max(0.8, Math.abs(y0) * 0.08) * amp);
                        weight = Math.max(0.05, w0 * (0.92 + 0.14 * amp * Math.cos(phase * 0.9 + i * 0.17)));
                    } else if (className.endsWith("EnvelopeRenderer")) {
                        x = x0;
                        y = y0 * (0.94 + 0.10 * amp * Math.sin(phase * 0.7 + i * 0.14));
                        weight = Math.max(0.05, w0 * (0.95 + 0.08 * amp * Math.cos(phase * 0.6 + i * 0.11)));
                    } else if (className.endsWith("SlopeRenderer")) {
                        x = x0;
                        y = y0 + Math.sin(phase * 0.9 + i * 0.5) * 0.9 * amp;
                        weight = Math.max(0.05, w0);
                    } else if (className.endsWith("LiveFFTRenderer")
                            || className.endsWith("FourierOverlayRenderer")
                            || className.endsWith("AutocorrelationRenderer")) {
                        x = x0;
                        y = y0 * (0.9 + 0.18 * amp * Math.sin(phase * 1.2 + i * 0.18));
                        weight = Math.max(0.05, w0);
                    } else {
                        x = x0;
                        y = y0 * (0.88 + 0.2 * amp * Math.sin(phase * 0.95 + i * 0.33));
                        weight = Math.max(0.05, w0 * (0.9 + 0.16 * amp * Math.cos(phase * 0.7 + i * 0.27)));
                    }
                } else if (predictive) {
                    x = x0;
                    y = y0 + Math.sin(phase * 0.75 + i * 0.24) * (Math.max(0.9, Math.abs(y0) * 0.07) * amp);
                    if (i % 79 == 0) {
                        y += (Math.sin(phase * 0.4) > 0 ? 1 : -1) * (2.4 * amp);
                    }
                    weight = Math.max(0.05, w0 * (0.93 + 0.12 * amp * Math.cos(phase * 0.6 + i * 0.21)));
                } else if (forensic) {
                    x = x0;
                    y = y0 * (0.96 + 0.08 * amp * Math.sin(phase * 0.55 + i * 0.13));
                    weight = Math.max(0.05, w0);
                } else if (security) {
                    x = x0;
                    y = y0;
                    double z = w0 * (0.92 + 0.16 * amp * Math.sin(phase * 0.6 + i * 0.17));
                    weight = clamp(z, -0.95, 0.95);
                } else if (common) {
                    x = x0;
                    y = y0 * (0.95 + 0.09 * amp * Math.sin(phase * 0.65 + i * 0.2));
                    weight = Math.max(0.05, w0 * (0.95 + 0.08 * amp * Math.cos(phase * 0.6 + i * 0.16)));
                } else if (specialized) {
                    if (className.endsWith("SankeyRenderer")
                            || className.endsWith("SankeyProRenderer")
                            || className.endsWith("AlluvialRenderer")
                            || className.endsWith("DependencyWheelRenderer")
                            || className.endsWith("ChordFlowRenderer")
                            || className.endsWith("MarimekkoRenderer")) {
                        x = x0;
                        y = Math.max(1.0, y0 * (0.86 + (0.2 * amp) * Math.sin(phase * 0.7 + i * 0.36)));
                        weight = Math.max(1.0, w0 * (0.84 + (0.2 * amp) * Math.cos(phase * 0.65 + i * 0.31)));
                    } else if (className.endsWith("HeatmapRenderer")
                            || className.endsWith("HeatmapContourRenderer")
                            || className.endsWith("SpectrogramRenderer")
                            || className.endsWith("HorizonRenderer")
                            || className.endsWith("HorizonChartRenderer")) {
                        x = x0;
                        y = Math.max(1.0, y0 + Math.sin(phase * 1.1 + i * 0.22) * (Math.max(2.0, y0 * 0.08) * amp));
                        weight = Math.max(1.0, w0);
                    } else if (className.endsWith("SunburstRenderer")
                            || className.endsWith("TreemapRenderer")
                            || className.endsWith("NetworkRenderer")
                            || className.endsWith("DendrogramRenderer")) {
                        x = x0;
                        y = Math.max(1.0, y0 * (0.9 + (0.16 * amp) * Math.sin(phase * 0.55 + i * 0.41)));
                        weight = Math.max(1.0, w0 * (0.9 + (0.14 * amp) * Math.cos(phase * 0.6 + i * 0.29)));
                    } else {
                        x = x0;
                        y = Math.max(1.0, y0 * (0.83 + (0.22 * amp) * Math.sin(phase * 0.9 + i * 0.46)));
                        weight = Math.max(1.0, w0 * (0.82 + (0.2 * amp) * Math.cos(phase * 0.8 + i * 0.38)));
                    }
                } else {
                    x = x0;
                    y = Math.max(1.0, y0 * (0.8 + (0.22 * amp) * Math.sin(phase * 0.85 + i * 0.52)));
                    weight = Math.max(1.0, w0 * (0.8 + (0.2 * amp) * Math.cos(phase * 0.7 + i * 0.47)));
                }

                double min;
                double max;
                if (className.endsWith("SmithChartRenderer") || className.endsWith("VSWRCircleRenderer")) {
                    min = y - 0.04;
                    max = y + 0.04;
                } else if (security) {
                    double intensity0 = i < baseMax.length ? baseMax[i] : 0.5;
                    double intensity = clamp(intensity0 * (0.9 + 0.18 * amp * Math.sin(phase * 0.5 + i * 0.12)), 0.0, 1.0);
                    min = y - 0.05;
                    max = intensity;
                } else if (analysis) {
                    double band = Math.max(0.5, Math.abs(y) * 0.09);
                    min = y - band;
                    max = y + band;
                } else {
                    min = y - Math.max(1.0, y * 0.08);
                    max = y + Math.max(1.0, y * 0.08);
                }
                // Start from the displayed baseline instead of jumping to the oscillator's offset.
                x = Math.fma(blend, x - x0, x0);
                y = Math.fma(blend, y - y0, y0);
                weight = Math.fma(blend, weight - w0, w0);
                double min0 = i < baseMin.length ? baseMin[i] : y0;
                double max0 = i < baseMax.length ? baseMax[i] : y0;
                min = Math.fma(blend, min - min0, min0);
                max = Math.fma(blend, max - max0, max0);
                defaultModel.setPoint(x, y, min, max, weight, label,
                        com.arbergashi.charts.model.ProvenanceFlags.ORIGINAL, (short) 0, 0L);
            }
        });
    }

    private void installMedicalShowcaseAnimation(CircularFastMedicalModel model, ArberChartPanel panel) {
        final int count = model.getPointCount();
        if (count <= 0) {
            return;
        }
        final int channels = model.getChannelCount();
        if (channels <= 0) {
            return;
        }

        final double[][] baseline = new double[channels][count];
        for (int i = 0; i < count; i++) {
            for (int c = 0; c < channels; c++) {
                baseline[c][i] = model.getY(i, c);
            }
        }

        final double sampleRate = 250.0;
        final double dt = 1.0 / sampleRate;
        final double[] t = {model.getX(count - 1)};
        final int[] cursor = {0};
        final double[] carry = {0.0};
        final double[] channelsBuffer = new double[channels];

        ShowcaseAnimation.install(panel, (elapsedSeconds, deltaSeconds) -> {
            carry[0] += sampleRate * deltaSeconds;
            int samples = (int) carry[0];
            carry[0] -= samples;

            for (int s = 0; s < samples; s++) {
                int idx = cursor[0] % count;
                t[0] += dt;
                for (int c = 0; c < channels; c++) {
                    double base = baseline[c][idx];
                    // Keep renderer character: use baseline shape and only subtle dynamic breathing.
                    double mod = 1.0 + 0.03 * Math.sin(t[0] * (0.9 + c * 0.2));
                    channelsBuffer[c] = base * mod;
                }
                model.add(t[0], channelsBuffer);
                cursor[0]++;
            }
            panel.repaint();
        });
    }

    private void installFinancialShowcaseAnimation(ShowcaseFinancialModel model,
                                                   ArberChartPanel panel,
                                                   String className) {
        int last = model.getPointCount() - 1;
        if (last < 0) return;
        double baselineOpen = model.getOpen(last);
        double baselineClose = model.getClose(last);
        double baselineHigh = model.getHigh(last);
        double baselineLow = model.getLow(last);
        double baselineVolume = model.getVolume(last);
        double speed = showcaseAnimationSpeed(className);
        double amplitude = showcaseAnimationAmplitude(className);
        if (speed <= 0.0 || amplitude <= 0.0) return;

        ShowcaseAnimation.install(panel, (elapsedSeconds, deltaSeconds) -> {
            double phase = elapsedSeconds * speed;
            double delta = amplitude * (Math.sin(phase * 0.9) * 0.9 + Math.sin(phase * 1.6) * 0.35);
            double close = Math.max(1.0, baselineClose + delta);
            double high = Math.max(baselineHigh, Math.max(baselineOpen, close) + Math.abs(delta) * 0.25);
            double low = Math.min(baselineLow, Math.min(baselineOpen, close) - Math.abs(delta) * 0.25);
            double volume = baselineVolume + Math.abs(delta) * 12_000_000;
            model.updateLiveBar(baselineOpen, high, low, close, volume);
        });
    }

    private static double showcaseAnimationSpeed(String className) {
        if (className == null) return 1.4;
        if (className.contains("AuditTrail")) return 0.0;
        if (className.contains("Candlestick") || className.contains("MACD") || className.contains("RSI")
                || className.contains("ADX") || className.contains("Ichimoku")) return 0.62;
        if (className.contains("BoxPlot") || className.contains("Violin") || className.contains("Histogram")
                || className.contains("KDE") || className.contains("QQPlot")) return 0.7;
        if (className.contains("LineRenderer") || className.contains("Spline") || className.contains("AreaRenderer")
                || className.contains("ScatterRenderer") || className.contains("BarRenderer")) return 0.78;
        if (className.contains("AdaptiveFunction") || className.contains("VectorField") || className.contains("ReferenceLine")) return 0.0;
        if (className.contains("Slope")) return 0.8;
        if (className.contains("Envelope")) return 0.72;
        if (className.contains("Predictive") || className.contains("AnomalyGap")) return 0.88;
        if (className.contains("Playback")) return 0.55;
        if (className.contains("VoxelCloud")) return 0.7;
        if (className.contains("PerformanceAudit")) return 0.72;
        if (className.contains("PhysicalScale")) return 0.0;
        if (className.contains("Outlier") || className.contains("Peak") || className.contains("Threshold")) return 1.0;
        if (className.contains("FFT") || className.contains("Fourier") || className.contains("Autocorrelation")) return 1.2;
        if (className.contains("Regression") || className.contains("Average") || className.contains("Trend")) return 0.95;
        if (className.contains("Smith") || className.contains("VSWR")) return 1.0;
        if (className.contains("Gauge") || className.contains("SemiDonut")) return 1.1;
        if (className.contains("Heatmap") || className.contains("Spectrogram")) return 0.95;
        if (className.contains("Sankey") || className.contains("Alluvial") || className.contains("Chord")) return 0.85;
        if (className.contains("Radar") || className.contains("Polar")) return 1.35;
        return 1.2;
    }

    private static double showcaseAnimationAmplitude(String className) {
        if (className == null) return 1.0;
        if (className.contains("AuditTrail")) return 0.0;
        if (className.contains("Candlestick") || className.contains("MACD") || className.contains("RSI")
                || className.contains("ADX") || className.contains("Ichimoku")) return 0.42;
        if (className.contains("BoxPlot") || className.contains("Violin") || className.contains("Histogram")
                || className.contains("KDE") || className.contains("QQPlot")) return 0.48;
        if (className.contains("LineRenderer") || className.contains("Spline") || className.contains("AreaRenderer")
                || className.contains("ScatterRenderer") || className.contains("BarRenderer")) return 0.52;
        if (className.contains("AdaptiveFunction") || className.contains("VectorField") || className.contains("ReferenceLine")) return 0.0;
        if (className.contains("Slope")) return 0.45;
        if (className.contains("Envelope")) return 0.55;
        if (className.contains("Predictive") || className.contains("AnomalyGap")) return 0.65;
        if (className.contains("Playback")) return 0.35;
        if (className.contains("VoxelCloud")) return 0.6;
        if (className.contains("PerformanceAudit")) return 0.45;
        if (className.contains("PhysicalScale")) return 0.0;
        if (className.contains("Outlier") || className.contains("Peak") || className.contains("Threshold")) return 0.7;
        if (className.contains("FFT") || className.contains("Fourier") || className.contains("Autocorrelation")) return 0.9;
        if (className.contains("Regression") || className.contains("Average") || className.contains("Trend")) return 0.75;
        if (className.contains("Smith") || className.contains("VSWR")) return 0.75;
        if (className.contains("Gauge") || className.contains("SemiDonut")) return 0.9;
        if (className.contains("Sankey") || className.contains("Alluvial") || className.contains("Chord")) return 0.7;
        if (className.contains("Heatmap") || className.contains("Spectrogram")) return 0.65;
        if (className.contains("Radar") || className.contains("Polar")) return 1.05;
        return 0.95;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static void applyAnalysisRendererAxisPreset(String className, ArberChartPanel panel) {
        if (className == null || panel == null) {
            return;
        }
        com.arbergashi.charts.api.AxisConfig x = new com.arbergashi.charts.api.AxisConfig();
        com.arbergashi.charts.api.AxisConfig y = new com.arbergashi.charts.api.AxisConfig();
        x.setShowGrid(true);
        y.setShowGrid(true);

        if (className.endsWith("AutocorrelationRenderer")) {
            x.setRequestedTickCount(12).setShowGrid(true);
            y.setRequestedTickCount(6).setShowGrid(true).setFixedRange(-1.0, 1.0);
        } else if (className.endsWith("LiveFFTRenderer")
                || className.endsWith("FourierOverlayRenderer")) {
            x.setRequestedTickCount(12).setShowGrid(true);
            y.setRequestedTickCount(8).setShowGrid(true);
        } else if (className.endsWith("VectorFieldRenderer")) {
            x.setRequestedTickCount(9).setShowGrid(true).setFixedRange(-10.0, 10.0);
            y.setRequestedTickCount(9).setShowGrid(true).setFixedRange(-10.0, 10.0);
        } else if (className.endsWith("AdaptiveFunctionRenderer")) {
            x.setRequestedTickCount(10).setShowGrid(true).setFixedRange(-36.0, 36.0);
            y.setRequestedTickCount(8).setShowGrid(true);
        } else if (className.endsWith("ThresholdRenderer")
                || className.endsWith("ReferenceLineRenderer")) {
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(8).setShowGrid(true);
        } else if (className.endsWith("SlopeRenderer")) {
            x.setRequestedTickCount(3).setShowGrid(true);
            y.setRequestedTickCount(7).setShowGrid(true);
        } else {
            x.setRequestedTickCount(10).setShowGrid(true);
            y.setRequestedTickCount(8).setShowGrid(true);
        }

        panel.setXAxisConfig(x);
        panel.setYAxisConfig(y);
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
            case "predictive" -> {
                xCfg.setRequestedTickCount(10);
                yCfg.setRequestedTickCount(8);
            }
            case "forensic" -> {
                xCfg.setRequestedTickCount(9);
                yCfg.setRequestedTickCount(7);
            }
            case "security" -> {
                xCfg.setRequestedTickCount(7).setFixedRange(-1.0, 1.0);
                yCfg.setRequestedTickCount(7).setFixedRange(-1.0, 1.0);
            }
            case "common" -> {
                xCfg.setRequestedTickCount(10);
                yCfg.setRequestedTickCount(7);
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
        ArberChartPanel discarded = chartPanels.remove(currentRendererKey);
        if (discarded != null) {
            ShowcaseAnimation.close(discarded);
            closeDemoRenderer(discarded.getPrimaryRenderer());
        }
        currentChartPanel = null;
        updateChartActions();
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(currentPalette().contentBackground());
        JLabel hint = new JLabel("Select a renderer in the catalog to open its chart.", SwingConstants.CENTER);
        hint.setForeground(currentPalette().muted());
        panel.add(hint, BorderLayout.CENTER);
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
            case "com.arbergashi.charts.render.financial.AuditTrailRenderer" ->
                    new AuditTrailRendererAdapter();
            case "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer" ->
                    new AdaptiveFunctionRenderer(x -> Math.sin(x * 0.12) * 32 + Math.cos(x * 0.05) * 18);
            case "com.arbergashi.charts.render.analysis.VectorFieldRenderer" ->
                    new ShowcaseVectorFieldRenderer();
            case "com.arbergashi.charts.render.standard.LineSpatialRenderer" ->
                    new com.arbergashi.charts.render.standard.LineSpatialRenderer()
                            .setZScale(0.012)
                            .setZOffset(0.0);
            case "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer" ->
                    new com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer(sampleLatencyTracker());
            case "com.arbergashi.charts.render.forensic.PlaybackStatusRenderer" ->
                    new PlaybackStatusRenderer(new DemoPlaybackController());
            case "com.arbergashi.charts.render.common.PerformanceAuditRenderer" ->
                    new PerformanceAuditRendererAdapter(sampleLatencyTracker());
            case "com.arbergashi.charts.render.common.PhysicalScaleRenderer" ->
                    new com.arbergashi.charts.render.common.PhysicalScaleRenderer().setEnabled(true);
            case "com.arbergashi.charts.render.specialized.SunburstRenderer" ->
                    new com.arbergashi.charts.render.specialized.SunburstRenderer();
            case "com.arbergashi.charts.render.circular.SunburstRenderer" ->
                    new com.arbergashi.charts.render.specialized.SunburstRenderer();
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
        if (SystemInfo.isLinux) {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", APP_NAME);
            System.setProperty("apple.awt.application.appearance", "system");
        }
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
        frame.getRootPane().putClientProperty("JRootPane.useWindowDecorations", Boolean.TRUE);
        frame.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", Boolean.TRUE);
        frame.getRootPane().putClientProperty("JRootPane.titleBarShowIcon", Boolean.TRUE);
        frame.getRootPane().putClientProperty("JRootPane.titleBarShowTitle", Boolean.FALSE);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground",
                DemoThemeSupport.uiColor("TitlePane.background", "Panel.background"));
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground",
                DemoThemeSupport.uiColor("TitlePane.foreground", "Label.foreground"));
    }

    private static void configurePlatformDialog(JDialog dialog) {
        if (SystemInfo.isMacOS) {
            if (SystemInfo.isMacFullWindowContentSupported) {
                dialog.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
                dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            }
            return;
        }

        dialog.getRootPane().putClientProperty("JRootPane.useWindowDecorations", Boolean.TRUE);
        dialog.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", Boolean.FALSE);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarShowIcon", Boolean.TRUE);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarShowTitle", Boolean.TRUE);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarBackground",
                DemoThemeSupport.uiColor("TitlePane.background", "Panel.background"));
        dialog.getRootPane().putClientProperty("JRootPane.titleBarForeground",
                DemoThemeSupport.uiColor("TitlePane.foreground", "Label.foreground"));
    }

    private void rebuildRendererPanelsForTheme() {
        String selectedKey = currentRendererKey;
        disposeRendererPanels();
        updateChartActions();
        detailHost.add(buildEmptyDetail(), "empty");
        detailLayout.show(detailHost, "empty");
        if (selectedKey != null && !selectedKey.isBlank()) {
            RendererCatalogEntry selected = catalog.getRequired(selectedKey);
            openRenderer(selected);
        }
        detailHost.revalidate();
        detailHost.repaint();
    }

    private void disposeRendererPanels() {
        for (ArberChartPanel panel : chartPanels.values()) {
            ShowcaseAnimation.close(panel);
            closeDemoRenderer(panel.getPrimaryRenderer());
        }
        detailHost.removeAll();
        rendererPanels.clear();
        chartPanels.clear();
        currentChartPanel = null;
    }

    private void disposeDemoResources() {
        disposeRendererPanels();
        if (metricsTimer != null) metricsTimer.stop();
        if (speedSearchTimer != null) speedSearchTimer.stop();
        if (searchEverywhereDialog != null) searchEverywhereDialog.dispose();
    }

    private static void closeDemoRenderer(ChartRenderer renderer) {
        // The demo constructs adaptive renderers as primary layers and owns their workers.
        if (renderer instanceof AdaptiveFunctionRenderer adaptive) adaptive.close();
    }

    private void applyDemoPalette() {
        DemoPalette palette = currentPalette();
        if (headerPanel != null) {
            headerPanel.setBackground(palette.windowBackground());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, palette.border()),
                    BorderFactory.createEmptyBorder(0, 14, 0, 12)));
            applyMutedLabels(headerPanel, palette.muted());
        }
        if (footerPanel != null) {
            footerPanel.setBackground(palette.windowBackground());
            footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, palette.border()));
        }
        if (sidebarPanel != null) {
            sidebarPanel.setBackground(palette.sidebarBackground());
            applyMutedLabels(sidebarPanel, palette.muted());
        }
        if (editorToolbar != null) {
            editorToolbar.setBackground(palette.contentBackground());
            editorToolbar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, palette.border()),
                    BorderFactory.createEmptyBorder(0, 16, 0, 12)));
            categoryLabel.setForeground(palette.muted());
            activeRendererLabel.setForeground(palette.foreground());
            if (currentRendererKey != null) {
                activeRendererLabel.setIcon(rendererIconForCategory(
                        catalog.getRequired(currentRendererKey).category(), palette.accent()));
            }
        }
        if (headerTitleLabel != null) {
            headerTitleLabel.setForeground(palette.foreground());
        }
        if (headerMarkLabel != null) {
            headerMarkLabel.setIcon(loadTablerOutlineIcon("chart-line.svg", 20,
                    UIManager.getIcon("FileView.fileIcon"), palette.accent()));
        }
        if (versionLabel != null) {
            versionLabel.setForeground(palette.softMuted());
        }
        if (themeSwitchButton != null) {
            themeSwitchButton.setIcon(loadTablerOutlineIcon("sun-moon.svg", 18,
                    UIManager.getIcon("Tree.expandedIcon"), palette.muted()));
        }
        if (searchEverywhereButton != null) {
            searchEverywhereButton.setIcon(loadTablerOutlineIcon("search.svg", 16,
                    UIManager.getIcon("FileView.fileIcon"), palette.muted()));
        }
        if (catalogFilter != null) {
            catalogFilter.putClientProperty("JTextField.leadingIcon", loadTablerOutlineIcon("search.svg", 15,
                    UIManager.getIcon("FileView.fileIcon"), palette.muted()));
        }
        countLabel.setForeground(palette.muted());
        statusLabel.setForeground(palette.muted());
        metricsLabel.setForeground(palette.muted());
        tree.setBackground(palette.sidebarBackground());
        tree.setForeground(palette.foreground());
        detailHost.setBackground(palette.contentBackground());
        if (searchEverywhereDialog != null) {
            searchEverywhereDialog.getRootPane().setBorder(BorderFactory.createLineBorder(palette.border()));
            searchEverywhereDialog.getContentPane().getComponent(0).setBackground(palette.surfaceBackground());
        }
    }

    private static void applyMutedLabels(Container container, Color muted) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && Boolean.TRUE.equals(label.getClientProperty("demo.muted"))) {
                label.setForeground(muted);
            } else if (child instanceof Container nested) {
                applyMutedLabels(nested, muted);
            }
        }
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
        private RendererTreeCell() {
        }

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
                Color iconColor = sel ? tree.getForeground() : DemoThemeSupport.uiColor("Label.foreground", "textText");
                setIcon(rendererIconForCategory(entry.category(), iconColor));
            } else if (user instanceof String label) {
                setText(capitalize(label) + "  " + node.getChildCount());
                setToolTipText(null);
                setFont(baseFont.deriveFont(Font.PLAIN, 13f));
                Color categoryColor = UIManager.getColor("Component.grayForeground");
                if (categoryColor == null) {
                    categoryColor = tree.getForeground();
                }
                setIcon(categoryIconFor(label, expanded, categoryColor));
                if (!sel) {
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

    private static Icon categoryIconFor(String category, boolean expanded, Color tint) {
        String normalized = category == null ? "" : category.toLowerCase(Locale.US);
        String file = switch (normalized) {
            case "financial" -> "chart-candle.svg";
            case "medical" -> "activity-heartbeat.svg";
            case "statistical" -> "chart-histogram.svg";
            case "analysis" -> "chart-scatter.svg";
            case "predictive" -> "chart-arrows.svg";
            case "forensic" -> "shield-search.svg";
            case "security" -> "shield.svg";
            case "specialized" -> "chart-sankey.svg";
            case "circular" -> "circle.svg";
            case "common" -> "chart-bar.svg";
            case "standard" -> "chart-line.svg";
            default -> expanded ? "folder-open.svg" : "folder.svg";
        };
        Icon fallback = expanded ? UIManager.getIcon("Tree.openIcon") : UIManager.getIcon("Tree.closedIcon");
        return loadTablerOutlineIcon(file, 16, fallback, tint);
    }

    private static Icon rendererIconForCategory(String category, Color tint) {
        String normalized = category == null ? "" : category.toLowerCase(Locale.US);
        String file = switch (normalized) {
            case "financial" -> "chart-candle.svg";
            case "medical" -> "activity-heartbeat.svg";
            case "statistical" -> "chart-histogram.svg";
            case "analysis" -> "chart-scatter.svg";
            case "predictive" -> "chart-arrows.svg";
            case "forensic" -> "shield-search.svg";
            case "security" -> "shield.svg";
            case "specialized" -> "chart-sankey.svg";
            case "circular" -> "circle.svg";
            default -> "chart-line.svg";
        };
        return loadTablerOutlineIcon(file, 16, UIManager.getIcon("Tree.leafIcon"), tint);
    }

    private static Icon loadTablerOutlineIcon(String fileName, int size, Icon fallback, Color tint) {
        try {
            String cacheKey = fileName + "|" + size + "|" + (tint != null ? tint.getRGB() : 0);
            Icon cached = ICON_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            java.net.URL iconUrl = DemoApplication.class.getClassLoader().getResource("icons/" + fileName);
            if (iconUrl != null) {
                FlatSVGIcon icon = new FlatSVGIcon(iconUrl);
                FlatSVGIcon derived = icon.derive(size, size);
                if (tint != null) {
                    FlatSVGIcon.ColorFilter filter = new FlatSVGIcon.ColorFilter((component, color) ->
                            new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), color.getAlpha()));
                    derived.setColorFilter(filter);
                }
                ICON_CACHE.put(cacheKey, derived);
                return derived;
            }
        } catch (Exception ignored) {
            // fall through to fallback
        }
        return fallback;
    }

    private static void applyApplicationIcons(JFrame frame) {
        try {
            java.net.URL iconUrl = DemoApplication.class.getClassLoader().getResource("icons/app-icon.svg");
            if (iconUrl == null) {
                return;
            }
            FlatSVGIcon svgIcon = new FlatSVGIcon(iconUrl);
            int[] sizes = {16, 20, 24, 32, 40, 48, 64, 96, 128, 256, 512};
            List<Image> images = new ArrayList<>(sizes.length);
            for (int size : sizes) {
                FlatSVGIcon derived = svgIcon.derive(size, size);
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                try {
                    derived.paintIcon(null, g2, 0, 0);
                } finally {
                    g2.dispose();
                }
                images.add(image);
            }
            frame.setIconImages(images);
            if (!images.isEmpty() && Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(images.get(images.size() - 1));
                }
            }
        } catch (Exception ignored) {
            // App icon is optional.
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
