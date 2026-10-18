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
import com.arbergashi.charts.model.FinancialChartModel;
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
import java.util.ArrayDeque;
import java.util.function.ToDoubleFunction;

/**
 * Headless render service with a lightweight session pool.
 *
 * <p>Returned images own their pixel data independently of the pool. Close the
 * service when it is no longer needed to release its drawing contexts.</p>
 */
public final class ServerRenderService implements AutoCloseable {
    static {
        com.arbergashi.charts.render.RendererBootstrap.ensureRegistered();
    }

    private static final System.Logger LOGGER = System.getLogger(ServerRenderService.class.getName());
    private final RenderSessionPool pool;
    private final boolean metricsEnabled;
    private final Timer renderTimer;
    private final Counter renderSuccess;
    private final Counter renderFailure;
    private final MeterRegistry registry;
    private final Gauge ownedActiveGauge;
    private final Gauge ownedIdleGauge;

    public ServerRenderService() {
        this(Math.max(1, Runtime.getRuntime().availableProcessors()), true, null);
    }

    public ServerRenderService(int poolSize) {
        this(poolSize, true, null);
    }

    public ServerRenderService(int poolSize, boolean metricsEnabled, MeterRegistry registry) {
        this.pool = new RenderSessionPool(Math.max(1, poolSize));
        this.metricsEnabled = metricsEnabled;
        this.registry = registry;
        if (registry != null) {
            this.renderTimer = Timer.builder("arber.charts.render.time").register(registry);
            this.renderSuccess = Counter.builder("arber.charts.render.count")
                    .tag("result", "success")
                    .register(registry);
            this.renderFailure = Counter.builder("arber.charts.render.count")
                    .tag("result", "failure")
                    .register(registry);
            this.ownedActiveGauge = registerPoolGauge("arber.charts.pool.active", RenderSessionPool::activeCount);
            this.ownedIdleGauge = registerPoolGauge("arber.charts.pool.idle", RenderSessionPool::idleCount);
        } else {
            this.renderTimer = null;
            this.renderSuccess = null;
            this.renderFailure = null;
            this.ownedActiveGauge = null;
            this.ownedIdleGauge = null;
        }
    }

    public BufferedImage renderToImage(ChartModel model, Dimension size) {
        return renderToImage(model, size, ChartThemes.getDarkTheme(), null);
    }

    /**
     * Renders a model to a PNG using the default theme and renderer.
     *
     * @param model the chart data
     * @param size the requested image size
     * @return the encoded PNG bytes
     * @throws IllegalStateException if no PNG writer is available or encoding fails
     */
    public byte[] renderToPng(ChartModel model, Dimension size) {
        return encodePng(renderToImage(model, size));
    }

    /**
     * Renders a model to a PNG using the supplied theme and renderer.
     *
     * @param model the chart data
     * @param size the requested image size
     * @param theme the chart theme, or {@code null} for the default theme
     * @param renderer the chart renderer, or {@code null} for automatic selection
     * @return the encoded PNG bytes
     * @throws IllegalStateException if no PNG writer is available or encoding fails
     */
    public byte[] renderToPng(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer) {
        return encodePng(renderToImage(model, size, theme, renderer));
    }

    /**
     * Renders with an explicit Y view window instead of the model-derived range.
     *
     * <p>Use this for renderers whose value domain differs from the model data,
     * e.g. oscillator indicators (MACD, Stochastic) that are shown in their own
     * pane with their own scale inside a chart panel.</p>
     *
     * @param model the chart data
     * @param size the requested image size
     * @param theme the chart theme, or {@code null} for the default theme
     * @param renderer the chart renderer, or {@code null} for automatic selection
     * @param viewMinY lower bound of the visible Y range
     * @param viewMaxY upper bound of the visible Y range
     * @return the encoded PNG bytes
     * @throws IllegalStateException if no PNG writer is available or encoding fails
     */
    public byte[] renderToPng(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer,
                              double viewMinY, double viewMaxY) {
        return encodePng(renderToImage(model, size, theme, renderer, viewMinY, viewMaxY));
    }

