package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.visualverifier.RendererDemoDataFactory;
import com.arbergashi.charts.visualverifier.RendererCatalogEntry;
import com.arbergashi.charts.visualverifier.dto.RenderRequest;
import org.springframework.stereotype.Service;

import java.awt.Dimension;

/**
 * Service for rendering charts to images.
 *
 * <p>Uses ArberCharts Server Bridge for headless PNG rendering.
 * Supports all 158 renderers with automatic demo data generation.
 *
 * @since 2.0.0
 */
@Service
public class ChartRenderService {

    private final ServerRenderService serverRenderService;
    private final RendererCatalogService catalogService;

    public ChartRenderService(ServerRenderService serverRenderService, RendererCatalogService catalogService) {
        this.serverRenderService = serverRenderService;
        this.catalogService = catalogService;
    }

    /**
     * Renders a chart to PNG bytes.
     *
     * @param request render request with renderer, dimensions, theme
     * @return PNG image bytes
     * @throws RuntimeException if rendering fails
     */
    public byte[] render(RenderRequest request) {
        try {
            RendererCatalogEntry catalogEntry = catalogService.getRenderer(request.renderer());
            ChartRenderer renderer = instantiateRenderer(catalogEntry.className());
            ChartModel model = buildDemoData(catalogEntry.category(), catalogEntry.className());
            ChartTheme theme = resolveTheme(request.theme());
            Dimension size = new Dimension(request.width(), request.height());

            return serverRenderService.renderToPng(model, size, theme, renderer);
        } catch (Exception e) {
            throw new RuntimeException("Render failed for " + request.renderer() + ": " + e.getMessage(), e);
        }
    }

    private ChartRenderer instantiateRenderer(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!ChartRenderer.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class is not a ChartRenderer: " + className);
            }
            return (ChartRenderer) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate renderer: " + className, e);
        }
    }

    private ChartTheme resolveTheme(String themeName) {
        return "dark".equalsIgnoreCase(themeName)
                ? ChartThemes.getDarkTheme()
                : ChartThemes.getLightTheme();
    }

    private ChartModel buildDemoData(String category, String rendererClass) {
        try {
            return RendererDemoDataFactory.build(category, rendererClass);
        } catch (Exception e) {
            // Fallback to simple circular model
            return new com.arbergashi.charts.model.CircularChartModel(1000);
        }
    }
}
