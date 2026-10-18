package com.arbergashi.charts.spring.autoconfigure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoConfigurationImportsTest {

    @Test
    void importsContainAllAutoConfigurations() throws IOException {
        String path = "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Missing auto-configuration imports resource");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("com.arbergashi.charts.spring.autoconfigure.ArberChartsAutoConfiguration"));
            assertTrue(text.contains("com.arbergashi.charts.spring.autoconfigure.ChartAutoConfiguration"));
        }
    }

    @Test
    void generatedMetadataDescribesServerAndNestedApplicationProperties() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/META-INF/spring-configuration-metadata.json")) {
            assertNotNull(input, "The starter must include generated configuration metadata for IDE completion");
            List<?> entries = JsonPath.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                    .read("$.properties");
            assertNotNull(entries);
            Map<String, Map<?, ?>> properties = entries.stream()
                    .map(entry -> (Map<?, ?>) entry)
                    .collect(Collectors.toMap(entry -> (String) entry.get("name"), entry -> entry));
            assertEquals(Set.of("arbercharts.server.pool-size", "arbercharts.server.metrics-enabled",
                    "arbercharts.theme", "arbercharts.export.enabled", "arbercharts.export.formats",
                    "arbercharts.export.directory", "arbercharts.performance.virtual-threads",
                    "arbercharts.performance.max-concurrent-renders", "management.endpoint.charts.access",
                    "management.endpoint.charts.cache.time-to-live"), properties.keySet());
            assertEquals("java.lang.Integer", properties.get("arbercharts.server.pool-size").get("type"));
            assertEquals(ArberChartsProperties.class.getName(),
                    properties.get("arbercharts.server.pool-size").get("sourceType"));
            assertEquals(true, properties.get("arbercharts.server.metrics-enabled").get("defaultValue"));
            assertEquals("dark", properties.get("arbercharts.theme").get("defaultValue"));
            assertNull(properties.get("arbercharts.export.directory").get("defaultValue"),
                    "The JVM temporary directory is resolved at runtime; java.io.tmpdir is not a literal path");
            assertEquals("org.springframework.boot.actuate.endpoint.Access",
                    properties.get("management.endpoint.charts.access").get("type"));
            assertEquals("unrestricted", properties.get("management.endpoint.charts.access").get("defaultValue"));
            assertEquals("java.time.Duration",
                    properties.get("management.endpoint.charts.cache.time-to-live").get("type"));
            assertEquals("0ms", properties.get("management.endpoint.charts.cache.time-to-live").get("defaultValue"));
            assertEquals("com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint",
                    properties.get("management.endpoint.charts.access").get("sourceType"));
        }
    }
}
