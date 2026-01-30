package com.arbergashi.charts.core.rendering;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless render sanity check: ensures the renderer can produce a PNG
 * with java.awt.headless=true and BridgeFactory server selection.
 */
public final class HeadlessSanityCheckTest {

    @Test
    void rendersCandlesticksHeadlessToPng() throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("arbercharts.bridge", "server");

        ArberCanvasProvider provider = ArberBridgeFactory.getBestAvailableProvider();
        assertNotNull(provider, "Expected server provider to be discovered");
        assertEquals("server", provider.getId(), "Expected server provider id");

        DefaultFinancialChartModel model = buildModel(1000);
        CandlestickRenderer renderer = new CandlestickRenderer();

        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 800, 600);
        DefaultPlotContext ctx = new DefaultPlotContext(
                bounds,
                model,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                ChartThemes.getDarkTheme()
        );

        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderer.render(g2, model, ctx);
        g2.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 0, "PNG output should not be empty");
    }

    private static DefaultFinancialChartModel buildModel(int count) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Headless");
        double price = 100.0;
        for (int i = 0; i < count; i++) {
            double open = price;
            double close = price + Math.sin(i * 0.03) * 2.0;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(i, open, high, low, close);
            price = close;
        }
        return model;
    }
}
