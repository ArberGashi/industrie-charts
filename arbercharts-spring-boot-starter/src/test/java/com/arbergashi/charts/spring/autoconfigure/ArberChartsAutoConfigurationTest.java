package com.arbergashi.charts.spring.autoconfigure;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                    assertNull(context.getStartupFailure());
                    ServerRenderService service = context.getBean(ServerRenderService.class);
                    assertEquals(0, service.getPoolIdle());
                    CountDownLatch entered = new CountDownLatch(4);
                    CountDownLatch release = new CountDownLatch(1);
                    ExecutorService executor = Executors.newFixedThreadPool(4);
                    try {
                        List<Future<BufferedImage>> results = new ArrayList<>();
                        for (int i = 0; i < 4; i++) {
                            results.add(executor.submit(() -> render(service, canvas -> {
                                entered.countDown();
                                await(release);
                                canvas.fillRect(0, 0, 32, 32);
                            })));
                        }
                        assertTrue(entered.await(5, TimeUnit.SECONDS), "All four renders must hold a session");
                        assertEquals(4, service.getPoolActive());
                        assertEquals(0, service.getPoolIdle());
                        release.countDown();
                        for (Future<BufferedImage> result : results) {
                            assertEquals(0xFF000000, result.get(5, TimeUnit.SECONDS).getRGB(8, 8));
                        }
                        assertEquals(0, service.getPoolActive());
                        assertEquals(3, service.getPoolIdle(), "Only the configured three sessions may remain idle");
                    } finally {
                        release.countDown();
                        executor.shutdownNow();
                        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void appliesMetricsSettingToSuccessfulAndFailedRenders(boolean enabled) {
        contextRunner.withPropertyValues("arbercharts.server.metrics-enabled=" + enabled)
                .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    ServerRenderService service = context.getBean(ServerRenderService.class);
                    SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
                    BufferedImage image = render(service, canvas -> canvas.fillRect(0, 0, 32, 32));
                    assertEquals(0xFF000000, image.getRGB(8, 8));
                    IllegalArgumentException failure = new IllegalArgumentException("Renderer failed");
                    assertSame(failure, assertThrows(IllegalArgumentException.class,
                            () -> render(service, canvas -> {
                                throw failure;
                            })));
                    double expectedCount = enabled ? 1.0 : 0.0;
                    assertEquals(expectedCount,
                            registry.get("arber.charts.render.count").tag("result", "success").counter().count());
                    assertEquals(expectedCount,
                            registry.get("arber.charts.render.count").tag("result", "failure").counter().count());
                    assertEquals(enabled ? 1L : 0L, registry.get("arber.charts.render.time").timer().count());
                    assertEquals(0, service.getPoolActive());
                    assertEquals(1, service.getPoolIdle());
                });
    }

    @Test
    void closesServerRenderServiceWhenTheApplicationContextCloses() {
        AtomicReference<ServerRenderService> captured = new AtomicReference<>();
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());
            ServerRenderService service = context.getBean(ServerRenderService.class);
            captured.set(service);
            render(service, canvas -> canvas.fillRect(0, 0, 32, 32));
            assertEquals(1, service.getPoolIdle());
        });
        ServerRenderService service = captured.get();
        assertNotNull(service);
        try {
            assertEquals(0, service.getPoolActive());
            assertEquals(0, service.getPoolIdle());
            assertThrows(IllegalStateException.class,
                    () -> render(service, canvas -> canvas.fillRect(0, 0, 32, 32)));
        } finally {
            service.close();
        }
    }

    @Test
    void preservesUserServiceWithTheDefaultBeanName() {
        contextRunner.withUserConfiguration(DefaultNameServiceConfiguration.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(ServerRenderService.class).size());
                    assertSame(context.getBean("serverRenderService"), context.getBean(ServerRenderService.class));
                });
    }

    @Test
    void preservesUserServiceWithADifferentBeanName() {
        contextRunner.withUserConfiguration(CustomNameServiceConfiguration.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(ServerRenderService.class).size());
                    assertSame(context.getBean("customRenderService"), context.getBean(ServerRenderService.class));
                });
    }

    private static BufferedImage render(ServerRenderService service, Consumer<ArberCanvas> draw) {
        DefaultChartModel model = new DefaultChartModel("auto-configuration");
        model.setXY(0, 0);
        model.setXY(1, 1);
        return service.renderToImage(model, new Dimension(32, 32), null,
                new BaseRenderer("auto-configuration-test") {
                    @Override
                    protected void drawData(ArberCanvas canvas, ChartModel data, PlotContext context) {
                        draw.accept(canvas);
                    }
                });
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Render release timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Render interrupted", e);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DefaultNameServiceConfiguration {
        @Bean
        ServerRenderService serverRenderService() {
            return new ServerRenderService(2, false, null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomNameServiceConfiguration {
        @Bean
        ServerRenderService customRenderService() {
            return new ServerRenderService(2, false, null);
        }
    }
}
