package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.visualverifier.RendererCatalog;
import com.arbergashi.charts.visualverifier.RendererCatalogEntry;
import com.arbergashi.charts.visualverifier.dto.RendererInfo;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for accessing the renderer catalog.
 *
 * <p>Provides access to all 158 ArberCharts renderers with category information.
 *
 * @since 2.0.0
 */
@Service
public class RendererCatalogService {
    private static final System.Logger LOGGER = System.getLogger(RendererCatalogService.class.getName());
    private static final Set<String> EXCLUDED_RENDERERS = Set.of(
            // Duplicate registry key "candlestick_hollow"; financial variant is preferred.
            "com.arbergashi.charts.render.specialized.CandlestickHollowRenderer"
    );
    private static final Map<String, Integer> CATEGORY_PRIORITY = Map.ofEntries(
            Map.entry("financial", 100),
            Map.entry("medical", 95),
            Map.entry("predictive", 90),
            Map.entry("standard", 85),
            Map.entry("statistical", 82),
            Map.entry("analysis", 80),
            Map.entry("circular", 78),
            Map.entry("forensic", 70),
            Map.entry("security", 68),
            Map.entry("common", 65),
            Map.entry("specialized", 50)
    );

    private final RendererCatalog catalog;
    private final List<RendererCatalogEntry> renderableEntries;
    private final Map<String, RendererCatalogEntry> renderableByClassName;

    public RendererCatalogService() {
        this.catalog = RendererCatalog.load();
        List<RendererCatalogEntry> filtered = catalog.all().stream()
                .filter(this::isRenderable)
                .toList();
        this.renderableEntries = deduplicateBySimpleName(filtered);
        this.renderableByClassName = renderableEntries.stream()
                .collect(Collectors.toMap(RendererCatalogEntry::className, e -> e, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Returns all renderers as RendererInfo DTOs.
     */
    public List<RendererInfo> getAllRenderers() {
        return renderableEntries.stream()
                .map(e -> RendererInfo.of(e.className(), e.category()))
                .collect(Collectors.toList());
    }

    /**
     * Returns renderers grouped by category.
     */
    public Map<String, List<RendererInfo>> getRenderersByCategory() {
        return renderableEntries.stream()
                .map(e -> RendererInfo.of(e.className(), e.category()))
                .collect(Collectors.groupingBy(RendererInfo::category));
    }

    /**
     * Returns category statistics (count per category).
     */
    public Map<String, Long> getCategoryStats() {
        return renderableEntries.stream()
                .collect(Collectors.groupingBy(
                        RendererCatalogEntry::category,
                        Collectors.counting()
                ));
    }

    /**
     * Returns total renderer count.
     */
    public int getTotalCount() {
        return renderableEntries.size();
    }

    /**
     * Returns a specific renderer entry by class name.
     */
    public RendererCatalogEntry getRenderer(String className) {
        RendererCatalogEntry entry = renderableByClassName.get(className);
        if (entry == null) {
            throw new IllegalArgumentException("Renderer is not compatible with visual verifier: " + className);
        }
        return entry;
    }

    private boolean isRenderable(RendererCatalogEntry entry) {
        if (EXCLUDED_RENDERERS.contains(entry.className())) {
            return false;
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = RendererCatalogService.class.getClassLoader();
            }
            Class<?> clazz = Class.forName(entry.className(), false, loader);
            boolean compatible = ChartRenderer.class.isAssignableFrom(clazz)
                    && !Modifier.isAbstract(clazz.getModifiers())
                    && !clazz.isInterface();
            if (!compatible) {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Skipping non-renderable catalog entry: {0}",
                        entry.className()
                );
            }
            return compatible;
        } catch (ClassNotFoundException ex) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Skipping missing catalog entry class: {0}",
                    entry.className()
            );
            return false;
        }
    }

    private List<RendererCatalogEntry> deduplicateBySimpleName(List<RendererCatalogEntry> entries) {
        Map<String, RendererCatalogEntry> selected = new LinkedHashMap<>();
        for (RendererCatalogEntry entry : entries) {
            RendererCatalogEntry current = selected.get(entry.simpleName());
            if (current == null || isPreferred(entry, current)) {
                selected.put(entry.simpleName(), entry);
            }
        }
        return selected.values().stream()
                .sorted(Comparator.comparing(RendererCatalogEntry::category).thenComparing(RendererCatalogEntry::simpleName))
                .toList();
    }

    private boolean isPreferred(RendererCatalogEntry candidate, RendererCatalogEntry existing) {
        int candidatePriority = categoryPriority(candidate.category());
        int existingPriority = categoryPriority(existing.category());
        if (candidatePriority != existingPriority) {
            return candidatePriority > existingPriority;
        }
        // Prefer shorter class names as a deterministic tie-breaker.
        return candidate.className().length() < existing.className().length();
    }

    private int categoryPriority(String category) {
        return CATEGORY_PRIORITY.getOrDefault(category, 60);
    }
}
