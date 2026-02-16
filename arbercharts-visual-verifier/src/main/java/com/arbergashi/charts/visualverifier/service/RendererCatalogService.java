package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.visualverifier.RendererCatalog;
import com.arbergashi.charts.visualverifier.RendererCatalogEntry;
import com.arbergashi.charts.visualverifier.dto.RendererInfo;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
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

    private final RendererCatalog catalog;
    private final List<RendererCatalogEntry> renderableEntries;

    public RendererCatalogService() {
        this.catalog = RendererCatalog.load();
        this.renderableEntries = catalog.all().stream()
                .filter(this::isRenderable)
                .toList();
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
        RendererCatalogEntry entry = catalog.getRequired(className);
        if (!isRenderable(entry)) {
            throw new IllegalArgumentException("Renderer is not compatible with visual verifier: " + className);
        }
        return entry;
    }

    private boolean isRenderable(RendererCatalogEntry entry) {
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
}
