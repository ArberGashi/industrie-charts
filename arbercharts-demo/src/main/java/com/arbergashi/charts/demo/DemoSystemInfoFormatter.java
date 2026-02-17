package com.arbergashi.charts.demo;

final class DemoSystemInfoFormatter {

    private DemoSystemInfoFormatter() {
    }

    static String format(boolean vectorAvailable, int rendererCount) {
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        return String.format("""
                Java Version: %s
                Java Vendor: %s
                OS: %s %s
                Architecture: %s
                Processors: %d
                Memory Used: %d MB / %d MB
                Memory Max: %d MB
                Vector API: %s
                Renderers: %d
                """,
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                runtime.availableProcessors(),
                usedMem, totalMem,
                maxMem,
                vectorAvailable ? "Available" : "Not available",
                rendererCount
        );
    }
}
