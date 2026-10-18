package com.arbergashi.charts.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound configuration values reserved for application-defined behavior.
 *
 * <p>{@link ChartAutoConfiguration} binds these values but does not apply the
 * theme, configure export services, select an executor or enforce concurrency
 * limits. Applications that use these settings must read and implement them.
 * The active server pool and timing settings belong to
 * {@link ArberChartsProperties} under {@code arbercharts.server}.
 *
 * <p>Example values for an application that implements the corresponding behavior:
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
     * Requested theme name, such as "dark", "light" or "obsidian"; not applied automatically.
     * Default: "dark"
     */
    private String theme = "dark";

    /**
     * Stored export preferences; the starter does not configure an exporter from them.
     */
    private ExportProperties export = new ExportProperties();

    /**
     * Stored performance preferences; the starter does not apply them.
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
     * Export preferences for an application-provided exporter.
     */
    public static class ExportProperties {

        /**
         * Requested export-enabled flag; no exporter is enabled by this value alone.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Requested export format names; this list does not verify backend availability.
         * Default: ["png", "svg", "pdf"]
         */
        private List<String> formats = List.of("png", "svg", "pdf");

        /**
         * Requested output directory; this holder does not create directories or write files.
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
     * Stored performance preferences; the starter does not apply them.
     */
    public static class PerformanceProperties {

        /**
         * Requested virtual-thread preference; does not create or configure an executor.
         * The ArberCharts runtime baseline remains Java 25.
         * Default: true
         */
        private boolean virtualThreads = true;

        /**
         * Requested concurrency limit; application code must enforce it.
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

