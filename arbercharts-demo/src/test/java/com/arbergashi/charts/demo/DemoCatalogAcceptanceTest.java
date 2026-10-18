package com.arbergashi.charts.demo;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.common.PerformanceAuditRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the customer demo through its catalog selection, asynchronous view
 * construction, real model factory, renderer factory and Swing paint path.
 * This detects blank or mismatched catalog entries; it does not certify every
 * numerical algorithm, native window interaction or animated frame.
 */
class DemoCatalogAcceptanceTest {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int MINIMUM_RENDERER_PIXELS = 100;

    @Test
    void everyCatalogEntryProducesVisibleRendererOutputInBothThemes() throws Exception {
        Path output = Path.of(System.getProperty("arbercharts.demo.acceptance.output",
                "target/catalog-acceptance"));
        runCatalog(output);
    }

    /**
     * Runs the same acceptance path against a packaged demo on the class path.
     * @param args optional output directory
     * @throws Exception if a catalog entry cannot be displayed or inspected
     */
    public static void main(String[] args) throws Exception {
        runCatalog(Path.of(args.length == 0 ? "target/catalog-acceptance" : args[0]));
    }

    private static void runCatalog(Path output) throws Exception {
        Files.createDirectories(output);
        List<RendererCatalogEntry> entries = RendererCatalog.load().entries();
        assertTrue(entries.size() >= 150, "The acceptance pass must include the complete catalog");
        List<String> results = new ArrayList<>();
        results.add("theme,entry,renderer,model,points,plotWidth,plotHeight,rendererPixels,auditPixels,result");
        List<String> failures = new ArrayList<>();
        ConcurrentLinkedQueue<Throwable> asynchronousFailures = new ConcurrentLinkedQueue<>();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> asynchronousFailures.add(failure));
        try {
            for (String theme : new String[]{"dark", "light"}) {
                try (Workspace workspace = Workspace.open(theme)) {
                    for (RendererCatalogEntry entry : entries) {
                        String prefix = theme + "/" + entry.category() + "/" + entry.simpleName();
                        Path directory = output.resolve(theme).resolve(entry.category());
                        Files.createDirectories(directory);
                        String row;
                        Measurement measurement = null;
                        try {
                            workspace.select(entry);
                            ArberChartPanel panel = workspace.awaitChart(asynchronousFailures);
                            Measurement rendered = onEdtValue(() -> workspace.measure(entry, panel, directory));
                            measurement = rendered;
                            Throwable asynchronous = asynchronousFailures.poll();
                            assertTrue(asynchronous == null, () -> "Asynchronous demo failure: " + asynchronous);
                            assertTrue(rendered.rendererPixels >= MINIMUM_RENDERER_PIXELS,
                                    () -> "The actual selected renderer contributes only "
                                            + rendered.rendererPixels + " plot pixels");
                            if (rendered.auditPixels >= 0) {
                                int requiredAuditPixels = entry.simpleName().equals("AuditTrailRenderer")
                                        ? 16 : MINIMUM_RENDERER_PIXELS;
                                assertTrue(rendered.auditPixels >= requiredAuditPixels,
                                        () -> "The audit overlay itself must be visible above its supporting line: "
                                                + rendered.auditPixels + " pixels");
                            }
                            row = measurement.row(theme, entry, "PASS");
                        } catch (Exception | AssertionError failure) {
                            Throwable cause = rootCause(failure);
                            String detail = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                            failures.add(prefix + " — " + detail);
                            row = measurement == null
                                    ? csv(theme) + "," + csv(entry.className()) + ",,,,,,,," + csv("FAIL: " + detail)
                                    : measurement.row(theme, entry, "FAIL: " + detail);
                            onEdt(() -> ImageIO.write(paint(workspace.component), "png",
                                    directory.resolve(entry.simpleName() + "-failed-workbench.png").toFile()));
                            asynchronousFailures.clear();
                        }
                        results.add(row);
                        Files.write(output.resolve("results.csv"), results);
                    }
                }
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
            Files.write(output.resolve("failures.txt"), failures);
        }
        assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures)
                + System.lineSeparator() + "Evidence: " + output.toAbsolutePath());
    }

    private static final class Workspace implements AutoCloseable {
        private final DemoApplication application;
        private final JPanel component;
        private final JTree tree;

        private Workspace(DemoApplication application, JPanel component, JTree tree) {
            this.application = application;
            this.component = component;
            this.tree = tree;
        }

        private static Workspace open(String theme) throws Exception {
            return onEdtValue(() -> {
                DemoThemeSupport.setupLookAndFeel();
                DemoThemeSupport.applyLookAndFeel(theme);
                DemoApplication application = new DemoApplication(theme);
                JPanel component = application.buildWorkspace();
                component.setSize(WIDTH, HEIGHT);
                layout(component);
                JTree tree = findTree(component);
                assertNotNull(tree, "The real customer catalog must be present");
                return new Workspace(application, component, tree);
            });
        }

        private void select(RendererCatalogEntry entry) throws Exception {
            onEdt(() -> {
                var nodes = ((DefaultMutableTreeNode) tree.getModel().getRoot()).depthFirstEnumeration();
                while (nodes.hasMoreElements()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
                    if (node.getUserObject() instanceof RendererCatalogEntry item
                            && item.className().equals(entry.className())) {
                        tree.setSelectionPath(new TreePath(node.getPath()));
                        return;
                    }
                }
                throw new AssertionError("Missing selectable catalog entry: " + entry.className());
            });
        }

        private ArberChartPanel awaitChart(ConcurrentLinkedQueue<Throwable> failures) throws Exception {
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (System.nanoTime() < deadline) {
                Throwable failure = failures.poll();
                if (failure != null) throw new AssertionError("Asynchronous view construction failed", failure);
                ArberChartPanel panel = onEdtValue(() -> (ArberChartPanel) field(application, "currentChartPanel"));
                if (panel != null) return panel;
                Thread.sleep(10);
            }
            throw new AssertionError("The selected catalog view did not finish loading");
        }

        private Measurement measure(RendererCatalogEntry entry, ArberChartPanel panel, Path directory)
                throws Exception {
            layout(component);
            assertEquals(entry.className(), field(application, "currentRendererKey"));
            assertTrue(SwingUtilities.isDescendingFrom(panel, component));
            for (Component current = panel; current != component; current = current.getParent()) {
                assertTrue(current != null && current.isVisible(), "The selected chart must be in the visible card");
            }
            assertTrue(panel.getWidth() >= 300 && panel.getHeight() >= 200,
                    "The workbench must assign usable space to its selected chart");
            assertTrue(panel.getLayerCount() > 0, "A selected demo requires a primary renderer layer");
            assertNotNull(panel.getPrimaryModel());
            String expectedClass = switch (entry.className()) {
                case "com.arbergashi.charts.render.common.PerformanceAuditRenderer" ->
                        PerformanceAuditRendererAdapter.class.getName();
                case "com.arbergashi.charts.render.financial.AuditTrailRenderer" ->
                        AuditTrailRendererAdapter.class.getName();
                case "com.arbergashi.charts.render.specialized.CandlestickHollowRenderer" ->
                        "com.arbergashi.charts.render.financial.CandlestickHollowRenderer";
                default -> entry.className();
            };
            assertTrue(Class.forName(expectedClass).isInstance(panel.getPrimaryRenderer()),
                    () -> "Catalog renderer " + entry.className() + " resolved to "
                            + panel.getPrimaryRenderer().getClass().getName());
            panel.setAnimationsEnabled(false);
            BufferedImage customerView = paint(component);
            ImageIO.write(customerView, "png", directory.resolve(entry.simpleName() + "-workbench.png").toFile());

            // The overlay legend itself dims when layers are hidden. Exclude it
            // from both oracle images without changing the real chart geometry.
            JComponent legend = (JComponent) field(panel, "interactiveLegendOverlay");
            boolean legendVisible = legend.isVisible();
            List<String> layerIds = new ArrayList<>();
            List<Boolean> visibility = new ArrayList<>();
            for (int index = 0; index < panel.getLayerCount(); index++) {
                String id = panel.getLayerId(index);
                layerIds.add(id);
                visibility.add(panel.getLayerVisibilityModel().isVisible(id));
            }
            BufferedImage hidden;
            BufferedImage visible;
            int auditPixels = -1;
            try {
                legend.setVisible(false);
                for (String id : layerIds) panel.getLayerVisibilityModel().setVisible(id, false);
                hidden = paint(panel);
                for (int index = 0; index < layerIds.size(); index++) {
                    panel.getLayerVisibilityModel().setVisible(layerIds.get(index), visibility.get(index));
                }
                visible = paint(panel);
                if (panel.getPrimaryRenderer() instanceof PerformanceAuditRendererAdapter adapter) {
                    // Its supporting line alone cannot prove that the audit label works.
                    PerformanceAuditRenderer audit = (PerformanceAuditRenderer) field(adapter, "audit");
                    audit.setEnabled(false);
                    try {
                        auditPixels = changedPlotPixels(panel, visible, paint(panel));
                    } finally {
                        audit.setEnabled(true);
                    }
                } else if (panel.getPrimaryRenderer() instanceof AuditTrailRendererAdapter adapter) {
                    assertTrue(Class.forName(entry.className()).isInstance(field(adapter, "audit")),
                            "The audit adapter must contain the selected core renderer");
                    // Provenance is represented by small markers. Compare their
                    // actual contribution separately from the supporting line.
                    String previous = ChartAssets.getString("Chart.audit.enabled", null);
                    ChartAssets.setProperty("Chart.audit.enabled", "false");
                    try {
                        auditPixels = changedPlotPixels(panel, visible, paint(panel));
                    } finally {
                        if (previous == null) ChartAssets.removeProperty("Chart.audit.enabled");
                        else ChartAssets.setProperty("Chart.audit.enabled", previous);
                    }
                }
            } finally {
                for (int index = 0; index < layerIds.size(); index++) {
                    panel.getLayerVisibilityModel().setVisible(layerIds.get(index), visibility.get(index));
                }
                legend.setVisible(legendVisible);
            }
            int rendererPixels = changedPlotPixels(panel, hidden, visible);
            if (rendererPixels < MINIMUM_RENDERER_PIXELS || (auditPixels >= 0 && auditPixels < MINIMUM_RENDERER_PIXELS)) {
                ImageIO.write(hidden, "png", directory.resolve(entry.simpleName() + "-data-hidden.png").toFile());
                ImageIO.write(visible, "png", directory.resolve(entry.simpleName() + "-data-visible.png").toFile());
            }
            if (panel.getPrimaryRenderer() instanceof PredictiveShadowRenderer shadow) {
                assertPredictionHorizonVisible(panel, shadow, hidden, visible);
            }
            ArberRect plot = panel.getDebugContext().getPlotBounds();
            return new Measurement(panel.getPrimaryRenderer().getClass().getName(),
                    panel.getPrimaryModel().getClass().getName(), panel.getPrimaryModel().getPointCount(),
                    plot.width(), plot.height(), rendererPixels, auditPixels);
        }

        @Override
        public void close() throws Exception {
            onEdt(() -> {
                Method dispose = DemoApplication.class.getDeclaredMethod("disposeDemoResources");
                dispose.setAccessible(true);
                dispose.invoke(application);
                ToolTipManager.sharedInstance().unregisterComponent(tree);
                component.removeAll();
            });
        }
    }

    private record Measurement(String renderer, String model, int points, double plotWidth, double plotHeight,
                               int rendererPixels, int auditPixels) {
        private String row(String theme, RendererCatalogEntry entry, String result) {
            return csv(theme) + "," + csv(entry.className()) + "," + csv(renderer) + "," + csv(model)
                    + "," + points + "," + plotWidth + "," + plotHeight + "," + rendererPixels
                    + "," + auditPixels + "," + csv(result);
        }
    }

    private static int changedPlotPixels(ArberChartPanel panel, BufferedImage first, BufferedImage second) {
        ArberRect plot = panel.getDebugContext().getPlotBounds();
        int offsetX = panel.getOverlayCanvasForTesting().getX();
        int offsetY = panel.getOverlayCanvasForTesting().getY();
        int minX = Math.max(0, (int) Math.ceil(plot.x()) + offsetX);
        int minY = Math.max(0, (int) Math.ceil(plot.y()) + offsetY);
        int maxX = Math.min(first.getWidth(), (int) Math.floor(plot.maxX()) + offsetX);
        int maxY = Math.min(first.getHeight(), (int) Math.floor(plot.maxY()) + offsetY);
        int changed = 0;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) changed++;
            }
        }
        return changed;
    }

    private static void assertPredictionHorizonVisible(ArberChartPanel panel, PredictiveShadowRenderer shadow,
                                                       BufferedImage hidden, BufferedImage visible) {
        var model = panel.getPrimaryModel();
        var context = panel.getDebugContext();
        int last = model.getPointCount() - 1;
        assertTrue(last >= 1, "The prediction demo needs a measured history");
        double lastX = model.getX(last);
        double step = model.getX(last) - model.getX(last - 1);
        int horizon = Math.max(2, ChartAssets.getInt("Chart.predictive.horizonPoints", 24));
        int visibleSamples = 0;
        double[] pixel = new double[2];
        for (int index = 1; index <= horizon; index++) {
            double x = lastX + step * index;
            double y = shadow.predictedForX(x);
            assertTrue(Double.isFinite(y), "The demo must produce every forecast sample: x=" + x);
            assertTrue(x >= context.getMinX() && x <= context.getMaxX()
                            && y >= context.getMinY() && y <= context.getMaxY(),
                    "The entire forecast must fit the actual viewport; clipped sample (" + x + ", " + y + ")");
            context.mapToPixel(x, y, pixel);
            int centerX = (int) Math.round(pixel[0]) + panel.getOverlayCanvasForTesting().getX();
            int centerY = (int) Math.round(pixel[1]) + panel.getOverlayCanvasForTesting().getY();
            boolean painted = false;
            for (int sy = Math.max(0, centerY - 3); sy <= Math.min(visible.getHeight() - 1, centerY + 3); sy++) {
                for (int sx = Math.max(0, centerX - 3); sx <= Math.min(visible.getWidth() - 1, centerX + 3); sx++) {
                    if (hidden.getRGB(sx, sy) != visible.getRGB(sx, sy)) painted = true;
                }
            }
            if (painted) visibleSamples++;
        }
        assertTrue(visibleSamples >= horizon * 3 / 4,
                "The forecast must actually paint across its horizon, visible samples=" + visibleSamples);
    }

    private static BufferedImage paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            component.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static JTree findTree(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTree tree) return tree;
            if (child instanceof Container container) {
                JTree found = findTree(container);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) layout(container);
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Throwable rootCause(Throwable failure) {
        while (failure.getCause() != null) failure = failure.getCause();
        return failure;
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ') + '"';
    }

    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception failure) {
                throw new RuntimeException(failure);
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
