package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-Configuration for ArberCharts in Spring Boot applications.
 *
 * <p>This configuration is automatically activated when ArberCharts is on the classpath.
 * It provides:
 * <ul>
 *   <li>Theme configuration from application.yml</li>
 *   <li>Export service with file management</li>
 *   <li>Actuator endpoints for monitoring (when actuator is present)</li>
 *   <li>Security integration for protected exports (when security is present)</li>
 * </ul>
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * arbercharts:
 *   theme: dark
 *   export:
 *     enabled: true
 *     formats: [png, svg, pdf]
 *     directory: /tmp/charts
 *   performance:
 *     virtual-threads: true
 *     max-concurrent-renders: 10
 *
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,charts
 * </pre>
 *
 * @since 2.0.0
 * @see ChartsProperties
 * @see ChartActuatorEndpoint
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.arbergashi.charts.model.ChartModel")
@EnableConfigurationProperties(ChartsProperties.class)
public class ChartAutoConfiguration {

    /**
     * Creates the Actuator endpoint for chart monitoring.
     *
     * <p>Only activated if Spring Boot Actuator is on the classpath
     * and the endpoint is enabled in configuration.
     *
     * <p>Access at: {@code /actuator/charts}
     *
     * @return the actuator endpoint
     * @since 2.0.0
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnAvailableEndpoint
    @ConditionalOnMissingBean
    public ChartActuatorEndpoint chartActuatorEndpoint() {
        return new ChartActuatorEndpoint();
    }
}

