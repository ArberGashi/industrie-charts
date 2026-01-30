package com.arbergashi.charts.platform.export;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.standard.LineRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ChartExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExportPng() {
        ArberChartPanel panel = createTestPanel();
        panel.setSize(400, 300);
        File outputFile = tempDir.resolve("test_export.png").toFile();

        ChartExportService.exportPng(panel, outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    public void testExportPngWithPreferredSize() {
        ArberChartPanel panel = createTestPanel();
        panel.setPreferredSize(new java.awt.Dimension(500, 400));
        // Panel ist nicht realized (Breite/Höhe = 0)
        assertEquals(0, panel.getWidth());

        File outputFile = tempDir.resolve("test_export_pref.png").toFile();
        ChartExportService.exportPng(panel, outputFile);

        assertTrue(outputFile.exists());
        // Da das Bild 500x400 skaliert sein sollte, sollte es größer sein als ein leeres Bild
        assertTrue(outputFile.length() > 0);
    }

    @Test
    public void testExportSvgMissingBackend() {
        ArberChartPanel panel = createTestPanel();
        File outputFile = tempDir.resolve("test.svg").toFile();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                ChartExportService.exportSvg(panel, outputFile)
        );
        assertTrue(ex.getMessage().contains("SVG export backend not found"));
    }

    @Test
    public void testExportPdfMissingBackend() {
        ArberChartPanel panel = createTestPanel();
        File outputFile = tempDir.resolve("test.pdf").toFile();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                ChartExportService.exportPdf(panel, outputFile)
        );
        assertTrue(ex.getMessage().contains("PDF export backend not found"));
    }

    private ArberChartPanel createTestPanel() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {5, 10, 5};

            @Override
            public String getName() {
                return "test";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        return new ArberChartPanel(model, new LineRenderer());
    }
}
