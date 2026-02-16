package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.dto.RendererInfo;
import com.arbergashi.charts.visualverifier.service.RendererCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for renderer catalog.
 *
 * <p>Provides access to all 158 renderers with category information
 * and statistics.
 *
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/renderers")
public class CatalogController {

    private final RendererCatalogService catalogService;

    public CatalogController(RendererCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Returns all renderers with statistics.
     */
    @GetMapping
    public Map<String, Object> getAllRenderers() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", catalogService.getTotalCount());
        result.put("categories", catalogService.getCategoryStats());
        result.put("renderers", catalogService.getAllRenderers());
        return result;
    }

    /**
     * Returns renderers grouped by category.
     */
    @GetMapping("/grouped")
    public Map<String, List<RendererInfo>> getGrouped() {
        return catalogService.getRenderersByCategory();
    }

    /**
     * Returns category statistics only.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", catalogService.getTotalCount());
        result.put("byCategory", catalogService.getCategoryStats());
        return result;
    }
}
