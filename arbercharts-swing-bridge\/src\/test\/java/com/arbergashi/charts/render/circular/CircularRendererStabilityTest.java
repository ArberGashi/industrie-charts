package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.DefaultChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CircularRendererStabilityTest {

    @Test
    public void testPolarAdvancedRendererStability() {
        PolarAdvancedRenderer renderer = new PolarAdvancedRenderer();
        renderer.setTheme(ChartThemes.getDarkTheme());

        DefaultChartModel model = new DefaultChartModel("Test");
        // Trigger potential r1=0 scenario
        model.setPoint(0, 50, 0, "Cat 1");
        
        render(renderer, model);
    }

    @Test
    public void testRadialStackedRendererStability() {
        RadialStackedRenderer renderer = new RadialStackedRenderer();
        renderer.setTheme(ChartThemes.getDarkTheme());

        DefaultChartModel model = new DefaultChartModel("Test");
        model.setPoint(0, 50, 0, "Cat 1");
        
        render(renderer, model);
    }

    @Test
    public void testDonutRendererStability() {
        DonutRenderer renderer = new DonutRenderer();
        renderer.setTheme(ChartThemes.getDarkTheme());

        DefaultChartModel model = new DefaultChartModel("Test");
        model.setPoint(0, 50, 0, "Cat 1");
        
        render(renderer, model);
    }

    @Test
    public void testSemiDonutRendererStability() {
        SemiDonutRenderer renderer = new SemiDonutRenderer();
        renderer.setTheme(ChartThemes.getDarkTheme());

        DefaultChartModel model = new DefaultChartModel("Test");
        model.setPoint(0, 50, 0, "Cat 1");
        
        render(renderer, model);
    }

    private void render(com.arbergashi.charts.render.ChartRenderer renderer, com.arbergashi.charts.model.ChartModel model) {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        PlotContext context = new PlotContext() {
            @Override public double getMinX() { return 0; }
            @Override public double getMaxX() { return 10; }
            @Override public double getMinY() { return 0; }
            @Override public double getMaxY() { return 100; }
            @Override public Rectangle2D getPlotBounds() { return new Rectangle2D.Double(50, 50, 700, 500); }
            @Override public void mapToPixel(double x, double y, double[] out) {}
            @Override public void mapToData(double px, double py, double[] dest) {}
        };

        assertDoesNotThrow(() -> {
            renderer.render(g2, model, context);
        });
        
        g2.dispose();
    }
}
