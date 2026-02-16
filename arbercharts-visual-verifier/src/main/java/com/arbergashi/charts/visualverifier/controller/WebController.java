package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.dto.RendererInfo;
import com.arbergashi.charts.visualverifier.service.RendererCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web UI controller for renderer catalog browsing.
 *
 * <p>Serves the Thymeleaf-based web interface for visually browsing
 * and testing all 157 renderers with animations and marketing colors.
 *
 * @since 2.0.0
 */
@Controller
public class WebController {

    private final RendererCatalogService catalogService;

    public WebController(RendererCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Main page - renderer catalog.
     */
    @GetMapping("/")
    public String index(Model model) {
        return renderers(model);
    }

    /**
     * Renderer catalog page with full marketing presentation.
     */
    @GetMapping("/renderers")
    public String renderers(Model model) {
        Map<String, List<RendererInfo>> groups = catalogService.getRenderersByCategory();
        int total = catalogService.getTotalCount();

        // Category colors for marketing presentation
        Map<String, String> categoryColors = getCategoryColors();
        Map<String, String> categoryIcons = getCategoryIcons();

        model.addAttribute("groups", groups);
        model.addAttribute("totalCount", total);
        model.addAttribute("categoryColors", categoryColors);
        model.addAttribute("categoryIcons", categoryIcons);
        model.addAttribute("vectorAvailable", isVectorApiAvailable());
        model.addAttribute("animationCapable", true);

        return "renderers";
    }

    /**
     * Returns marketing colors for each category.
     */
    private Map<String, String> getCategoryColors() {
        Map<String, String> colors = new HashMap<>();
        colors.put("Standard", "#3B82F6");        // Blue
        colors.put("Financial", "#10B981");       // Green
        colors.put("Statistical", "#8B5CF6");     // Purple
        colors.put("Specialized", "#F59E0B");     // Amber
        colors.put("Medical", "#EF4444");         // Red
        colors.put("Circular", "#EC4899");        // Pink
        colors.put("Forensic", "#6366F1");        // Indigo
        colors.put("Predictive", "#F97316");      // Orange
        colors.put("Analysis", "#14B8A6");        // Teal
        colors.put("Security", "#A855F7");        // Violet
        colors.put("Common", "#06B6D4");          // Cyan
        return colors;
    }

    /**
     * Returns icons for each category.
     */
    private Map<String, String> getCategoryIcons() {
        Map<String, String> icons = new HashMap<>();
        icons.put("Standard", "📊");
        icons.put("Financial", "💰");
        icons.put("Statistical", "📈");
        icons.put("Specialized", "🎯");
        icons.put("Medical", "🏥");
        icons.put("Circular", "⭕");
        icons.put("Forensic", "🔍");
        icons.put("Predictive", "🔮");
        icons.put("Analysis", "🔬");
        icons.put("Security", "🔒");
        icons.put("Common", "⚡");
        return icons;
    }

    private boolean isVectorApiAvailable() {
        try {
            Class.forName("jdk.incubator.vector.FloatVector");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

