package com.arbergashi.charts.core.nativeapi;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;

final class CommandStreamCanvas implements ArberCanvas {
    static final int VERSION = 1;

    // Opcodes
    static final int OP_SET_COLOR = 0x01;
    static final int OP_SET_STROKE = 0x02;
    static final int OP_MOVE_TO = 0x03;
    static final int OP_LINE_TO = 0x04;
    static final int OP_POLYLINE = 0x05;
    static final int OP_DRAW_RECT = 0x06;
    static final int OP_FILL_RECT = 0x07;
    static final int OP_FILL_POLYGON = 0x08;
    static final int OP_SET_CLIP = 0x09;
    static final int OP_CLEAR_CLIP = 0x0A;
    static final int OP_DRAW_TEXT = 0x0B;

    private final CommandStreamWriter writer;

    CommandStreamCanvas(CommandStreamWriter writer) {
        this.writer = writer;
    }

    @Override
    public void setColor(ArberColor color) {
        if (color == null) return;
        if (!writer.putByte(OP_SET_COLOR)) return;
        writer.putInt(color.argb());
    }

    @Override
    public void setStroke(float width) {
        if (!writer.putByte(OP_SET_STROKE)) return;
        writer.putFloat(width);
    }

    @Override
    public void moveTo(float x, float y) {
        if (!writer.putByte(OP_MOVE_TO)) return;
        writer.putFloat(x);
        writer.putFloat(y);
    }

    @Override
    public void lineTo(float x, float y) {
        if (!writer.putByte(OP_LINE_TO)) return;
        writer.putFloat(x);
        writer.putFloat(y);
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 1) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 1) return;
        if (!writer.putByte(OP_POLYLINE)) return;
        writer.putInt(n);
        for (int i = 0; i < n; i++) {
            writer.putFloat(xs[i]);
            writer.putFloat(ys[i]);
        }
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        if (!writer.putByte(OP_DRAW_RECT)) return;
        writer.putFloat(x);
        writer.putFloat(y);
        writer.putFloat(w);
        writer.putFloat(h);
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        if (!writer.putByte(OP_FILL_RECT)) return;
        writer.putFloat(x);
        writer.putFloat(y);
        writer.putFloat(w);
        writer.putFloat(h);
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null || count <= 2) return;
        int n = Math.min(count, Math.min(xs.length, ys.length));
        if (n <= 2) return;
        if (!writer.putByte(OP_FILL_POLYGON)) return;
        writer.putInt(n);
        for (int i = 0; i < n; i++) {
            writer.putFloat(xs[i]);
            writer.putFloat(ys[i]);
        }
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        // Not encoded in v1 command stream (caller may pre-expand into primitives).
    }

    @Override
    public ArberMatrix getTransform() {
        return ArberMatrices.identity();
    }

    @Override
    public void setClip(ArberRect clip) {
        if (clip == null) {
            if (!writer.putByte(OP_CLEAR_CLIP)) return;
            return;
        }
        if (!writer.putByte(OP_SET_CLIP)) return;
        writer.putFloat((float) clip.x());
        writer.putFloat((float) clip.y());
        writer.putFloat((float) clip.width());
        writer.putFloat((float) clip.height());
    }

    @Override
    public ArberRect getClip() {
        return null;
    }

    @Override
    public void drawText(float x, float y, String text) {
        if (text == null || text.isEmpty()) return;
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!writer.putByte(OP_DRAW_TEXT)) return;
        writer.putFloat(x);
        writer.putFloat(y);
        writer.putShort((short) Math.min(0xFFFF, bytes.length));
        writer.putBytes(bytes, Math.min(0xFFFF, bytes.length));
    }
}
