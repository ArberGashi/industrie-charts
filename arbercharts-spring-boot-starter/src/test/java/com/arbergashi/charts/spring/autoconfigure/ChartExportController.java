package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Dimension;

@RestController
public class ChartExportController {
    private final ServerRenderService renderService;

    public ChartExportController(ServerRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping(value = "/test-chart.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getChart() {
        DefaultFinancialChartModel model = generateFinancialCandles(1000);
        byte[] image = renderService.renderToPng(model, new Dimension(800, 600));
        return ResponseEntity.ok(image);
    }

    private static DefaultFinancialChartModel generateFinancialCandles(int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("SpringTest");
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
