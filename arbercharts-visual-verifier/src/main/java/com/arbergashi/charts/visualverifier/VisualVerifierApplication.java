package com.arbergashi.charts.visualverifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ArberCharts Visual Verifier - Spring Boot Application.
 *
 * <p>Server-side testing platform for all 157 ArberCharts renderers.
 * Provides REST APIs for rendering, benchmarking, and visual regression testing.
 *
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>{@code GET /} - Web UI for browsing renderers</li>
 *   <li>{@code GET /api/render} - Render chart to PNG/SVG</li>
 *   <li>{@code GET /api/renderers} - List all renderers by category</li>
 *   <li>{@code GET /api/benchmark} - Performance benchmark</li>
 *   <li>{@code GET /actuator/health} - Health check</li>
 *   <li>{@code GET /actuator/metrics} - Metrics endpoint</li>
 * </ul>
 *
 * <p><strong>License:</strong> ArberCharts binaries are licensed under the MIT License.
 * See {@code META-INF/LICENSE} for full license text.
 *
 * @since 2.0.0
 * @author Arber Gashi
 */
@SpringBootApplication
public class VisualVerifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisualVerifierApplication.class, args);
    }
}
