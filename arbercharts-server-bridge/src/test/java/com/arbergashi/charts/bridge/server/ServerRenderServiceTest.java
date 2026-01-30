package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.model.DefaultFinancialChartModel;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRenderServiceTest {
    @Test
    void rendersNonEmptyImage() {
        DefaultFinancialChartModel model = generateFinancialCandles(10_000);
        ServerRenderService service = new ServerRenderService(1);

        BufferedImage image = service.renderToImage(model, new Dimension(800, 600));
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        long sum = 0;
        for (int i = 0; i < pixels.length; i += 97) {
            sum += (pixels[i] & 0xFFFFFFFFL);
        }
        assertTrue(sum > 0, "rendered image should contain non-zero pixels");
    }

    private static DefaultFinancialChartModel generateFinancialCandles(int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("ServerTest");
        double price = 100.0;
        for (int i = 0; i < points; i++) {
            double t = i;
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(t, open, high, low, close);
            price = close;
        }
        return model;
    }
}
