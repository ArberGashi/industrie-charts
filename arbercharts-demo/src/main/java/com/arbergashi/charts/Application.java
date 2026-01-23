package com.arbergashi.charts;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.ui.ChartExportService;
import com.arbergashi.charts.uielements.PropertyGrid;
import com.arbergashi.charts.uielements.CommandPalette;
import com.arbergashi.charts.actions.ChartActions;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.formdev.flatlaf.util.UIScale;
import com.arbergashi.charts.uielements.InvisibleSplitPane;
import com.arbergashi.charts.uielements.NavigationTree;
import com.arbergashi.charts.rendererpanels.DemoPanelFactory;
import com.arbergashi.charts.rendererpanels.ChartHost;
import com.arbergashi.charts.uielements.AboutDialog;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import com.formdev.flatlaf.util.SystemFileChooser;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Application extends JFrame {

    private static final Logger LOG = Logger.getLogger(Application.class.getName());

    // Kept as fields because they are accessed from multiple instance methods.
    // mainPanel can be local, but keeping it as field keeps the structure symmetrical; no runtime impact.
    private InvisibleSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel topPanel;
    private JPanel statusBar;
    private JLabel statusLabel;
    private JLabel appTitleLabel;
    private JButton searchButton;
    private NavigationTree navigationTree;
    private PropertyGrid propertyGrid;
    private ArberChartPanel activeChartPanel;
    private InvisibleSplitPane innerSplit;
    private boolean propertyGridVisible = false;
    private int lastPropertyGridDivider = 700;

    private ChartActions.ToggleThemeAction toggleThemeAction;
    private ChartActions.ExportAction exportAction;
    private ChartActions.SettingsAction settingsAction;
    private ChartActions.TogglePropertyGridAction propertyGridAction;

    // IntelliJ-like sidebar selection groups
    private final ButtonGroup leftSidebarGroup = new ButtonGroup();
    private final ButtonGroup rightSidebarGroup = new ButtonGroup();

    private String currentTitle = "Line Chart";

    public Application() {
        if (!SystemInfo.isMacOS) {
            getRootPane().putClientProperty("flatlaf.menuBarEmbedded", true);
        }

        initActions();
        setApplicationIcon();
        if (SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        // Theme/LAF is handled by FlatLaf + resources/themes/*.properties.

        initialize();
        syncMacMenuBarAppearance();
        setTitle("ArberCharts");

        // Use a stable default window size (no DemoConfig persistence).
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setApplicationIcon() {
        try {
            // Create a small multi-resolution set so different OS shells can pick the best size.
            int s16 = UIScale.scale(16);
            int s32 = UIScale.scale(32);
            int s64 = UIScale.scale(64);
            int s128 = UIScale.scale(128);
            int s256 = UIScale.scale(256);

            Image i16 = new FlatSVGIcon("icons/appicon.svg", s16, s16).getImage();
            Image i32 = new FlatSVGIcon("icons/appicon.svg", s32, s32).getImage();
            Image i64 = new FlatSVGIcon("icons/appicon.svg", s64, s64).getImage();
            Image i128 = new FlatSVGIcon("icons/appicon.svg", s128, s128).getImage();
            Image i256 = new FlatSVGIcon("icons/appicon.svg", s256, s256).getImage();

            java.util.List<Image> icons = new java.util.ArrayList<>();
            if (i16 != null) icons.add(i16);
            if (i32 != null) icons.add(i32);
            if (i64 != null) icons.add(i64);
            if (i128 != null) icons.add(i128);
            if (i256 != null) icons.add(i256);

            // JFrame icon (Windows/Linux titlebar/task switcher)
            if (i256 != null) {
                setIconImage(i256);
            }
            if (!icons.isEmpty()) {
                setIconImages(icons);
            }

            // System icon (macOS Dock / Windows Taskbar / Linux depending on DE)
            Image best = icons.isEmpty() ? null : icons.getLast();
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar tb = java.awt.Taskbar.getTaskbar();
                if (tb.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE) && best != null) {
                    tb.setIconImage(best);
                }
            }
        } catch (Throwable ignored) {
            // If icon loading fails, keep default Java icon.
        }
    }

    private void initActions() {
        toggleThemeAction = new ChartActions.ToggleThemeAction(() -> {
            // After LAF switch, rebuild the currently selected panel with the new chart theme.
            updateContent(currentTitle);
            updateColors();
            syncMacMenuBarAppearance();
            SwingUtilities.updateComponentTreeUI(this);
            repaint();
        });
        exportAction = new ChartActions.ExportAction(() -> activeChartPanel);
        settingsAction = new ChartActions.SettingsAction();
        propertyGridAction = new ChartActions.TogglePropertyGridAction(this::setPropertyGridVisible);

        // Register Command Palette Shortcut (Ctrl+P / Cmd+P)
        String commandKey = SystemInfo.isMacOS ? "meta P" : "control P";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(commandKey), "openCommandPalette");
        getRootPane().getActionMap().put("openCommandPalette", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommandPalette.show(Application.this, navigationTree.getAllNavItems(), item -> updateContent(item.getTitle()));
            }
        });
    }

    public void initialize() {
        createMenuBar();
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // Status Bar
        statusBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusBar.setPreferredSize(new Dimension(getWidth(), 24));
        statusBar.setBackground(UIManager.getColor("StatusBar.background"));
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);

        // Top Panel
        topPanel = new JPanel(new BorderLayout());
        topPanel.setPreferredSize(new Dimension(getWidth(), UIScale.scale(38)));
        topPanel.setBackground(UIManager.getColor("TitlePane.background"));
        
        // Mouse Adapter for Title Bar (Drag and Double-Click)
        MouseAdapter titleBarMouseAdapter = new MouseAdapter() {
            private Point initialClick;

            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (initialClick == null) return;
                Point windowLoc = getLocation();
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(windowLoc.x + xMoved, windowLoc.y + yMoved);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int state = getExtendedState();
                    if ((state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                        setExtendedState(JFrame.NORMAL);
                    } else {
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                }
            }
        };

        topPanel.addMouseListener(titleBarMouseAdapter);
        topPanel.addMouseMotionListener(titleBarMouseAdapter);
        
        searchButton = new JButton("Search charts...");
        FlatSVGIcon searchIcon = new FlatSVGIcon("icons/search.svg", UIScale.scale(14), UIScale.scale(14));
        Color fg = UIManager.getColor("TextField.foreground");
        if (fg != null) {
            searchIcon.setColorFilter(new FlatSVGIcon.ColorFilter(_ -> fg));
        }
        searchButton.setIcon(searchIcon);
        searchButton.setPreferredSize(new Dimension(searchButton.getPreferredSize().width, UIScale.scale(24)));
        searchButton.putClientProperty("JButton.buttonType", "roundRect");
        searchButton.putClientProperty("FlatLaf.style", "font: $TextField.font; arc: 10; margin: 2,10,2,10; background: darken($TitlePane.background, 5%); foreground: $TextField.foreground; borderWidth: 0; focusWidth: 0");
        searchButton.setFocusable(false);
        searchButton.setToolTipText("Search (Ctrl+K)");
        searchButton.addActionListener(e -> {
            CommandPalette.show(Application.this, navigationTree.getAllNavItems(), item -> updateContent(item.getTitle()));
        });

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        appTitleLabel = new JLabel("ArberCharts Studio");
        applyTitleStyles();
        titleBlock.add(appTitleLabel);

        titleBlock.addMouseListener(titleBarMouseAdapter);
        titleBlock.addMouseMotionListener(titleBarMouseAdapter);

        if (SystemInfo.isMacFullWindowContentSupported) {
            JPanel leftContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            leftContent.setOpaque(false);
            leftContent.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(8), 0, 0, 0));
            Component strut = Box.createHorizontalStrut(70);
            strut.addMouseListener(titleBarMouseAdapter);
            strut.addMouseMotionListener(titleBarMouseAdapter);
            leftContent.add(strut);
            leftContent.add(titleBlock);
            leftContent.addMouseListener(titleBarMouseAdapter);
            leftContent.addMouseMotionListener(titleBarMouseAdapter);
            topPanel.add(leftContent, BorderLayout.WEST);

            JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            topRightPanel.setOpaque(false);
            topRightPanel.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(4), 0, 0, 0));
            topRightPanel.add(searchButton);

            topRightPanel.addMouseListener(titleBarMouseAdapter);
            topRightPanel.addMouseMotionListener(titleBarMouseAdapter);

            topPanel.add(topRightPanel, BorderLayout.EAST);
        } else {
            JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            topRightPanel.setOpaque(false);
            topRightPanel.add(searchButton);

            // Add listeners to topRightPanel as well so clicking near the button works
            topRightPanel.addMouseListener(titleBarMouseAdapter);
            topRightPanel.addMouseMotionListener(titleBarMouseAdapter);

            topPanel.add(topRightPanel, BorderLayout.EAST);
            topPanel.add(titleBlock, BorderLayout.CENTER);
        }

        // Side Panels (Toolbars like IntelliJ)
        leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(UIScale.scale(40), getHeight()));
        leftPanel.setBackground(UIManager.getColor("SideBar.background"));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(true);

        rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(UIScale.scale(40), getHeight()));
        rightPanel.setBackground(UIManager.getColor("SideBar.background"));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(true);

        // Populate modern toolbars with buttons using Actions
        leftPanel.add(Box.createVerticalStrut(6));
        AbstractButton settingsBtn = ChartActions.createToolbarButton(settingsAction);
        leftSidebarGroup.add(settingsBtn);
        leftPanel.add(settingsBtn);

        rightPanel.add(Box.createVerticalStrut(6));
        AbstractButton exportBtn = ChartActions.createToolbarButton(exportAction);
        rightSidebarGroup.add(exportBtn);
        rightPanel.add(exportBtn);
        rightPanel.add(Box.createVerticalStrut(6));
        AbstractButton propertyGridBtn = ChartActions.createToolbarButton(propertyGridAction);
        rightPanel.add(propertyGridBtn);
        rightPanel.add(Box.createVerticalStrut(6));
        // IntelliJ-like: theme switch is an action button, not a tool-window toggle.
        AbstractButton themeBtn = ChartActions.createToolbarActionButton(toggleThemeAction);
        rightPanel.add(themeBtn);

        // IntelliJ-like behavior: keep one button selected per side (no "all off" state)
        enforceSingleSelection(settingsBtn, leftSidebarGroup);
        enforceSingleSelection(exportBtn, rightSidebarGroup);

        // Default selections (IntelliJ usually has a tool window active)
        settingsBtn.setSelected(true);
        // exportBtn is a tool-window toggle on the right stripe; do not preselect it by default.
        exportBtn.setSelected(false);

        // Navigation Tree
        navigationTree = new NavigationTree();
        navigationTree.addTreeSelectionListener(_ -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) navigationTree.getLastSelectedPathComponent();
            if (node == null) return;
            if (node.getChildCount() > 0) return;
            Object userObject = node.getUserObject();
            if (userObject instanceof NavigationTree.NavItem) {
                String title = ((NavigationTree.NavItem) userObject).getTitle();
                updateContent(title);
            }
        });

        // Property Grid
        propertyGrid = new PropertyGrid();
        
        JScrollPane treeScrollPane = new JScrollPane(navigationTree);
        treeScrollPane.setBorder(null);

        // Center Content
        splitPane = new InvisibleSplitPane();
        splitPane.setLeftComponent(treeScrollPane); 
        
        // Nested split for PropertyGrid on the right
        innerSplit = new InvisibleSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        ChartTheme chartTheme = FlatLaf.isLafDark() ? ChartThemes.defaultDark() : ChartThemes.defaultLight();
        JPanel initialPanel = DemoPanelFactory.createPanel("Line Chart", chartTheme);
        activeChartPanel = extractChartPanel(initialPanel);
        configureChartPanel(activeChartPanel);
        innerSplit.setLeftComponent(initialPanel);
        innerSplit.setRightComponent(new JPanel());
        innerSplit.setDividerLocation(1.0);
        innerSplit.setDividerSize(4);
        innerSplit.setResizeWeight(1.0);
        
        splitPane.setRightComponent(innerSplit);
        splitPane.setResizeWeight(0.2);
        // Use the requested 4px invisible divider
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);

        // Layout
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            if (activeChartPanel != null) {
                activeChartPanel.resetZoom();
            }
        });
    }

    private void setPropertyGridVisible(boolean visible) {
        propertyGridVisible = visible;
        if (innerSplit == null) return;

        if (visible) {
            innerSplit.setRightComponent(propertyGrid);
            int width = innerSplit.getWidth();
            int target = width > 0 ? width - UIScale.scale(320) : lastPropertyGridDivider;
            innerSplit.setDividerLocation(target);
            innerSplit.setDividerSize(4);
        } else {
            lastPropertyGridDivider = innerSplit.getDividerLocation();
            innerSplit.setRightComponent(new JPanel());
            innerSplit.setDividerLocation(1.0);
            innerSplit.setDividerSize(4);
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        // Avoid JMenuItem.setAction() icon casting pitfalls by creating explicit menu items.
        JMenuItem exportItem = new JMenuItem(exportAction);
        // Ensure the icon is an Icon (defensive against any accidental String assignment).
        Object exportIcon = exportAction.getValue(Action.SMALL_ICON);
        if (exportIcon instanceof Icon icon) {
            exportItem.setIcon(icon);
        } else {
            exportItem.setIcon(null);
        }
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        JMenu viewMenu = new JMenu("View");
        JMenuItem themeItem = new JMenuItem(toggleThemeAction);
        Object themeIcon = toggleThemeAction.getValue(Action.SMALL_ICON);
        if (themeIcon instanceof Icon icon) {
            themeItem.setIcon(icon);
        } else {
            themeItem.setIcon(null);
        }
        viewMenu.add(themeItem);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem settingsItem = new JMenuItem(settingsAction);
        Object settingsIcon = settingsAction.getValue(Action.SMALL_ICON);
        if (settingsIcon instanceof Icon icon) {
            settingsItem.setIcon(icon);
        } else {
            settingsItem.setIcon(null);
        }
        settingsMenu.add(settingsItem);

        // macOS: About belongs in the system application menu.
        // Windows/Linux: keep it in Help menu.
        if (!SystemInfo.isMacOS) {
            JMenu helpMenu = new JMenu("Help");
            helpMenu.add(new AbstractAction("About") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AboutDialog.show(Application.this);
                }
            });
            menuBar.add(helpMenu);
        }

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(settingsMenu);

        setJMenuBar(menuBar);

        // Register macOS system About handler
        if (SystemInfo.isMacOS) {
            registerMacOsAboutHandler();
        }
    }

    private void registerMacOsAboutHandler() {
        try {
            if (!java.awt.Desktop.isDesktopSupported()) return;
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(ignored -> AboutDialog.show(this));
            }
        } catch (Throwable ignored) {
            // If not supported (or restricted), fall back to having no system About handler.
        }
    }

    private void updateContent(String title) {
        if (title == null || title.isBlank()) {
            title = "Line Chart";
        }
        currentTitle = title;

        ChartTheme chartTheme = FlatLaf.isLafDark() ? ChartThemes.defaultDark() : ChartThemes.defaultLight();
        JPanel newPanel = DemoPanelFactory.createPanel(title, chartTheme);
        activeChartPanel = extractChartPanel(newPanel);
        configureChartPanel(activeChartPanel);

        if (splitPane.getRightComponent() instanceof JSplitPane inner) {
            inner.setLeftComponent(newPanel);
        }
        statusLabel.setText("Selected: " + title);
        
        // Update Property Grid with actual properties if available
        propertyGrid.clear();
        if (newPanel instanceof ArberChartPanel chartPanel) {
            propertyGrid.setTargetPanel(chartPanel);
            propertyGrid.addProperty("Type", title);
            propertyGrid.addProperty("Render Mode", "Antialiased (Core)");
            propertyGrid.addProperty("Layer Count", String.valueOf(chartPanel.getLayerCount()));
            propertyGrid.addProperty("Legend", chartPanel.isLegendVisible() ? "Visible" : "Hidden");
            propertyGrid.addProperty("Tooltips", "Enabled");
            propertyGrid.addProperty("Interactivity", "Zoom & Pan active");
            SwingUtilities.invokeLater(chartPanel::resetZoom);
        } else {
            propertyGrid.setTargetPanel(null);
            propertyGrid.addProperty("Type", title);
            propertyGrid.addProperty("Status", "Demo Only");
        }

        SwingUtilities.invokeLater(() -> {
            if (activeChartPanel != null) {
                activeChartPanel.revalidate();
                activeChartPanel.resetZoom();
                activeChartPanel.repaint();
            }
            repaint();
        });
    }

    private void toggleTheme(ActionEvent e) {
        toggleThemeAction.actionPerformed(e);
    }
    
    private void updateColors() {
        if (statusBar != null) statusBar.setBackground(UIManager.getColor("StatusBar.background"));
        if (topPanel != null) topPanel.setBackground(UIManager.getColor("TitlePane.background"));
        if (leftPanel != null) leftPanel.setBackground(UIManager.getColor("SideBar.background"));
        if (rightPanel != null) rightPanel.setBackground(UIManager.getColor("SideBar.background"));
        applyTitleStyles();
        
        if (searchButton != null) {
            // Re-apply style to ensure theme-dependent variables (like fonts or colors in style string) are updated.
            // Although FlatLaf usually handles this, forcing an update here ensures the button follows the theme.
            searchButton.updateUI();
            
            // Re-set the icon to ensure it picks up the new 'currentColor' from the button's foreground
            if (searchButton.getIcon() instanceof FlatSVGIcon icon) {
                Color fg = UIManager.getColor("TextField.foreground");
                if (fg != null) {
                    icon.setColorFilter(new FlatSVGIcon.ColorFilter(_ -> fg));
                }
                searchButton.setIcon(icon.derive(UIScale.scale(14), UIScale.scale(14)));
            }
            
            // Restore preferred size after UI update because updateUI() might reset it
            searchButton.setPreferredSize(new Dimension(searchButton.getPreferredSize().width, UIScale.scale(24)));
        }
    }

    private void configureChartPanel(ArberChartPanel panel) {
        if (panel == null) return;
        panel.withExportHandler((p, format) -> {
            File target = promptExportFile(format);
            if (target == null) return;
            try {
                String fmt = format != null ? format.toLowerCase() : "png";
                switch (fmt) {
                    case "png", "png8k" -> ChartExportService.exportPng(p, target);
                    case "svg" -> ChartExportService.exportSvg(p, target);
                    case "pdf" -> ChartExportService.exportPdf(p, target);
                    default -> ChartExportService.exportPng(p, target);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private ArberChartPanel extractChartPanel(JPanel panel) {
        if (panel instanceof ArberChartPanel chartPanel) {
            return chartPanel;
        }
        if (panel instanceof ChartHost host) {
            return host.getChartPanel();
        }
        return null;
    }

    private void applyTitleStyles() {
        if (appTitleLabel == null) return;
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = appTitleLabel.getFont();

        appTitleLabel.setFont(base.deriveFont(Font.BOLD, UIScale.scale(13f)));

        Color fg = UIManager.getColor("Label.foreground");
        if (fg != null) {
            appTitleLabel.setForeground(fg);
        }
    }

    private void syncMacMenuBarAppearance() {
        if (!SystemInfo.isMacOS) return;
        String appearance = FlatLaf.isLafDark() ? "dark" : "light";
        System.setProperty("apple.awt.application.appearance", appearance);
        JMenuBar bar = getJMenuBar();
        if (bar != null) {
            bar.updateUI();
        }
    }

    private File promptExportFile(String format) {
        String fmt = format != null ? format.toLowerCase() : "png";
        String ext = fmt.equals("png8k") ? "png" : fmt;
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle("Export Chart as " + ext.toUpperCase());
        chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter(ext.toUpperCase() + " Image", ext));
        if (chooser.showSaveDialog(this) != SystemFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ext)) {
            f = new File(f.getParentFile(), f.getName() + "." + ext);
        }
        return f;
    }

    private static void enforceSingleSelection(AbstractButton button, ButtonGroup group) {
        // ButtonGroup allows "all off" states for toggle buttons; IntelliJ stripes do not.
        // We re-select the button if it would become unselected and no other is selected.
        ItemListener guard = e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                if (group.getSelection() == null) {
                    SwingUtilities.invokeLater(() -> button.setSelected(true));
                }
            }
        };

        button.addItemListener(guard);
    }

    public static void main(String[] args) {
        // Touch args to avoid unused-parameter warnings (keeps canonical main signature)
        if (args != null && args.length > 0 && "--help".equalsIgnoreCase(args[0])) {
            System.out.println("ArberCharts demo (no CLI options).");
        }

        applyJbrTuning();

        // Global Error Handler
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            LOG.log(Level.SEVERE, "Uncaught exception in thread: " + thread.getName(), ex);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred:\n" + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE));
        });

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "ArberCharts");
            System.setProperty("apple.awt.application.appearance", "system");
        }

        // IMPORTANT: We must install FlatLaf at startup, otherwise UI defaults from
        // resources/themes/*.properties won't exist and UIManager lookups will return null.
        try {
            FlatLaf.registerCustomDefaultsSource("themes");

            // Enable native window decorations for Windows 10/11 and Linux
            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            }

            // Default to dark to match modern developer tooling; user can toggle in-app.
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Throwable t) {
            // If FlatLaf setup fails for any reason, fall back to current system LAF.
        }

        EventQueue.invokeLater(() -> new Application().setVisible(true));
    }

    private static void applyJbrTuning() {
        // Prefer runtime defaults, only set when absent to avoid overriding user flags.
        setPropertyIfAbsent("awt.useSystemAAFontSettings", "on");
        setPropertyIfAbsent("swing.aatext", "true");
        setPropertyIfAbsent("sun.java2d.uiScale.enabled", "true");

        if (SystemInfo.isWindows) {
            setPropertyIfAbsent("sun.java2d.dpiaware", "true");
            setPropertyIfAbsent("sun.java2d.opengl", "true");
        } else if (SystemInfo.isLinux) {
            setPropertyIfAbsent("sun.java2d.opengl", "true");
        } else if (SystemInfo.isMacOS) {
            setPropertyIfAbsent("sun.java2d.metal", "true");
        }
    }

    private static void setPropertyIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
