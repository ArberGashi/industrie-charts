package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DemoWorkspaceTest {
    private static final String LINE = "com.arbergashi.charts.render.standard.LineRenderer";
    private static final String CANDLESTICK = "com.arbergashi.charts.render.financial.CandlestickRenderer";
    private static final String ADAPTIVE = "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer";

    private DemoApplication application;
    private JPanel workspace;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void createWorkspace() throws Exception {
        onEdt(() -> {
            String theme = DemoThemeSupport.setupLookAndFeel();
            application = new DemoApplication(theme);
            workspace = application.buildWorkspace();
            workspace.setSize(1200, 800);
            layout(workspace);
        });
        awaitActiveChart();
    }

    @AfterEach
    void disposeWorkspace() throws Exception {
        onEdt(() -> {
            ToolTipManager.sharedInstance().unregisterComponent(findTree(workspace));
            invokeApplicationAction("disposeDemoResources");
            workspace.removeAll();
        });
    }

    @Test
    void opensTheLineChartFromTheCatalogOnStartup() throws Exception {
        onEdt(() -> {
            JTree tree = findTree(workspace);
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            assertEquals(LINE, ((RendererCatalogEntry) selected.getUserObject()).className());
            assertNotNull(activeExportChart());
            assertTrue(SwingUtilities.isDescendingFrom(activeExportChart(), workspace));
        });
    }

    @Test
    void reselectingACachedChartExportsTheVisibleChart() throws Exception {
        ArberChartPanel originalLine = onEdtValue(this::activeExportChart);
        onEdt(() -> select(CANDLESTICK));
        ArberChartPanel candlestick = awaitActiveChart();
        assertNotSame(originalLine, candlestick);

        onEdt(() -> {
            select(LINE);
            assertSame(originalLine, activeExportChart(), "Export must follow the selected cached view");
            layout(workspace);
            ChartExportService.exportPng(activeExportChart(), temporaryDirectory.resolve("active-chart.png").toFile());
        });

        BufferedImage exported = ImageIO.read(temporaryDirectory.resolve("active-chart.png").toFile());
        assertEquals(originalLine.getWidth(), exported.getWidth());
        assertEquals(originalLine.getHeight(), exported.getHeight());
        onEdt(() -> assertTrue(containsExpectedSeries(exported, originalLine),
                "PNG export must contain the plotted series at its mapped data coordinates"));
    }

    @Test
    void reloadingAnAdaptiveViewClosesItsOwnedExecutor() throws Exception {
        onEdt(() -> select(ADAPTIVE));
        ArberChartPanel original = awaitActiveChart();
        ExecutorService executor = adaptiveExecutor(original);
        assertFalse(executor.isShutdown());

        onEdt(() -> invokeApplicationAction("refreshCurrent"));
        assertTrue(executor.isShutdown(), "Reload must release the discarded renderer's workers");
        ArberChartPanel replacement = awaitActiveChart();
        assertNotSame(original, replacement);
        assertFalse(adaptiveExecutor(replacement).isShutdown());
    }

    @Test
    void themeReplacementClosesCachedAdaptiveViewsToo() throws Exception {
        onEdt(() -> select(ADAPTIVE));
        ExecutorService cachedExecutor = adaptiveExecutor(awaitActiveChart());
        onEdt(() -> select(LINE));
        awaitActiveChart();
        assertFalse(cachedExecutor.isShutdown(), "A cached view remains reusable while browsing");

        onEdt(() -> invokeApplicationAction("toggleTheme"));
        assertTrue(cachedExecutor.isShutdown(), "Theme rebuild must release hidden discarded views");
        awaitActiveChart();
    }

    @Test
    void overlayCatalogEntriesUseTheirCoreRenderers() throws Exception {
        for (String name : new String[]{"com.arbergashi.charts.render.common.PhysicalScaleRenderer",
                "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer"}) {
            onEdt(() -> select(name));
            ArberChartPanel chart = awaitActiveChart();
            onEdt(() -> assertEquals(name, chart.getPrimaryRenderer().getClass().getName()));
        }
    }

    @Test
    void showcaseMotionStartsFromTheDisplayedBaselineWithoutAnInitialJump() throws Exception {
        onEdt(() -> {
            ArberChartPanel panel = activeExportChart();
            ChartModel model = panel.getPrimaryModel();
            double[] x = model.getXData();
            double[] y = model.getYData();
            double[] weight = model.getWeightData();
            ShowcaseAnimation.Frame frame = showcaseFrame(panel);
            frame.update(0.0, 0.0);
            assertArrayEquals(x, model.getXData());
            assertArrayEquals(y, model.getYData());
            assertArrayEquals(weight, model.getWeightData());
            frame.update(0.25, 0.25);
            assertEquals(x.length, model.getPointCount());
            assertFalse(java.util.Arrays.equals(y, model.getYData()), "The signal must move after the gentle start");
        });
    }

    @Test
    void standardSeriesFitTheInitialViewportAfterReplacingThePlaceholderModel() throws Exception {
        onEdt(() -> {
            layout(workspace);
            ArberChartPanel panel = activeExportChart();
            ChartModel model = panel.getPrimaryModel();
            PlotContext context = panel.getDebugContext();
            for (int index = 0; index < model.getPointCount(); index++) {
                assertTrue(model.getY(index) >= context.getMinY() && model.getY(index) <= context.getMaxY(),
                        "The initial viewport must include the installed standard series");
            }
        });
    }

    @Test
    void aSolidBackgroundCannotPassTheExportOracle() throws Exception {
        onEdt(() -> {
            layout(workspace);
            ArberChartPanel panel = activeExportChart();
            BufferedImage empty = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var graphics = empty.createGraphics();
            try {
                graphics.setColor(new java.awt.Color(panel.getTheme().getBackground().argb(), true));
                graphics.fillRect(0, 0, empty.getWidth(), empty.getHeight());
            } finally {
                graphics.dispose();
            }
            assertFalse(containsExpectedSeries(empty, panel));
        });
    }

    @Test
    void updatingTheLiveFinancialBarChangesTheVisibleChartWithoutGrowingItsHistory() throws Exception {
        onEdt(() -> select(CANDLESTICK));
        ArberChartPanel panel = awaitActiveChart();
        onEdt(() -> {
            layout(workspace);
            panel.setAnimationsEnabled(false);
            ShowcaseFinancialModel model = (ShowcaseFinancialModel) panel.getPrimaryModel();
            int count = model.getPointCount();
            int last = count - 1;
            Path beforePath = temporaryDirectory.resolve("live-before.png");
            Path afterPath = temporaryDirectory.resolve("live-after.png");
            ChartExportService.exportPng(panel, beforePath.toFile());
            double close = model.getClose(last) + 1.5;
            model.updateLiveBar(model.getOpen(last), Math.max(model.getHigh(last), close),
                    model.getLow(last), close, model.getVolume(last) + 10_000);
            ChartExportService.exportPng(panel, afterPath.toFile());
            BufferedImage before = ImageIO.read(beforePath.toFile());
            BufferedImage after = ImageIO.read(afterPath.toFile());
            int changes = 0;
            for (int y = 0; y < before.getHeight(); y++) {
                for (int x = 0; x < before.getWidth(); x++) {
                    if (before.getRGB(x, y) != after.getRGB(x, y)) changes++;
                }
            }
            assertEquals(count, model.getPointCount());
            assertTrue(changes > 10, "The live bar must update inside the chart's visible viewport");
        });
    }

    @Test
    void vectorFieldUsesOneHeldFrameTimeAndOnlyChangesWhenItsSimulationAdvances() throws Exception {
        onEdt(() -> select("com.arbergashi.charts.render.analysis.VectorFieldRenderer"));
        ArberChartPanel panel = awaitActiveChart();
        onEdt(() -> {
            layout(workspace);
            panel.setAnimationsEnabled(false);
            ShowcaseAnimation.Frame frame = showcaseFrame(panel);
            frame.update(0.0, 0.0);
            BufferedImage first = paint(panel);
            BufferedImage repeated = paint(panel);
            assertArrayEquals(first.getRGB(0, 0, first.getWidth(), first.getHeight(), null, 0, first.getWidth()),
                    repeated.getRGB(0, 0, repeated.getWidth(), repeated.getHeight(), null, 0, repeated.getWidth()));
            frame.update(1.0, 1.0);
            BufferedImage advanced = paint(panel);
            int changes = 0;
            for (int y = 0; y < first.getHeight(); y++) {
                for (int x = 0; x < first.getWidth(); x++) {
                    if (first.getRGB(x, y) != advanced.getRGB(x, y)) changes++;
                }
            }
            assertTrue(changes > 100, "Advancing the active simulation must visibly rotate the vector field");
            BufferedImage held = paint(panel);
            assertArrayEquals(advanced.getRGB(0, 0, advanced.getWidth(), advanced.getHeight(), null, 0, advanced.getWidth()),
                    held.getRGB(0, 0, held.getWidth(), held.getHeight(), null, 0, held.getWidth()));
        });
    }

    private static ShowcaseAnimation.Frame showcaseFrame(ArberChartPanel panel) throws ReflectiveOperationException {
        Object animation = panel.getClientProperty(ShowcaseAnimation.class.getName());
        Field field = ShowcaseAnimation.class.getDeclaredField("frame");
        field.setAccessible(true);
        return (ShowcaseAnimation.Frame) field.get(animation);
    }

    private static BufferedImage paint(ArberChartPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            panel.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static ExecutorService adaptiveExecutor(ArberChartPanel panel) throws ReflectiveOperationException {
        AdaptiveFunctionRenderer renderer = (AdaptiveFunctionRenderer) panel.getPrimaryRenderer();
        Field field = AdaptiveFunctionRenderer.class.getDeclaredField("executor");
        field.setAccessible(true);
        return (ExecutorService) field.get(renderer);
    }

    private void invokeApplicationAction(String name) throws ReflectiveOperationException {
        Method method = DemoApplication.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(application);
    }

    private ArberChartPanel awaitActiveChart() throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            ArberChartPanel chart = onEdtValue(this::activeExportChart);
            if (chart != null) {
                return chart;
            }
            Thread.sleep(10);
        }
        fail("The selected demo chart did not finish loading");
        return null;
    }

    private ArberChartPanel activeExportChart() throws ReflectiveOperationException {
        Field field = DemoApplication.class.getDeclaredField("currentChartPanel");
        field.setAccessible(true);
        return (ArberChartPanel) field.get(application);
    }

    private void select(String className) {
        JTree tree = findTree(workspace);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        var nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof RendererCatalogEntry entry && entry.className().equals(className)) {
                tree.setSelectionPath(new TreePath(node.getPath()));
                return;
            }
        }
        fail("Missing catalog entry: " + className);
    }

    private static JTree findTree(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTree tree) {
                return tree;
            }
            if (child instanceof Container container) {
                JTree tree = findTree(container);
                if (tree != null) {
                    return tree;
                }
            }
        }
        return null;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) {
                layout(container);
            }
        }
    }

    private static boolean containsExpectedSeries(BufferedImage image, ArberChartPanel panel) {
        ChartModel model = panel.getPrimaryModel();
        PlotContext context = panel.getDebugContext();
        ArberColor color = context.getTheme().getSeriesColor(0);
        double[] point = new double[2];
        int visibleSamples = 0;
        for (int fraction = 1; fraction < 10; fraction++) {
            int index = (model.getPointCount() - 1) * fraction / 10;
            context.mapToPixel(model.getX(index), model.getY(index), point);
            int centerX = (int) Math.round(point[0]) + panel.getOverlayCanvasForTesting().getX();
            int centerY = (int) Math.round(point[1]) + panel.getOverlayCanvasForTesting().getY();
            boolean found = false;
            for (int y = Math.max(0, centerY - 3); y <= Math.min(image.getHeight() - 1, centerY + 3); y++) {
                for (int x = Math.max(0, centerX - 3); x <= Math.min(image.getWidth() - 1, centerX + 3); x++) {
                    int actual = image.getRGB(x, y);
                    if (Math.abs(((actual >>> 16) & 255) - color.red()) <= 20
                            && Math.abs(((actual >>> 8) & 255) - color.green()) <= 20
                            && Math.abs((actual & 255) - color.blue()) <= 20) found = true;
                }
            }
            if (found) visibleSamples++;
        }
        // The other series can legitimately cover an individual sample, especially
        // at the fixture's shared event spikes. Still require the actual first series
        // at a majority of independently mapped, distributed data coordinates.
        return visibleSamples >= 5;
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

    private static <T> T onEdtValue(CheckedSupplier<T> supplier) throws Exception {
        AtomicReference<T> value = new AtomicReference<>();
        onEdt(() -> value.set(supplier.get()));
        return value.get();
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
