package com.arbergashi.charts.core.nativeapi;

import com.arbergashi.charts.api.types.ArberFont;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NativeTextContractTest {
    @Test
    void fontSelectionAndMetricsDoNotChangeTheVersionOneTextProtocol() {
        BufferWriter writer = new BufferWriter();
        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        canvas.setFont(new ArberFont("SansSerif", ArberFont.BOLD, 18));
        assertEquals(0, writer.position(), "Version one has no font-selection opcode");
        assertEquals(21, canvas.measureTextWidth("A😀B"), "Fallback counts Unicode code points");
        assertEquals(14, canvas.getTextLineHeight());
        assertEquals(11, canvas.getTextAscent());

        String text = "Latency μs";
        byte[] expected = text.getBytes(StandardCharsets.UTF_8);
        canvas.drawText(12.5f, 24.75f, text);
        ByteBuffer commands = writer.buffer.flip();
        assertEquals(0x0B, commands.get() & 0xFF);
        assertEquals(12.5f, commands.getFloat());
        assertEquals(24.75f, commands.getFloat());
        assertEquals(expected.length, Short.toUnsignedInt(commands.getShort()));
        byte[] actual = new byte[commands.remaining()];
        commands.get(actual);
        assertArrayEquals(expected, actual);
        assertEquals(1, CommandStreamCanvas.VERSION);
    }

    @Test
    void absentAndNonFiniteTextDoesNotCreateCommands() {
        BufferWriter writer = new BufferWriter();
        CommandStreamCanvas canvas = new CommandStreamCanvas(writer);
        canvas.drawText(1, 2, null);
        canvas.drawText(1, 2, "");
        canvas.drawText(Float.NaN, 2, "Ignored");
        canvas.drawText(1, Float.NEGATIVE_INFINITY, "Ignored");
        assertEquals(0, writer.position());
        assertEquals(0, canvas.measureTextWidth(null));
        assertEquals(0, canvas.measureTextWidth(""));
        assertThrows(IllegalArgumentException.class,
                () -> canvas.setFont(new ArberFont("SansSerif", ArberFont.PLAIN, Float.NaN)));
    }

    private static final class BufferWriter implements CommandStreamWriter {
        private final ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        @Override public boolean putByte(int value) { buffer.put((byte) value); return true; }
        @Override public void putInt(int value) { buffer.putInt(value); }
        @Override public void putFloat(float value) { buffer.putFloat(value); }
        @Override public void putShort(short value) { buffer.putShort(value); }
        @Override public void putBytes(byte[] data, int length) { buffer.put(data, 0, length); }
        @Override public void putIntAt(int offset, int value) { buffer.putInt(offset, value); }
        @Override public int position() { return buffer.position(); }
    }
}
