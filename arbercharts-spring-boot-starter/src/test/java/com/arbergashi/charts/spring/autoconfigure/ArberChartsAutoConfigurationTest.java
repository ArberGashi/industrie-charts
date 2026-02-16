package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.bridge.server.ServerRenderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArberChartsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArberChartsAutoConfiguration.class));

    @Test
    void createsServerRenderServiceBeanWithConfiguredPoolSize() {
        contextRunner
                .withPropertyValues(
                        "arbercharts.server.pool-size=3",
                        "arbercharts.server.metrics-enabled=false"
                )
                .run(context -> {
                    ServerRenderService service = context.getBean(ServerRenderService.class);
                    assertNotNull(service);
                    assertEquals(0, service.getPoolIdle());
                });
    }
}
