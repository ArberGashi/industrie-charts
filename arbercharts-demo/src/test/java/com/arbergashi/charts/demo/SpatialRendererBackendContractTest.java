package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.bridge.swing.AwtCanvasAdapter;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultTernaryChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.platform.swing.spatial.SwingSpatialBatchConsumer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.specialized.SmithChartRenderer;
import com.arbergashi.charts.render.specialized.TernaryPlotRenderer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises production spatial dispatch, including the wrapper consumers. */
class SpatialRendererBackendContractTest {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final ChartTheme THEME = ChartThemes.getDarkTheme();

    @ParameterizedTest
    @EnumSource(Backend.class)
    void pointStylesPreserveTransparentAndSemitransparentAlpha(Backend backend) {
        BufferedImage image = backend.render(new AlphaMarkerRenderer(), new DefaultChartModel("alpha"));
        assertEquals(0, image.getRGB(100, 100), "An invisible marker must not become opaque black or red");
        assertEquals(0x80FF0000, image.getRGB(150, 100));
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void singleTernaryCompositionRemainsVisible(Backend backend) {
        DefaultTernaryChartModel model = ternary(new double[][]{{1, 1, 1}});
        TernaryPlotRenderer renderer = new TernaryPlotRenderer();
        BufferedImage image = backend.render(renderer, model);
        BufferedImage direct = direct(renderer, model);
        assertNotEquals(0, image.getRGB(200, 194) >>> 24);
        assertEquals(direct.getRGB(200, 194), image.getRGB(200, 194));
        assertTrue(nonTransparent(image) < 40, "A singleton must be a small marker, not a path");
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void ternaryMarkersAreIndependentAndInvalidCompositionsDoNotCreatePoints(Backend backend) {
        DefaultTernaryChartModel model = ternary(new double[][]{
                {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {0, 0, 0}, {-1, 2, 0}, {Double.NaN, 1, 1}});
        TernaryPlotRenderer renderer = new TernaryPlotRenderer();
        renderer.setMultiColor(true);
        BufferedImage image = backend.render(renderer, model);
        int[][] points = {{200, 18}, {48, 282}, {352, 282}};
        for (int i = 0; i < points.length; i++) {
            int[] point = points[i];
            assertNotEquals(0, image.getRGB(point[0], point[1]) >>> 24);
            assertEquals(THEME.getSeriesColor(i).argb(), image.getRGB(point[0], point[1]),
                    "Each composition must retain its own theme palette color");
        }
        assertEquals(0, image.getRGB(124, 150), "No line may connect the A and B compositions");
        assertEquals(0, image.getRGB(200, 282), "No line may connect the B and C compositions");
        assertEquals(0, image.getRGB(200, 194), "No invalid composition may become a center marker");
        assertTrue(nonTransparent(image) < 120);
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void largeTernaryChunksKeepEveryPointIsolated(Backend backend) {
        List<TernaryChartModel.TernaryPoint> points = new ArrayList<>();
        for (int i = 0; i < 1030; i++) {
            points.add(new DefaultTernaryChartModel.DefaultTernaryPoint(1, i % 2, 1));
        }
        DefaultTernaryChartModel model = new DefaultTernaryChartModel(points, List.of("A", "B", "C"));
        BufferedImage image = backend.render(new TernaryPlotRenderer(), model);
        assertTrue(nonTransparent(image) < 80, "Chunk boundaries cannot join the two marker positions");
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void smithSingletonPreservesForegroundEndpointMarker(Backend backend) {
        DefaultChartModel model = new DefaultChartModel("smith");
        model.setXY(0, 0);
        SmithChartRenderer renderer = new SmithChartRenderer();
        BufferedImage image = backend.render(renderer, model);
        BufferedImage direct = direct(renderer, model);
        assertNotEquals(0, image.getRGB(200, 150) >>> 24);
        assertEquals(direct.getRGB(200, 150), image.getRGB(200, 150));
        assertTrue(nonTransparent(image) < 50);
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void smithTraceMarkersAndEndpointsMatchCanvasSemantics(Backend backend) {
        DefaultChartModel model = new DefaultChartModel("smith");
        model.setXY(-0.6, 0);
        model.setXY(0, 0.2);
        model.setXY(0.6, 0);
        SmithChartRenderer renderer = new SmithChartRenderer();
        BufferedImage image = backend.render(renderer, model);
        BufferedImage direct = direct(renderer, model);
        for (int[] endpoint : new int[][]{{117, 150}, {283, 150}}) {
            assertEquals(direct.getRGB(endpoint[0], endpoint[1]), image.getRGB(endpoint[0], endpoint[1]));
            assertNotEquals(0, image.getRGB(endpoint[0], endpoint[1]) >>> 24);
        }
        assertNotEquals(0, image.getRGB(200, 122) >>> 24, "The intermediate marker remains visible");
        assertTrue(hasPixelNear(image, 158, 136), "The trace remains connected between samples");
        assertTrue(hasPixelNear(image, 242, 136));
        assertEquals(0, image.getRGB(200, 200), "No capacity tail may become a phantom point");
    }

    private static DefaultTernaryChartModel ternary(double[][] components) {
        List<TernaryChartModel.TernaryPoint> points = new ArrayList<>();
        for (double[] values : components) {
            points.add(new DefaultTernaryChartModel.DefaultTernaryPoint(values[0], values[1], values[2]));
        }
        return new DefaultTernaryChartModel(points, List.of("A", "B", "C"));
    }

    private static DefaultPlotContext context(ChartModel model) {
        return new DefaultPlotContext(new ArberRect(0, 0, WIDTH, HEIGHT), model, -1, 1, -1, 1, THEME);
    }

    private static BufferedImage direct(SpatialChunkRenderer renderer, ChartModel model) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = graphics(image);
        try {
            renderer.render(new AwtCanvasAdapter(graphics), model, context(model));
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return graphics;
    }

    private static int nonTransparent(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) if ((image.getRGB(x, y) >>> 24) != 0) count++;
        }
        return count;
    }

    private static boolean hasPixelNear(BufferedImage image, int x, int y) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) if ((image.getRGB(x + dx, y + dy) >>> 24) != 0) return true;
        }
        return false;
    }

    private enum Backend {
        SWING {
            BufferedImage render(SpatialChunkRenderer renderer, ChartModel model) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = graphics(image);
                try {
                    SwingSpatialBatchConsumer consumer = new SwingSpatialBatchConsumer(new SpatialPathBatchBuilder())
                            .setGraphics(graphics);
                    renderer.renderSpatial(model, context(model), consumer);
                    consumer.flush();
                    return image;
                } finally {
                    graphics.dispose();
                }
            }
        },
        SERVER {
            BufferedImage render(SpatialChunkRenderer renderer, ChartModel model) {
                try (ServerRenderService service = new ServerRenderService(1, false, null)) {
                    return service.renderToImage(model, new Dimension(WIDTH, HEIGHT), THEME, renderer, -1, 1, -1, 1);
                }
            }
        };

        abstract BufferedImage render(SpatialChunkRenderer renderer, ChartModel model);
    }

    private static final class AlphaMarkerRenderer extends BaseRenderer implements SpatialChunkRenderer {
        private final SpatialBuffer buffer = new SpatialBuffer(2);

        AlphaMarkerRenderer() { super("alpha-marker"); }

        @Override
        public void renderSpatial(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
            double[] points = buffer.getInputCoords();
            points[0] = 100;
            points[1] = 100;
            points[2] = 1;
            points[3] = 150;
            points[4] = 100;
            points[5] = 1;
            buffer.setPointMetadata(0, SpatialStyleDescriptor.pack(0x00FF0000, 10, 0,
                    SpatialStyleDescriptor.MARKER_SQUARE), true);
            buffer.setPointMetadata(1, SpatialStyleDescriptor.pack(0x80FF0000, 10, 0,
                    SpatialStyleDescriptor.MARKER_SQUARE), true);
            consumer.accept(buffer, 2);
        }

        @Override
        public void accept(SpatialBuffer source, int count) { }
    }
}
