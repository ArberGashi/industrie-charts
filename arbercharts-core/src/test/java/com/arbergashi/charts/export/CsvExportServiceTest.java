package com.arbergashi.charts.export;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularChartModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CsvExportService}.
 *
 * @since 2.0.0
 */
class CsvExportServiceTest {

    private ChartModel model;

    @BeforeEach
    void setUp() {
        model = new CircularChartModel(100);
        // Add test data
        for (int i = 0; i < 10; i++) {
            ((CircularChartModel) model).setPoint(i, i * 10.0, 1.0, "Point" + i);
        }
    }

    @Test
    void exportToFile_createsValidCsv(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("test.csv").toFile();

        CsvExportService.export(model, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());

        // Check header
        assertTrue(content.startsWith("X,Y,Weight,Label"));

        // Check data rows exist
        assertTrue(content.contains("0,0,1,Point0"));
        assertTrue(content.contains("9,90,1,Point9"));
    }

    @Test
    void exportWithCustomDelimiter(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("test_semicolon.csv").toFile();

        CsvExportService.builder()
            .delimiter(';')
            .export(model, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("X;Y;Weight;Label"));
        assertTrue(content.contains("0;0;1;Point0"));
    }

    @Test
    void exportWithEuropeanFormat(@TempDir Path tempDir) throws Exception {
        // Add data with decimal values
        CircularChartModel decimalModel = new CircularChartModel(10);
        decimalModel.setPoint(1.5, 25.75, 1.0, "Test");

        File outputFile = tempDir.resolve("test_european.csv").toFile();

        CsvExportService.builder()
            .delimiter(';')
            .decimalSeparator(',')
            .export(decimalModel, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("1,5;25,75"), "Should use comma as decimal separator");
    }

    @Test
    void exportWithoutHeader(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("test_noheader.csv").toFile();

        CsvExportService.builder()
            .includeHeader(false)
            .export(model, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertFalse(content.startsWith("X,Y"));
        assertTrue(content.startsWith("0,0,1"));
    }

    @Test
    void exportWithCustomHeaders(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("test_custom_headers.csv").toFile();

        CsvExportService.builder()
            .headerNames("Zeit", "Wert", "Gewicht", "Bezeichnung")
            .export(model, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.startsWith("Zeit,Wert,Gewicht,Bezeichnung"));
    }

    @Test
    void exportExcelCompatible(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("test_excel.csv").toFile();

        CsvExportService.builder()
            .excelCompatible(true)
            .export(model, outputFile);

        byte[] bytes = Files.readAllBytes(outputFile.toPath());

        // Check for UTF-8 BOM
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    void exportToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        CsvExportService.builder()
            .includeHeader(true)
            .export(model, writer);

        String content = writer.toString();
        assertTrue(content.contains("X,Y,Weight,Label"));
        assertTrue(content.contains("0,0,1,Point0"));
    }

    @Test
    void exportWithPrecision(@TempDir Path tempDir) throws Exception {
        CircularChartModel precisionModel = new CircularChartModel(10);
        precisionModel.setPoint(1.123456789, 99.987654321, 1.0, null);

        File outputFile = tempDir.resolve("test_precision.csv").toFile();

        CsvExportService.builder()
            .precision(2)
            .export(precisionModel, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("1.12,99.99"), "Should round to 2 decimal places");
    }

    @Test
    void exportHandlesSpecialCharacters(@TempDir Path tempDir) throws Exception {
        CircularChartModel specialModel = new CircularChartModel(10);
        specialModel.setPoint(1, 10, 1.0, "Label with, comma");
        specialModel.setPoint(2, 20, 1.0, "Label with \"quotes\"");
        specialModel.setPoint(3, 30, 1.0, "Label with\nnewline");

        File outputFile = tempDir.resolve("test_special.csv").toFile();

        CsvExportService.export(specialModel, outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"Label with, comma\""), "Comma should be quoted");
        assertTrue(content.contains("\"Label with \"\"quotes\"\"\""), "Quotes should be escaped");
    }

    @Test
    void exportCreatesParentDirectories(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("nested/dir/test.csv").toFile();

        CsvExportService.export(model, outputFile);

        assertTrue(outputFile.exists());
    }

    @Test
    void exportThrowsOnNullModel(@TempDir Path tempDir) {
        File outputFile = tempDir.resolve("test.csv").toFile();

        assertThrows(NullPointerException.class, () ->
            CsvExportService.export(null, outputFile));
    }

    @Test
    void exportThrowsOnNullFile() {
        assertThrows(NullPointerException.class, () ->
            CsvExportService.export(model, (File) null));
    }
}

