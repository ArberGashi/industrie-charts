package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartAutoConfigurationTest {
    // Match SpringApplication's relaxed enum conversion for endpoint access properties.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment()
                    .setConversionService(new ApplicationConversionService()))
            .withConfiguration(AutoConfigurations.of(ChartAutoConfiguration.class))
            .withPropertyValues("arbercharts.theme=light");

    @Test
    void preservesTheDirectEndpointFactoryForExistingCallers() {
        ChartAutoConfiguration configuration = new ChartAutoConfiguration();
        ChartActuatorEndpoint endpoint = configuration.chartActuatorEndpoint();

        assertNotNull(endpoint);
        assertNotSame(endpoint, configuration.chartActuatorEndpoint());
    }

    @Test
    void bindsPropertiesWithoutOptionalActuator() {
        contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate"))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals("light", context.getBean(ChartsProperties.class).getTheme());
                    assertFalse(context.containsBean("chartActuatorEndpoint"));
                });
    }

    @Test
    void bindsPropertiesWithoutActuatorAutoConfiguration() {
        contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate.autoconfigure"))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals("light", context.getBean(ChartsProperties.class).getTheme());
                    assertFalse(context.containsBean("chartActuatorEndpoint"));
                });
    }

    @Test
    void createsEndpointWhenActuatorIsAvailableAndExposed() {
        contextRunner.withPropertyValues("management.endpoints.web.exposure.include=charts")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(ChartActuatorEndpoint.class));
                });
    }

    @Test
    void doesNotCreateEndpointWithoutExposure() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(ChartActuatorEndpoint.class).isEmpty());
        });
    }

    @Test
    void doesNotCreateEndpointWhenAccessIsDeniedDespiteExposure() {
        contextRunner.withPropertyValues(
                        "management.endpoints.web.exposure.include=charts",
                        "management.endpoint.charts.access=none")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(ChartActuatorEndpoint.class).isEmpty());
                });
    }

    @Test
    void preservesUserEndpointWithTheDefaultBeanName() {
        contextRunner.withPropertyValues("management.endpoints.web.exposure.include=charts")
                .withUserConfiguration(DefaultNameEndpointConfiguration.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(ChartActuatorEndpoint.class).size());
                    assertSame(context.getBean("chartActuatorEndpoint"), context.getBean(ChartActuatorEndpoint.class));
                });
    }

    @Test
    void preservesUserEndpointWithADifferentBeanName() {
        contextRunner.withPropertyValues("management.endpoints.web.exposure.include=charts")
                .withUserConfiguration(CustomNameEndpointConfiguration.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(ChartActuatorEndpoint.class).size());
                    assertSame(context.getBean("customChartEndpoint"), context.getBean(ChartActuatorEndpoint.class));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DefaultNameEndpointConfiguration {
        @Bean
        ChartActuatorEndpoint chartActuatorEndpoint() {
            return new ChartActuatorEndpoint();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomNameEndpointConfiguration {
        @Bean
        ChartActuatorEndpoint customChartEndpoint() {
            return new ChartActuatorEndpoint();
        }
    }
}
