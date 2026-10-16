package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.domain.legend.LegendDockSide;
import com.arbergashi.charts.domain.render.AxisRole;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.ui.HighPrecisionCrosshair;
import com.arbergashi.charts.platform.ui.TooltipOverlay;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.legend.InteractiveLegendOverlay;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationContractTest {
    @Test
    void medicalCalibrationUsesMillimetersAcrossResizeAndRasterScale() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer());
            panel.setXAxisConfig(new AxisConfig().setMedicalScale(25));
            panel.setYAxisConfig(new AxisConfig().setMedicalScale(10));
            for (int width : new int[]{800, 1200}) {
                panel.setSize(width, 500);
                panel.doLayout();
                for (int rasterScale : new int[]{1, 2}) {
                    paint(panel, rasterScale);
                    PlotContext context = panel.getDebugContext();
                    double[] start = new double[2];
                    double[] end = new double[2];
                    context.mapToPixel(0, 0, start);
                    context.mapToPixel(1, 1, end);
                    assertEquals(25 * 96.0 / 25.4, end[0] - start[0], 1e-8);
                    assertEquals(10 * 96.0 / 25.4, start[1] - end[1], 1e-8);
                }
            }
        });
    }

    @Test
    void allDockPositionsSharePaintAndPointerCoordinates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (LegendDockSide side : LegendDockSide.values()) {
                ProbeRenderer renderer = new ProbeRenderer();
                ArberChartPanel panel = panel(renderer).setDockedLegend(side);
                panel.doLayout();
                BufferedImage image = paint(panel, 1);
                JComponent canvas = panel.getOverlayCanvasForTesting();
                ArberRect plot = panel.getDebugContext().getPlotBounds();
                assertTrue(plot.getMaxX() <= canvas.getWidth());
                assertTrue(plot.getMaxY() <= canvas.getHeight());
                int x = (int) Math.round(renderer.pixel[0]) + canvas.getX();
                int y = (int) Math.round(renderer.pixel[1]) + canvas.getY();
                assertEquals(0xffff0000, image.getRGB(x, y), side.toString());
                double[] data = new double[2];
                panel.getDebugContext().mapToData(x - canvas.getX(), y - canvas.getY(), data);
                assertEquals(1.0, data[0], 0.01);
                assertEquals(1.0, data[1], 0.01);
            }
        });
    }

    @Test
    void themeAndLookAndFeelUpdatesKeepDockedLegendExclusive() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (LegendDockSide side : LegendDockSide.values()) {
                ArberChartPanel panel = panel(new ProbeRenderer()).setDockedLegend(side);
                panel.setTheme(ChartThemes.getLightTheme());
                panel.updateUI();
                assertFalse(legend(panel).isVisible());
            }
        });
    }

    @Test
    void legendClickTargetsOneOfTwoSameTypeSeriesAndUsesItsModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProbeRenderer first = new ProbeRenderer();
            ProbeRenderer second = new ProbeRenderer();
            ArberChartPanel panel = panel(first);
            ChartModel other = model("Beta");
            panel.setLayer(other, second);
            paint(panel, 1);
            InteractiveLegendOverlay legend = legend(panel);
            Rectangle bounds = legend.getLegendBounds();
            int x = bounds.x + 27;
            int y = bounds.y + 12;
            assertSame(legend, SwingUtilities.getDeepestComponentAt(panel.getOverlayCanvasForTesting(), x, y));
            int firstCount = first.draws;
            int secondCount = second.draws;
            click(legend, MouseEvent.MOUSE_CLICKED, x, y);
            paint(panel, 1);
            assertEquals(firstCount, first.draws, "Only the clicked series is hidden");
            assertEquals(secondCount + 1, second.draws);
            assertSame(other, second.legendModel);
        });
    }

    @Test
    void legendFocusReceivesPerSeriesModelsAndNearestIndices() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProbeRenderer first = new ProbeRenderer();
            ProbeRenderer second = new ProbeRenderer();
            ArberChartPanel panel = panel(first);
            DefaultChartModel other = new DefaultChartModel("Beta");
            other.setXYArrays(new double[]{0, 0.5, 1, 2}, new double[]{0, 0.5, 1, 2});
            panel.setLayer(other, second);
            paint(panel, 1);
            JComponent canvas = panel.getOverlayCanvasForTesting();
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 1, 0,
                    (int) first.pixel[0], (int) first.pixel[1], 0, false));
            paint(panel, 1);
            assertSame(other, second.focusModel);
            assertEquals(2, second.focusIndex);
            assertNotNull(second.focusContext);
        });
    }

    @Test
    void coordinateTransformIsUsedForDrawingAndInverseMapping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProbeRenderer renderer = new ProbeRenderer();
            ArberChartPanel panel = panel(renderer);
            panel.setXAxisConfig(new AxisConfig().setFixedRange(0, 10));
            panel.setYAxisConfig(new AxisConfig().setFixedRange(0, 10));
            int[] calls = {0};
            panel.setCoordinateTransformer(new CoordinateTransformer() {
                @Override
                public void mapToPixel(PlotContext base, double x, double y, double[] out) {
                    calls[0]++;
                    base.mapToPixel(x * 2, y * 3, out);
                }

                @Override
                public void mapToData(PlotContext base, double x, double y, double[] out) {
                    base.mapToData(x, y, out);
                    out[0] /= 2;
                    out[1] /= 3;
                }
            });
            paint(panel, 1);
            ArberRect plot = renderer.context.getPlotBounds();
            assertTrue(calls[0] > 0);
            assertEquals(plot.x() + plot.width() * 0.2, renderer.pixel[0], 1e-8);
            assertEquals(plot.y() + plot.height() * 0.7, renderer.pixel[1], 1e-8);
            double[] data = new double[2];
            panel.getDebugContext().mapToData(renderer.pixel[0], renderer.pixel[1], data);
            assertEquals(1, data[0], 1e-8);
            assertEquals(1, data[1], 1e-8);
        });
    }

    @Test
    void secondarySeriesUsesIndependentConfiguredYAxis() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProbeRenderer primary = new ProbeRenderer();
            ProbeRenderer secondary = new ProbeRenderer();
            secondary.setYAxisRole(AxisRole.SECONDARY);
            ArberChartPanel panel = panel(primary);
            panel.setLayer(model("Secondary"), secondary);
            panel.setYAxisConfig(new AxisConfig().setFixedRange(-1, 1));
            panel.setYAxisSecondaryConfig(new AxisConfig().setFixedRange(-1000, 1000));
            paint(panel, 1);
            assertEquals(-1, primary.context.getMinY());
            assertEquals(1, primary.context.getMaxY());
            assertEquals(-1000, secondary.context.getMinY());
            assertEquals(1000, secondary.context.getMaxY());
            assertEquals(primary.context.getPlotBounds(), secondary.context.getPlotBounds());
            assertTrue(Math.abs(primary.pixel[1] - secondary.pixel[1]) > 100);
        });
    }

    @Test
    void crosshairAndTooltipKeepSecondaryAxisPrecisionWhenSnapping() throws Exception {
        Field yLabel = HighPrecisionCrosshair.class.getDeclaredField("yLabel");
        yLabel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            for (boolean inverted : new boolean[]{false, true}) {
                DefaultChartModel primaryModel = new DefaultChartModel("Primary");
                primaryModel.setXYArrays(new double[]{0.25}, new double[]{500});
                ProbeRenderer primary = new ProbeRenderer();
                ArberChartPanel panel = new ArberChartPanel(primaryModel, primary).setLegend(false);
                DefaultChartModel secondaryModel = new DefaultChartModel("Secondary");
                secondaryModel.setXYArrays(new double[]{0.75, 1.25}, new double[]{0.00025, 0.0005});
                ProbeRenderer secondary = new ProbeRenderer();
                secondary.setYAxisRole(AxisRole.SECONDARY);
                panel.setLayer(secondaryModel, secondary);
                panel.setLocale(Locale.US);
                panel.setXAxisConfig(new AxisConfig().setFixedRange(0, 2));
                panel.setYAxisConfig(new AxisConfig().setFixedRange(-1000, 1000).setUnitSuffix("mA"));
                panel.setYAxisSecondaryConfig(new AxisConfig().setFixedRange(0, 0.001)
                        .setInverted(inverted).setUnitSuffix("mV"));
                panel.setSize(800, 500);
                panel.doLayout();
                paint(panel, 1);
                JComponent canvas = panel.getOverlayCanvasForTesting();
                HighPrecisionCrosshair crosshair = overlayComponent(panel, HighPrecisionCrosshair.class);
                TooltipOverlay tooltip = overlayComponent(panel, TooltipOverlay.class);
                double[] pixel = new double[2];
                String[] expectedY = {"0.00025 mV", "0.0005 mV"};
                try {
                    for (int i = 0; i < secondaryModel.getPointCount(); i++) {
                        secondary.context.mapToPixel(secondaryModel.getX(i), secondaryModel.getY(i), pixel);
                        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 1, 0,
                                (int) Math.round(pixel[0]), (int) Math.round(pixel[1]), 0, false));
                        assertEquals(expectedY[i], assertDoesNotThrow(() -> yLabel.get(crosshair)),
                                "Snapping must retain the secondary axis precision, including inverted axes");
                        assertEquals(List.of("X: " + secondaryModel.getX(i) + "  Y: " + expectedY[i]), tooltip.getLines());
                        assertTrue(tooltip.isVisible());
                    }
                    primary.context.mapToPixel(0.25, 500, pixel);
                    canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 1, 0,
                            (int) Math.round(pixel[0]), (int) Math.round(pixel[1]), 0, false));
                    assertEquals("500 mA", assertDoesNotThrow(() -> yLabel.get(crosshair)));
                    assertEquals(List.of("X: 0.25  Y: 500 mA"), tooltip.getLines());
                } finally {
                    crosshair.removeNotify();
                }
            }
        });
    }

    @Test
    void invertedWheelZoomKeepsPointerDataValueFixed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer()).setAnimationsEnabled(false);
            panel.setXAxisConfig(new AxisConfig().setInverted(true));
            paint(panel, 1);
            JComponent canvas = panel.getOverlayCanvasForTesting();
            double[] before = new double[2];
            panel.getDebugContext().mapToData(130, 150, before);
            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, 1, 0, 130, 150, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
            double[] after = new double[2];
            panel.getDebugContext().mapToData(130, 150, after);
            assertEquals(before[0], after[0], 1e-8);
        });
    }

    @Test
    void invertedDragMovesDataWithPointer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer()).setAnimationsEnabled(false);
            panel.setXAxisConfig(new AxisConfig().setInverted(true));
            paint(panel, 1);
            JComponent canvas = panel.getOverlayCanvasForTesting();
            double[] before = new double[2];
            panel.getDebugContext().mapToPixel(1, 1, before);
            click(canvas, MouseEvent.MOUSE_PRESSED, 200, 150);
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 2, InputEvent.BUTTON1_DOWN_MASK,
                    240, 150, 0, false, MouseEvent.BUTTON1));
            double[] after = new double[2];
            panel.getDebugContext().mapToPixel(1, 1, after);
            assertEquals(40, after[0] - before[0], 1e-8);
        });
    }

    @Test
    void transformedWheelZoomAndPanRemainInViewportCoordinates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean inverted : new boolean[]{false, true}) {
                ArberChartPanel panel = panel(new ProbeRenderer()).setAnimationsEnabled(false);
                panel.setXAxisConfig(new AxisConfig().setInverted(inverted));
                panel.setCoordinateTransformer(new CoordinateTransformer() {
                    @Override
                    public void mapToPixel(PlotContext base, double x, double y, double[] out) {
                        base.mapToPixel(x * 2, y, out);
                    }

                    @Override
                    public void mapToData(PlotContext base, double x, double y, double[] out) {
                        base.mapToData(x, y, out);
                        out[0] /= 2;
                    }
                });
                paint(panel, 1);
                JComponent canvas = panel.getOverlayCanvasForTesting();
                double[] before = new double[2];
                panel.getDebugContext().mapToData(200, 200, before);
                canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, 1, 0, 200, 200, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                double[] after = new double[2];
                panel.getDebugContext().mapToData(200, 200, after);
                assertEquals(before[0], after[0], 1e-8, "Zoom must retain the transformed data under the pointer");
                click(canvas, MouseEvent.MOUSE_PRESSED, 200, 200);
                canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 2, InputEvent.BUTTON1_DOWN_MASK,
                        240, 200, 0, false, MouseEvent.BUTTON1));
                panel.getDebugContext().mapToPixel(before[0], before[1], after);
                assertEquals(240, after[0], 1e-8, "Transformed data must move with the drag, including inverted axes");
            }
        });
    }

    @Test
    void freezeMeasurementHasVisibleBoundsAndPaintsText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer()).setLegend(false).setFreeze(true);
            BufferedImage before = paint(panel, 1);
            JComponent canvas = panel.getOverlayCanvasForTesting();
            click(canvas, MouseEvent.MOUSE_PRESSED, 250, 200);
            click(canvas, MouseEvent.MOUSE_PRESSED, 450, 200);
            assertSame(canvas, panel.getFreezeMeasurementLabel().getParent());
            assertTrue(panel.getFreezeMeasurementLabel().getWidth() > 0);
            assertTrue(panel.getFreezeMeasurementLabel().isVisible());
            BufferedImage after = paint(panel, 1);
            Rectangle bounds = panel.getFreezeMeasurementLabel().getBounds();
            assertTrue(differentPixels(before, after, bounds) > 10);
            panel.setFreeze(false);
            assertFalse(panel.getFreezeMeasurementLabel().isVisible());
        });
    }

    @Test
    void axisLabelsExcludeOutwardTicksAndReserveLongLabelSpace() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer());
            panel.setXAxisConfig(new AxisConfig().setFixedRange(0.1, 1.9));
            panel.setYAxisConfig(new AxisConfig().setFixedRange(0.1, 1.9));
            PlotContext context = panel.getDebugContext();
            BufferedImage axes = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = axes.createGraphics();
            try { panel.drawAxisLabels(graphics, context); }
            finally { graphics.dispose(); }
            assertEquals(0, nontransparent(axes, new Rectangle(0, 0, 800, 20)), "Outward Y ticks must not paint above the plot");
            assertEquals(0, nontransparent(axes, new Rectangle(0, 460, 30, 40)), "Outward X ticks must not paint left of the plot");
            double initialLeft = context.getPlotBounds().x();
            panel.setYAxisConfig(new AxisConfig().setFixedRange(0, 2_000_000).setUnitSuffix("millivolts"));
            assertTrue(panel.getDebugContext().getPlotBounds().x() > initialLeft + 40);
        });
    }

    @Test
    void hostPropagatesIndependentGridFlagsAndLocaleFormatting() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer());
            panel.setLocale(Locale.GERMANY);
            panel.setXAxisConfig(new AxisConfig().setShowGrid(false));
            PlotContext context = panel.getDebugContext();
            assertFalse(context.isGridVisibleX());
            assertTrue(context.isGridVisibleY());
            BufferedImage noX = paint(panel, 1);
            panel.setXAxisConfig(new AxisConfig().setShowGrid(true));
            BufferedImage both = paint(panel, 1);
            assertTrue(differentPixels(noX, both, new Rectangle(70, 70, 650, 350)) > 100);
            panel.setXAxisConfig(new AxisConfig().setFixedRange(0, 2));
            panel.setYAxisConfig(new AxisConfig().setFixedRange(0, 2));
            BufferedImage germanLabels = paint(panel, 1);
            panel.setLocale(Locale.US);
            BufferedImage englishLabels = paint(panel, 1);
            assertTrue(differentPixels(germanLabels, englishLabels, new Rectangle(0, 35, 48, 420)) > 0,
                    "Changing the host locale must change the painted decimal labels");
        });
    }

    @Test
    void externalSurfaceIsNotErasedByOverlayRepaints() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ArberChartPanel panel = panel(new ProbeRenderer()).setLegend(false).setExternalRenderSurface(true);
            BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(java.awt.Color.MAGENTA);
                graphics.fillRect(0, 0, 800, 500);
                panel.paint(graphics);
                assertEquals(java.awt.Color.MAGENTA.getRGB(), image.getRGB(400, 250));
                panel.renderExternal(graphics);
                assertTrue(image.getRGB(400, 250) != java.awt.Color.MAGENTA.getRGB());
            } finally { graphics.dispose(); }
        });
    }

    @Test
    void rendererPreferredXRangeControlsAutomaticDomain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProbeRenderer renderer = new ProbeRenderer() {
                @Override public double[] getPreferredXRange(ChartModel model) { return new double[]{0, 500}; }
            };
            ArberChartPanel panel = panel(renderer);
            assertTrue(panel.getDebugContext().getMaxX() >= 500);
            panel.setXAxisConfig(new AxisConfig().setFixedRange(0, 25));
            assertEquals(25, panel.getDebugContext().getMaxX());
        });
    }

    private static ArberChartPanel panel(ProbeRenderer renderer) {
        ArberChartPanel panel = new ArberChartPanel(model("Alpha"), renderer);
        panel.setSize(800, 500);
        panel.doLayout();
        return panel;
    }

    private static DefaultChartModel model(String name) {
        DefaultChartModel model = new DefaultChartModel(name);
        model.setXYArrays(new double[]{0, 1, 2}, new double[]{0, 1, 2});
        return model;
    }

    private static InteractiveLegendOverlay legend(ArberChartPanel panel) {
        for (Component child : panel.getOverlayCanvasForTesting().getComponents()) {
            if (child instanceof InteractiveLegendOverlay legend) return legend;
        }
        throw new AssertionError("Missing overlay legend");
    }

    private static <T extends JComponent> T overlayComponent(ArberChartPanel panel, Class<T> type) {
        for (Component child : panel.getOverlayCanvasForTesting().getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
        }
        throw new AssertionError("Missing overlay component: " + type.getSimpleName());
    }

    private static BufferedImage paint(ArberChartPanel panel, int scale) {
        BufferedImage image = new BufferedImage(panel.getWidth() * scale, panel.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(scale, scale);
            panel.paint(graphics);
        } finally { graphics.dispose(); }
        return image;
    }

    private static void click(Component component, int type, int x, int y) {
        component.dispatchEvent(new MouseEvent(component, type, 1, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static int differentPixels(BufferedImage first, BufferedImage second, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) count++;
            }
        }
        return count;
    }

    private static int nontransparent(BufferedImage image, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }

    private static class ProbeRenderer extends BaseRenderer {
        private final double[] pixel = new double[2];
        private PlotContext context;
        private ChartModel legendModel;
        private ChartModel focusModel;
        private PlotContext focusContext;
        private int focusIndex = -1;
        private int draws;

        ProbeRenderer() { super("probe"); }

        @Override
        protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
            this.context = context;
            draws++;
            context.mapToPixel(1, 1, pixel);
            canvas.setColor(new ArberColor(0xffff0000));
            canvas.fillRect((float) pixel[0] - 3, (float) pixel[1] - 3, 6, 6);
        }

        @Override
        public ArberColor getLegendColor(ChartModel model) {
            legendModel = model;
            return super.getLegendColor(model);
        }

        @Override
        public Map<String, Object> getFocusValues(int index, ChartModel model, PlotContext context) {
            focusModel = model;
            focusIndex = index;
            focusContext = context;
            return Map.of("Y", model.getY(index));
        }
    }
}
