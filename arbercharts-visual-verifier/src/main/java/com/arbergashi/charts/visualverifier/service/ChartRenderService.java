package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.visualverifier.RendererDemoDataFactory;
import com.arbergashi.charts.visualverifier.RendererCatalogEntry;
import com.arbergashi.charts.visualverifier.dto.RenderRequest;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Service for rendering charts to images.
 *
 * <p>Uses ArberCharts Server Bridge for headless PNG rendering.
 * Supports all 158 renderers with automatic demo data generation.
 *
 * @since 2.0.0
 */
@Service
public class ChartRenderService {
    private static final System.Logger LOGGER = System.getLogger(ChartRenderService.class.getName());

    private final ServerRenderService serverRenderService;
    private final RendererCatalogService catalogService;

    public ChartRenderService(ServerRenderService serverRenderService, RendererCatalogService catalogService) {
        this.serverRenderService = serverRenderService;
        this.catalogService = catalogService;
    }

    /**
     * Renders a chart to PNG bytes.
     *
     * @param request render request with renderer, dimensions, theme
     * @return PNG image bytes
     * @throws RuntimeException if rendering fails
     */
    public byte[] render(RenderRequest request) {
        RendererCatalogEntry catalogEntry = catalogService.getRenderer(request.renderer());
        ChartTheme theme = resolveTheme(request.theme());
        Dimension size = new Dimension(request.width(), request.height());
        List<ChartModel> candidates = buildCandidateModels(catalogEntry);
        RuntimeException lastFailure = null;

        try {
            for (ChartModel model : candidates) {
                try {
                    ChartRenderer renderer = instantiateRenderer(catalogEntry.className());
                    byte[] png = serverRenderService.renderToPng(model, size, theme, renderer);
                    if (png != null && png.length > 0) {
                        return png;
                    }
                } catch (RuntimeException ex) {
                    lastFailure = ex;
                }
            }
        } catch (RuntimeException ex) {
            lastFailure = ex;
        }

        if (lastFailure != null) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Render fallback activated for {0}: {1}",
                    request.renderer(),
                    lastFailure.getMessage()
            );
        }

        return buildDiagnosticPng(catalogEntry.simpleName(), size, request.theme(), lastFailure);
    }

    private List<ChartModel> buildCandidateModels(RendererCatalogEntry entry) {
        List<ChartModel> models = new ArrayList<>(10);
        models.add(buildDemoData(entry.category(), entry.className()));

        // Compatibility probes for renderers with strict model-type requirements.
        addModel(models, "standard", "com.arbergashi.charts.render.standard.LineRenderer");
        addModel(models, "financial", "com.arbergashi.charts.render.financial.CandlestickRenderer");
        addModel(models, "statistical", "com.arbergashi.charts.render.statistical.BoxPlotRenderer");
        addModel(models, "medical", "com.arbergashi.charts.render.medical.ECGRenderer");
        addModel(models, "circular", "com.arbergashi.charts.render.circular.PieRenderer");
        addModel(models, "specialized", "com.arbergashi.charts.render.specialized.AlluvialRenderer");
        addModel(models, "specialized", "com.arbergashi.charts.render.specialized.TernaryPlotRenderer");
        addModel(models, "specialized", "com.arbergashi.charts.render.specialized.ChordFlowRenderer");

        return models;
    }

    private void addModel(List<ChartModel> models, String category, String rendererClass) {
        try {
            models.add(RendererDemoDataFactory.build(category, rendererClass));
        } catch (RuntimeException ignored) {
            // Ignore model probe failures; remaining candidates are still usable.
        }
    }

    private ChartRenderer instantiateRenderer(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!ChartRenderer.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class is not a ChartRenderer: " + className);
            }
            return (ChartRenderer) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate renderer: " + className, e);
        }
    }

    private ChartTheme resolveTheme(String themeName) {
        return "dark".equalsIgnoreCase(themeName)
                ? ChartThemes.getDarkTheme()
                : ChartThemes.getLightTheme();
    }

    private ChartModel buildDemoData(String category, String rendererClass) {
        try {
            return RendererDemoDataFactory.build(category, rendererClass);
        } catch (Exception e) {
            // Fallback to simple circular model
            return new com.arbergashi.charts.model.CircularChartModel(1000);
        }
    }

    private byte[] buildDiagnosticPng(String rendererName, Dimension size, String themeName, Throwable failure) {
        int width = Math.max(320, size.width);
        int height = Math.max(200, size.height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean dark = "dark".equalsIgnoreCase(themeName);
            Color bg1 = dark ? new Color(22, 28, 38) : new Color(243, 247, 255);
            Color bg2 = dark ? new Color(33, 41, 58) : new Color(226, 238, 255);
            g.setPaint(new GradientPaint(0, 0, bg1, width, height, bg2));
            g.fillRect(0, 0, width, height);

            int cardW = Math.max(260, width - 48);
            int cardH = Math.max(120, height - 48);
            int cardX = (width - cardW) / 2;
            int cardY = (height - cardH) / 2;

            g.setColor(dark ? new Color(14, 18, 27, 220) : new Color(255, 255, 255, 235));
            g.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 20, 20));
            g.setStroke(new BasicStroke(2f));
            g.setColor(dark ? new Color(88, 163, 255, 180) : new Color(47, 106, 255, 180));
            g.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 20, 20));

            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(dark ? new Color(231, 239, 255) : new Color(20, 40, 84));
            g.drawString(rendererName, cardX + 20, cardY + 36);

            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(dark ? new Color(170, 188, 220) : new Color(58, 92, 146));
            g.drawString("Fallback preview rendered", cardX + 20, cardY + 62);

            String reason = failure == null ? "unknown renderer issue" : safeFailureMessage(failure);
            g.drawString(reason, cardX + 20, cardY + 86);

            g.setColor(new Color(255, 99, 71, 190));
            g.fillRoundRect(cardX + 20, cardY + 98, 110, 26, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("DIAGNOSTIC", cardX + 31, cardY + 116);
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build diagnostic fallback PNG", e);
        }
    }

    private String safeFailureMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        if (message.length() > 84) {
            return message.substring(0, 81) + "...";
        }
        return message;
    }
}
