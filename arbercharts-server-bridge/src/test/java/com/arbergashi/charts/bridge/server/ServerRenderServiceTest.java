package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRenderServiceTest {
    @Test
    void usesDerivedRendererRangesBeforeFinancialFallback() {
        ChartModel model = generateFinancialCandles(3);
        try (ServerRenderService service = new ServerRenderService(1)) {
            service.renderToImage(model, new Dimension(200, 100), null,
                    rangesRenderer(new double[]{10, 30}, new double[]{-2, 4}, context -> {
                        assertEquals(10, context.getMinX());
                        assertEquals(30, context.getMaxX());
                        assertEquals(-2, context.getMinY());
                        assertEquals(4, context.getMaxY());
                        double[] pixel = new double[2];
                        context.mapToPixel(20, 1, pixel);
                        assertEquals(100, pixel[0], 1e-9);
                        assertEquals(50, pixel[1], 1e-9);
                    }));
        }
    }

    @Test
    void preservesExplicitBoundsAndFillsOnlyMissingBoundsFromDerivedRanges() {
        ChartModel model = generateFinancialCandles(3);
        try (ServerRenderService service = new ServerRenderService(1)) {
            service.renderToImage(model, new Dimension(200, 100), null,
                    rangesRenderer(new double[]{10, 30}, new double[]{-2, 4}, context -> {
                        assertEquals(-10, context.getMinX());
                        assertEquals(40, context.getMaxX());
                        assertEquals(-20, context.getMinY());
                        assertEquals(20, context.getMaxY());
                    }), -10, 40, -20, 20);
            service.renderToImage(model, new Dimension(200, 100), null,
                    rangesRenderer(new double[]{10, 30}, new double[]{-2, 4}, context -> {
                        assertEquals(-10, context.getMinX());
                        assertEquals(30, context.getMaxX());
                        assertEquals(-2, context.getMinY());
                        assertEquals(20, context.getMaxY());
                    }), -10, Double.NaN, Double.NaN, 20);
        }
    }

    @Test
    void ignoresInvalidPreferredRangesAndKeepsFinancialHighLowFallback() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("range");
        model.setOHLC(10, 100, 120, 80, 110);
        model.setOHLC(20, 110, 130, 90, 120);
        List<double[]> invalid = List.of(new double[0], new double[]{1}, new double[]{2, 1},
                new double[]{1, 1}, new double[]{Double.NaN, 1}, new double[]{0, Double.POSITIVE_INFINITY});
        try (ServerRenderService service = new ServerRenderService(1)) {
            for (double[] range : invalid) {
                service.renderToImage(model, new Dimension(200, 100), null, rangesRenderer(range, range, context -> {
                    assertEquals(10, context.getMinX());
                    assertEquals(20, context.getMaxX());
                    assertEquals(80, context.getMinY());
                    assertEquals(130, context.getMaxY());
                }));
            }
        }
    }

    @Test
    void defaultRendererRangesFallBackToOrdinaryModelCoordinates() {
        DefaultChartModel model = new DefaultChartModel("range");
        model.setXY(2, -5);
        model.setXY(8, 9);
        try (ServerRenderService service = new ServerRenderService(1)) {
            service.renderToImage(model, new Dimension(200, 100), null, rangesRenderer(null, null, context -> {
                assertEquals(2, context.getMinX());
                assertEquals(8, context.getMaxX());
                assertEquals(-5, context.getMinY());
                assertEquals(9.9, context.getMaxY(), 1e-9, "Ordinary model scaling retains its 10% headroom");
            }));
        }
    }

    @Test
    void rendersNonEmptyImage() {
        DefaultFinancialChartModel model = generateFinancialCandles(10_000);
        try (ServerRenderService service = new ServerRenderService(1)) {
            BufferedImage image = service.renderToImage(model, new Dimension(800, 600));
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            long sum = 0;
            for (int i = 0; i < pixels.length; i += 97) {
                sum += (pixels[i] & 0xFFFFFFFFL);
            }
            assertTrue(sum > 0, "rendered image should contain non-zero pixels");
        }
    }

    @Test
    void resetsPooledDrawingStateAndPreservesPreviouslyReturnedImages() {
        try (ServerRenderService service = new ServerRenderService(1)) {
            ChartModel model = generateFinancialCandles(2);
            Dimension size = new Dimension(64, 64);
            BufferedImage first = service.renderToImage(model, size, null, renderer(canvas -> {
                canvas.setClip(new ArberRect(24, 24, 16, 16));
                canvas.setStroke(7f);
                canvas.setColor(new ArberColor(0xFFFF0000));
                canvas.fillRect(0, 0, 64, 64);
                canvas.moveTo(50, 50);
            }));
            BufferedImage second = service.renderToImage(model, size, null, renderer(canvas -> {
                assertNull(canvas.getClip());
                canvas.lineTo(8, 0);
                canvas.fillRect(2, 2, 4, 4);
                canvas.drawLine(8, 12, 40, 12);
            }));
            assertEquals(0xFFFF0000, first.getRGB(30, 30), "A result must not alias a reused session buffer");
            assertEquals(0, second.getRGB(30, 30), "The previous frame must be cleared");
            assertEquals(0xFF000000, second.getRGB(4, 0), "Current point must reset to the origin");
            assertEquals(0xFF000000, second.getRGB(3, 3), "Clip and color must reset");
            assertEquals(0xFF000000, second.getRGB(20, 12));
            assertEquals(0, second.getRGB(20, 14), "Stroke width must reset");
            assertEquals(0, first.getRGB(3, 3));
        }
    }

    @Test
    void closesReplacedAndIdleCanvases() {
        AtomicReference<ImageBufferCanvas> first = new AtomicReference<>();
        AtomicReference<ImageBufferCanvas> second = new AtomicReference<>();
        ChartModel model = generateFinancialCandles(2);
        ServerRenderService service = new ServerRenderService(1);
        try (service) {
            service.renderToImage(model, new Dimension(32, 32), null,
                    renderer(canvas -> first.set((ImageBufferCanvas) canvas)));
            service.renderToImage(model, new Dimension(64, 64), null,
                    renderer(canvas -> second.set((ImageBufferCanvas) canvas)));
            assertThrows(IllegalStateException.class, first.get()::reset);
            assertEquals(1, service.getPoolIdle());
            assertEquals(0, service.getPoolActive());
        }
        service.close();
        assertThrows(IllegalStateException.class, second.get()::reset);
        assertEquals(0, service.getPoolIdle());
        assertThrows(IllegalStateException.class,
                () -> service.renderToImage(model, new Dimension(32, 32)));
    }

    @Test
    void closesOverflowSessionsInsteadOfDiscardingTheirContexts() throws Exception {
        List<ImageBufferCanvas> canvases = new CopyOnWriteArrayList<>();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (ServerRenderService service = new ServerRenderService(1)) {
            ChartRenderer renderer = renderer(canvas -> {
                canvases.add((ImageBufferCanvas) canvas);
                entered.countDown();
                await(finish);
                canvas.setColor(new ArberColor(0xFFFF0000));
                canvas.fillRect(0, 0, 32, 32);
            });
            Future<BufferedImage> a = executor.submit(() -> service.renderToImage(
                    generateFinancialCandles(2), new Dimension(32, 32), null, renderer));
            Future<BufferedImage> b = executor.submit(() -> service.renderToImage(
                    generateFinancialCandles(2), new Dimension(32, 32), null, renderer));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(2, service.getPoolActive());
            } finally {
                finish.countDown();
            }
            assertEquals(0xFFFF0000, a.get(5, TimeUnit.SECONDS).getRGB(4, 4));
            assertEquals(0xFFFF0000, b.get(5, TimeUnit.SECONDS).getRGB(4, 4));
            assertEquals(0, service.getPoolActive());
            assertEquals(1, service.getPoolIdle());
            int closedCount = 0;
            for (ImageBufferCanvas canvas : canvases) {
                try {
                    canvas.reset();
                } catch (IllegalStateException expected) {
                    closedCount++;
                }
            }
            assertEquals(1, closedCount, "Exactly one session fits in the idle pool");
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
        for (ImageBufferCanvas canvas : canvases) {
            assertThrows(IllegalStateException.class, canvas::reset);
        }
    }

    @Test
    void letsActiveRendersFinishAndClosesThemAfterShutdown() throws Exception {
        AtomicReference<ImageBufferCanvas> captured = new AtomicReference<>();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerRenderService service = new ServerRenderService(1)) {
            ChartModel model = generateFinancialCandles(2);
            Future<BufferedImage> result = executor.submit(() -> service.renderToImage(
                    model, new Dimension(32, 32), null, renderer(canvas -> {
                        captured.set((ImageBufferCanvas) canvas);
                        entered.countDown();
                        await(finish);
                        canvas.setColor(new ArberColor(0xFFFF0000));
                        canvas.fillRect(0, 0, 32, 32);
                    })));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                service.close();
                assertEquals(1, service.getPoolActive());
                assertThrows(IllegalStateException.class,
                        () -> service.renderToImage(model, new Dimension(32, 32)));
            } finally {
                finish.countDown();
            }
            assertEquals(0xFFFF0000, result.get(5, TimeUnit.SECONDS).getRGB(8, 8));
            assertThrows(IllegalStateException.class, captured.get()::reset);
            assertEquals(0, service.getPoolIdle());
            assertEquals(0, service.getPoolActive());
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void returnsFailedSessionsAndResetsTheirStateForTheNextRender() {
        try (ServerRenderService service = new ServerRenderService(1)) {
            ChartModel model = generateFinancialCandles(2);
            Dimension size = new Dimension(32, 32);
            assertThrows(IllegalArgumentException.class, () -> service.renderToImage(model, size, null,
                    renderer(canvas -> {
                        canvas.setClip(new ArberRect(0, 0, 1, 1));
                        canvas.setColor(new ArberColor(0xFFFF0000));
                        throw new IllegalArgumentException("Renderer failed");
                    })));
            assertEquals(0, service.getPoolActive());
            assertEquals(1, service.getPoolIdle());
            BufferedImage image = service.renderToImage(model, size, null,
                    renderer(canvas -> canvas.fillRect(0, 0, 32, 32)));
            assertEquals(0xFF000000, image.getRGB(20, 20));
        }
    }

    private static ChartRenderer renderer(Consumer<ArberCanvas> draw) {
        return new BaseRenderer("server-test") {
            @Override
            protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
                draw.accept(canvas);
            }
        };
    }

    private static ChartRenderer rangesRenderer(double[] xRange, double[] yRange, Consumer<PlotContext> check) {
        return new BaseRenderer("range-test") {
            @Override
            public double[] getPreferredXRange(ChartModel model) { return xRange; }

            @Override
            public double[] getPreferredYRange(ChartModel model) { return yRange; }

            @Override
            protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
                check.accept(context);
            }
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Render was not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Render interrupted", e);
        }
    }

    private static DefaultFinancialChartModel generateFinancialCandles(int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("ServerTest");
        double price = 100.0;
        for (int i = 0; i < points; i++) {
            double t = i;
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(t, open, high, low, close);
            price = close;
        }
        return model;
    }
}
