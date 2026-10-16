package com.arbergashi.charts.springdemo;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.model.StatisticalChartModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChartWorkbenchHttpTest {
    @LocalServerPort
    private int port;

    @Test
    void servesWorkbenchAndCatalogFromTheRunningApplication() throws Exception {
        try (HttpClient client = client()) {
            HttpResponse<byte[]> index = get(client, "/");
            assertEquals(200, index.statusCode());
            assertTrue(text(index).contains("Charts workbench"));
            assertTrue(index.headers().firstValue("Content-Security-Policy").orElseThrow().contains("script-src 'self'"));
            assertEquals(200, get(client, "/workbench.js").statusCode());
            assertEquals(200, get(client, "/workbench.css").statusCode());
            String catalog = text(get(client, "/api/examples"));
            for (String id : List.of("line", "area", "step", "bar", "scatter", "candlestick", "donut", "radar",
                    "heatmap", "boxplot", "kde")) {
                assertTrue(catalog.contains("\"id\":\"" + id + "\""), id);
            }
            assertTrue(catalog.contains(SpringBootVersion.getVersion()));
            assertTrue(catalog.contains("Synthetic demo data"));
            assertEquals(200, get(client, "/actuator/health").statusCode());
            assertEquals(404, get(client, "/actuator/env").statusCode());
        }
    }

    @ParameterizedTest
    @CsvSource({"line,dark", "line,light", "area,dark", "area,light", "step,dark", "step,light",
            "bar,dark", "bar,light", "scatter,dark", "scatter,light", "candlestick,dark", "candlestick,light",
            "donut,dark", "donut,light", "radar,dark", "radar,light", "heatmap,dark", "heatmap,light",
            "boxplot,dark", "boxplot,light", "kde,dark", "kde,light"})
    void everyAdvertisedExampleProducesVisibleChartPixels(String id, String theme) throws Exception {
        try (HttpClient client = client()) {
            HttpResponse<byte[]> response = get(client,
                    "/api/charts/" + id + ".png?width=640&height=360&samples=48&theme=" + theme);
            assertEquals(200, response.statusCode(), () -> text(response));
            assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("image/png"));
            assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
            BufferedImage image = decode(response);
            assertEquals(640, image.getWidth());
            assertEquals(360, image.getHeight());
            assertEquals(theme.equals("dark") ? 0xFF1E1F22 : 0xFFFFFFFF, image.getRGB(0, 0));
            assertTrue(coloredPixels(image, 65, 29, 611, 295) > 100,
                    "Expected visible renderer output, not just axes and a background.");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"width=0", "width=2561", "height=239", "height=1441", "samples=15",
            "samples=2049", "samples=abc", "theme=unknown", "scale=0", "scale=3", "width=640&height=360&scale=2"})
    void rejectsInvalidOrExcessiveRequestsBeforeRendering(String query) throws Exception {
        try (HttpClient client = client()) {
            HttpResponse<byte[]> response = get(client, "/api/charts/line.png?" + query);
            assertEquals(400, response.statusCode());
            assertFalse(response.headers().firstValue("Content-Type").orElse("").startsWith("image/png"));
        }
    }

    @Test
    void rejectsUnknownExamples() throws Exception {
        try (HttpClient client = client()) {
            assertEquals(404, get(client, "/api/charts/not-a-renderer.png").statusCode());
        }
    }

    @Test
    void exportsRequestedDimensionsAndAttachmentName() throws Exception {
        try (HttpClient client = client()) {
            HttpResponse<byte[]> response = get(client,
                    "/api/charts/scatter.png?width=1280&height=720&samples=96&theme=light&scale=2&download=true");
            assertEquals(200, response.statusCode());
            assertEquals("attachment; filename=\"charts-scatter-light.png\"",
                    response.headers().firstValue("Content-Disposition").orElseThrow());
            BufferedImage image = decode(response);
            assertEquals(1280, image.getWidth());
            assertEquals(720, image.getHeight());
        }
    }

    @Test
    void concurrentRendersKeepRequestSpecificStateAndRemainDeterministic() throws Exception {
        try (HttpClient client = client()) {
            List<String> paths = List.of(
                    "/api/charts/line.png?width=640&height=360&samples=96&theme=dark",
                    "/api/charts/donut.png?width=720&height=480&samples=48&theme=light",
                    "/api/charts/heatmap.png?width=800&height=400&samples=256&theme=dark",
                    "/api/charts/kde.png?width=960&height=540&samples=96&theme=light");
            List<byte[]> expected = paths.stream().map(path -> {
                try {
                    return get(client, path).body();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
            List<CompletableFuture<HttpResponse<byte[]>>> pending = paths.stream()
                    .map(path -> client.sendAsync(request(path), HttpResponse.BodyHandlers.ofByteArray())).toList();
            for (int i = 0; i < pending.size(); i++) {
                HttpResponse<byte[]> response = pending.get(i).get();
                assertEquals(200, response.statusCode());
                assertArrayEquals(expected.get(i), response.body());
            }
        }
    }


    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void donutPreservesCategorySharesColorsAndItsEmptyCenter(String theme) throws Exception {
        try (HttpClient client = client()) {
            BufferedImage image = decode(get(client,
                    "/api/charts/donut.png?width=640&height=360&samples=100&theme=" + theme));
            int[] colors = theme.equals("dark")
                    ? new int[]{0xFF75A6FF, 0xFF62C9BC, 0xFFF0B96A, 0xFFB49BEA, 0xFFED8F9C}
                    : new int[]{0xFF3574D8, 0xFF168579, 0xFFD08722, 0xFF8061B9, 0xFFC55572};
            double[] shares = {0.34, 0.26, 0.18, 0.14, 0.08};
            double accumulated = 0;
            for (int i = 0; i < shares.length; i++) {
                double angle = Math.PI / 2 - (accumulated + shares[i] / 2) * 2 * Math.PI;
                int x = (int) Math.round(320 + Math.cos(angle) * 90);
                int y = (int) Math.round(180 - Math.sin(angle) * 90);
                assertEquals(colors[i], image.getRGB(x, y), "Sector " + i + " must represent its category share.");
                accumulated += shares[i];
            }
            int background = theme.equals("dark") ? 0xFF1E1F22 : 0xFFFFFFFF;
            assertEquals(background, image.getRGB(320, 145), "The hole must remain clear above the center text.");
            assertEquals(background, image.getRGB(64, 28), "A donut must not acquire a Cartesian grid.");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"dark", "light"})
    void radarContainsAFilledProfileAcrossItsCenterWithoutCartesianAxes(String theme) throws Exception {
        try (HttpClient client = client()) {
            BufferedImage image = decode(get(client,
                    "/api/charts/radar.png?width=800&height=480&samples=96&theme=" + theme));
            int accent = theme.equals("dark") ? 0xFF75A6FF : 0xFF3574D8;
            int background = theme.equals("dark") ? 0xFF1E1F22 : 0xFFFFFFFF;
            int expectedFill = composite(accent, background, 63);
            int filledPixels = 0;
            for (int y = 252; y < 272; y++) {
                for (int x = 380; x < 420; x++) {
                    if (colorWithin(image.getRGB(x, y), expectedFill, 1)) filledPixels++;
                }
            }
            assertTrue(filledPixels > 700,
                    "The center must contain the 25% profile fill; neutral grid lines alone must not pass.");
            assertEquals(theme.equals("dark") ? 0xFF1E1F22 : 0xFFFFFFFF, image.getRGB(64, 32));
        }
    }

    @Test
    void heatmapPreservesTheFieldOrientationAndExportsItsQuantitativeColorScale() throws Exception {
        try (HttpClient client = client()) {
            BufferedImage image = decode(get(client,
                    "/api/charts/heatmap.png?width=640&height=360&samples=1024&theme=light"));
            // The plot spans x=64..528 and y=28..296; data Y increases upwards.
            int lowPeak = image.getRGB(64 + (int) (3.0 / 10 * 464), 296 - (int) (2.0 / 6 * 268));
            int highPeak = image.getRGB(64 + (int) (7.0 / 10 * 464), 296 - (int) (4.2 / 6 * 268));
            int aboveLowPeak = image.getRGB(64 + (int) (3.0 / 10 * 464), 296 - (int) (4.7 / 6 * 268));
            int belowHighPeak = image.getRGB(64 + (int) (7.0 / 10 * 464), 296 - (int) (1.0 / 6 * 268));
            assertTrue(luminance(lowPeak) > luminance(aboveLowPeak) + 80);
            assertTrue(luminance(highPeak) > luminance(belowHighPeak) + 60);
            assertEquals(0xFFF4DC79, image.getRGB(550, 28), "The top of the scale denotes the maximum response.");
            assertEquals(0xFF293B86, image.getRGB(550, 295), "The bottom of the scale denotes the minimum response.");
        }
    }

    @Test
    void boxplotExportsAllSixMedianMarksAtTheirMeasuredValues() throws Exception {
        DemoExamples.Dataset data = DemoExamples.find("boxplot").create(96, new ArberColor(0xFF3574D8));
        StatisticalChartModel model = (StatisticalChartModel) data.model();
        try (HttpClient client = client()) {
            BufferedImage image = decode(get(client,
                    "/api/charts/boxplot.png?width=960&height=540&samples=96&theme=light"));
            for (int i = 0; i < model.getPointCount(); i++) {
                int x = (int) Math.round(64 + (model.getX(i) - 0.5) / 6 * 868) + 8;
                int y = (int) Math.round(28 + (1 - model.getMedian(i) / 100) * 448);
                assertTrue(hasColorNear(image, x, y, 0xFF252A34), "Missing median for batch " + model.getLabel(i));
            }
        }
    }

    @Test
    void densityCurveMatchesIndependentGaussianEvaluationAtBothPopulationPeaks() throws Exception {
        DemoExamples.Dataset data = DemoExamples.find("kde").create(256, new ArberColor(0xFF3574D8));
        try (HttpClient client = client()) {
            BufferedImage image = decode(get(client,
                    "/api/charts/kde.png?width=960&height=540&samples=256&theme=light"));
            for (double observation : new double[]{2.8, 5.1}) {
                double density = 0;
                for (int i = 0; i < data.model().getPointCount(); i++) {
                    double z = (observation - data.model().getY(i)) / 0.18;
                    density += Math.exp(-0.5 * z * z) / (0.18 * Math.sqrt(2 * Math.PI) * 256);
                }
                int x = (int) Math.round(64 + (observation - data.minX()) / (data.maxX() - data.minX()) * 868);
                int expectedTop = (int) Math.round(28 + (1 - density / data.maxY()) * 448);
                int actualTop = 476;
                for (int y = 28; y < 476; y++) {
                    if (coloredPixels(image, x - 1, y, x + 2, y + 1) > 0) {
                        actualTop = y;
                        break;
                    }
                }
                assertTrue(Math.abs(expectedTop - actualTop) <= 5,
                        "Density at " + observation + " must match the Gaussian sum: expected y="
                                + expectedTop + ", actual y=" + actualTop);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"donut,16", "donut,2048", "radar,16", "radar,2048", "heatmap,16", "heatmap,2048",
            "boxplot,16", "boxplot,2048", "kde,16", "kde,2048"})
    void newExamplesSupportTheSmallestDensePreviewAndBothInputLimits(String id, int samples) throws Exception {
        try (HttpClient client = client()) {
            HttpResponse<byte[]> response = get(client, "/api/charts/" + id
                    + ".png?width=640&height=480&scale=2&theme=light&samples=" + samples);
            assertEquals(200, response.statusCode(), () -> text(response));
            BufferedImage image = decode(response);
            assertEquals(640, image.getWidth());
            assertEquals(480, image.getHeight());
            assertTrue(coloredPixels(image, 128, 56, 520, 350) > 100);
        }
    }

    private static int coloredPixels(BufferedImage image, int left, int top, int right, int bottom) {
        int count = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 255;
                int green = (rgb >>> 8) & 255;
                int blue = rgb & 255;
                if (Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) > 45) count++;
            }
        }
        return count;
    }

    private static boolean hasColorNear(BufferedImage image, int x, int y, int color) {
        for (int yy = y - 2; yy <= y + 2; yy++) {
            for (int xx = x - 1; xx <= x + 1; xx++) {
                if (image.getRGB(xx, yy) == color) return true;
            }
        }
        return false;
    }

    private static int composite(int foreground, int background, int alpha) {
        int color = 0xFF000000;
        for (int shift : new int[]{16, 8, 0}) {
            int source = (foreground >>> shift) & 255;
            int destination = (background >>> shift) & 255;
            color |= (int) Math.round((source * alpha + destination * (255 - alpha)) / 255.0) << shift;
        }
        return color;
    }

    private static boolean colorWithin(int actual, int expected, int tolerance) {
        // Java2D's intermediate premultiplied image may round a channel by one unit.
        for (int shift : new int[]{16, 8, 0}) {
            if (Math.abs(((actual >>> shift) & 255) - ((expected >>> shift) & 255)) > tolerance) return false;
        }
        return true;
    }

    private static double luminance(int color) {
        return 0.2126 * ((color >>> 16) & 255) + 0.7152 * ((color >>> 8) & 255) + 0.0722 * (color & 255);
    }

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).GET().build();
    }

    private HttpResponse<byte[]> get(HttpClient client, String path) throws Exception {
        return client.send(request(path), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    private static String text(HttpResponse<byte[]> response) {
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private static BufferedImage decode(HttpResponse<byte[]> response) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        assertNotNull(image, "The response must be a decodable PNG.");
        return image;
    }
}
