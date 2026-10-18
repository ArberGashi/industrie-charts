package com.arbergashi.charts.demo;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;
import com.formdev.flatlaf.util.UIScale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoWorkbenchLayoutTest {
    private static final String LONG_RENDERER = "com.arbergashi.charts.render.financial.PointAndFigureAdvancedRenderer";

    static Stream<Arguments> layouts() {
        var layouts = Stream.<Arguments>builder();
        for (String theme : new String[]{"light", "dark"}) {
            for (int[] size : new int[][]{{1360, 880}, {1000, 680}}) {
                for (float scale : new float[]{1f, 1.5f}) {
                    for (boolean macTitleBar : new boolean[]{true, false}) {
                        layouts.add(Arguments.of(theme, size[0], size[1], scale, macTitleBar));
                    }
                }
            }
        }
        return layouts.build();
    }

    @ParameterizedTest(name = "{0}, {1}x{2}, UI scale {3}, macOS title row {4}")
    @MethodSource("layouts")
    void selectingALongerRendererUsesTheAvailableToolbarWidth(
            String theme, int width, int height, float scale, boolean macTitleBar) throws Exception {
        String previousTheme = System.getProperty("demo.theme");
        String previousScale = System.getProperty("flatlaf.uiScale");
        Object previousDefaultFont = UIManager.get("defaultFont");
        try {
            SwingUtilities.invokeAndWait(() -> verifyRendererTitle(theme, width, height, scale, macTitleBar));
        } finally {
            restoreProperty("demo.theme", previousTheme);
            restoreProperty("flatlaf.uiScale", previousScale);
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", previousDefaultFont);
                DemoThemeSupport.setupLookAndFeel();
            });
        }
    }

    private static void verifyRendererTitle(String theme, int width, int height, float scale, boolean macTitleBar) {
        DemoApplication application = null;
        JTree tree = null;
        try {
            System.setProperty("demo.theme", theme);
            System.setProperty("flatlaf.uiScale", Float.toString(scale));
            application = new DemoApplication(DemoThemeSupport.setupLookAndFeel());
            assertEquals(scale, UIScale.getUserScaleFactor(), 0.001f, "The requested UI scaling must actually be active");
            JPanel workspace = application.buildWorkspace(macTitleBar);
            Method buildMenu = DemoApplication.class.getDeclaredMethod("buildMenuBar", JFrame.class, boolean.class);
            buildMenu.setAccessible(true);
            JMenuBar menuBar = (JMenuBar) buildMenu.invoke(application, null, macTitleBar);
            menuBar.setSize(width, menuBar.getPreferredSize().height);
            layout(menuBar);
            workspace.setSize(width, height);
            layout(workspace);
            tree = (JTree) component(application, "tree");
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            var entries = root.depthFirstEnumeration();
            while (entries.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) entries.nextElement();
                if (node.getUserObject() instanceof RendererCatalogEntry entry
                        && entry.className().equals(LONG_RENDERER)) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
            layout(workspace);

            JPanel toolbar = (JPanel) component(application, "editorToolbar");
            JLabel title = (JLabel) component(application, "activeRendererLabel");
            Component category = component(application, "categoryLabel");
            Component reload = component(application, "reloadButton");
            Component export = component(application, "exportButton");
            JButton search = (JButton) component(application, "searchEverywhereButton");
            JButton themeSwitch = (JButton) component(application, "themeSwitchButton");
            assertEquals("PointAndFigureAdvancedRenderer", title.getText());
            assertTrue(title.getWidth() >= title.getPreferredSize().width,
                    "The selected renderer name must use the available width at " + width + " in " + theme);
            Rectangle titleBounds = boundsIn(title, toolbar);
            Rectangle categoryBounds = boundsIn(category, toolbar);
            Rectangle reloadBounds = boundsIn(reload, toolbar);
            Rectangle exportBounds = boundsIn(export, toolbar);
            assertTrue(titleBounds.getMaxY() < categoryBounds.y,
                    "The renderer name and its category must have separate rows with breathing room");
            assertTrue(categoryBounds.getMaxY() < toolbar.getHeight(), "The category must fit inside the toolbar");
            boolean stacked = reloadBounds.y > categoryBounds.getMaxY();
            assertEquals(width == 1000 && scale == 1.5f, stacked,
                    "Only the narrow, scaled editor needs an additional chart-action row");
            if (stacked) {
                assertTrue(reloadBounds.y - categoryBounds.getMaxY() >= UIScale.scale(8),
                        "Wrapped actions need a clear gap below the renderer identity");
                assertTrue(reloadBounds.getMaxY() < toolbar.getHeight());
            } else {
                assertTrue(titleBounds.getMaxX() < reloadBounds.x, "Chart actions must not overlap the renderer name");
            }
            assertTrue(reloadBounds.getMaxX() < exportBounds.x, "Reload and export need separate hit areas");
            assertTrue(exportBounds.getMaxX() < toolbar.getWidth(), "The export action needs a trailing inset");
            assertButtonTextFits((JButton) reload);
            assertButtonTextFits((JButton) export);

            assertTrue(toolbar.getHeight() <= UIScale.scale(stacked ? 98 : 56),
                    "The toolbar must stay compact and wrap only when horizontal space is insufficient");
            assertTrue(reload.getHeight() >= UIScale.scale(28));
            assertTrue(export.getHeight() >= UIScale.scale(28));
            assertSame(reload.getParent(), export.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(reload, toolbar));
            assertFalse(SwingUtilities.isDescendingFrom(search, toolbar),
                    "Global search belongs in the window chrome, not the chart action area");
            assertFalse(SwingUtilities.isDescendingFrom(themeSwitch, toolbar));

            BorderLayout workspaceLayout = (BorderLayout) workspace.getLayout();
            JComponent shell = (JComponent) workspaceLayout.getLayoutComponent(BorderLayout.CENTER);
            JSplitPane split = findSplit(shell, "catalogSplit");
            assertNotNull(split);
            assertTrue(SwingUtilities.isDescendingFrom(toolbar, split.getRightComponent()),
                    "Renderer details and actions must remain in the editor beside the catalog");
            Container titleControls;
            if (macTitleBar) {
                JPanel titleBar = (JPanel) workspaceLayout.getLayoutComponent(BorderLayout.NORTH);
                assertEquals(UIScale.scale(36), titleBar.getHeight());
                assertEquals(titleBar.getHeight(), shell.getY(), "The split workspace starts directly below the native title area");
                assertEquals(1, labels(titleBar).size(), "The title area contains only the application title");
                BorderLayout titleLayout = (BorderLayout) titleBar.getLayout();
                JComponent windowButtons = (JComponent) titleLayout.getLayoutComponent(BorderLayout.WEST);
                assertEquals("mac horizontal zeroInFullScreen", windowButtons.getClientProperty(
                        FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_PLACEHOLDER));
                JLabel caption = (JLabel) titleLayout.getLayoutComponent(BorderLayout.CENTER);
                assertEquals("Industrie Charts", caption.getText());
                assertNull(caption.getIcon());
                assertEquals(SwingConstants.CENTER, caption.getHorizontalAlignment());
                assertTrue(caption.getFontMetrics(caption.getFont()).stringWidth(caption.getText()) <= caption.getWidth());
                assertEquals(Boolean.TRUE, caption.getClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION));
                assertEquals(0, caption.getMouseListeners().length, "Native window gestures must not be intercepted");
                assertEquals(0, caption.getMouseMotionListeners().length);
                titleControls = titleBar;
            } else {
                assertNull(workspaceLayout.getLayoutComponent(BorderLayout.NORTH),
                        "Windows and Linux must not gain a second global toolbar row");
                assertEquals(0, shell.getY());
                titleControls = menuBar;
            }
            assertTrue(SwingUtilities.isDescendingFrom(search, titleControls));
            assertTrue(SwingUtilities.isDescendingFrom(themeSwitch, titleControls));
            Rectangle searchBounds = boundsIn(search, titleControls);
            Rectangle themeBounds = boundsIn(themeSwitch, titleControls);
            assertTrue(searchBounds.x > titleControls.getWidth() / 2, "Window tools must stay at the trailing edge");
            assertTrue(searchBounds.getMaxX() < themeBounds.x);
            assertTrue(themeBounds.getMaxX() <= titleControls.getWidth());
            for (JButton button : new JButton[]{search, themeSwitch}) {
                Rectangle bounds = boundsIn(button, titleControls);
                assertTrue(bounds.y >= 0 && bounds.getMaxY() <= titleControls.getHeight());
                assertEquals(UIScale.scale(30), button.getHeight());
                assertEquals(UIScale.scale(18), button.getIcon().getIconWidth());
                assertTrue(button.getText() == null || button.getText().isEmpty());
                assertFalse(button.getAccessibleContext().getAccessibleName().isBlank());
                assertFalse(button.getToolTipText().isBlank());
                assertTrue(button.isFocusable());
                assertEquals(1, button.getActionListeners().length);
            }
            assertFalse(buttons(workspace).stream().anyMatch(button -> "About".equals(button.getText())),
                    "About must not occupy the working area");
            assertMenuAndShortcuts(application, menuBar, macTitleBar);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        } finally {
            if (tree != null) ToolTipManager.sharedInstance().unregisterComponent(tree);
            if (application != null) {
                try {
                    Method dispose = DemoApplication.class.getDeclaredMethod("disposeDemoResources");
                    dispose.setAccessible(true);
                    dispose.invoke(application);
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError(exception);
                }
            }
        }
    }

    private static void assertMenuAndShortcuts(DemoApplication application, JMenuBar menuBar, boolean macOS)
            throws ReflectiveOperationException {
        JMenu help = null;
        for (int index = 0; index < menuBar.getMenuCount(); index++) {
            JMenu menu = menuBar.getMenu(index);
            if (menu != null && "Help".equals(menu.getText())) help = menu;
        }
        assertNotNull(help);
        JMenuItem about = help.getItem(0);
        assertEquals("About Industrie Charts", about.getText());
        assertTrue(about.isVisible());
        assertEquals(1, about.getActionListeners().length);

        JRootPane root = new JRootPane();
        Method shortcuts = DemoApplication.class.getDeclaredMethod("setupKeyboardShortcuts", JRootPane.class);
        shortcuts.setAccessible(true);
        shortcuts.invoke(application, root);
        int mask = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
        for (int key : new int[]{KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_T, KeyEvent.VK_F, KeyEvent.VK_B, KeyEvent.VK_1, KeyEvent.VK_2}) {
            assertNotNull(root.getActionForKeyStroke(KeyStroke.getKeyStroke(key, mask)),
                    "Existing workbench shortcuts must remain registered");
        }
        Method configure = DemoApplication.class.getDeclaredMethod("configurePlatformWindow", JRootPane.class, boolean.class);
        configure.setAccessible(true);
        configure.invoke(null, root, macOS);
        if (macOS) {
            assertEquals(Boolean.TRUE, root.getClientProperty("apple.awt.fullWindowContent"));
            assertEquals(Boolean.TRUE, root.getClientProperty("apple.awt.transparentTitleBar"));
            assertEquals(Boolean.FALSE, root.getClientProperty("apple.awt.windowTitleVisible"));
            assertNull(root.getClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT),
                    "The macOS row must not depend on the Windows-only title-height property");
        } else {
            assertEquals(Boolean.TRUE, root.getClientProperty(FlatClientProperties.MENU_BAR_EMBEDDED));
            assertEquals(Boolean.TRUE, root.getClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS));
            assertEquals(Boolean.TRUE, root.getClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE));
            assertEquals(Boolean.FALSE, root.getClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON));
        }
    }

    static void assertButtonTextFits(JButton button) {
        Insets insets = button.getInsets();
        Rectangle available = new Rectangle(0, 0,
                button.getWidth() - insets.left - insets.right,
                button.getHeight() - insets.top - insets.bottom);
        String rendered = SwingUtilities.layoutCompoundLabel(button, button.getFontMetrics(button.getFont()),
                button.getText(), button.getIcon(), button.getVerticalAlignment(), button.getHorizontalAlignment(),
                button.getVerticalTextPosition(), button.getHorizontalTextPosition(), available,
                new Rectangle(), new Rectangle(), button.getIconTextGap());
        assertEquals(button.getText(), rendered, "The actual font and insets must not truncate the action caption");
    }

    private static JSplitPane findSplit(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JSplitPane split && name.equals(split.getName())) return split;
            if (child instanceof Container nested) {
                JSplitPane found = findSplit(nested, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static List<JLabel> labels(Container parent) {
        List<JLabel> labels = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel label) labels.add(label);
            if (child instanceof Container nested) labels.addAll(labels(nested));
        }
        return labels;
    }

    private static List<JButton> buttons(Container parent) {
        List<JButton> buttons = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) buttons.add(button);
            if (child instanceof Container nested) buttons.addAll(buttons(nested));
        }
        return buttons;
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }

    private static Component component(DemoApplication application, String name) throws ReflectiveOperationException {
        Field field = DemoApplication.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Component) field.get(application);
    }

    private static Rectangle boundsIn(Component component, Container parent) {
        return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), parent);
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container nested) layout(nested);
        }
    }
}
