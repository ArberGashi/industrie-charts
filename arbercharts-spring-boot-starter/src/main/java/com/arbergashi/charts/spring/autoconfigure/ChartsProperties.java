package com.arbergashi.charts.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for ArberCharts.
 *
 * <p>Bind these properties in your application.yml or application.properties:
 * <pre>
 * arbercharts:
 *   theme: dark
 *   export:
 *     enabled: true
 *     formats: [png, svg, pdf]
 *     directory: /tmp/charts
 *   performance:
 *     virtual-threads: true
 *     max-concurrent-renders: 10
 * </pre>
 *
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "arbercharts")
public class ChartsProperties {

    /**
     * Theme name (e.g., "dark", "light", "obsidian").
     * Default: "dark"
     */
    private String theme = "dark";

    /**
     * Export configuration.
     */
    private ExportProperties export = new ExportProperties();

    /**
     * Performance tuning configuration.
     */
    private PerformanceProperties performance = new PerformanceProperties();

    // Getters and Setters

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public ExportProperties getExport() {
        return export;
    }

    public void setExport(ExportProperties export) {
        this.export = export;
    }

    public PerformanceProperties getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceProperties performance) {
        this.performance = performance;
    }

    /**
     * Export-related configuration.
     */
    public static class ExportProperties {

        /**
         * Enable chart export functionality.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Supported export formats.
         * Default: ["png", "svg", "pdf"]
         */
        private List<String> formats = List.of("png", "svg", "pdf");

        /**
         * Directory for exported files.
         * Default: System temp directory
         */
        private String directory = System.getProperty("java.io.tmpdir");

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getFormats() {
            return formats;
        }

        public void setFormats(List<String> formats) {
            this.formats = formats;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    /**
     * Performance tuning configuration.
     */
    public static class PerformanceProperties {

        /**
         * Use Virtual Threads (Project Loom) for concurrent rendering.
         * Requires Java 21+.
         * Default: true
         */
        private boolean virtualThreads = true;

        /**
         * Maximum number of concurrent render operations.
         * Default: 10
         */
        private int maxConcurrentRenders = 10;

        // Getters and Setters

        public boolean isVirtualThreads() {
            return virtualThreads;
        }

        public void setVirtualThreads(boolean virtualThreads) {
            this.virtualThreads = virtualThreads;
        }

        public int getMaxConcurrentRenders() {
            return maxConcurrentRenders;
        }

        public void setMaxConcurrentRenders(int maxConcurrentRenders) {
            this.maxConcurrentRenders = maxConcurrentRenders;
        }
    }
}

