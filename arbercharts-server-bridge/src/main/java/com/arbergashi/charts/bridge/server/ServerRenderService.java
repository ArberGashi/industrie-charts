package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.engine.spatial.SpatialFillConsumer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatch;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Headless render service with a lightweight session pool.
 */
public final class ServerRenderService {
    private static final System.Logger LOGGER = System.getLogger(ServerRenderService.class.getName());
    private final RenderSessionPool pool;
    private final boolean metricsEnabled;
    private final Timer renderTimer;
    private final Counter renderSuccess;
    private final Counter renderFailure;

    public ServerRenderService() {
        this(Math.max(1, Runtime.getRuntime().availableProcessors()), true, null);
    }

    public ServerRenderService(int poolSize) {
        this(poolSize, true, null);
    }

    public ServerRenderService(int poolSize, boolean metricsEnabled, MeterRegistry registry) {
        this.pool = new RenderSessionPool(Math.max(1, poolSize));
        this.metricsEnabled = metricsEnabled;
        if (registry != null) {
            this.renderTimer = Timer.builder("arber.charts.render.time").register(registry);
            this.renderSuccess = Counter.builder("arber.charts.render.count")
                    .tag("result", "success")
                    .register(registry);
            this.renderFailure = Counter.builder("arber.charts.render.count")
                    .tag("result", "failure")
                    .register(registry);
            Gauge.builder("arber.charts.pool.active", pool, RenderSessionPool::activeCount)
                    .register(registry);
            Gauge.builder("arber.charts.pool.idle", pool, RenderSessionPool::idleCount)
                    .register(registry);
        } else {
            this.renderTimer = null;
            this.renderSuccess = null;
            this.renderFailure = null;
        }
    }

    public BufferedImage renderToImage(ChartModel model, Dimension size) {
        return renderToImage(model, size, ChartThemes.getDarkTheme(), null);
    }

