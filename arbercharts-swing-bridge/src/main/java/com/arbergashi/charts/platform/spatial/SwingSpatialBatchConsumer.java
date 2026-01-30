package com.arbergashi.charts.platform.swing.spatial;

import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialFillConsumer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatch;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.platform.render.ColorCache;
import com.arbergashi.charts.platform.render.StrokeCache;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Streaming consumer that renders spatial path batches directly to Graphics2D.
 *
 * @since 1.7.0
 */
public final class SwingSpatialBatchConsumer implements SpatialChunkConsumer, SpatialFillConsumer {
    private SpatialPathBatchBuilder builder;
    private Graphics2D graphics;
    private Color color;
    private Stroke stroke;
    private final int[] quadX = new int[4];
    private final int[] quadY = new int[4];

    public SwingSpatialBatchConsumer(SpatialPathBatchBuilder builder) {
        this.builder = builder;
    }

    public SpatialPathBatchBuilder getBuilder() {
        return builder;
    }

    public SwingSpatialBatchConsumer setBuilder(SpatialPathBatchBuilder builder) {
        if (builder != null) {
            this.builder = builder;
        }
        return this;
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    public SwingSpatialBatchConsumer setGraphics(Graphics2D graphics) {
        this.graphics = graphics;
        return this;
    }

    public Color getColor() {
        return color;
    }

    public SwingSpatialBatchConsumer setColor(Color color) {
        this.color = color;
        return this;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public SwingSpatialBatchConsumer setStroke(Stroke stroke) {
        this.stroke = stroke;
        return this;
    }

    public SwingSpatialBatchConsumer reset() {
        if (builder.isAutoReset()) {
            builder.reset();
        }
        return this;
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
        builder.accept(buffer, count);
    }

    public void flush() {
        if (graphics == null) {
            builder.reset();
            return;
        }
        if (color != null) {
            graphics.setColor(color);
        }
        if (stroke != null) {
            graphics.setStroke(stroke);
        }
        drawBatch(builder.getBatch());
        if (builder.isAutoReset()) {
            builder.reset();
        }
    }

    @Override
    public void fillQuad(double x1, double y1,
                         double x2, double y2,
                         double x3, double y3,
                         double x4, double y4) {
        if (graphics == null) {
            return;
        }
        quadX[0] = (int) x1;
        quadY[0] = (int) y1;
        quadX[1] = (int) x2;
        quadY[1] = (int) y2;
        quadX[2] = (int) x3;
        quadY[2] = (int) y3;
        quadX[3] = (int) x4;
        quadY[3] = (int) y4;
        graphics.fillPolygon(quadX, quadY, 4);
    }

    private void drawBatch(SpatialPathBatch batch) {
        int count = batch.getPointCount();
        if (count <= 1) {
            return;
        }
        double[] xs = batch.getXData();
        double[] ys = batch.getYData();

        boolean hasLast = false;
        int lastX = 0;
        int lastY = 0;
        long lastStyleKey = Long.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            long styleKey = batch.getStyleKey(i);
            int markerId = SpatialStyleDescriptor.unpackMarkerId(styleKey);
            if (styleKey != lastStyleKey) {
                applyStyle(styleKey);
                lastStyleKey = styleKey;
            }
            int x = (int) xs[i];
            int y = (int) ys[i];
            if (builder.isMoveTo(i) || !hasLast) {
                lastX = x;
                lastY = y;
                hasLast = true;
                if (markerId != 0) {
                    drawMarker(markerId, x, y);
                }
                continue;
            }
            graphics.drawLine(lastX, lastY, x, y);
            if (markerId != 0) {
                drawMarker(markerId, x, y);
            }
            lastX = x;
            lastY = y;
        }
    }

    private void applyStyle(long styleKey) {
        int argb = SpatialStyleDescriptor.unpackArgb(styleKey);
        float strokeWidth = SpatialStyleDescriptor.unpackStrokeWidth(styleKey);
        int dashId = SpatialStyleDescriptor.unpackDashId(styleKey);
        int markerId = SpatialStyleDescriptor.unpackMarkerId(styleKey);
        if (graphics == null) {
            return;
        }
        // markerId reserved for future symbol rendering (v1.8.0+)
        graphics.setColor(ColorCache.get(argb));
        graphics.setStroke(DashStrokeCache.get(dashId, strokeWidth));
    }

    private void drawMarker(int markerId, int x, int y) {
        if (graphics == null) {
            return;
        }
        switch (markerId) {
            case 1 -> {
                graphics.drawLine(x - 2, y, x + 2, y);
                graphics.drawLine(x, y - 2, x, y + 2);
            }
            case 2 -> graphics.fillRect(x - 1, y - 1, 3, 3);
            default -> {
            }
        }
    }

    private static final class DashStrokeCache {
        private static final int INITIAL_CAPACITY = 64;
        private static final long[] keys = new long[INITIAL_CAPACITY];
        private static final Stroke[] values = new Stroke[INITIAL_CAPACITY];
        private static int size = 0;

        private static Stroke get(int dashId, float width) {
            if (dashId == 0) {
                return StrokeCache.get(width);
            }
            long key = (((long) dashId) << 32) | (Float.floatToIntBits(width) & 0xFFFFFFFFL);
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    return values[i];
                }
            }
            Stroke stroke = createStroke(dashId, width);
            if (size < keys.length) {
                keys[size] = key;
                values[size] = stroke;
                size++;
            }
            return stroke;
        }

        private static Stroke createStroke(int dashId, float width) {
            float[] pattern = switch (dashId) {
                case 1 -> new float[]{5f, 5f};
                case 2 -> new float[]{2f, 4f};
                case 3 -> new float[]{8f, 4f, 2f, 4f};
                default -> null;
            };
            if (pattern == null) {
                return StrokeCache.get(width);
            }
            return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, pattern, 0f);
        }
    }
}
