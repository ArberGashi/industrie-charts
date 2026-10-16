package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.util.ArrayList;
import java.util.List;

final class FinancialRendererTestSupport {
    private FinancialRendererTestSupport() {
    }

    static RecordingCanvas render(BaseRenderer renderer, ChartModel model, PlotContext context) {
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context);
        return canvas;
    }

    record Context(double minX, double maxX, double minY, double maxY,
                   ArberRect plotBounds, boolean invertedY) implements PlotContext {
        @Override public double getMinX() { return minX; }
        @Override public double getMaxX() { return maxX; }
        @Override public double getMinY() { return minY; }
        @Override public double getMaxY() { return maxY; }
        @Override public ArberRect getPlotBounds() { return plotBounds; }
        @Override public boolean isInvertedY() { return invertedY; }

        @Override
        public void mapToPixel(double x, double y, double[] out) {
            out[0] = plotBounds.x() + (x - minX) / (maxX - minX) * plotBounds.width();
            double fraction = (y - minY) / (maxY - minY);
            out[1] = plotBounds.y() + (invertedY ? fraction : 1.0 - fraction) * plotBounds.height();
        }

        @Override
        public void mapToData(double x, double y, double[] out) {
            out[0] = minX + (x - plotBounds.x()) / plotBounds.width() * (maxX - minX);
            double fraction = (y - plotBounds.y()) / plotBounds.height();
            out[1] = minY + (invertedY ? fraction : 1.0 - fraction) * (maxY - minY);
        }
    }

    record Draw(String type, int color, float stroke, List<Float> coordinates, String text) {
        float coordinate(int index) {
            return coordinates.get(index);
        }
    }

    static final class RecordingCanvas implements ArberCanvas {
        final List<Draw> draws = new ArrayList<>();
        private int color;
        private float stroke = 1;
        private float currentX;
        private float currentY;
        private ArberRect clip;

        List<Draw> ofType(String type) {
            return draws.stream().filter(draw -> draw.type().equals(type)).toList();
        }

        private void add(String type, float... coordinates) {
            List<Float> values = new ArrayList<>(coordinates.length);
            for (float value : coordinates) values.add(value);
            draws.add(new Draw(type, color, stroke, List.copyOf(values), ""));
        }

        private void addPoints(String type, float[] xs, float[] ys, int count) {
            float[] coordinates = new float[count * 2];
            for (int i = 0; i < count; i++) {
                coordinates[i * 2] = xs[i];
                coordinates[i * 2 + 1] = ys[i];
            }
            add(type, coordinates);
        }

        @Override public void setColor(ArberColor value) { color = value.argb(); }
        @Override public void setStroke(float value) { stroke = value; }
        @Override public void moveTo(float x, float y) { currentX = x; currentY = y; }
        @Override public void lineTo(float x, float y) {
            add("line", currentX, currentY, x, y);
            moveTo(x, y);
        }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) {
            addPoints("polyline", xs, ys, count);
        }
        @Override public void drawRect(float x, float y, float width, float height) {
            add("outline", x, y, width, height);
        }
        @Override public void fillRect(float x, float y, float width, float height) {
            add("fill", x, y, width, height);
        }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) {
            addPoints("polygon", xs, ys, count);
        }
        @Override public void drawVoxelField(VoxelBuffer buffer) {
            for (int i = 0; i < buffer.count(); i++) {
                int voxelColor = buffer.argb() == null ? color : buffer.argb()[i];
                draws.add(new Draw("voxel", voxelColor, stroke,
                        List.of(buffer.x()[i], buffer.y()[i]), ""));
            }
        }
        @Override public void drawText(float x, float y, String text) {
            draws.add(new Draw("text", color, stroke, List.of(x, y), text));
        }
        @Override public ArberMatrix getTransform() { return ArberMatrices.identity(); }
        @Override public void setClip(ArberRect value) { clip = value; }
        @Override public ArberRect getClip() { return clip; }
    }
}
