package com.arbergashi.charts.core.nativeapi;

import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Native export surface for GraalVM shared library builds.
 *
 * <p>Handle-based, primitive-only API for native callers (Qt/Swift).
 * Rendering requires a canvas handle registered by the host bridge.</p>
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ArberNativeExport {
    public static final int TYPE_LINE = 0;
    public static final int TYPE_FINANCIAL = 1;

    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final Map<Long, ChartSession> CHARTS = new ConcurrentHashMap<>();
    private static final Map<Long, ArberCanvas> CANVASES = new ConcurrentHashMap<>();

    private ArberNativeExport() {
    }

    @CEntryPoint(name = "arber_create_chart")
    public static long createChart(IsolateThread thread, int type) {
        ChartSession session = switch (type) {
            case TYPE_FINANCIAL -> ChartSession.financial();
            case TYPE_LINE -> ChartSession.line();
            default -> ChartSession.line();
        };
        long id = NEXT_ID.getAndIncrement();
        CHARTS.put(id, session);
        return id;
    }

    @CEntryPoint(name = "arber_destroy_chart")
    public static int destroyChart(IsolateThread thread, long handle) {
        return CHARTS.remove(handle) != null ? 0 : -1;
    }

    @CEntryPoint(name = "arber_set_viewport")
    public static int setViewport(IsolateThread thread, long handle, double x, double y, double w, double h) {
        ChartSession session = CHARTS.get(handle);
        if (session == null) return -1;
        session.bounds = new ArberRect(x, y, w, h);
        return 0;
    }

    @CEntryPoint(name = "arber_update_data")
    public static int updateData(IsolateThread thread, long handle, CDoublePointer data, int length) {
        ChartSession session = CHARTS.get(handle);
        if (session == null) return -1;
        if (data.isNull() || length <= 0) return -2;

        if (session.model instanceof DefaultFinancialChartModel fin) {
            int step = 5;
            for (int i = 0; i + 4 < length; i += step) {
                double t = data.read(i);
                double open = data.read(i + 1);
                double high = data.read(i + 2);
                double low = data.read(i + 3);
                double close = data.read(i + 4);
                fin.setOHLC(t, open, high, low, close);
            }
            return 0;
        }

        if (session.model instanceof DefaultChartModel xy) {
            for (int i = 0; i + 1 < length; i += 2) {
                double x = data.read(i);
                double y = data.read(i + 1);
                xy.setXY(x, y);
            }
            return 0;
        }

        return -3;
    }

    @CEntryPoint(name = "arber_render")
    public static int render(IsolateThread thread, long chartHandle, long canvasHandle) {
        ChartSession session = CHARTS.get(chartHandle);
        if (session == null) return -1;
        ArberCanvas canvas = CANVASES.get(canvasHandle);
        if (canvas == null) return -2;
        try {
            ArberRect bounds = session.bounds != null ? session.bounds : new ArberRect(0, 0, 0, 0);
            DefaultPlotContext context = new DefaultPlotContext(
                    bounds,
                    session.model,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    session.theme,
                    session.hints
            );
            session.renderer.render(canvas, session.model, context);
            return 0;
        } catch (Exception e) {
            return -3;
        }
    }

    @CEntryPoint(name = "arber_render_to_buffer")
    public static int renderToBuffer(IsolateThread thread, long chartHandle, CCharPointer buffer, int capacity) {
        ChartSession session = CHARTS.get(chartHandle);
        if (session == null) return -1;
        if (buffer.isNull() || capacity <= 8) return -2;

        PointerWriter writer = new PointerWriter(buffer, capacity);
        // header: u32 version, u32 byteCount
        writer.putInt(CommandStreamCanvas.VERSION);
        writer.putInt(0);

        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        try {
            ArberRect bounds = session.bounds != null ? session.bounds : new ArberRect(0, 0, 0, 0);
            DefaultPlotContext context = new DefaultPlotContext(
                    bounds,
                    session.model,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    session.theme,
                    session.hints
            );
            session.renderer.render(canvas, session.model, context);
        } catch (Exception e) {
            return -3;
        }

        int byteCount = writer.position();
        writer.putIntAt(4, byteCount);
        return byteCount;
    }

    /**
     * Registers a canvas handle for native rendering.
     * This is intended for bridge-side Java code, not C callers.
     */
    static void registerCanvas(long handle, ArberCanvas canvas) {
        if (canvas == null) {
            CANVASES.remove(handle);
        } else {
            CANVASES.put(handle, canvas);
        }
    }

    private static final class ChartSession {
        final ChartModel model;
        final ChartRenderer renderer;
        final ChartTheme theme;
        final ChartRenderHints hints;
        ArberRect bounds;

        private ChartSession(ChartModel model, ChartRenderer renderer, ChartTheme theme, ChartRenderHints hints) {
            this.model = model;
            this.renderer = renderer;
            this.theme = theme;
            this.hints = hints;
        }

        static ChartSession line() {
            return new ChartSession(new DefaultChartModel("Series"), new LineRenderer(), ChartThemes.getDarkTheme(), null);
        }

        static ChartSession financial() {
            return new ChartSession(new DefaultFinancialChartModel("Financial"), new CandlestickRenderer(), ChartThemes.getDarkTheme(), null);
        }
    }

    private static final class PointerWriter implements CommandStreamWriter {
        private final CCharPointer buffer;
        private final int capacity;
        private int pos;

        PointerWriter(CCharPointer buffer, int capacity) {
            this.buffer = buffer;
            this.capacity = capacity;
        }

        @Override
        public boolean putByte(int value) {
            if (pos + 1 > capacity) return false;
            buffer.write(pos, (byte) value);
            pos += 1;
            return true;
        }

        @Override
        public void putInt(int value) {
            if (pos + 4 > capacity) return;
            buffer.write(pos, (byte) (value & 0xFF));
            buffer.write(pos + 1, (byte) ((value >>> 8) & 0xFF));
            buffer.write(pos + 2, (byte) ((value >>> 16) & 0xFF));
            buffer.write(pos + 3, (byte) ((value >>> 24) & 0xFF));
            pos += 4;
        }

        @Override
        public void putFloat(float value) {
            putInt(Float.floatToIntBits(value));
        }

        @Override
        public void putShort(short value) {
            if (pos + 2 > capacity) return;
            buffer.write(pos, (byte) (value & 0xFF));
            buffer.write(pos + 1, (byte) ((value >>> 8) & 0xFF));
            pos += 2;
        }

        @Override
        public void putBytes(byte[] data, int length) {
            if (data == null || length <= 0) return;
            int n = Math.min(length, data.length);
            if (pos + n > capacity) return;
            for (int i = 0; i < n; i++) {
                buffer.write(pos + i, data[i]);
            }
            pos += n;
        }

        @Override
        public void putIntAt(int offset, int value) {
            if (offset + 4 > capacity) return;
            buffer.write(offset, (byte) (value & 0xFF));
            buffer.write(offset + 1, (byte) ((value >>> 8) & 0xFF));
            buffer.write(offset + 2, (byte) ((value >>> 16) & 0xFF));
            buffer.write(offset + 3, (byte) ((value >>> 24) & 0xFF));
        }

        @Override
        public int position() {
            return pos;
        }
    }
}
