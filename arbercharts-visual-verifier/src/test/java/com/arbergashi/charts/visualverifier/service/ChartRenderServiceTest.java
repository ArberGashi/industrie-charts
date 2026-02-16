package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.visualverifier.dto.RenderRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartRenderServiceTest {

    @Test
    void rejectsRendererOutsideCatalog() {
        ChartRenderService service = new ChartRenderService(null, new RendererCatalogService());
        RenderRequest request = new RenderRequest("java.lang.String", 800, 600, "light");

        assertThrows(RuntimeException.class, () -> service.render(request));
    }
}
