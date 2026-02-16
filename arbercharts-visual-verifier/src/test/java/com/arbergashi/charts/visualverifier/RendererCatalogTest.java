package com.arbergashi.charts.visualverifier;

import com.arbergashi.charts.visualverifier.service.RendererCatalogService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the renderer catalog is available and usable.
 */
public class RendererCatalogTest {
    @Test
    void catalogLoadsWithEntries() {
        RendererCatalogService service = new RendererCatalogService();
        assertTrue(service.getTotalCount() > 0, "Expected at least one renderer in catalog");
        assertTrue(service.getCategoryStats().size() > 0, "Expected at least one category");
    }

    @Test
    void knownRendererIsResolvable() {
        RendererCatalogService service = new RendererCatalogService();
        assertNotNull(service.getRenderer("com.arbergashi.charts.render.standard.LineRenderer"));
    }

    @Test
    void incompatibleCatalogEntryIsRejected() {
        RendererCatalogService service = new RendererCatalogService();
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRenderer("com.arbergashi.charts.render.common.PerformanceAuditRenderer")
        );
    }
}
