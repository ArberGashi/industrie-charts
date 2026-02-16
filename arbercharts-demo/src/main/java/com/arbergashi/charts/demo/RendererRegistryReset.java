package com.arbergashi.charts.demo;

import com.arbergashi.charts.internal.RendererRegistry;
import java.lang.reflect.Field;
import java.util.Map;

final class RendererRegistryReset {
    private RendererRegistryReset() {
    }

    static void reset() {
        try {
            clearMap("FACTORIES");
            clearMap("METADATA");
            clearMap("AFFINITIES");
        } catch (Exception ignore) {
            // Best-effort reset for demo use.
        }
    }

    private static void clearMap(String fieldName) throws Exception {
        Field field = RendererRegistry.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object registry = field.get(null);
        if (registry instanceof Map<?, ?> map) {
            map.clear();
        }
    }
}
