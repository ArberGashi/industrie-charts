package com.arbergashi.charts.export;

import com.arbergashi.charts.model.ChartModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * Exports chart data to CSV format.
 *
 * <p>This service provides zero-dependency CSV export functionality for chart data.
 * It supports various configuration options for different regional formats.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Export to file or Writer</li>
 *   <li>Configurable delimiter (comma, semicolon, tab)</li>
 *   <li>Configurable decimal separator</li>
 *   <li>Optional header row</li>
 *   <li>UTF-8 encoding with optional BOM for Excel compatibility</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple export
 * CsvExportService.export(model, new File("data.csv"));
 *
 * // With custom options
 * CsvExportService.builder()
 *     .delimiter(';')
 *     .decimalSeparator(',')
 *     .includeHeader(true)
 *     .excelCompatible(true)
 *     .export(model, new File("data.csv"));
 * }</pre>
 *
 * @since 2.0.0
 * @author Arber Gashi
 */
public final class CsvExportService {

    private CsvExportService() {
        // Utility class
    }

    /**
     * Exports chart data to a CSV file with default settings.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Delimiter: comma (,)</li>
     *   <li>Decimal separator: period (.)</li>
     *   <li>Header: included</li>
     *   <li>Encoding: UTF-8</li>
     * </ul>
     *
     * @param model the chart model containing the data
     * @param outputFile the output CSV file
     * @throws IOException if writing fails
     * @throws IllegalArgumentException if model or outputFile is null
     */
    public static void export(ChartModel model, File outputFile) throws IOException {
        builder().export(model, outputFile);
    }

    /**
     * Exports chart data to a CSV file at the specified path.
     *
     * @param model the chart model containing the data
     * @param outputPath the output file path
     * @throws IOException if writing fails
     */
    public static void export(ChartModel model, Path outputPath) throws IOException {
        export(model, outputPath.toFile());
    }

    /**
     * Creates a new CSV export builder for custom configuration.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring CSV export options.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * CsvExportService.builder()
     *     .delimiter(';')           // European format
     *     .decimalSeparator(',')    // European decimal
     *     .includeHeader(true)
     *     .headerNames("Zeit", "Wert", "Gewicht")
     *     .export(model, file);
     * }</pre>
     */
    public static final class Builder {

        private char delimiter = ',';
        private char decimalSeparator = '.';
        private boolean includeHeader = true;
        private String[] headerNames = {"X", "Y", "Weight", "Label"};
        private boolean excelCompatible = false;
        private String lineEnding = System.lineSeparator();
        private int precision = 6;

        private Builder() {
        }

        /**
         * Sets the field delimiter character.
         *
         * @param delimiter the delimiter (default: comma)
         * @return this builder
         */
        public Builder delimiter(char delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * Sets the decimal separator for numeric values.
         *
         * @param separator the decimal separator (default: period)
         * @return this builder
         */
        public Builder decimalSeparator(char separator) {
            this.decimalSeparator = separator;
            return this;
        }

        /**
         * Configures whether to include a header row.
         *
         * @param include true to include header (default: true)
         * @return this builder
         */
        public Builder includeHeader(boolean include) {
            this.includeHeader = include;
            return this;
        }

        /**
         * Sets custom header names.
         *
         * @param names the header names (X, Y, Weight, Label)
         * @return this builder
         */
        public Builder headerNames(String... names) {
            this.headerNames = names;
            return this;
        }

        /**
         * Enables Excel compatibility mode.
         *
         * <p>When enabled:
         * <ul>
         *   <li>Adds UTF-8 BOM for proper encoding detection</li>
         *   <li>Uses CRLF line endings</li>
         * </ul>
         *
         * @param compatible true for Excel compatibility
         * @return this builder
         */
        public Builder excelCompatible(boolean compatible) {
            this.excelCompatible = compatible;
            if (compatible) {
                this.lineEnding = "\r\n";
            }
            return this;
        }

        /**
         * Sets the numeric precision (decimal places).
         *
         * @param precision number of decimal places (default: 6)
         * @return this builder
         */
        public Builder precision(int precision) {
            this.precision = Math.max(0, Math.min(precision, 15));
            return this;
        }

        /**
         * Exports the chart data to a file.
         *
         * @param model the chart model
         * @param outputFile the output file
         * @throws IOException if writing fails
         */
        public void export(ChartModel model, File outputFile) throws IOException {
            Objects.requireNonNull(model, "model must not be null");
            Objects.requireNonNull(outputFile, "outputFile must not be null");

            // Ensure parent directories exist
            File parent = outputFile.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create directory: " + parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputFile.toPath(), StandardCharsets.UTF_8)) {
                export(model, writer);
            }
        }

        /**
         * Exports the chart data to a writer.
         *
         * @param model the chart model
         * @param writer the output writer
         * @throws IOException if writing fails
         */
        public void export(ChartModel model, Writer writer) throws IOException {
            Objects.requireNonNull(model, "model must not be null");
            Objects.requireNonNull(writer, "writer must not be null");

            // Write BOM for Excel if needed
            if (excelCompatible) {
                writer.write('\uFEFF');
            }

            // Create number formatter
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator(decimalSeparator);
            StringBuilder pattern = new StringBuilder("0");
            if (precision > 0) {
                pattern.append('.');
                pattern.append("#".repeat(precision));
            }
            DecimalFormat format = new DecimalFormat(pattern.toString(), symbols);

            // Write header
            if (includeHeader && headerNames != null && headerNames.length > 0) {
                for (int i = 0; i < headerNames.length; i++) {
                    if (i > 0) writer.write(delimiter);
                    writeEscaped(writer, headerNames[i]);
                }
                writer.write(lineEnding);
            }

            // Write data rows
            int size = model.getPointCount();
            for (int i = 0; i < size; i++) {
                double x = model.getX(i);
                double y = model.getY(i);
                double weight = model.getWeight(i);
                String label = model.getLabel(i);

                // X value
                writer.write(format.format(x));
                writer.write(delimiter);

                // Y value
                writer.write(format.format(y));
                writer.write(delimiter);

                // Weight
                writer.write(format.format(weight));
                writer.write(delimiter);

                // Label (escaped)
                writeEscaped(writer, label != null ? label : "");

                writer.write(lineEnding);
            }

            writer.flush();
        }

        private void writeEscaped(Writer writer, String value) throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }

            boolean needsQuotes = value.indexOf(delimiter) >= 0
                    || value.indexOf('"') >= 0
                    || value.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0;

            if (needsQuotes) {
                writer.write('"');
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (c == '"') {
                        writer.write("\"\"");
                    } else {
                        writer.write(c);
                    }
                }
                writer.write('"');
            } else {
                writer.write(value);
            }
        }
    }
}

