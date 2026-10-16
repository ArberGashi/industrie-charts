package com.arbergashi.charts.core.nativeapi;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.core.testing.LargeScaleDataGenerator;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandStreamCanvasTest {
    @Test
    void clipCanBeSavedRestoredAndCleared() {
        ByteArrayWriter writer = new ByteArrayWriter(new byte[256]);
        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        ArberRect outer = new ArberRect(2, 3, 100, 80);
        ArberRect inner = new ArberRect(10, 15, 20, 25);

        assertNull(canvas.getClip());
        canvas.setClip(outer);
        ArberRect saved = canvas.getClip();
        canvas.setClip(inner);
        assertEquals(inner, canvas.getClip());
        canvas.setClip(saved);
        assertEquals(outer, canvas.getClip());
        canvas.setClip(null);
        assertNull(canvas.getClip());

        ByteBuffer commands = ByteBuffer.wrap(writer.buffer, 0, writer.position()).order(ByteOrder.LITTLE_ENDIAN);
        for (ArberRect expected : new ArberRect[]{outer, inner, outer}) {
            assertEquals(CommandStreamCanvas.OP_SET_CLIP, commands.get() & 0xFF);
            assertEquals((float) expected.x(), commands.getFloat());
            assertEquals((float) expected.y(), commands.getFloat());
            assertEquals((float) expected.width(), commands.getFloat());
            assertEquals((float) expected.height(), commands.getFloat());
        }
        assertEquals(CommandStreamCanvas.OP_CLEAR_CLIP, commands.get() & 0xFF);
        assertEquals(0, commands.remaining());
    }

    @Test
    void voxelFieldsExpandToPortableFillCommands() {
        ByteArrayWriter writer = new ByteArrayWriter(new byte[256]);
        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        canvas.setStroke(3f);
        canvas.setColor(new ArberColor(0xFFFF0000));
        canvas.drawVoxelField(new VoxelBuffer(new float[]{2, 6}, new float[]{4, 8}, null,
            new int[]{0xFF00FF00}, 3));

        ByteBuffer commands = ByteBuffer.wrap(writer.buffer, 0, writer.position()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(CommandStreamCanvas.OP_SET_STROKE, commands.get() & 0xFF);
        assertEquals(3f, commands.getFloat());
        assertEquals(CommandStreamCanvas.OP_SET_COLOR, commands.get() & 0xFF);
        assertEquals(0xFFFF0000, commands.getInt());
        assertEquals(CommandStreamCanvas.OP_SET_COLOR, commands.get() & 0xFF);
        assertEquals(0xFF00FF00, commands.getInt());
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                assertEquals(CommandStreamCanvas.OP_SET_COLOR, commands.get() & 0xFF);
                assertEquals(0xFFFF0000, commands.getInt());
            }
            assertEquals(CommandStreamCanvas.OP_FILL_RECT, commands.get() & 0xFF);
            assertEquals(2f + 4f * i, commands.getFloat());
            assertEquals(4f + 4f * i, commands.getFloat());
            assertEquals(3f, commands.getFloat());
            assertEquals(3f, commands.getFloat());
        }
        assertEquals(0, commands.remaining());
    }

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
