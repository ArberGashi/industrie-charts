package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.visualverifier.RendererCatalog;
import com.arbergashi.charts.visualverifier.RendererCatalogEntry;
import com.arbergashi.charts.visualverifier.dto.RendererInfo;
import org.springframework.stereotype.Service;

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

    private final RendererCatalog catalog;

    public RendererCatalogService() {
        this.catalog = RendererCatalog.load();
    }

    /**
     * Returns all renderers as RendererInfo DTOs.
     */
    public List<RendererInfo> getAllRenderers() {
        return catalog.all().stream()
                .map(e -> RendererInfo.of(e.className(), e.category()))
                .collect(Collectors.toList());
    }

    /**
     * Returns renderers grouped by category.
     */
    public Map<String, List<RendererInfo>> getRenderersByCategory() {
        return catalog.all().stream()
                .map(e -> RendererInfo.of(e.className(), e.category()))
                .collect(Collectors.groupingBy(RendererInfo::category));
    }

    /**
     * Returns category statistics (count per category).
     */
    public Map<String, Long> getCategoryStats() {
        return catalog.all().stream()
                .collect(Collectors.groupingBy(
                        RendererCatalogEntry::category,
                        Collectors.counting()
                ));
    }

    /**
     * Returns total renderer count.
     */
    public int getTotalCount() {
        return catalog.all().size();
    }

    /**
     * Returns a specific renderer entry by class name.
     */
    public RendererCatalogEntry getRenderer(String className) {
        return catalog.getRequired(className);
    }
}

