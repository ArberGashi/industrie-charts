package com.arbergashi.charts.actions;

import com.arbergashi.charts.ui.ArberChartPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChartActions {

    /**
     * Client property key used to store the SVG classpath path for an Action.
     * We keep Action.SMALL_ICON as a real Icon (needed by JMenu/JMenuItem) and
     * use this key for theme-aware toolbar buttons.
     */
    private static final String KEY_SVG_PATH = "arbercharts.svgPath";

    /**
     * Cache tinted icons per (path + rgb + size) to avoid re-parsing SVGs.
     * Cleared automatically on Look&Feel changes via toolbar button updateUI().
     */
    private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();
    private static final int SIDEBAR_ICON_SIZE = UIScale.scale(16);

    private static int sidebarIconSize() {
        return SIDEBAR_ICON_SIZE;
    }

    /**
     * Creates an IntelliJ-style sidebar toggle button.
     * <p>
     * This intentionally follows the same tint logic as {@link com.arbergashi.charts.uielements.NavigationTree}:
     * <ul>
     *   <li>Selected/hover uses Tree.selectionForeground (if available)</li>
     *   <li>Otherwise uses component foreground</li>
     *   <li>Fallback to Label.foreground</li>
     * </ul>
     */
    public static AbstractButton createToolbarButton(Action action) {
        JToggleButton btn = new JToggleButton(action) {
            @Override
            public void updateUI() {
                super.updateUI();
                ICON_CACHE.clear();
                applyToolWindowStripeStyle(this);
                updateActionIconTint(this);
            }
        };

        btn.setHideActionText(true);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        applyToolWindowStripeStyle(btn);

        btn.addChangeListener(_ -> updateActionIconTint(btn));

        updateActionIconTint(btn);
        return btn;
    }

    /**
     * Creates an IntelliJ-like sidebar action button (not a toggle/tool-window).
     * Uses the same icon tinting rules as the toolbar toggle buttons.
     */
    public static AbstractButton createToolbarActionButton(Action action) {
        JButton btn = new JButton(action) {
            @Override
            public void updateUI() {
                super.updateUI();
                ICON_CACHE.clear();
                applyToolWindowStripeStyle(this);
                updateActionIconTint(this);
            }
        };

        btn.setHideActionText(true);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        applyToolWindowStripeStyle(btn);

        btn.getModel().addChangeListener(_ -> updateActionIconTint(btn));

        updateActionIconTint(btn);
        return btn;
    }

    private static void applyToolWindowStripeStyle(AbstractButton btn) {
        // FlatLaf: render like tool window stripe buttons (IntelliJ-like)
        btn.putClientProperty("JButton.buttonType", "toolBarButton");

        int padV = UIScale.scale(6);
        int padH = UIScale.scale(8);
        btn.setBorder(BorderFactory.createEmptyBorder(padV, padH, padV, padH));

        // Ensure we don't show any default focus ring
        btn.putClientProperty("JComponent.outline", null);
    }

    private static void updateActionIconTint(AbstractButton btn) {
        Action a = btn.getAction();
        if (a == null) return;

        Object svgPath = a.getValue(KEY_SVG_PATH);
        if (!(svgPath instanceof String iconPath)) return;

        int s = sidebarIconSize();

        boolean selected = btn.getModel().isSelected();
        boolean rollover = btn.getModel().isRollover();

        // IntelliJ-like: icon becomes "active" only when selected.
        // Hover is slightly emphasized but not the selected color.
        Color tint = resolveIconTint(btn, selected, rollover);
        Icon icon = loadTintedIcon(iconPath, s, tint);
        if (icon != null) {
            btn.setIcon(icon);
        }

        applyStripeBackground(btn, selected, rollover, btn.getModel().isPressed());
        btn.repaint();
    }

    private static void applyStripeBackground(AbstractButton btn, boolean selected, boolean rollover, boolean pressed) {
        Color bg = null;

        if (selected) {
            bg = UIManager.getColor("ToggleButton.selectedBackground");
            if (bg == null) bg = UIManager.getColor("Tree.selectionBackground");
            if (bg == null) bg = UIManager.getColor("List.selectionBackground");
        } else if (pressed) {
            bg = UIManager.getColor("ToggleButton.pressedBackground");
            if (bg == null) bg = UIManager.getColor("Button.pressedBackground");
        } else if (rollover) {
            bg = UIManager.getColor("ToggleButton.hoverBackground");
            if (bg == null) bg = UIManager.getColor("Button.hoverBackground");
        }

        if (bg != null) {
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
            btn.setBackground(bg);
        } else {
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
        }
    }

    private static Color resolveIconTint(JComponent owner, boolean selected, boolean rollover) {
        if (selected) {
            Color c = UIManager.getColor("Tree.selectionForeground");
            if (c == null) c = UIManager.getColor("List.selectionForeground");
            if (c != null) return c;
        }

        // For hover, slightly prefer Label.foreground (usually higher contrast) over component fg.
        if (rollover) {
            Color c = UIManager.getColor("Label.foreground");
            if (c != null) return c;
        }

        Color fg = owner.getForeground();
        if (fg != null) return fg;
        Color labelFg = UIManager.getColor("Label.foreground");
        return labelFg != null ? labelFg : Color.BLACK;
    }

    private static Icon loadTintedIcon(String path, int size, Color tint) {
        int rgb = tint.getRGB();
        String key = path + "|" + size + "|" + rgb;
        return ICON_CACHE.computeIfAbsent(key, _ -> createTintedIcon(path, size, tint));
    }

    private static Icon createTintedIcon(String path, int size, Color tint) {
        try {
            FlatSVGIcon svg = new FlatSVGIcon(path, size, size);
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(_ -> tint));
            return svg;
        } catch (Throwable t) {
            return null;
        }
    }


    private static void setupActionSvgIcon(Action action, String path) {
        int s = sidebarIconSize();
        // Always provide a real Icon for menus etc.
        action.putValue(Action.SMALL_ICON, new FlatSVGIcon(path, s, s));
        // Store path for theme-aware toolbar button tinting.
        action.putValue(KEY_SVG_PATH, path);
    }

    public static class ToggleThemeAction extends AbstractAction {
        private final Runnable onThemeChanged;

        public ToggleThemeAction(Runnable onThemeChanged) {
            super("Toggle Theme");
            this.onThemeChanged = onThemeChanged;
            setupActionSvgIcon(this, "icons/theme.svg");
            putValue(Action.SHORT_DESCRIPTION, "Switch between light and dark theme");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Ensure our demo theme properties are used after switching.
            FlatLaf.registerCustomDefaultsSource("themes");

            if (FlatLaf.isLafDark()) {
                FlatLightLaf.setup();
            } else {
                FlatDarkLaf.setup();
            }

            if (onThemeChanged != null) {
                onThemeChanged.run();
            }
        }
    }

    public static class ExportAction extends AbstractAction {
        private final java.util.function.Supplier<ArberChartPanel> panelSupplier;

        public ExportAction(java.util.function.Supplier<ArberChartPanel> panelSupplier) {
            super("Export");
            this.panelSupplier = panelSupplier;
            setupActionSvgIcon(this, "icons/export.svg");
            putValue(Action.SHORT_DESCRIPTION, "Export chart as image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ArberChartPanel panel = panelSupplier.get();
            if (panel != null) {
                // Show a small popup menu to choose format
                JPopupMenu menu = new JPopupMenu();
                menu.add(new AbstractAction("Export as PNG") {
                    @Override
                    public void actionPerformed(ActionEvent __) {
                        panel.exportAs("png");
                    }
                });
                menu.add(new AbstractAction("Export as PNG (8K)") {
                    @Override
                    public void actionPerformed(ActionEvent __) {
                        panel.exportAs("png8k");
                    }
                });
                menu.add(new AbstractAction("Export as SVG") {
                    @Override
                    public void actionPerformed(ActionEvent __) {
                        panel.exportAs("svg");
                    }
                });

                if (e.getSource() instanceof Component comp) {
                    menu.show(comp, 0, comp.getHeight());
                } else {
                    menu.show(null, 100, 100);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No active chart to export.");
            }
        }
    }

    public static class SettingsAction extends AbstractAction {
        public SettingsAction() {
            super("Settings");
            setupActionSvgIcon(this, "icons/settings.svg");
            putValue(Action.SHORT_DESCRIPTION, "Open application settings");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Window owner = null;
            final JDialog dialog = new JDialog(owner, "ArberCharts Settings", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setLayout(new BorderLayout());

            JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
            content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            content.add(new JLabel("Anti-Aliasing:"));
            content.add(new JCheckBox("", true));

            content.add(new JLabel("Animation Speed:"));
            content.add(new JComboBox<>(new String[]{"None", "Fast (200ms)", "Medium (500ms)", "Slow (1000ms)"}));

            content.add(new JLabel("HiDPI Scaling:"));
            content.add(new JLabel("System Managed"));

            content.add(new JLabel("Renderer Backend:"));
            content.add(new JLabel("Java2D (Pipeline: " + System.getProperty("sun.java2d.opengl", "false").replace("true", "OpenGL").replace("false", "Software") + ")"));

            dialog.add(content, BorderLayout.CENTER);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(_ -> dialog.dispose());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(closeBtn);
            dialog.add(south, BorderLayout.SOUTH);

            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }
    }

    public static class TogglePropertyGridAction extends AbstractAction {
        private final Consumer<Boolean> onToggle;

        public TogglePropertyGridAction(Consumer<Boolean> onToggle) {
            super("Property Grid");
            this.onToggle = onToggle;
            setupActionSvgIcon(this, "icons/renderers.svg");
            putValue(Action.SHORT_DESCRIPTION, "Show or hide the Property Grid");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean selected = true;
            if (e.getSource() instanceof AbstractButton button) {
                selected = button.isSelected();
            }
            onToggle.accept(selected);
        }
    }
}