    public byte[] renderToPng(ChartModel model, Dimension size) {
        BufferedImage image = renderToImage(model, size);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to encode PNG", e);
        }
    }

    public byte[] renderToPng(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer) {
        BufferedImage image = renderToImage(model, size, theme, renderer);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to encode PNG", e);
        }
    }

    public BufferedImage renderToImage(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer) {
        if (model == null || size == null) {
            throw new IllegalArgumentException("model and size are required");
        }
        long start = metricsEnabled ? System.nanoTime() : 0L;
        int width = Math.max(1, size.width);
        int height = Math.max(1, size.height);

        RenderSession session = pool.acquire();
        try {
            session.ensureCanvas(width, height);
            Arrays.fill(session.canvas.pixels(), 0);

            ArberRect bounds = new ArberRect(0, 0, width, height);
            DefaultPlotContext context = new DefaultPlotContext(
                    bounds,
                    model,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    theme != null ? theme : ChartThemes.getDarkTheme(),
                    null
            );

            ChartRenderer resolved = renderer != null ? renderer : selectRenderer(model);
            if (resolved instanceof SpatialChunkRenderer spatialRenderer) {
                renderSpatial(session, spatialRenderer, model, context);
            } else {
                resolved.render(session.canvas, model, context);
            }

            BufferedImage image = toImage(session.canvas);
            if (metricsEnabled) {
                long elapsed = System.nanoTime() - start;
                double millis = elapsed / 1_000_000.0;
                int points = model.getPointCount();
                LOGGER.log(System.Logger.Level.DEBUG,
                        "[Arber] Rendered {0} points in {1,number,0.###}ms",
                        points, millis);
                if (renderTimer != null) {
                    renderTimer.record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
                }
                if (renderSuccess != null) {
                    renderSuccess.increment();
                }
            }
            return image;
        } catch (RuntimeException e) {
            if (metricsEnabled && renderFailure != null) {
                renderFailure.increment();
            }
            throw e;
        } finally {
            pool.release(session);
        }
    }

    public int getPoolActive() {
        return pool.activeCount();
    }

    public int getPoolIdle() {
        return pool.idleCount();
    }

    public long getPoolContentionCount() {
        return pool.contentionCount();
    }

    private static ChartRenderer selectRenderer(ChartModel model) {
        if (model instanceof DefaultFinancialChartModel) {
            return new CandlestickRenderer();
        }
        return new LineRenderer();
    }

    private static void renderSpatial(RenderSession session,
                                      SpatialChunkRenderer renderer,
                                      ChartModel model,
                                      DefaultPlotContext context) {
        SpatialPathBatchBuilder builder = renderer.getSpatialPathBatchBuilder();
        if (builder == null) {
            builder = new SpatialPathBatchBuilder();
        } else {
            builder.reset();
        }

        SpatialServerConsumer consumer = new SpatialServerConsumer(session.canvas, builder, session);
        renderer.renderSpatial(model, context, consumer);

        SpatialPathBatch batch = builder.getBatch();
        int count = batch.getPointCount();
        if (count <= 0) {
            return;
        }

        session.ensureSpatialBuffers(count);
        float[] outX = session.spatialX;
        float[] outY = session.spatialY;
        int outCount = 0;
        long currentStyle = Long.MIN_VALUE;

        double[] xs = batch.getXData();
        double[] ys = batch.getYData();

        for (int i = 0; i < count; i++) {
            if (!batch.isVisible(i)) {
                outCount = flushSpatial(session.canvas, outX, outY, outCount);
                continue;
            }
            if (builder.isMoveTo(i)) {
                outCount = flushSpatial(session.canvas, outX, outY, outCount);
            }
            long style = batch.getStyleKey(i);
            if (style != currentStyle) {
                outCount = flushSpatial(session.canvas, outX, outY, outCount);
                applySpatialStyle(session.canvas, style);
                currentStyle = style;
            }
            outX[outCount] = (float) xs[i];
            outY[outCount] = (float) ys[i];
            outCount++;
        }
        flushSpatial(session.canvas, outX, outY, outCount);
    }

    private static void applySpatialStyle(ImageBufferCanvas canvas, long styleKey) {
        int argb = SpatialStyleDescriptor.unpackArgb(styleKey);
        float stroke = SpatialStyleDescriptor.unpackStrokeWidth(styleKey);
        canvas.setColor(new ArberColor(argb));
        canvas.setStroke(stroke);
    }

    private static int flushSpatial(ImageBufferCanvas canvas, float[] xs, float[] ys, int count) {
        if (count > 1) {
            canvas.drawPolyline(xs, ys, count);
        }
        return 0;
    }

    private static BufferedImage toImage(ImageBufferCanvas canvas) {
        int width = canvas.width();
        int height = canvas.height();
        int[] pixels = canvas.pixels();

        DirectColorModel colorModel = new DirectColorModel(
                32,
                0x00FF0000,
                0x0000FF00,
                0x000000FF,
                0xFF000000
        );
        WritableRaster raster = WritableRaster.createPackedRaster(
                new DataBufferInt(pixels, pixels.length),
                width,
                height,
                width,
                new int[]{0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000},
                null
        );
        return new BufferedImage(colorModel, raster, false, null);
    }

    private static final class RenderSession {
        private ImageBufferCanvas canvas;
        private float[] spatialX = new float[0];
        private float[] spatialY = new float[0];
        private final float[] quadX = new float[4];
        private final float[] quadY = new float[4];

        void ensureCanvas(int width, int height) {
            if (canvas == null || canvas.width() != width || canvas.height() != height) {
                canvas = new ImageBufferCanvas(width, height);
            }
        }

        void ensureSpatialBuffers(int required) {
            if (spatialX.length >= required) {
                return;
            }
            int next = 1;
            while (next < required && next > 0) {
                next <<= 1;
            }
            if (next <= 0) {
                next = required;
            }
            spatialX = new float[next];
            spatialY = new float[next];
        }
    }

    private static final class SpatialServerConsumer implements com.arbergashi.charts.engine.spatial.SpatialChunkConsumer, SpatialFillConsumer {
        private final ImageBufferCanvas canvas;
        private final SpatialPathBatchBuilder builder;
        private final RenderSession session;

        private SpatialServerConsumer(ImageBufferCanvas canvas, SpatialPathBatchBuilder builder, RenderSession session) {
            this.canvas = canvas;
            this.builder = builder;
            this.session = session;
        }

        @Override
        public void accept(com.arbergashi.charts.engine.spatial.SpatialBuffer buffer, int count) {
            builder.accept(buffer, count);
        }

        @Override
        public void fillQuad(double x1, double y1,
                             double x2, double y2,
                             double x3, double y3,
                             double x4, double y4) {
            long styleKey = builder.getStyleKey();
            applySpatialStyle(canvas, styleKey);
            session.quadX[0] = (float) x1;
            session.quadY[0] = (float) y1;
            session.quadX[1] = (float) x2;
            session.quadY[1] = (float) y2;
            session.quadX[2] = (float) x3;
            session.quadY[2] = (float) y3;
            session.quadX[3] = (float) x4;
            session.quadY[3] = (float) y4;
            canvas.fillPolygon(session.quadX, session.quadY, 4);
        }
    }

    private static final class RenderSessionPool {
        private final ArrayBlockingQueue<RenderSession> queue;
        private final java.util.concurrent.atomic.AtomicInteger active = new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicLong contention = new java.util.concurrent.atomic.AtomicLong();

        RenderSessionPool(int size) {
            this.queue = new ArrayBlockingQueue<>(size);
        }

        RenderSession acquire() {
            RenderSession session = queue.poll();
            if (session == null) {
                contention.incrementAndGet();
            }
            active.incrementAndGet();
            return (session != null) ? session : new RenderSession();
        }

        void release(RenderSession session) {
            if (session == null) return;
            active.decrementAndGet();
            queue.offer(session);
        }

        int activeCount() {
            return active.get();
        }

        int idleCount() {
            return queue.size();
        }

        long contentionCount() {
            return contention.get();
        }
    }
}
