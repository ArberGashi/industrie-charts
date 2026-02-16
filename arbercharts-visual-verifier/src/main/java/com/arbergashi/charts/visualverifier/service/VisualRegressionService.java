package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.visualverifier.config.VerifierProperties;
import com.arbergashi.charts.visualverifier.dto.ComparisonResult;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Service for visual regression testing.
 *
 * <p>Compares rendered charts against baseline snapshots to detect visual regressions.
 * Supports configurable tolerance, diff generation, and snapshot management.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Pixel-by-pixel comparison with configurable tolerance</li>
 *   <li>Diff image generation highlighting differences</li>
 *   <li>Automatic snapshot versioning and retention</li>
 *   <li>Zero-GC friendly image comparison algorithm</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
@Service
public class VisualRegressionService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ChartRenderService renderService;
    private final VerifierProperties properties;

    public VisualRegressionService(ChartRenderService renderService, VerifierProperties properties) {
        this.renderService = renderService;
        this.properties = properties;
    }

    /**
     * Compares a rendered chart against its baseline snapshot.
     *
     * @param rendererClass fully qualified renderer class name
     * @param width         chart width
     * @param height        chart height
     * @param theme         theme name
     * @return comparison result with match percentage and optional diff
     */
    public ComparisonResult compare(String rendererClass, int width, int height, String theme) {
        try {
            // Render current version
            byte[] currentPng = renderService.render(
                    new com.arbergashi.charts.visualverifier.dto.RenderRequest(rendererClass, width, height, theme)
            );

            // Load baseline
            Path baselinePath = getBaselinePath(rendererClass, width, height, theme);
            if (!Files.exists(baselinePath)) {
                // No baseline - save current as baseline
                saveSnapshot(baselinePath, currentPng);
                return ComparisonResult.noBaseline(rendererClass);
            }

            byte[] baselinePng = Files.readAllBytes(baselinePath);

            // Compare images
            BufferedImage current = ImageIO.read(new ByteArrayInputStream(currentPng));
            BufferedImage baseline = ImageIO.read(new ByteArrayInputStream(baselinePng));

            if (current.getWidth() != baseline.getWidth() || current.getHeight() != baseline.getHeight()) {
                return ComparisonResult.sizeMismatch(rendererClass,
                        baseline.getWidth(), baseline.getHeight(),
                        current.getWidth(), current.getHeight());
            }

            // Pixel comparison
            double tolerance = properties.getComparison().getTolerance();
            PixelComparisonResult pixelResult = comparePixels(current, baseline, tolerance);

            byte[] diffPng = null;
            if (!pixelResult.matches && properties.getComparison().isGenerateDiff()) {
                diffPng = generateDiffImage(current, baseline);
            }

            return ComparisonResult.builder()
                    .renderer(rendererClass)
                    .matches(pixelResult.matches)
                    .matchPercentage(pixelResult.matchPercentage)
                    .totalPixels(pixelResult.totalPixels)
                    .diffPixels(pixelResult.diffPixels)
                    .diffImage(diffPng)
                    .build();

        } catch (Exception e) {
            return ComparisonResult.error(rendererClass, e.getMessage());
        }
    }

    /**
     * Updates the baseline snapshot for a renderer.
     *
     * @param rendererClass renderer class name
     * @param width         chart width
     * @param height        chart height
     * @param theme         theme name
     * @return true if baseline was updated successfully
     */
    public boolean updateBaseline(String rendererClass, int width, int height, String theme) {
        try {
            byte[] png = renderService.render(
                    new com.arbergashi.charts.visualverifier.dto.RenderRequest(rendererClass, width, height, theme)
            );
            Path baselinePath = getBaselinePath(rendererClass, width, height, theme);
            saveSnapshot(baselinePath, png);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Runs visual regression tests on all renderers.
     *
     * @return summary of all comparison results
     */
    public java.util.List<ComparisonResult> runAllTests() {
        var catalog = com.arbergashi.charts.visualverifier.RendererCatalog.load();
        java.util.List<ComparisonResult> results = new java.util.ArrayList<>();

        for (var entry : catalog.all()) {
            ComparisonResult result = compare(entry.className(), 800, 600, "light");
            results.add(result);
        }

        return results;
    }

    private Path getBaselinePath(String rendererClass, int width, int height, String theme) {
        String simpleName = rendererClass.substring(rendererClass.lastIndexOf('.') + 1);
        String filename = String.format("%s_%dx%d_%s.png", simpleName, width, height, theme);
        return Path.of(properties.getSnapshot().getDirectory(), "baselines", filename);
    }

    private void saveSnapshot(Path path, byte[] png) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, png, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private PixelComparisonResult comparePixels(BufferedImage current, BufferedImage baseline, double tolerance) {
        int width = current.getWidth();
        int height = current.getHeight();
        int totalPixels = width * height;
        int diffPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = current.getRGB(x, y);
                int b = baseline.getRGB(x, y);

                if (!pixelsMatch(c, b, tolerance)) {
                    diffPixels++;
                }
            }
        }

        double matchPercentage = 1.0 - ((double) diffPixels / totalPixels);
        boolean matches = matchPercentage >= (1.0 - tolerance);

        return new PixelComparisonResult(matches, matchPercentage, totalPixels, diffPixels);
    }

    private boolean pixelsMatch(int c, int b, double tolerance) {
        if (c == b) return true;

        int cr = (c >> 16) & 0xFF;
        int cg = (c >> 8) & 0xFF;
        int cb = c & 0xFF;

        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        double diff = (Math.abs(cr - br) + Math.abs(cg - bg) + Math.abs(cb - bb)) / (3.0 * 255.0);
        return diff <= tolerance;
    }

    private byte[] generateDiffImage(BufferedImage current, BufferedImage baseline) throws IOException {
        int width = current.getWidth();
        int height = current.getHeight();
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int diffColor = properties.getComparison().getDiffColor();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = current.getRGB(x, y);
                int b = baseline.getRGB(x, y);

                if (c != b) {
                    diff.setRGB(x, y, diffColor);
                } else {
                    // Semi-transparent baseline
                    diff.setRGB(x, y, (b & 0x00FFFFFF) | 0x40000000);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(diff, "PNG", out);
        return out.toByteArray();
    }

    private record PixelComparisonResult(boolean matches, double matchPercentage, int totalPixels, int diffPixels) {}
}
