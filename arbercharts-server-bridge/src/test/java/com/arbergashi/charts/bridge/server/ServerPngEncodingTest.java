package com.arbergashi.charts.bridge.server;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated("Temporarily changes global ImageIO PNG writer registrations")
@Execution(ExecutionMode.SAME_THREAD)
class ServerPngEncodingTest {
    @Test
    void everyOverloadProducesADecodablePngWithTheRequestedSizeAndPixels() throws Exception {
        try (ServerRenderService service = new ServerRenderService(1)) {
            for (int overload = 0; overload < 3; overload++) {
                byte[] encoded = encode(service, overload);
                assertTrue(encoded.length > 8);
                assertArrayEquals(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10},
                        Arrays.copyOf(encoded, 8));
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded));
                assertNotNull(image);
                assertEquals(67, image.getWidth());
                assertEquals(41, image.getHeight());
                if (overload > 0) {
                    assertEquals(0xFF1248AC, image.getRGB(12, 14));
                    assertEquals(0, image.getRGB(0, 0));
                } else {
                    assertTrue(Arrays.stream(image.getRGB(0, 0, 67, 41, null, 0, 67))
                            .anyMatch(pixel -> (pixel >>> 24) != 0));
                }
            }
        }
    }

    @Test
    void everyOverloadRejectsMissingPngWritersInsteadOfReturningEmptyBytes() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        List<ImageWriterSpi> pngWriters = new ArrayList<>();
        registry.getServiceProviders(ImageWriterSpi.class, true).forEachRemaining(provider -> {
            if (Arrays.stream(provider.getFormatNames()).anyMatch("png"::equalsIgnoreCase)) {
                pngWriters.add(provider);
            }
        });
        assertFalse(pngWriters.isEmpty(), "The test must start with a registered PNG writer");
        try (ServerRenderService service = new ServerRenderService(1)) {
            try {
                pngWriters.forEach(registry::deregisterServiceProvider);
                for (int overload = 0; overload < 3; overload++) {
                    int selected = overload;
                    IllegalStateException error = assertThrows(IllegalStateException.class,
                            () -> encode(service, selected));
                    assertTrue(error.getMessage().contains("no PNG image writer"));
                    assertEquals(0, service.getPoolActive());
                    assertEquals(1, service.getPoolIdle());
                }
            } finally {
                pngWriters.forEach(registry::registerServiceProvider);
            }
        }
    }

    private static byte[] encode(ServerRenderService service, int overload) {
        DefaultChartModel model = new DefaultChartModel("png");
        model.setXY(0, 1);
        model.setXY(1, 2);
        Dimension size = new Dimension(67, 41);
        BaseRenderer renderer = new BaseRenderer("png-test") {
            @Override
            protected void drawData(ArberCanvas canvas, ChartModel ignored, PlotContext context) {
                canvas.setColor(new ArberColor(0xFF1248AC));
                canvas.fillRect(10, 10, 20, 20);
            }
        };
        return switch (overload) {
            case 0 -> service.renderToPng(model, size);
            case 1 -> service.renderToPng(model, size, null, renderer);
            case 2 -> service.renderToPng(model, size, null, renderer, 0, 3);
            default -> throw new IllegalArgumentException("Unknown overload");
        };
    }
}
