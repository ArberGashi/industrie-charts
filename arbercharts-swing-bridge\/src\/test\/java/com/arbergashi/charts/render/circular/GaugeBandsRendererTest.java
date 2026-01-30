package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class GaugeBandsRendererTest {

    @Test
    public void testRender() {
        GaugeBandsRenderer renderer = new GaugeBandsRenderer();
        renderer.setTheme(ChartThemes.getDarkTheme());

        DefaultChartModel model = new DefaultChartModel("Test");
        model.setPoint(0, 75, 0, "");

        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 400, 400);
        PlotContext context = new DefaultPlotContext(
                bounds,
                0,
                1,
                0,
                100,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR
        );

        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        assertDoesNotThrow(() -> renderer.render(g2, model, context));

        g2.dispose();
    }
}
