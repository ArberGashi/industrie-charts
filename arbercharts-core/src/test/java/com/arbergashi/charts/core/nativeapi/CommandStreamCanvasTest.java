package com.arbergashi.charts.core.nativeapi;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.testing.LargeScaleDataGenerator;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandStreamCanvasTest {
    @Test
    void commandStreamProducesHeaderAndPayload() {
        DefaultFinancialChartModel model = LargeScaleDataGenerator.generateFinancialCandles("Test", 10_000);
        CandlestickRenderer renderer = new CandlestickRenderer();
        ArberRect bounds = new ArberRect(0, 0, 800, 600);
        DefaultPlotContext context = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN, ChartThemes.getDarkTheme());

        byte[] buffer = new byte[2 * 1024 * 1024];
        ByteArrayWriter writer = new ByteArrayWriter(buffer);

        writer.putInt(CommandStreamCanvas.VERSION);
        writer.putInt(0);

        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        renderer.render(canvas, model, context);

        int byteCount = writer.position();
        writer.putIntAt(4, byteCount);

        assertTrue(byteCount > 8, "command stream must include payload beyond header");
        assertEquals(CommandStreamCanvas.VERSION, readU32(buffer, 0));
        assertEquals(byteCount, readU32(buffer, 4));

        int firstOpcode = buffer[8] & 0xFF;
        assertTrue(firstOpcode >= CommandStreamCanvas.OP_SET_COLOR && firstOpcode <= CommandStreamCanvas.OP_DRAW_TEXT);
    }

    private static int readU32(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF)
                | ((buffer[offset + 1] & 0xFF) << 8)
                | ((buffer[offset + 2] & 0xFF) << 16)
                | ((buffer[offset + 3] & 0xFF) << 24);
    }

    private static final class ByteArrayWriter implements CommandStreamWriter {
        private final byte[] buffer;
        private int pos;

        private ByteArrayWriter(byte[] buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean putByte(int value) {
            if (pos + 1 > buffer.length) return false;
            buffer[pos++] = (byte) value;
            return true;
        }

        @Override
        public void putInt(int value) {
            if (pos + 4 > buffer.length) return;
            buffer[pos++] = (byte) (value & 0xFF);
            buffer[pos++] = (byte) ((value >>> 8) & 0xFF);
            buffer[pos++] = (byte) ((value >>> 16) & 0xFF);
            buffer[pos++] = (byte) ((value >>> 24) & 0xFF);
        }

        @Override
        public void putFloat(float value) {
            putInt(Float.floatToIntBits(value));
        }

        @Override
        public void putShort(short value) {
            if (pos + 2 > buffer.length) return;
            buffer[pos++] = (byte) (value & 0xFF);
            buffer[pos++] = (byte) ((value >>> 8) & 0xFF);
        }

        @Override
        public void putBytes(byte[] data, int length) {
            if (data == null || length <= 0) return;
            int n = Math.min(length, data.length);
            if (pos + n > buffer.length) return;
            System.arraycopy(data, 0, buffer, pos, n);
            pos += n;
        }

        @Override
        public void putIntAt(int offset, int value) {
            if (offset + 4 > buffer.length) return;
            buffer[offset] = (byte) (value & 0xFF);
            buffer[offset + 1] = (byte) ((value >>> 8) & 0xFF);
            buffer[offset + 2] = (byte) ((value >>> 16) & 0xFF);
            buffer[offset + 3] = (byte) ((value >>> 24) & 0xFF);
        }

        @Override
        public int position() {
            return pos;
        }
    }
}
