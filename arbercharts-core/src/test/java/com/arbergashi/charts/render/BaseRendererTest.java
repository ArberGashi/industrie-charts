package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.geometry.TextAnchor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BaseRendererTest {

    @Test
    void multiColorToggleIsHonored() {
        TestRenderer renderer = new TestRenderer();
        assertFalse(renderer.exposeMultiColor());
        renderer.setMultiColor(true);
        assertTrue(renderer.exposeMultiColor());
    }

    @Test
    void anchorPointResolvesCorrectly() {
        TestRenderer renderer = new TestRenderer();
        ArberRect bounds = new ArberRect(0, 0, 100, 50);
        ArberPoint out = new ArberPoint();

        renderer.exposeAnchor(bounds, TextAnchor.TOP_LEFT, out);
        assertEquals(0.0, out.x(), 0.0001);
        assertEquals(0.0, out.y(), 0.0001);

        renderer.exposeAnchor(bounds, TextAnchor.BOTTOM_RIGHT, out);
        assertEquals(100.0, out.x(), 0.0001);
        assertEquals(50.0, out.y(), 0.0001);

        renderer.exposeAnchor(bounds, TextAnchor.CENTER, out);
        assertEquals(50.0, out.x(), 0.0001);
        assertEquals(25.0, out.y(), 0.0001);
    }

    @Test
    void resolvedThemeThrowsWhenMissing() {
        TestRenderer renderer = new TestRenderer();
        PlotContext context = new PlotContext() {
            @Override public double getMinX() { return 0; }
            @Override public double getMaxX() { return 1; }
            @Override public double getMinY() { return 0; }
            @Override public double getMaxY() { return 1; }
            @Override public ArberRect getPlotBounds() { return new ArberRect(0, 0, 10, 10); }
            @Override public void mapToPixel(double x, double y, double[] out) {}
            @Override public void mapToData(double pixelX, double pixelY, double[] dest) {}
            @Override public ChartTheme getTheme() { return null; }
        };
        assertThrows(NullPointerException.class, () -> renderer.exposeResolvedTheme(context));
    }

    private static final class TestRenderer extends BaseRenderer {
        TestRenderer() {
            super("test");
        }

        @Override
        protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        }

        @Override
        public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return "test";
        }

        boolean exposeMultiColor() {
            return isMultiColor();
        }

        ArberPoint exposeAnchor(ArberRect bounds, TextAnchor anchor, ArberPoint out) {
            return anchorPoint(bounds, anchor, out);
        }

        ChartTheme exposeResolvedTheme(PlotContext context) {
            return getResolvedTheme(context);
        }
    }
}
