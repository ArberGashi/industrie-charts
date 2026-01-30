package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpatialIntegrityTest {

    @Test
    void precisionGate_passesForOrthographic() {
        SpatialProjector projector = new OrthographicProjector()
                .setCenterX(400.0)
                .setCenterY(300.0)
                .setScale(1.0);
        assertDoesNotThrow(() -> SpatialValidator.validatePrecisionGate(projector, 2048));
    }

    @Test
    void precisionGate_passesForPerspective() {
        SpatialProjector projector = new PerspectiveProjector()
                .setCenterX(400.0)
                .setCenterY(300.0)
                .setScale(1.0)
                .setZBias(1.0);
        assertDoesNotThrow(() -> SpatialValidator.validatePrecisionGate(projector, 2048));
    }

    @Test
    void formattedReport_includesStatus() {
        SpatialProjector projector = new OrthographicProjector()
                .setCenterX(400.0)
                .setCenterY(300.0)
                .setScale(1.0);
        String report = SpatialValidator.getFormattedValidationReport("sanity", projector, 256);
        assertTrue(report.contains("status=PASS"));
        assertTrue(report.contains("scenario=sanity"));
    }
}
