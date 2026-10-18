package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Binds chart settings and conditionally registers the chart Actuator endpoint.
 *
 * <p>{@link ChartsProperties} receives values under {@code arbercharts}. This
 * configuration does not apply its theme, export or performance settings to a
 * renderer, create an export service, or configure export security.
 *
 * <p>The endpoint requires Spring Boot Actuator and endpoint availability. It
 * exposes manually recorded statistics and fixed placeholders; see
 * {@link ChartActuatorEndpoint} for the measurement limits. The separate
 * {@link ArberChartsAutoConfiguration} creates the server rendering service
 * configured by {@link ArberChartsProperties}.
 *
 * <p>Example endpoint exposure in application.yml:
 * <pre>
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
     * Creates the endpoint for manually recorded chart statistics.
     *
     * <p>Only activated if Spring Boot Actuator is on the classpath, the endpoint
     * is available, and no replacement endpoint bean has been registered.
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

