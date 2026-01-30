package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.bridge.server.ServerRenderService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ServerRenderService.class)
@EnableConfigurationProperties(ArberChartsProperties.class)
public class ArberChartsAutoConfiguration {

    @Bean
    public ServerRenderService serverRenderService(ArberChartsProperties properties,
                                                   ObjectProvider<MeterRegistry> registryProvider) {
        MeterRegistry registry = registryProvider.getIfAvailable();
        return new ServerRenderService(properties.getPoolSize(), properties.isMetricsEnabled(), registry);
    }
}
