package com.arbergashi.charts.spring.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