    private static byte[] encodePng(BufferedImage image) {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            if (!javax.imageio.ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("Failed to encode PNG: no PNG image writer is available");
            }
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to encode PNG", e);
        }
    }

    public BufferedImage renderToImage(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer) {
        return renderToImage(model, size, theme, renderer, Double.NaN, Double.NaN);
    }

    /**
     * Renders with an explicit Y view window instead of the model-derived range.
     *
     * @param viewMinY lower bound of the visible Y range (NaN = derive from model)
     * @param viewMaxY upper bound of the visible Y range (NaN = derive from model)
     */
    public BufferedImage renderToImage(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer,
                                       double viewMinY, double viewMaxY) {
        return renderToImage(model, size, theme, renderer, Double.NaN, Double.NaN, viewMinY, viewMaxY);
    }

    /**
     * Renders with an explicit view window on both axes.
     *
     * <p>Use an extended X window to make forward-looking overlays visible
     * (e.g. predictive renderers that draw beyond the last data point), or to
     * render a fixed time window of a larger series. Any NaN bound is derived
     * from the model.</p>
     *
     * @param viewMinX lower bound of the visible X range (NaN = derive from model)
     * @param viewMaxX upper bound of the visible X range (NaN = derive from model)
     * @param viewMinY lower bound of the visible Y range (NaN = derive from model)
     * @param viewMaxY upper bound of the visible Y range (NaN = derive from model)
     */
    public BufferedImage renderToImage(ChartModel model, Dimension size, ChartTheme theme, ChartRenderer renderer,
                                       double viewMinX, double viewMaxX,
                                       double viewMinY, double viewMaxY) {
        if (model == null || size == null) {
            throw new IllegalArgumentException("model and size are required");
        }
        long start = metricsEnabled ? System.nanoTime() : 0L;
        int width = Math.max(1, size.width);
        int height = Math.max(1, size.height);

        RenderSession session = pool.acquire();
        try {
            session.ensureCanvas(width, height);
            session.canvas.reset();

            ArberRect bounds = new ArberRect(0, 0, width, height);
            ChartRenderer resolved = renderer != null ? renderer : selectRenderer(model);
            if (Double.isNaN(viewMinX) || Double.isNaN(viewMaxX)) {
                double[] preferred = resolved.getPreferredXRange(model);
                if (isValidRange(preferred)) {
                    if (Double.isNaN(viewMinX)) viewMinX = preferred[0];
                    if (Double.isNaN(viewMaxX)) viewMaxX = preferred[1];
                }
            }
            if (Double.isNaN(viewMinY) || Double.isNaN(viewMaxY)) {
                double[] preferred = resolved.getPreferredYRange(model);
                if (isValidRange(preferred)) {
                    if (Double.isNaN(viewMinY)) viewMinY = preferred[0];
                    if (Double.isNaN(viewMaxY)) viewMaxY = preferred[1];
                }
            }
            if ((Double.isNaN(viewMinY) || Double.isNaN(viewMaxY)) && model instanceof FinancialChartModel fin) {
                double[] lows = fin.getLowData();
                double[] highs = fin.getHighData();
                int count = Math.min(fin.getPointCount(), Math.min(lows.length, highs.length));
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < count; i++) {
                    double low = lows[i];
                    double high = highs[i];
                    if (Double.isFinite(low)) {
                        min = Math.min(min, low);
                    }
                    if (Double.isFinite(high)) {
                        max = Math.max(max, high);
                    }
                }
                if (min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
                    if (min == max) {
                        double pad = Math.abs(min) * 0.01 + 1e-6;
                        min -= pad;
                        max += pad;
                    }
                    if (Double.isNaN(viewMinY)) viewMinY = min;
                    if (Double.isNaN(viewMaxY)) viewMaxY = max;
                }
            }

            DefaultPlotContext context = new DefaultPlotContext(
                    bounds,
                    model,
                    viewMinX,
                    viewMaxX,
                    viewMinY,
                    viewMaxY,
                    theme != null ? theme : ChartThemes.getDarkTheme(),
                    null
            );

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

    /**
     * Releases idle drawing contexts and rejects new render requests.
     * In-flight renders may finish; their contexts are released on return.
     * Pool gauges registered by this service are removed; shared render counters
     * and timers remain available. Repeated calls have no effect.
     */
    @Override
    public void close() {
        pool.close();
        removeOwnedGauge(ownedActiveGauge);
        removeOwnedGauge(ownedIdleGauge);
    }

    private Gauge registerPoolGauge(String name, ToDoubleFunction<RenderSessionPool> value) {
        synchronized (registry) {
            var existing = registry.getMeters();
            Gauge gauge = Gauge.builder(name, pool, value).register(registry);
            // Registration may return an existing meter, including one mapped by a filter.
            return existing.stream().anyMatch(meter -> meter == gauge) ? null : gauge;
        }
    }

    private void removeOwnedGauge(Gauge gauge) {
        if (gauge == null) {
            return;
        }
        synchronized (registry) {
            // Micrometer removes by ID; an externally replaced meter must remain registered.
            if (registry.getMeters().stream().anyMatch(meter -> meter == gauge)) {
                registry.remove(gauge);
            }
        }
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
        applyDefaultSpatialStyle(session.canvas, context);

        SpatialServerConsumer consumer = new SpatialServerConsumer(session.canvas, builder, session, context);
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
                outCount = flushSpatial(session.canvas, outX, outY, outCount, currentStyle);
                continue;
            }
            if (builder.isMoveTo(i)) {
                outCount = flushSpatial(session.canvas, outX, outY, outCount, currentStyle);
            }
            long style = batch.getStyleKey(i);
            if (style != currentStyle) {
                boolean continuesPath = outCount > 0;
                float previousX = continuesPath ? outX[outCount - 1] : 0f;
                float previousY = continuesPath ? outY[outCount - 1] : 0f;
                outCount = flushSpatial(session.canvas, outX, outY, outCount, currentStyle);
                if (style == 0L) {
                    // No style emitted by the renderer: keep the theme default
                    // instead of unpacking key 0 into a fully transparent color.
                    applyDefaultSpatialStyle(session.canvas, context);
                } else {
                    applySpatialStyle(session.canvas, style);
                }
                currentStyle = style;
                if (continuesPath) {
                    // A point's style applies to its incoming segment, matching
                    // the Swing consumer. A style change does not start a new path.
                    outX[outCount] = previousX;
                    outY[outCount] = previousY;
                    outCount++;
                }
            }
            outX[outCount] = (float) xs[i];
            outY[outCount] = (float) ys[i];
            outCount++;
            drawSpatialMarker(session, style, (float) xs[i], (float) ys[i]);
        }
        flushSpatial(session.canvas, outX, outY, outCount, currentStyle);
    }

    private static boolean isValidRange(double[] range) {
        return range != null && range.length >= 2 && Double.isFinite(range[0])
                && Double.isFinite(range[1]) && range[0] < range[1];
    }

    private static void applyDefaultSpatialStyle(ImageBufferCanvas canvas, DefaultPlotContext context) {
        ChartTheme theme = (context != null) ? context.getTheme() : null;
        ArberColor color = (theme != null && theme.getAccentColor() != null)
                ? theme.getAccentColor()
                : (theme != null ? theme.getSeriesColor(0) : null);
        if (color == null) {
            color = new ArberColor(0xFF6AA9FF);
        }
        canvas.setColor(color);
        canvas.setStroke(1.5f);
    }

    private static void drawSpatialMarker(RenderSession session, long style, float x, float y) {
        ImageBufferCanvas canvas = session.canvas;
        switch (SpatialStyleDescriptor.unpackMarkerId(style)) {
            case 1 -> {
                canvas.drawLine(x - 2, y, x + 2, y);
                canvas.drawLine(x, y - 2, x, y + 2);
            }
            case 2 -> canvas.fillRect(x - 1, y - 1, 3, 3);
            case SpatialStyleDescriptor.MARKER_OCTAGON -> {
                float radius = SpatialStyleDescriptor.unpackStrokeWidth(style) * 0.5f;
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI * 0.25;
                    session.markerX[i] = (float) (x + Math.cos(angle) * radius);
                    session.markerY[i] = (float) (y + Math.sin(angle) * radius);
                }
                canvas.fillPolygon(session.markerX, session.markerY, 8);
            }
            case SpatialStyleDescriptor.MARKER_SQUARE -> {
                float size = SpatialStyleDescriptor.unpackStrokeWidth(style);
                canvas.fillRect(x - size * 0.5f, y - size * 0.5f, size, size);
            }
            default -> {
            }
        }
    }

    private static void applySpatialStyle(ImageBufferCanvas canvas, long styleKey) {
        int argb = SpatialStyleDescriptor.unpackArgb(styleKey);
        float stroke = SpatialStyleDescriptor.unpackStrokeWidth(styleKey);
        canvas.setColor(new ArberColor(argb));
        canvas.setSpatialStroke(stroke, SpatialStyleDescriptor.unpackDashId(styleKey));
    }

    private static int flushSpatial(ImageBufferCanvas canvas, float[] xs, float[] ys, int count, long styleKey) {
        if (count > 1) {
            int dashId = SpatialStyleDescriptor.unpackDashId(styleKey);
            if (dashId >= 1 && dashId <= 3) {
                // Spatial segments restart their dash phase at each point,
                // matching the existing Swing consumer's drawLine semantics.
                for (int i = 1; i < count; i++) {
                    canvas.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
                }
            } else {
                canvas.drawPolyline(xs, ys, count);
            }
        }
        return 0;
    }

    private static BufferedImage toImage(ImageBufferCanvas canvas) {
        int width = canvas.width();
        int height = canvas.height();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] output = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // Results outlive the pooled session, including PNG encoding after release.
        System.arraycopy(canvas.pixels(), 0, output, 0, output.length);
        return image;
    }

    private static final class RenderSession {
        private ImageBufferCanvas canvas;
        private float[] spatialX = new float[0];
        private float[] spatialY = new float[0];
        private final float[] quadX = new float[4];
        private final float[] quadY = new float[4];
        private final float[] markerX = new float[8];
        private final float[] markerY = new float[8];

        void ensureCanvas(int width, int height) {
            if (canvas == null || canvas.width() != width || canvas.height() != height) {
                ImageBufferCanvas replacement = new ImageBufferCanvas(width, height);
                close();
                canvas = replacement;
            }
        }

        void close() {
            if (canvas != null) {
                canvas.close();
                canvas = null;
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
        private final DefaultPlotContext context;

        private SpatialServerConsumer(ImageBufferCanvas canvas, SpatialPathBatchBuilder builder,
                                      RenderSession session, DefaultPlotContext context) {
            this.canvas = canvas;
            this.builder = builder;
            this.session = session;
            this.context = context;
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
            if (styleKey == 0L) {
                applyDefaultSpatialStyle(canvas, context);
            } else {
                applySpatialStyle(canvas, styleKey);
            }
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
        private final ArrayDeque<RenderSession> queue = new ArrayDeque<>();
        private final int capacity;
        private int active;
        private long contention;
        private boolean closed;

        RenderSessionPool(int size) {
            this.capacity = size;
        }

        synchronized RenderSession acquire() {
            if (closed) {
                throw new IllegalStateException("Render service is closed");
            }
            RenderSession session = queue.pollFirst();
            if (session == null) {
                contention++;
                session = new RenderSession();
            }
            active++;
            return session;
        }

        synchronized void release(RenderSession session) {
            if (session == null) return;
            active--;
            if (closed || queue.size() >= capacity) {
                session.close();
            } else {
                queue.addLast(session);
            }
        }

        synchronized void close() {
            if (closed) return;
            closed = true;
            RenderSession session;
            while ((session = queue.pollFirst()) != null) {
                session.close();
            }
        }

        synchronized int activeCount() {
            return active;
        }

        synchronized int idleCount() {
            return queue.size();
        }

        synchronized long contentionCount() {
            return contention;
        }
    }
}
