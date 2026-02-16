package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.dto.RenderRequest;
import com.arbergashi.charts.visualverifier.dto.TooltipResponse;
import com.arbergashi.charts.visualverifier.service.ChartRenderService;
import com.arbergashi.charts.visualverifier.service.TooltipService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for chart rendering and tooltip data.
 *
 * <p>Renders any of the 158 ArberCharts renderers to PNG with configurable
 * dimensions and themes. Returns performance metrics in response headers.
 * Also provides tooltip data for chart interactions.
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
@RestController
public class RenderController {

    private final ChartRenderService renderService;
    private final TooltipService tooltipService;

    public RenderController(ChartRenderService renderService, TooltipService tooltipService) {
        this.renderService = renderService;
        this.tooltipService = tooltipService;
    }

    /**
     * Renders a chart to PNG using path variable.
     *
     * @param renderer fully qualified renderer class name
     * @param width    chart width (default 800, max 4096)
     * @param height   chart height (default 600, max 4096)
     * @param theme    theme name: light or dark (default light)
     * @return PNG image with X-Render-Time-Ms header
     */
    @GetMapping(value = "/api/render/{renderer}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> render(
            @PathVariable String renderer,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "light") String theme
    ) {
        return doRender(renderer, width, height, theme);
    }

    /**
     * Renders a chart to PNG using query parameter (for web UI compatibility).
     *
     * @param className fully qualified renderer class name
     * @param width     chart width (default 800, max 4096)
     * @param height    chart height (default 600, max 4096)
     * @param theme     theme name: light or dark (default light)
     * @return PNG image with X-Render-Time-Ms header
     */
    @GetMapping(value = "/api/renderer", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> renderByQuery(
            @RequestParam String className,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "light") String theme
    ) {
        return doRender(className, width, height, theme);
    }

    /**
     * Returns tooltip data for a chart at specific coordinates.
     *
     * <p>Generates contextual tooltip information based on the renderer type
     * and mouse position. Uses ZERO-GC compliant formatting.</p>
     *
     * @param className renderer class name
     * @param width     chart width
     * @param height    chart height
     * @param x         mouse x coordinate
     * @param y         mouse y coordinate
     * @return tooltip response with display content
     */
    @GetMapping(value = "/api/tooltip", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TooltipResponse> tooltip(
            @RequestParam String className,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "0") int x,
            @RequestParam(defaultValue = "0") int y
    ) {
        TooltipResponse response = tooltipService.generateTooltip(className, width, height, x, y);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<byte[]> doRender(String renderer, int width, int height, String theme) {
        long start = System.nanoTime();
        RenderRequest request = new RenderRequest(renderer, width, height, theme);
        byte[] png = renderService.render(request);
        long renderTimeMs = (System.nanoTime() - start) / 1_000_000;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Render-Time-Ms", String.valueOf(renderTimeMs));
        headers.set("X-Renderer", renderer);
        headers.set("X-Dimensions", width + "x" + height);
        headers.set("Cache-Control", "public, max-age=300");

        return ResponseEntity.ok().headers(headers).body(png);
    }
}
