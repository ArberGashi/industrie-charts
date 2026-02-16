package com.arbergashi.charts.visualverifier.config;

/**
 * Configuration properties for ArberCharts Visual Verifier.
 *
 * <p>Configurable via {@code arbercharts.verifier.*} in application.yml.
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
public class VerifierProperties {

    private Snapshot snapshot = new Snapshot();
    private Comparison comparison = new Comparison();
    private Benchmark benchmark = new Benchmark();
    private Rendering rendering = new Rendering();

    public Snapshot getSnapshot() { return snapshot; }
    public void setSnapshot(Snapshot snapshot) { this.snapshot = snapshot; }

    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }

    public Benchmark getBenchmark() { return benchmark; }
    public void setBenchmark(Benchmark benchmark) { this.benchmark = benchmark; }

    public Rendering getRendering() { return rendering; }
    public void setRendering(Rendering rendering) { this.rendering = rendering; }

    /**
     * Snapshot configuration for visual regression testing.
     */
    public static class Snapshot {
        private String directory = System.getProperty("user.home") + "/.arbercharts/snapshots";
        private String format = "png";
        private int compression = 9;
        private int retentionDays = 30;

        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public int getCompression() { return compression; }
        public void setCompression(int compression) { this.compression = compression; }

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    /**
     * Visual comparison configuration.
     */
    public static class Comparison {
        private double tolerance = 0.01;
        private boolean generateDiff = true;
        private int diffColor = 0xFF0000; // Red (RGB without alpha)

        public double getTolerance() { return tolerance; }
        public void setTolerance(double tolerance) { this.tolerance = tolerance; }

        public boolean isGenerateDiff() { return generateDiff; }
        public void setGenerateDiff(boolean generateDiff) { this.generateDiff = generateDiff; }

        public int getDiffColor() { return diffColor; }
        public void setDiffColor(int diffColor) { this.diffColor = diffColor; }
    }

    /**
     * Benchmark configuration.
     */
    public static class Benchmark {
        private int warmup = 100;
        private int iterations = 1000;
        private boolean enableGc = false;
        private int threads = 1;

        public int getWarmup() { return warmup; }
        public void setWarmup(int warmup) { this.warmup = warmup; }

        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }

        public boolean isEnableGc() { return enableGc; }
        public void setEnableGc(boolean enableGc) { this.enableGc = enableGc; }

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }
    }

    /**
     * Rendering defaults.
     */
    public static class Rendering {
        private int defaultWidth = 800;
        private int defaultHeight = 600;
        private String defaultTheme = "light";
        private int maxWidth = 4096;
        private int maxHeight = 4096;
        private long maxFileSize = 10_485_760L;

        public int getDefaultWidth() { return defaultWidth; }
        public void setDefaultWidth(int defaultWidth) { this.defaultWidth = defaultWidth; }

        public int getDefaultHeight() { return defaultHeight; }
        public void setDefaultHeight(int defaultHeight) { this.defaultHeight = defaultHeight; }

        public String getDefaultTheme() { return defaultTheme; }
        public void setDefaultTheme(String defaultTheme) { this.defaultTheme = defaultTheme; }

        public int getMaxWidth() { return maxWidth; }
        public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }

        public int getMaxHeight() { return maxHeight; }
        public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }

        public long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
    }
}

