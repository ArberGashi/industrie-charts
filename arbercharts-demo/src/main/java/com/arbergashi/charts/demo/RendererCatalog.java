package com.arbergashi.charts.demo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of all available chart renderers for the demo application.
 *
 * <p>Loads renderer metadata from a resource file ({@code /data/renderer-catalog.txt})
 * containing category and fully-qualified class names.
 *
 * <h2>File Format</h2>
 * <pre>
 * # Comment lines start with #
 * category|com.arbergashi.charts.render.category.RendererClassName
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * RendererCatalog catalog = RendererCatalog.load();
 * for (RendererCatalogEntry entry : catalog.entries()) {
 *     System.out.println(entry.simpleName() + " in " + entry.category());
 * }
 * }</pre>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
final class RendererCatalog {
    private static final String RESOURCE_PATH = "/data/renderer-catalog.txt";
    private final List<RendererCatalogEntry> entries;
    private final Map<String, RendererCatalogEntry> byClassName;

    private RendererCatalog(List<RendererCatalogEntry> entries) {
        this.entries = entries;
        Map<String, RendererCatalogEntry> map = new LinkedHashMap<>();
        for (RendererCatalogEntry entry : entries) {
            map.put(entry.className(), entry);
        }
        this.byClassName = Collections.unmodifiableMap(map);
    }

    static RendererCatalog load() {
        List<RendererCatalogEntry> loaded = new ArrayList<>();
        InputStream input = RendererCatalog.class.getResourceAsStream(RESOURCE_PATH);
        if (input == null) {
            throw new IllegalStateException("Missing renderer catalog resource: " + RESOURCE_PATH);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length != 2) {
                    continue;
                }
                String category = parts[0].trim().toLowerCase(Locale.US);
                String className = parts[1].trim();
                if (category.isEmpty() || className.isEmpty()) {
                    continue;
                }
                loaded.add(RendererCatalogEntry.of(category, className));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read renderer catalog", ex);
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("Renderer catalog is empty");
        }
        return new RendererCatalog(Collections.unmodifiableList(loaded));
    }

    List<RendererCatalogEntry> entries() {
        return entries;
    }

    RendererCatalogEntry getRequired(String className) {
        RendererCatalogEntry entry = byClassName.get(className);
        if (entry == null) {
            throw new IllegalArgumentException("Renderer not in catalog: " + className);
        }
        return entry;
    }
}
