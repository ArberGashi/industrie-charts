package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.common.PhysicalScaleRenderer;
import com.arbergashi.charts.render.medical.CalibrationRenderer;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DemoOverlayWorkspaceTest {
    private DemoApplication application;
    private JPanel workspace;

    @AfterEach
    void disposeWorkspace() throws Exception {
        if (application == null) return;
        onEdt(() -> {
            ToolTipManager.sharedInstance().unregisterComponent(findTree(workspace));
            var method = DemoApplication.class.getDeclaredMethod("disposeDemoResources");
            method.setAccessible(true);
            method.invoke(application);
            workspace.removeAll();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void physicalReferenceHasAVisibleSignalAndKeepsItsLengthWhenTheWorkspaceResizes(String theme) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.common.PhysicalScaleRenderer");
        onEdt(() -> {
            PhysicalScaleRenderer renderer = assertInstanceOf(PhysicalScaleRenderer.class, panel.getPrimaryRenderer());
            for (int width : new int[]{1360, 1000}) {
                workspace.setSize(width, 800);
                layout(workspace);
                BufferedImage withMarker = paint(panel);
                assertSignalVisible(panel, withMarker);
                renderer.setEnabled(false);
                panel.setTheme(panel.getTheme());
                BufferedImage withoutMarker = paint(panel);
                int longestMarkerRow = 0;
                int markerPixels = 0;
                for (int y = 0; y < withMarker.getHeight(); y++) {
                    int row = 0;
                    for (int x = 0; x < withMarker.getWidth(); x++) {
                        if (withMarker.getRGB(x, y) != withoutMarker.getRGB(x, y)) row++;
                    }
                    markerPixels += row;
                    longestMarkerRow = Math.max(longestMarkerRow, row);
                }
                double expectedLength = ChartAssets.getFloat("Chart.scale.physical.barMm", 10f)
                        * panel.getDebugContext().getPixelsPerMillimeter();
                assertTrue(markerPixels > 100, "The actual reference and its label must remain visible");
                assertEquals(expectedLength, longestMarkerRow, 3.0,
                        "The reference uses physical density, not the available chart width");
                renderer.setEnabled(true);
                panel.setTheme(panel.getTheme());
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void circularLatencyShowsTheGaugeAndItsActualTimingText(String theme) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer");
        onEdt(() -> {
            assertEquals("com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer",
                    panel.getPrimaryRenderer().getClass().getName());
            assertTrue(panel.getPrimaryModel().getY(0) >= 0 && panel.getPrimaryModel().getY(0) <= 100);
            BufferedImage withLabel = paint(panel);
            assertFalse(panel.isAxisLabelsVisible(), "The latency gauge must not display unrelated Cartesian tick labels");
            assertFalse(panel.getDebugContext().isGridVisibleX());
            assertFalse(panel.getDebugContext().isGridVisibleY());
            int gaugePixels = 0;
            ArberColor accent = panel.getTheme().getAccentColor();
            ArberColor bearish = panel.getTheme().getBearishColor();
            for (int y = withLabel.getHeight() / 5; y < withLabel.getHeight() * 4 / 5; y++) {
                for (int x = withLabel.getWidth() / 5; x < withLabel.getWidth() * 4 / 5; x++) {
                    int pixel = withLabel.getRGB(x, y);
                    if (near(pixel, accent) || near(pixel, bearish)) gaugePixels++;
                }
            }
            assertTrue(gaugePixels > 1000, "The demo must show the gauge below the latency overlay");
            String enabled = ChartAssets.getString("Chart.circular.performance.enabled", "true");
            try {
                ChartAssets.setProperty("Chart.circular.performance.enabled", "false");
                panel.setTheme(panel.getTheme());
                BufferedImage withoutLabel = paint(panel);
                int labelPixels = 0;
                for (int y = withLabel.getHeight() * 4 / 5; y < withLabel.getHeight(); y++) {
                    for (int x = 0; x < withLabel.getWidth(); x++) {
                        if (withLabel.getRGB(x, y) != withoutLabel.getRGB(x, y)) labelPixels++;
                    }
                }
                assertTrue(labelPixels > 100, "The actual p99/p99.9 label must remain visible over the gauge");
            } finally {
                ChartAssets.setProperty("Chart.circular.performance.enabled", enabled);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void simplePointAndFigureShowsCrossesAndRingsInActualBoxColumns(String theme) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.financial.PointAndFigureRenderer");
        onEdt(() -> {
            assertInstanceOf(com.arbergashi.charts.render.financial.PointAndFigureRenderer.class, panel.getPrimaryRenderer());
            FinancialDemoDataTest.assertPointAndFigureColumns(panel.getPrimaryModel());
            if (panel.getClientProperty(ShowcaseAnimation.class.getName()) != null) {
                animationFrame(panel).update(2, 0.016);
                FinancialDemoDataTest.assertPointAndFigureColumns(panel.getPrimaryModel());
            }
            BufferedImage visible = paint(panel);
            assertTrue(primaryPixelCount(panel, visible) > 1000, "The actual X/O boxes must form a visible chart");
            var model = panel.getPrimaryModel();
            int cross = -1, ring = -1;
            for (int i = 0; i < model.getPointCount(); i++) {
                if (cross < 0 && model.getWeight(i) > 0) cross = i;
                if (ring < 0 && model.getWeight(i) < 0) ring = i;
            }
            assertTrue(cross >= 0 && ring >= 0);
            double[] pixel = new double[2];
            var offset = panel.getOverlayCanvasForTesting();
            var context = panel.getDebugContext();
            context.mapToPixel(model.getX(cross), model.getY(cross), pixel);
            int x = (int) Math.round(pixel[0]) + offset.getX();
            int y = (int) Math.round(pixel[1]) + offset.getY();
            assertTrue(containsColor(visible, x, y, panel.getTheme().getBullishColor(), 1),
                    "The rising X must cross through the box centre");
            assertTrue(containsColor(visible, x + 4, y + 4, panel.getTheme().getBullishColor(), 1));
            assertTrue(containsColor(visible, x - 4, y + 4, panel.getTheme().getBullishColor(), 1));
            context.mapToPixel(model.getX(ring), model.getY(ring), pixel);
            x = (int) Math.round(pixel[0]) + offset.getX();
            y = (int) Math.round(pixel[1]) + offset.getY();
            assertFalse(containsColor(visible, x, y, panel.getTheme().getBearishColor(), 2),
                    "The falling O must retain an empty centre");
            assertTrue(containsColor(visible, x + 6, y, panel.getTheme().getBearishColor(), 1));
            assertTrue(containsColor(visible, x - 6, y, panel.getTheme().getBearishColor(), 1));
        });
    }

    private static boolean containsColor(BufferedImage image, int cx, int cy, ArberColor color, int radius) {
        for (int y = Math.max(0, cy - radius); y <= Math.min(image.getHeight() - 1, cy + radius); y++) {
            for (int x = Math.max(0, cx - radius); x <= Math.min(image.getWidth() - 1, cx + radius); x++) {
                if (near(image.getRGB(x, y), color)) return true;
            }
        }
        return false;
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void pointAndFigureUsesItsActualPriceRange(String theme) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.financial.PointAndFigureAdvancedRenderer");
        onEdt(() -> {
            assertInstanceOf(FinancialChartModel.class, panel.getPrimaryModel());
            BufferedImage visible = paint(panel);
            var context = panel.getDebugContext();
            assertTrue(context.getMaxY() > 100, "The price series must not be clipped to the former -2..2 axis");
            assertTrue(primaryPixelCount(panel, visible) > 100, "The actual X/O columns must be visible");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void calibrationShowsItsSignalAndAnInstanceEnabledPhysicalPulse(String theme) throws Exception {
        String global = ChartAssets.getString("Chart.calibration.enabled", null);
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.medical.CalibrationRenderer");
        onEdt(() -> {
            CalibrationRenderer renderer = assertInstanceOf(CalibrationRenderer.class, panel.getPrimaryRenderer());
            BufferedImage withPulse = paint(panel);
            renderer.setEnabled(false);
            panel.setTheme(panel.getTheme());
            BufferedImage signal = paint(panel);
            assertSignalVisible(panel, signal);
            int minX = signal.getWidth(), minY = signal.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < signal.getHeight(); y++) {
                for (int x = 0; x < signal.getWidth(); x++) {
                    if (withPulse.getRGB(x, y) != signal.getRGB(x, y)) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            double density = panel.getDebugContext().getPixelsPerMillimeter();
            assertEquals(6.25 * density, maxX - minX, 3,
                    "200 ms at 25 mm/s plus its quarter-width tail must retain the physical scale");
            assertEquals(10 * density, maxY - minY, 3, "1 mV must occupy 10 mm at the configured density");
            assertEquals(global, ChartAssets.getString("Chart.calibration.enabled", null),
                    "The demo must not enable other consumers' calibration overlays globally");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void calibrationAnimationDoesNotConnectAcrossTheMedicalRingBoundary(String theme) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render.medical.CalibrationRenderer");
        onEdt(() -> {
            paint(panel);
            CircularFastMedicalModel model = assertInstanceOf(CircularFastMedicalModel.class, panel.getPrimaryModel());
            assertEquals(model.getCapacity(), model.getPointCount(), "The real medical fixture starts with a full ring");
            assertEquals(0, model.getHeadIndex());
            Object animation = panel.getClientProperty(ShowcaseAnimation.class.getName());
            Field frame = ShowcaseAnimation.class.getDeclaredField("frame");
            frame.setAccessible(true);
            long stamp = model.getUpdateStamp();
            ((ShowcaseAnimation.Frame) frame.get(animation)).update(0.8, 0.8);
            assertEquals(stamp + 200, model.getUpdateStamp(), "The actual animation must overwrite 200 samples");
            assertEquals(200, model.getHeadIndex());
            assertEquals(0.8, model.getX(0), 1e-9);
            assertEquals(0.796, model.getX(model.getPointCount() - 1), 1e-9);
            panel.setTheme(panel.getTheme());
            assertSignalVisible(panel, paint(panel));

            List<double[]> segments = renderedSegments(calibrationSignal(panel), panel);
            assertTrue(segments.size() > 1000, "The wrapped signal must still contain meaningful waveform geometry");
            double[] gapStart = new double[2];
            double[] gapEnd = new double[2];
            panel.getDebugContext().mapToPixel(0.8, 0, gapStart);
            panel.getDebugContext().mapToPixel(0.88, 0, gapEnd);
            for (double[] segment : segments) {
                assertTrue(segment[2] >= segment[0],
                        "Monotonic sample times must not create a backwards line across the physical ring boundary");
                assertFalse(segment[0] < gapEnd[0] - 0.5 && segment[2] > gapStart[0] + 0.5,
                        "The ECG supporting trace must preserve its erase gap at the moving sweep position");
            }
        });
    }

    static Stream<Arguments> medicalShowcases() {
        return RendererCatalog.load().entries().stream().filter(entry -> entry.category().equals("medical"))
                .flatMap(entry -> Stream.of("dark", "light").map(theme -> Arguments.of(theme, entry.className())));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("medicalShowcases")
    void medicalAnimationSurvivesRepeatedSweepsWithoutMovingTheCustomersView(String theme, String className)
            throws Exception {
        ArberChartPanel panel = open(theme, className);
        onEdt(() -> {
            paint(panel);
            CircularFastMedicalModel model = assertInstanceOf(CircularFastMedicalModel.class, panel.getPrimaryModel());
            assertEquals(1500, model.getPointCount(), "Each medical demo supplies a complete synthetic sample");
            double[] initialView = viewport(panel);
            double[] initialScale = pixelScale(panel);
            double[] baselineX = model.getXData().clone();
            double[] baseline = model.getRawChannelArray(0).clone();
            boolean completeCurve = className.endsWith("SpirometryRenderer") || className.endsWith("VCGRenderer");
            boolean calibration = panel.getPrimaryRenderer() instanceof CalibrationRenderer;
            assertMedicalAxesMatchTheQuantity(panel, className);
            ChartRenderer signal = calibration ? calibrationSignal(panel) : panel.getPrimaryRenderer();
            ShowcaseAnimation.Frame animation = animationFrame(panel);
            for (int frame = 1; frame <= 1175; frame++) {
                animation.update(frame * 0.016, 0.016);
                // Sample inside each sweep, including three completed six-second ring periods.
                if (frame % 375 != 50) continue;
                if (!completeCurve) {
                    assertEquals((frame * 4) % 1500, model.getRawHeadIndex(),
                            "Time sweeps must advance their physical write position at 250 Hz");
                }
                for (int i = 0; i < model.getPointCount(); i++) {
                    assertEquals(completeCurve ? baselineX[i] : i / 250.0, model.getXData()[i], 1e-9,
                            "The sample must retain its original time or measurement-domain position");
                    assertEquals(baseline[i], model.getRawChannelArray(0)[i], Math.abs(baseline[i]) * 0.031 + 1e-12,
                            "Animation must preserve the waveform at the physical sample position");
                }
                BufferedImage visible = paint(panel);
                assertArrayEquals(initialView, viewport(panel), 1e-9, "Animation must not move or resize the viewport");
                assertArrayEquals(initialScale, pixelScale(panel), 1e-9, "Medical calibration must remain stable");
                if (calibration || completeCurve) assertSignalVisible(panel, visible);
                else assertSignalSpansDemoDomain(panel, visible);
                List<double[]> segments = renderedSegments(signal, panel);
                assertTrue(segments.size() > 1000, "The actual renderer must still draw the medical signal");
                var plot = panel.getDebugContext().getPlotBounds();
                for (double[] segment : segments) {
                    assertTrue(segment[0] >= plot.x() - 0.01 && segment[0] <= plot.maxX() + 0.01
                                    && segment[2] >= plot.x() - 0.01 && segment[2] <= plot.maxX() + 0.01
                                    && segment[1] >= plot.y() - 0.01 && segment[1] <= plot.maxY() + 0.01
                                    && segment[3] >= plot.y() - 0.01 && segment[3] <= plot.maxY() + 0.01,
                            "The initial fitted view must include every displayed channel, without clipped signal segments");
                }
                if (completeCurve) {
                    if (className.endsWith("VCGRenderer")) MedicalDemoDataTest.assertVcgLoop(model);
                    else MedicalDemoDataTest.assertSpirometryLoop(model);
                    assertLoopSegmentsRemainContinuous(panel, segments);
                } else {
                    double maxStep = initialScale[0] / 250.0 + 0.01;
                    for (double[] segment : segments) {
                        assertTrue(segment[2] >= segment[0] && segment[2] - segment[0] <= maxStep,
                                "The time sweep must not connect nonadjacent samples across a wrapped ring");
                    }
                }
            }
            assertFalse(java.util.Arrays.equals(baseline, model.getRawChannelArray(0)),
                    "The animation must update the signal, not merely freeze a visible initial frame");

            var canvas = panel.getOverlayCanvasForTesting();
            int pointerX = canvas.getWidth() / 3;
            int pointerY = canvas.getHeight() / 2;
            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseWheelEvent.MOUSE_WHEEL, 1, 0,
                    pointerX, pointerY, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -2));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 2, 0,
                    pointerX, pointerY, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 3, InputEvent.BUTTON1_DOWN_MASK,
                    pointerX + 30, pointerY, 0, false, MouseEvent.NOBUTTON));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 4, 0,
                    pointerX + 30, pointerY, 1, false, MouseEvent.BUTTON1));
            paint(panel);
            double[] manualView = viewport(panel);
            double[] manualScale = pixelScale(panel);
            assertFalse(java.util.Arrays.equals(initialView, manualView), "The real interaction must change the view");
            for (int frame = 1176; frame <= 1550; frame++) animation.update(frame * 0.016, 0.016);
            paint(panel);
            assertArrayEquals(manualView, viewport(panel), 1e-9, "Another full sweep must preserve manual zoom and pan");
            assertArrayEquals(manualScale, pixelScale(panel), 1e-9, "Animation must retain the selected scale");
        });
    }

    private static void assertMedicalAxesMatchTheQuantity(ArberChartPanel panel, String className) throws Exception {
        Field xField = ArberChartPanel.class.getDeclaredField("xAxisConfig");
        Field yField = ArberChartPanel.class.getDeclaredField("yAxisConfig");
        xField.setAccessible(true);
        yField.setAccessible(true);
        AxisConfig x = (AxisConfig) xField.get(panel);
        AxisConfig y = (AxisConfig) yField.get(panel);
        String simple = className.substring(className.lastIndexOf('.') + 1);
        boolean ecg = List.of("CalibrationRenderer", "ECGRenderer", "ECGRhythmRenderer", "MedicalSweepRenderer",
                "SweepEraseEKGRenderer").contains(simple);
        if (ecg) {
            assertEquals(25.0, x.getMedicalMmPerUnit());
            assertEquals(10.0, y.getMedicalMmPerUnit());
            double density = panel.getDebugContext().getPixelsPerMillimeter();
            assertArrayEquals(new double[]{25 * density, 10 * density}, pixelScale(panel), 1e-9);
        } else {
            assertNull(x.getResolvedUnitsPerPixel(4.0), "Non-ECG quantities must not inherit ECG paper speed");
            assertNull(y.getResolvedUnitsPerPixel(4.0), "Non-ECG quantities must not inherit ECG voltage gain");
        }
        String xUnit = simple.equals("VCGRenderer") ? "mV" : simple.equals("SpirometryRenderer") ? "L" : "s";
        assertEquals(xUnit, x.getUnitSuffix());
        if (simple.equals("SpirometryRenderer")) assertEquals("L/s", y.getUnitSuffix());
        if (simple.equals("SpectrogramMedicalRenderer")) assertEquals("Hz", y.getUnitSuffix());
        if (simple.equals("UltrasoundMModeRenderer")) assertEquals("mm", y.getUnitSuffix());
        if (simple.equals("SpectrogramMedicalRenderer") || simple.equals("UltrasoundMModeRenderer")) {
            assertTrue(panel.getDebugContext().getMinY() <= 0 && panel.getDebugContext().getMaxY() < 60,
                    "The intensity views must fit their coordinate channel and zero baseline, not palette values");
        }
    }

    private static void assertLoopSegmentsRemainContinuous(ArberChartPanel panel, List<double[]> segments) {
        var bounds = panel.getDebugContext().getPlotBounds();
        double[] first = segments.getFirst();
        double[] last = segments.getLast();
        assertEquals(first[0], last[2], 0.01, "The actually rendered loop must close horizontally");
        assertEquals(first[1], last[3], 0.01, "The actually rendered loop must close vertically");
        int forward = 0, backward = 0;
        for (double[] segment : segments) {
            double dx = (segment[2] - segment[0]) / bounds.width();
            double dy = (segment[3] - segment[1]) / bounds.height();
            assertTrue(Math.hypot(dx, dy) < 0.03, "The actual loop must contain only local segments, never a wrap chord");
            if (dx > 0) forward++;
            if (dx < 0) backward++;
        }
        assertTrue(forward > 500 && backward > 500, "A closed XY loop must traverse the domain in both directions");
    }

    private static ShowcaseAnimation.Frame animationFrame(ArberChartPanel panel) throws Exception {
        Object animation = panel.getClientProperty(ShowcaseAnimation.class.getName());
        assertNotNull(animation, "The actual selected showcase must install its animation");
        Field frame = ShowcaseAnimation.class.getDeclaredField("frame");
        frame.setAccessible(true);
        return (ShowcaseAnimation.Frame) frame.get(animation);
    }

    private static ChartRenderer calibrationSignal(ArberChartPanel panel) throws Exception {
        // Inspect the actual configured supporting renderer, not a test replacement.
        Field preData = ArberChartPanel.class.getDeclaredField("preDataLayers");
        preData.setAccessible(true);
        List<?> layers = (List<?>) preData.get(panel);
        assertEquals(1, layers.size());
        Field renderer = layers.getFirst().getClass().getDeclaredField("renderer");
        renderer.setAccessible(true);
        return (ChartRenderer) renderer.get(layers.getFirst());
    }

    private static List<double[]> renderedSegments(ChartRenderer renderer, ArberChartPanel panel) {
        List<double[]> segments = new ArrayList<>();
        double[] previous = {Double.NaN, Double.NaN};
        ArberCanvas recorder = (ArberCanvas) Proxy.newProxyInstance(ArberCanvas.class.getClassLoader(),
                new Class<?>[]{ArberCanvas.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("moveTo")) {
                        previous[0] = ((Number) arguments[0]).doubleValue();
                        previous[1] = ((Number) arguments[1]).doubleValue();
                    } else if (method.getName().equals("lineTo")) {
                        double x = ((Number) arguments[0]).doubleValue();
                        double y = ((Number) arguments[1]).doubleValue();
                        segments.add(new double[]{previous[0], previous[1], x, y});
                        previous[0] = x;
                        previous[1] = y;
                    } else if (method.getName().equals("drawPolyline")) {
                        float[] xs = (float[]) arguments[0];
                        float[] ys = (float[]) arguments[1];
                        int count = (int) arguments[2];
                        for (int i = 1; i < count; i++) {
                            segments.add(new double[]{xs[i - 1], ys[i - 1], xs[i], ys[i]});
                        }
                    } else if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, arguments);
                    }
                    return null;
                });
        renderer.render(recorder, panel.getPrimaryModel(), panel.getDebugContext());
        return segments;
    }

    private static double[] pixelScale(ArberChartPanel panel) {
        double[] origin = new double[2];
        double[] unit = new double[2];
        panel.getDebugContext().mapToPixel(0, 0, origin);
        panel.getDebugContext().mapToPixel(1, 1, unit);
        return new double[]{unit[0] - origin[0], origin[1] - unit[1]};
    }

    private static void assertSignalSpansDemoDomain(ArberChartPanel panel, BufferedImage visible) {
        setPrimaryLayersVisible(panel, false);
        BufferedImage hidden = paint(panel);
        setPrimaryLayersVisible(panel, true);
        var context = panel.getDebugContext();
        var bounds = context.getPlotBounds();
        var offset = panel.getOverlayCanvasForTesting();
        double[] start = new double[2];
        double[] end = new double[2];
        context.mapToPixel(0, 0, start);
        context.mapToPixel(6, 0, end);
        int populatedBands = 0;
        for (int band = 0; band < 8; band++) {
            int pixels = 0;
            int left = (int) (start[0] + (end[0] - start[0]) * band / 8) + offset.getX();
            int right = (int) (start[0] + (end[0] - start[0]) * (band + 1) / 8) + offset.getX();
            for (int y = (int) bounds.y() + offset.getY(); y < bounds.maxY() + offset.getY(); y++) {
                for (int x = Math.max(0, left); x < Math.min(visible.getWidth(), right); x++) {
                    if (visible.getRGB(x, y) != hidden.getRGB(x, y)) pixels++;
                }
            }
            if (pixels >= 10) populatedBands++;
        }
        assertTrue(populatedBands >= 7, "The actual signal must occupy at least seven of eight time-domain bands");
    }

    @ParameterizedTest
    @CsvSource({
            "dark,analysis.MinMaxMarkerRenderer", "light,analysis.MinMaxMarkerRenderer",
            "dark,analysis.PeakDetectionRenderer", "light,analysis.PeakDetectionRenderer",
            "dark,forensic.PlaybackStatusRenderer", "light,forensic.PlaybackStatusRenderer",
            "dark,predictive.PredictiveShadowRenderer", "light,predictive.PredictiveShadowRenderer",
            "dark,standard.PredictivePathRenderer", "light,standard.PredictivePathRenderer"
    })
    void markerAndForecastDemosShowTheirHistoricalSignal(String theme, String renderer) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render." + renderer);
        onEdt(() -> {
            paint(panel);
            setPrimaryLayersVisible(panel, false);
            assertSignalVisible(panel, paint(panel));
        });
    }

    @ParameterizedTest
    @CsvSource({
            "dark,predictive.PredictiveShadowRenderer", "light,predictive.PredictiveShadowRenderer",
            "dark,standard.PredictivePathRenderer", "light,standard.PredictivePathRenderer",
            "dark,financial.PredictiveCandleRenderer", "light,financial.PredictiveCandleRenderer"
    })
    void forecastsFitOnResetAndAnimationPreservesManualZoom(String theme, String renderer) throws Exception {
        ArberChartPanel panel = open(theme, "com.arbergashi.charts.render." + renderer);
        onEdt(() -> {
            BufferedImage visible = paint(panel);
            assertTrue(primaryPixelCount(panel, visible) > 100, "The forecast itself must contribute visible pixels");
            assertPreferredRangesVisible(panel);
            if (renderer.endsWith("PredictiveCandleRenderer")) {
                FinancialChartModel model = assertInstanceOf(FinancialChartModel.class, panel.getPrimaryModel());
                assertTrue(model.getHigh(0) > model.getLow(0), "Historical candles require actual OHLC data");
                setPrimaryLayersVisible(panel, false);
                assertTrue(countFinancialPixels(panel, paint(panel)) > 1000,
                        "Historical candles must remain visible below the forecast");
                setPrimaryLayersVisible(panel, true);
            }
            double initialWidth = panel.getDebugContext().getMaxX() - panel.getDebugContext().getMinX();
            var canvas = panel.getOverlayCanvasForTesting();
            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseWheelEvent.MOUSE_WHEEL, 1, 0,
                    canvas.getWidth() / 2, canvas.getHeight() / 2, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -2));
            paint(panel);
            double[] zoomed = viewport(panel);
            assertTrue(zoomed[1] - zoomed[0] < initialWidth, "The real wheel interaction must change the view");
            int pointerX = canvas.getWidth() / 2;
            int pointerY = canvas.getHeight() / 2;
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 2, 0,
                    pointerX, pointerY, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 3, InputEvent.BUTTON1_DOWN_MASK,
                    pointerX + 30, pointerY, 0, false, MouseEvent.NOBUTTON));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 4, 0,
                    pointerX + 30, pointerY, 1, false, MouseEvent.BUTTON1));
            paint(panel);
            double[] manualView = viewport(panel);
            assertTrue(Math.abs(manualView[0] - zoomed[0]) > 1e-6, "The real drag interaction must pan the view");
            Object animation = panel.getClientProperty(ShowcaseAnimation.class.getName());
            Field frame = ShowcaseAnimation.class.getDeclaredField("frame");
            frame.setAccessible(true);
            ((ShowcaseAnimation.Frame) frame.get(animation)).update(2.0, 0.016);
            panel.setTheme(panel.getTheme());
            paint(panel);
            assertArrayEquals(manualView, viewport(panel), 1e-9,
                    "A live update must not reset the customer's zoom or pan");
            panel.resetZoom();
            paint(panel);
            assertPreferredRangesVisible(panel);
            assertFalse(java.util.Arrays.equals(manualView, viewport(panel)), "Reset must fit the updated data and forecast");
        });
    }

    private static void assertPreferredRangesVisible(ArberChartPanel panel) {
        double[] x = panel.getPrimaryRenderer().getPreferredXRange(panel.getPrimaryModel());
        double[] y = panel.getPrimaryRenderer().getPreferredYRange(panel.getPrimaryModel());
        var context = panel.getDebugContext();
        assertNotNull(x);
        assertNotNull(y);
        assertTrue(context.getMinX() <= x[0] && context.getMaxX() >= x[1], "Complete forecast X range must fit");
        assertTrue(context.getMinY() <= y[0] && context.getMaxY() >= y[1], "Complete forecast Y range must fit");
    }

    private static double[] viewport(ArberChartPanel panel) {
        var context = panel.getDebugContext();
        return new double[]{context.getMinX(), context.getMaxX(), context.getMinY(), context.getMaxY()};
    }

    private static void setPrimaryLayersVisible(ArberChartPanel panel, boolean visible) {
        for (int i = 0; i < panel.getLayerCount(); i++) {
            panel.getLayerVisibilityModel().setVisible(panel.getLayerId(i), visible);
        }
    }

    private static int primaryPixelCount(ArberChartPanel panel, BufferedImage visible) {
        setPrimaryLayersVisible(panel, false);
        BufferedImage hidden = paint(panel);
        setPrimaryLayersVisible(panel, true);
        var bounds = panel.getDebugContext().getPlotBounds();
        var offset = panel.getOverlayCanvasForTesting();
        int count = 0;
        for (int y = (int) bounds.y() + offset.getY(); y < bounds.maxY() + offset.getY(); y++) {
            for (int x = (int) bounds.x() + offset.getX(); x < bounds.maxX() + offset.getX(); x++) {
                if (visible.getRGB(x, y) != hidden.getRGB(x, y)) count++;
            }
        }
        return count;
    }

    private static int countFinancialPixels(ArberChartPanel panel, BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (near(image.getRGB(x, y), panel.getTheme().getBullishColor())
                        || near(image.getRGB(x, y), panel.getTheme().getBearishColor())) count++;
            }
        }
        return count;
    }

    private ArberChartPanel open(String theme, String className) throws Exception {
        onEdt(() -> {
            DemoThemeSupport.setupLookAndFeel();
            DemoThemeSupport.applyLookAndFeel(theme);
            application = new DemoApplication(theme);
            workspace = application.buildWorkspace();
            workspace.setSize(1360, 800);
            layout(workspace);
            JTree tree = findTree(workspace);
            assertNotNull(tree);
            var root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            var nodes = root.depthFirstEnumeration();
            while (nodes.hasMoreElements()) {
                var node = (DefaultMutableTreeNode) nodes.nextElement();
                if (node.getUserObject() instanceof RendererCatalogEntry entry && entry.className().equals(className)) {
                    tree.setSelectionPath(new TreePath(node.getPath()));
                    return;
                }
            }
            fail("Missing renderer: " + className);
        });
        AtomicReference<ArberChartPanel> active = new AtomicReference<>();
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (active.get() == null && System.nanoTime() < deadline) {
            onEdt(() -> {
                Field field = DemoApplication.class.getDeclaredField("currentChartPanel");
                field.setAccessible(true);
                active.set((ArberChartPanel) field.get(application));
                if (active.get() != null) {
                    active.get().setAnimationsEnabled(false);
                    layout(workspace);
                    // Axis labels measure their required margins from the initial rendered range.
                    for (int pass = 0; pass < 3; pass++) paint(active.get());
                }
            });
            if (active.get() == null) Thread.sleep(10);
        }
        assertNotNull(active.get(), "The selected workspace view must load");
        return active.get();
    }

    private static void assertSignalVisible(ArberChartPanel panel, BufferedImage image) {
        var model = panel.getPrimaryModel();
        var context = panel.getDebugContext();
        var offset = panel.getOverlayCanvasForTesting();
        ArberColor color = panel.getTheme().getSeriesColor(0);
        double[] pixel = new double[2];
        int visible = 0;
        for (int fraction = 1; fraction < 10; fraction++) {
            int index = (model.getPointCount() - 1) * fraction / 10;
            context.mapToPixel(model.getX(index), model.getY(index), pixel);
            int cx = (int) Math.round(pixel[0]) + offset.getX();
            int cy = (int) Math.round(pixel[1]) + offset.getY();
            boolean found = false;
            for (int y = Math.max(0, cy - 3); y <= Math.min(image.getHeight() - 1, cy + 3); y++) {
                for (int x = Math.max(0, cx - 3); x <= Math.min(image.getWidth() - 1, cx + 3); x++) {
                    if (near(image.getRGB(x, y), color)) found = true;
                }
            }
            if (found) visible++;
        }
        assertTrue(visible >= 7, "The model's signal must be visible at its mapped coordinates");
    }

    private static boolean near(int pixel, ArberColor color) {
        return Math.abs(((pixel >>> 16) & 255) - color.red()) <= 20
                && Math.abs(((pixel >>> 8) & 255) - color.green()) <= 20
                && Math.abs((pixel & 255) - color.blue()) <= 20;
    }

    private static BufferedImage paint(ArberChartPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            panel.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static JTree findTree(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTree tree) return tree;
            if (child instanceof Container container) {
                JTree tree = findTree(container);
                if (tree != null) return tree;
            }
        }
        return null;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
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
