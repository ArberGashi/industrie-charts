package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

/**
 * Captures the released Swing workbench from its shaded JAR without opening a window.
 * Compile this source into a temporary directory with the demo JAR on the classpath,
 * then run with {@code -Djava.awt.headless=true} and the image output directory.
 */
public final class WorkbenchScreenshots {
    private static final int SCALE = 2;
    private static final Field CURRENT_CHART;
    private static final Method DISPOSE;

    static {
        try {
            CURRENT_CHART = DemoApplication.class.getDeclaredField("currentChartPanel");
            CURRENT_CHART.setAccessible(true);
            DISPOSE = DemoApplication.class.getDeclaredMethod("disposeDemoResources");
            DISPOSE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private WorkbenchScreenshots() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected the output image directory");
        Path artifact = Path.of(DemoApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(artifact) || !artifact.toString().endsWith(".jar")) {
            throw new IllegalStateException("The actual packaged demo JAR must provide DemoApplication");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(artifact)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
        }
        System.out.println("Artifact: " + artifact);
        System.out.println("SHA-256: " + HexFormat.of().formatHex(digest.digest()));
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        capture("dark", 1360, 880, output.resolve("workbench-dark.png"));
        capture("light", 1360, 880, output.resolve("workbench-light.png"));
        capture("light", 1000, 680, output.resolve("workbench-light-compact.png"));
    }

    private static void capture(String theme, int width, int height, Path output) throws Exception {
        AtomicReference<DemoApplication> application = new AtomicReference<>();
        AtomicReference<JPanel> workspace = new AtomicReference<>();
        try {
            onEdt(() -> {
                System.setProperty("demo.theme", theme);
                application.set(new DemoApplication(DemoThemeSupport.setupLookAndFeel()));
                JPanel root = application.get().buildWorkspace();
                workspace.set(root);
                root.setSize(width, height);
                layout(root);
            });
            awaitChart(application.get());
            onEdt(() -> {
                JPanel root = workspace.get();
                layout(root);
                ArberChartPanel chart = (ArberChartPanel) CURRENT_CHART.get(application.get());
                chart.setAnimationsEnabled(false);
                if (chart.isShowing()) throw new IllegalStateException("The capture must stay offscreen");
                ChartModel model = chart.getPrimaryModel();
                var context = chart.getDebugContext();
                for (int index = 0; index < model.getPointCount(); index++) {
                    double value = model.getY(index);
                    if (value < context.getMinY() || value > context.getMaxY()) {
                        throw new IllegalStateException("Initial series is clipped at data index " + index);
                    }
                }
                BufferedImage image = new BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.scale(SCALE, SCALE);
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    root.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                if (!ImageIO.write(image, "png", output.toFile())) throw new IllegalStateException("PNG writer unavailable");
                System.out.println(output.getFileName() + ": " + image.getWidth() + "x" + image.getHeight()
                        + " pixels (" + width + "x" + height + " logical, " + SCALE + "x)");
            });
        } finally {
            onEdt(() -> {
                if (workspace.get() != null) unregisterTooltips(workspace.get());
                if (application.get() != null) DISPOSE.invoke(application.get());
                if (workspace.get() != null) workspace.get().removeAll();
            });
        }
    }

    private static void awaitChart(DemoApplication application) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        AtomicReference<ArberChartPanel> chart = new AtomicReference<>();
        while (System.nanoTime() < deadline) {
            onEdt(() -> chart.set((ArberChartPanel) CURRENT_CHART.get(application)));
            if (chart.get() != null) return;
            Thread.sleep(10);
        }
        throw new IllegalStateException("The initial chart did not finish loading within five seconds");
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container nested) layout(nested);
        }
    }

    private static void unregisterTooltips(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTree tree) ToolTipManager.sharedInstance().unregisterComponent(tree);
            if (component instanceof Container nested) unregisterTooltips(nested);
        }
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

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
