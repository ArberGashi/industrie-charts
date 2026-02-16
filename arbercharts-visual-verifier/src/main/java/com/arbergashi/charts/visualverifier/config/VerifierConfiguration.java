package com.arbergashi.charts.visualverifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ArberCharts Visual Verifier.
 *
 * <p>Provides beans and configuration properties for the visual testing platform.
 * The {@code ServerRenderService} is automatically provided by the ArberCharts
 * Spring Boot Starter via {@code ArberChartsAutoConfiguration}.
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
@Configuration
public class VerifierConfiguration {


    /**
     * Creates the verifier properties bean.
     *
     * @return verifier properties
     */
    @Bean
    @ConfigurationProperties(prefix = "arbercharts.verifier")
    public VerifierProperties verifierProperties() {
        return new VerifierProperties();
    }
}

