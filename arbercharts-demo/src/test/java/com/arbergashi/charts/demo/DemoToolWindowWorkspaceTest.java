package com.arbergashi.charts.demo;

import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.formdev.flatlaf.util.UIScale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class DemoToolWindowWorkspaceTest {
    private static final String LONG_RENDERER = "com.arbergashi.charts.render.financial.PointAndFigureAdvancedRenderer";
    private static boolean backendProbeInitialized;

    static Stream<Arguments> appearances() {
        return Stream.of(Arguments.of("light", 1f), Arguments.of("dark", 1f),
                Arguments.of("light", 1.5f), Arguments.of("dark", 1.5f));
    }

    static Stream<Arguments> scaledAppearances() {
        return Stream.of(Arguments.of("light", 1.5f), Arguments.of("dark", 1.5f));
    }

    @ParameterizedTest(name = "Catalog retention: {0}, scale {1}")
    @MethodSource("appearances")
    void catalogControlsPreserveTheActualFilteredTreeScrollAndChart(String theme, float scale) throws Exception {
        try (Fixture fixture = new Fixture(theme, scale)) {
            onEdt(() -> {
                fixture.assertVisibility(true, false);
                JTextField filter = fixture.field("catalogFilter");
                JTree tree = fixture.field("tree");
                filter.setText("renderer");
                layout(fixture.workspace);
                JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tree);
                assertNotNull(scroll);
                scroll.getViewport().setViewPosition(new Point(0, 180));
                Point position = scroll.getViewport().getViewPosition();
                assertTrue(position.y > 0, "The fixture must exercise a genuinely scrolled catalog");
                Object model = tree.getModel();
                TreePath selection = tree.getSelectionPath();
                int rows = tree.getRowCount();
                ArberChartPanel chart = fixture.chart();
                Object data = chart.getPrimaryModel();

                fixture.button("catalogToggleButton").doClick(0);
                fixture.assertVisibility(false, false);
                fixture.menu(fixture.action("catalogToggleAction")).doClick(0);
                fixture.assertVisibility(true, false);
                fixture.shortcut("catalogToggleAction").actionPerformed(new ActionEvent(tree, 0, "shortcut"));
                fixture.assertVisibility(false, false);
                fixture.shortcut("catalogToggleAction").actionPerformed(new ActionEvent(tree, 0, "shortcut"));
                layout(fixture.workspace);
                fixture.assertVisibility(true, false);

                assertEquals("renderer", filter.getText());
                assertSame(model, tree.getModel(), "Opening a panel must not reconstruct the filtered tree");
                assertEquals(selection, tree.getSelectionPath());
                assertEquals(rows, tree.getRowCount(), "Expanded categories must remain expanded");
                assertEquals(position, scroll.getViewport().getViewPosition());
                assertSame(chart, fixture.chart());
                assertSame(data, fixture.chart().getPrimaryModel());
                assertTrue(chart.isFocusable(), "Closing a tool panel must have a usable chart focus target");
            });
        }
    }

    @ParameterizedTest(name = "Shared actions: {0}, scale {1}")
    @MethodSource("appearances")
    void toolMenuToolbarAndShortcutActionsShareLoadingStateAndReloadTheVisibleChart(String theme, float scale)
            throws Exception {
        try (Fixture fixture = new Fixture(theme, scale)) {
            onEdt(() -> {
                fixture.button("toolsToggleButton").doClick(0);
                fixture.assertVisibility(true, true);
                for (String name : new String[]{"reloadAction", "exportPngAction", "benchmarkAction"}) {
                    Action action = fixture.action(name);
                    assertSame(action, fixture.menu(action).getAction());
                    AbstractButton tool = fixture.toolButton(action);
                    assertSame(action, tool.getAction());
                    assertTrue(tool.isEnabled());
                    if (action.getValue(Action.ACCELERATOR_KEY) != null) {
                        assertSame(action, fixture.shortcut(name));
                    }
                }
                assertSame(fixture.action("reloadAction"), fixture.button("reloadButton").getAction());
                assertSame(fixture.action("exportPngAction"), fixture.button("exportButton").getAction());
                fixture.select(LONG_RENDERER);
                fixture.assertExportActionsEnabled(false);
            });
            ArberChartPanel original = fixture.awaitChart();
            onEdt(() -> {
                fixture.assertExportActionsEnabled(true);
                assertEquals("PointAndFigureAdvancedRenderer\nFinancial", ((JTextArea) fixture.field("rendererInfo")).getText());
                fixture.toolButton(fixture.action("reloadAction")).doClick(0);
                fixture.assertExportActionsEnabled(false);
            });
            ArberChartPanel replacement = fixture.awaitChart();
            assertNotSame(original, replacement, "The actual tools button must reload the displayed chart");
            onEdt(() -> {
                fixture.assertExportActionsEnabled(true);
                assertEquals(LONG_RENDERER, replacement.getPrimaryRenderer().getClass().getName());
                assertTrue(SwingUtilities.isDescendingFrom(replacement, fixture.workspace));
            });
        }
    }

    @ParameterizedTest(name = "Width and resize: {0}, scale {1}")
    @MethodSource("appearances")
    void userWidthsSurviveHideReopenAndTemporaryWindowConstraints(String theme, float scale) throws Exception {
        try (Fixture fixture = new Fixture(theme, scale)) {
            onEdt(() -> {
                fixture.select(LONG_RENDERER);
                fixture.button("toolsToggleButton").doClick(0);
                layout(fixture.workspace);
                JSplitPane catalog = fixture.split("catalogSplit");
                JSplitPane tools = fixture.split("toolsSplit");
                catalog.setDividerLocation(UIScale.scale(315));
                tools.setDividerLocation(tools.getWidth() - dividerWidth(tools) - UIScale.scale(275));
                layout(fixture.workspace);
                assertEquals(UIScale.scale(315), catalog.getLeftComponent().getWidth());
                assertEquals(UIScale.scale(275), tools.getRightComponent().getWidth());
                assertEquals(UIScale.scale(6), dividerWidth(catalog));
                assertEquals(UIScale.scale(6), dividerWidth(tools));

                fixture.button("catalogToggleButton").doClick(0);
                fixture.button("catalogToggleButton").doClick(0);
                fixture.button("toolsToggleButton").doClick(0);
                fixture.button("toolsToggleButton").doClick(0);
                layout(fixture.workspace);
                assertEquals(UIScale.scale(315), catalog.getLeftComponent().getWidth());
                assertEquals(UIScale.scale(275), tools.getRightComponent().getWidth());

                fixture.resize(1000, 680);
                fixture.assertVisibility(scale == 1f, true);
                assertTrue(catalog.getRightComponent().getWidth() >= UIScale.scale(420));
                assertTrue(tools.getRightComponent().getWidth() >= UIScale.scale(160));
                for (String name : new String[]{"reloadAction", "exportPngAction", "benchmarkAction"}) {
                    DemoWorkbenchLayoutTest.assertButtonTextFits((JButton) fixture.toolButton(fixture.action(name)));
                }
                if (scale == 1.5f) fixture.button("catalogToggleButton").doClick(0);
                else fixture.button("toolsToggleButton").doClick(0);
                layout(fixture.workspace);
                fixture.assertVisibility(true, false);
                assertTrue(catalog.getLeftComponent().getWidth() >= UIScale.scale(160));
                fixture.resize(1800, 900);
                fixture.assertVisibility(true, false);
                assertEquals(UIScale.scale(315), catalog.getLeftComponent().getWidth());
                fixture.button("toolsToggleButton").doClick(0);
                layout(fixture.workspace);
                fixture.assertVisibility(true, true);
                assertEquals(UIScale.scale(275), tools.getRightComponent().getWidth());
            });
        }
    }

    @ParameterizedTest(name = "Explicit opening priority: {0}, scale {1}")
    @MethodSource("scaledAppearances")
    void explicitToolOpeningBeatsOldCatalogFocusWhilePassiveResizePreservesFocusedSide(String theme, float scale)
            throws Exception {
        try (Fixture fixture = new Fixture(theme, scale)) {
            onEdt(() -> {
                KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                TestFocusManager focus = new TestFocusManager(fixture.field("catalogFilter"));
                KeyboardFocusManager.setCurrentKeyboardFocusManager(focus);
                try {
                    fixture.select(LONG_RENDERER);
                    fixture.resize(1000, 680);
                    fixture.shortcut("toolsToggleAction").actionPerformed(new ActionEvent(fixture.workspace, 0, "shortcut"));
                    layout(fixture.workspace);
                    fixture.assertVisibility(false, true);
                    fixture.resize(1800, 900);
                    fixture.button("catalogToggleButton").doClick(0);
                    fixture.assertVisibility(true, true);
                    fixture.resize(1000, 680);
                    fixture.assertVisibility(true, false);
                } finally {
                    KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
                }
            });
        }
    }

    @ParameterizedTest(name = "Font-dependent action width: {0}, scale {1}")
    @MethodSource("appearances")
    void actionWidthsFollowTheActualFontInsteadOfFreezingAnEarlyPreferredSize(String theme, float scale) throws Exception {
        try (Fixture fixture = new Fixture(theme, scale)) {
            onEdt(() -> {
                fixture.button("toolsToggleButton").doClick(0);
                JButton reload = (JButton) fixture.button("reloadButton");
                JButton export = (JButton) fixture.button("exportButton");
                int oldWidth = reload.getPreferredSize().width;
                Font font = new Font(Font.SANS_SERIF, Font.BOLD, UIScale.scale(18));
                for (AbstractButton button : new AbstractButton[]{reload, export,
                        fixture.toolButton(fixture.action("reloadAction"))}) {
                    button.setFont(font);
                    // Offscreen trees have no validation root to invalidate BoxLayout's cached sizes.
                    button.getParent().invalidate();
                }
                layout(fixture.workspace);
                assertTrue(reload.getPreferredSize().width > oldWidth,
                        "Native or user font changes must update the calculated action width");
                DemoWorkbenchLayoutTest.assertButtonTextFits(reload);
                DemoWorkbenchLayoutTest.assertButtonTextFits(export);
                DemoWorkbenchLayoutTest.assertButtonTextFits((JButton) fixture.toolButton(fixture.action("reloadAction")));
            });
        }
    }

    private static final class TestFocusManager extends DefaultKeyboardFocusManager {
        private final Component focus;

        TestFocusManager(Component focus) {
            this.focus = focus;
        }

        @Override
        public Component getFocusOwner() {
            return focus;
        }
    }

    @Test
    void theStandardDistributionDoesNotOfferExportsWhoseOptionalBackendIsAbsent() throws Exception {
        ClassLoader loader = com.arbergashi.charts.platform.export.ChartExportService.class.getClassLoader();
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.pdfbox.pdmodel.PDDocument", false, loader));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.jfree.svg.SVGGraphics2D", false, loader));
        try (Fixture fixture = new Fixture("light", 1f)) {
            onEdt(() -> {
                fixture.button("toolsToggleButton").doClick(0);
                for (String name : new String[]{"exportPdfAction", "exportSvgAction"}) {
                    Action action = fixture.action(name);
                    assertFalse(action.isEnabled(), "An unavailable export must stay disabled even after chart loading");
                    for (int index = 0; index < fixture.menuBar.getMenuCount(); index++) {
                        JMenu menu = fixture.menuBar.getMenu(index);
                        if (menu == null) continue;
                        for (Component child : menu.getMenuComponents()) {
                            if (child instanceof JMenuItem item) assertNotSame(action, item.getAction());
                        }
                    }
                    for (Component child : descendants((JPanel) fixture.field("chartToolsPanel"))) {
                        if (child instanceof AbstractButton button) assertNotSame(action, button.getAction());
                    }
                }
                fixture.assertExportActionsEnabled(true);
            });
        }
    }

    @Test
    void optionalBackendDetectionDoesNotInitializeTheBackendClass() throws Exception {
        Method available = DemoApplication.class.getDeclaredMethod("exportBackendAvailable", String.class);
        available.setAccessible(true);
        assertFalse(backendProbeInitialized);
        assertEquals(Boolean.TRUE, available.invoke(null, InitializationProbe.class.getName()));
        assertFalse(backendProbeInitialized, "Checking optional capability must not run backend initialization");
        assertEquals(Boolean.FALSE, available.invoke(null, "missing.demo.ExportBackend"));
    }

    private static final class InitializationProbe {
        static {
            backendProbeInitialized = true;
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final String previousTheme = System.getProperty("demo.theme");
        private final String previousScale = System.getProperty("flatlaf.uiScale");
        private final Object previousFont = UIManager.get("defaultFont");
        private DemoApplication application;
        private JPanel workspace;
        private JMenuBar menuBar;
        private JRootPane root;

        Fixture(String theme, float scale) throws Exception {
            onEdt(() -> {
                System.setProperty("demo.theme", theme);
                System.setProperty("flatlaf.uiScale", Float.toString(scale));
                application = new DemoApplication(DemoThemeSupport.setupLookAndFeel());
                Method menu = DemoApplication.class.getDeclaredMethod("buildMenuBar", JFrame.class);
                menu.setAccessible(true);
                menuBar = (JMenuBar) menu.invoke(application, (JFrame) null);
                workspace = application.buildWorkspace();
                root = new JRootPane();
                Method shortcuts = DemoApplication.class.getDeclaredMethod("setupKeyboardShortcuts", JRootPane.class);
                shortcuts.setAccessible(true);
                shortcuts.invoke(application, root);
                resize(1800, 900);
            });
            awaitChart();
        }

        @SuppressWarnings("unchecked")
        <T> T field(String name) throws ReflectiveOperationException {
            Field field = DemoApplication.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(application);
        }

        Action action(String name) throws ReflectiveOperationException {
            return field(name);
        }

        AbstractButton button(String name) throws ReflectiveOperationException {
            return field(name);
        }

        ArberChartPanel chart() throws ReflectiveOperationException {
            return field("currentChartPanel");
        }

        Action shortcut(String name) throws ReflectiveOperationException {
            Action action = action(name);
            Object binding = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(
                    (KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
            Action bound = root.getActionMap().get(binding);
            assertSame(action, bound);
            return bound;
        }

        JMenuItem menu(Action action) {
            for (int index = 0; index < menuBar.getMenuCount(); index++) {
                JMenu menu = menuBar.getMenu(index);
                if (menu == null) continue;
                for (Component child : menu.getMenuComponents()) {
                    if (child instanceof JMenuItem item && item.getAction() == action) return item;
                }
            }
            throw new AssertionError("No menu item for " + action.getValue(Action.NAME));
        }

        AbstractButton toolButton(Action action) throws ReflectiveOperationException {
            return descendants((Container) field("chartToolsPanel")).stream()
                    .filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                    .filter(button -> button.getAction() == action).findFirst().orElseThrow();
        }

        JSplitPane split(String name) {
            return descendants(workspace).stream().filter(JSplitPane.class::isInstance).map(JSplitPane.class::cast)
                    .filter(split -> name.equals(split.getName())).findFirst().orElseThrow();
        }

        void assertVisibility(boolean catalog, boolean tools) throws ReflectiveOperationException {
            for (String prefix : new String[]{"catalog", "tools"}) {
                boolean expected = prefix.equals("catalog") ? catalog : tools;
                Action action = action(prefix + "ToggleAction");
                assertEquals(expected, action.getValue(Action.SELECTED_KEY));
                assertEquals(expected, button(prefix + "ToggleButton").isSelected());
                assertEquals(expected, menu(action).isSelected());
            }
            assertEquals(catalog, ((JPanel) field("sidebarPanel")).isVisible());
            assertEquals(tools, ((JPanel) field("chartToolsPanel")).isVisible());
            for (String name : new String[]{"catalogToggleButton", "toolsToggleButton"}) {
                AbstractButton toggle = button(name);
                assertTrue(toggle.isVisible());
                assertFalse(toggle.getAccessibleContext().getAccessibleName().isBlank());
                assertFalse(toggle.getToolTipText().isBlank());
                assertEquals(UIScale.scale(30), toggle.getWidth());
            }
        }

        void assertExportActionsEnabled(boolean enabled) throws ReflectiveOperationException {
            for (String name : new String[]{"exportPngAction", "benchmarkAction"}) {
                Action action = action(name);
                assertEquals(enabled, action.isEnabled());
                assertEquals(enabled, menu(action).isEnabled());
                assertEquals(enabled, toolButton(action).isEnabled());
            }
            assertEquals(enabled, button("exportButton").isEnabled());
        }

        void select(String name) throws ReflectiveOperationException {
            JTree tree = field("tree");
            var nodes = ((DefaultMutableTreeNode) tree.getModel().getRoot()).depthFirstEnumeration();
            while (nodes.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                if (node.getUserObject() instanceof RendererCatalogEntry entry && entry.className().equals(name)) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    return;
                }
            }
            fail("Missing catalog renderer: " + name);
        }

        void resize(int width, int height) {
            workspace.setSize(width, height);
            layout(workspace);
        }

        ArberChartPanel awaitChart() throws Exception {
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (System.nanoTime() < deadline) {
                AtomicReference<ArberChartPanel> current = new AtomicReference<>();
                onEdt(() -> {
                    layout(workspace);
                    current.set(chart());
                });
                if (current.get() != null) return current.get();
                Thread.sleep(10);
            }
            throw new AssertionError("The real selected chart did not finish loading");
        }

        @Override
        public void close() throws Exception {
            onEdt(() -> {
                ToolTipManager.sharedInstance().unregisterComponent((JTree) field("tree"));
                Method dispose = DemoApplication.class.getDeclaredMethod("disposeDemoResources");
                dispose.setAccessible(true);
                dispose.invoke(application);
                restore("demo.theme", previousTheme);
                restore("flatlaf.uiScale", previousScale);
                UIManager.put("defaultFont", previousFont);
                DemoThemeSupport.setupLookAndFeel();
            });
        }
    }

    private static List<Component> descendants(Container container) {
        List<Component> result = new ArrayList<>();
        for (Component child : container.getComponents()) {
            result.add(child);
            if (child instanceof Container nested) result.addAll(descendants(nested));
        }
        return result;
    }

    private static int dividerWidth(JSplitPane split) {
        return ((BasicSplitPaneUI) split.getUI()).getDivider().getWidth();
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layout(nested);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }

    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
