package com.arbergashi.charts.core.rendering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers the best available ArberCanvasProvider via ServiceLoader.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ArberBridgeFactory {
    private static final Logger LOGGER = Logger.getLogger(ArberBridgeFactory.class.getName());
    private static final String OVERRIDE_KEY = "arbercharts.bridge";

    private ArberBridgeFactory() {}

    public static ArberCanvasProvider getBestAvailableProvider() {
        String override = System.getProperty(OVERRIDE_KEY, "").trim();
        List<ArberCanvasProvider> providers = loadProviders();
        if (!override.isEmpty()) {
            for (ArberCanvasProvider p : providers) {
                if (override.equalsIgnoreCase(p.getId()) && p.isSupported()) {
                    logSelection(p, "override");
                    return p;
                }
            }
        }
        return providers.stream()
                .filter(ArberCanvasProvider::isSupported)
                .max(Comparator.comparingInt(ArberCanvasProvider::getPriority))
                .map(p -> {
                    logSelection(p, "auto");
                    return p;
                })
                .orElse(null);
    }

    private static List<ArberCanvasProvider> loadProviders() {
        List<ArberCanvasProvider> list = new ArrayList<>();
        try {
            ServiceLoader.load(ArberCanvasProvider.class).forEach(list::add);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "ServiceLoader failed", t);
        }
        return list;
    }

    private static void logSelection(ArberCanvasProvider p, String mode) {
        if (p == null) return;
        LOGGER.info(() -> "[ArberCharts] Rendering Bridge (" + mode + "): " + p.getId());
    }
}
