package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.circular.DonutRenderer;
import com.arbergashi.charts.render.circular.PieRenderer;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.financial.VolumeRenderer;
import com.arbergashi.charts.render.medical.MedicalSweepRenderer;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.testing.RasterTestCanvas;
import org.junit.jupiter.api.Test;

import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RendererVisualRegressionTest {

    @Test
    void candlestickIsDeterministicAndNonEmpty() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Candles");
        double price = 100.0;
        for (int i = 0; i < 400; i++) {
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(i, open, high, low, close);
            price = close;
        }

        long hash1 = renderHash(new CandlestickRenderer(), model);
        long hash2 = renderHash(new CandlestickRenderer(), model);

        assertEquals(hash1, hash2, "Candlestick render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Candlestick render should not be empty.");
    }

    @Test
    void lineRendererIsDeterministicAndNonEmpty() {
        DefaultChartModel model = new DefaultChartModel("Line");
        for (int i = 0; i < 800; i++) {
            double x = i;
            double y = Math.sin(i * 0.05) * 50.0 + Math.cos(i * 0.03) * 20.0;
            model.setXY(x, y);
        }

        long hash1 = renderHash(new LineRenderer(), model);
        long hash2 = renderHash(new LineRenderer(), model);

        assertEquals(hash1, hash2, "Line render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Line render should not be empty.");
    }

    @Test
    void pieRendererIsDeterministicAndNonEmpty() {
        DefaultChartModel model = new DefaultChartModel("Pie");
        String[] labels = {"Equities", "Rates", "FX", "Commodities", "Alt"};
        double[] weights = {40, 22, 14, 12, 12};
        for (int i = 0; i < labels.length; i++) {
            model.setPoint(i, weights[i], 0.0, 0.0, weights[i], labels[i]);
        }

        long hash1 = renderHash(new PieRenderer(), model);
        long hash2 = renderHash(new PieRenderer(), model);

        assertEquals(hash1, hash2, "Pie render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Pie render should not be empty.");
    }

    @Test
    void radarRendererIsDeterministicAndNonEmpty() {
        DefaultChartModel model = new DefaultChartModel("Radar");
        double[] values = {62, 48, 91, 70, 79, 55};
        for (int i = 0; i < values.length; i++) {
            model.setPoint(i, values[i], 0.0, 0.0, values[i], "R" + (i + 1));
        }

        long hash1 = renderHash(new RadarRenderer(), model, 320, 240);
        long hash2 = renderHash(new RadarRenderer(), model, 320, 240);

        assertEquals(hash1, hash2, "Radar render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Radar render should not be empty.");
    }

    @Test
    void heatmapRendererIsDeterministicAndNonEmpty() {
        DefaultChartModel model = new DefaultChartModel("Heatmap");
        int side = 48;
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                model.setXY(c, r);
            }
        }

        long hash1 = renderHash(new HeatmapRenderer(), model);
        long hash2 = renderHash(new HeatmapRenderer(), model);

        assertEquals(hash1, hash2, "Heatmap render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Heatmap render should not be empty.");
    }

    @Test
    void medicalSweepRendererIsDeterministicAndNonEmpty() {
        ChartModel model = new com.arbergashi.charts.model.CircularFastMedicalModel(4096, 2);
        double t = 0.0;
        for (int i = 0; i < 2000; i++) {
            double y = Math.sin(t * 0.05) * 0.8 + Math.sin(t * 0.12) * 0.2;
            ((com.arbergashi.charts.model.CircularFastMedicalModel) model).add(t, new double[]{t, y});
            t += 1.0;
        }

        long hash1 = renderHash(new MedicalSweepRenderer(), model);
        long hash2 = renderHash(new MedicalSweepRenderer(), model);

        assertEquals(hash1, hash2, "Medical sweep render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Medical sweep render should not be empty.");
    }

    @Test
    void volumeRendererIsDeterministicAndNonEmpty() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Volume");
        double price = 100.0;
        for (int i = 0; i < 500; i++) {
            double wave = Math.sin(i * 0.06) * 3.0 + Math.cos(i * 0.04) * 2.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.0;
            double low = Math.min(open, close) - 1.0;
            double volume = 500 + 300 * Math.abs(Math.sin(i * 0.05));
            model.setOHLC(i, open, high, low, close, volume, null);
            price = close;
        }

        long hash1 = renderHash(new VolumeRenderer(), model);
        long hash2 = renderHash(new VolumeRenderer(), model);

        assertEquals(hash1, hash2, "Volume render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Volume render should not be empty.");
    }

    @Test
    void donutRendererIsDeterministicAndNonEmpty() {
        DefaultChartModel model = new DefaultChartModel("Donut");
        String[] labels = {"Core", "Addons", "Support", "Ops"};
        double[] weights = {38, 24, 18, 20};
        for (int i = 0; i < labels.length; i++) {
            model.setPoint(i, weights[i], 0.0, 0.0, weights[i], labels[i]);
        }

        DonutRenderer renderer = new DonutRenderer().setCenterText("Revenue").setCenterSubText("LTS");
        long hash1 = renderHash(renderer, model);
        long hash2 = renderHash(new DonutRenderer().setCenterText("Revenue").setCenterSubText("LTS"), model);

        assertEquals(hash1, hash2, "Donut render hash must be deterministic.");
        assertTrue(hash1 != 0L, "Donut render should not be empty.");
    }

    private long renderHash(ChartRenderer renderer, ChartModel model) {
        return renderHash(renderer, model, 640, 360);
    }

    private long renderHash(ChartRenderer renderer, ChartModel model, int width, int height) {
        RasterTestCanvas canvas = new RasterTestCanvas(width, height);
        ArberRect bounds = new ArberRect(0, 0, width, height);
        DefaultPlotContext context = new DefaultPlotContext(
                bounds,
                model,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                ChartThemes.getDarkTheme(),
                null
        );
        renderer.render(canvas, model, context);
        return crc32(canvas.pixels());
    }

    private long crc32(int[] pixels) {
        CRC32 crc = new CRC32();
        for (int value : pixels) {
            crc.update(value);
            crc.update(value >>> 8);
            crc.update(value >>> 16);
            crc.update(value >>> 24);
        }
        return crc.getValue();
    }
}
