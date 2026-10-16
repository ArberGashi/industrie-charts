package com.arbergashi.charts.springdemo;

import com.arbergashi.charts.api.BasicChartTheme;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.grid.PolarGeometry;
import com.arbergashi.charts.util.ColorUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Semaphore;

/** Composes demo-owned labels, legends and grids around the shared Core renderers. */
@Service
final class DemoChartService {
    private final ServerRenderService renderer;
    private final Semaphore capacity = new Semaphore(4);

    DemoChartService(ServerRenderService renderer) {
        this.renderer = renderer;
    }

    byte[] render(DemoExamples.Example example, int width, int height, int samples, String themeName, int scale) {
        validate(width, height, samples, themeName, scale);
        if (!capacity.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "All render slots are in use. Try again shortly.");
        }
        try {
            ChartTheme theme = theme(themeName, example.presentation(), scale);
            DemoExamples.Dataset data = example.create(samples, theme.getSeriesColor(0));
            PlotArea area = plotArea(example.presentation(), width, height, scale);
            BufferedImage plot = renderer.renderToImage(data.model(), new Dimension(area.width(), area.height()),
                    theme, data.renderer(), data.minX(), data.maxX(), data.minY(), data.maxY());
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setColor(awt(theme.getBackground()));
                graphics.fillRect(0, 0, width, height);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12 * scale));
                if (example.presentation() != DemoExamples.Presentation.DONUT
                        && example.presentation() != DemoExamples.Presentation.RADAR) {
                    drawAxes(graphics, example, data, theme, area, scale);
                }
                graphics.drawImage(plot, area.left(), area.top(), null);
                switch (example.presentation()) {
                    case RADAR -> drawRadarLabels(graphics, data.model(), theme, area, scale);
                    case HEATMAP -> drawColorScale(graphics, data.model(), theme, area, scale);
                    default -> { }
                }
            } finally {
                graphics.dispose();
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IllegalStateException("No PNG encoder is available.");
                }
                return output.toByteArray();
            } catch (IOException exception) {
                throw new IllegalStateException("The chart could not be encoded as PNG.", exception);
            }
        } finally {
            capacity.release();
        }
    }

    private static PlotArea plotArea(DemoExamples.Presentation presentation, int width, int height, int scale) {
        int left = 64;
        int right = 28;
        int top = 28;
        int bottom = 64;
        switch (presentation) {
            case DONUT -> { left = 16; right = 16; top = 16; bottom = 16; }
            case RADAR -> { left = 48; right = 48; top = 32; bottom = 44; }
            case HEATMAP -> right = 112;
            default -> { }
        }
        return new PlotArea(left * scale, top * scale, width - (left + right) * scale,
                height - (top + bottom) * scale);
    }

    private static void validate(int width, int height, int samples, String theme, int scale) {
        if (scale != 1 && scale != 2) {
            badRequest("Scale must be 1 or 2.");
        }
        if (width < 320 * scale || width > 2560 || height < 240 * scale || height > 1440) {
            badRequest("Image size must be between 320 × 240 logical pixels and 2560 × 1440 output pixels.");
        }
        if (samples < 16 || samples > 2048) {
            badRequest("Samples must be between 16 and 2048.");
        }
        if (!theme.equals("dark") && !theme.equals("light")) {
            badRequest("Theme must be dark or light.");
        }
    }

    private static void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ChartTheme theme(String name, DemoExamples.Presentation presentation, int scale) {
        boolean dark = name.equals("dark");
        ArberColor accent = new ArberColor(dark ? 0xFF75A6FF : 0xFF3574D8);
        int[] palette = presentation == DemoExamples.Presentation.HEATMAP
                ? new int[]{0xFF293B86, 0xFF258B9A, 0xFF8BC6A4, 0xFFF4DC79}
                : dark ? new int[]{0xFF75A6FF, 0xFF62C9BC, 0xFFF0B96A, 0xFFB49BEA, 0xFFED8F9C, 0xFFA8C978}
                : new int[]{0xFF3574D8, 0xFF168579, 0xFFD08722, 0xFF8061B9, 0xFFC55572, 0xFF658735};
        ArberColor[] series = new ArberColor[palette.length];
        for (int i = 0; i < series.length; i++) series[i] = new ArberColor(palette[i]);
        return new BasicChartTheme(
                new ArberColor(dark ? 0xFF1E1F22 : 0xFFFFFFFF),
                new ArberColor(dark ? 0xFFD7DBE0 : 0xFF252A34),
                new ArberColor(dark ? 0xFF35383F : 0xFFE7EAF0),
                new ArberColor(dark ? 0xFF9CA4B3 : 0xFF697386), accent,
                series, new ArberFont(Font.SANS_SERIF, ArberFont.PLAIN, 12 * scale));
    }

    private static void drawAxes(Graphics2D graphics, DemoExamples.Example example, DemoExamples.Dataset data,
                                 ChartTheme theme, PlotArea area, int scale) {
        FontMetrics metrics = graphics.getFontMetrics();
        boolean boxplot = example.presentation() == DemoExamples.Presentation.BOXPLOT;
        boolean grid = example.presentation() != DemoExamples.Presentation.HEATMAP;
        for (int tick = 0; tick <= 5; tick++) {
            double fraction = tick / 5.0;
            int x = area.left() + (int) Math.round(fraction * area.width());
            int y = area.top() + (int) Math.round((1 - fraction) * area.height());
            if (grid) {
                graphics.setColor(awt(theme.getGridColor()));
                if (!boxplot) graphics.drawLine(x, area.top(), x, area.bottom());
                graphics.drawLine(area.left(), y, area.right(), y);
            }
            graphics.setColor(awt(theme.getAxisLabelColor()));
            String yLabel = number(data.minY() + fraction * (data.maxY() - data.minY()),
                    (data.maxY() - data.minY()) / 5);
            graphics.drawString(yLabel, area.left() - metrics.stringWidth(yLabel) - 12 * scale,
                    y + (metrics.getAscent() - metrics.getDescent()) / 2);
            if (!boxplot) {
                String xLabel = number(data.minX() + fraction * (data.maxX() - data.minX()),
                        (data.maxX() - data.minX()) / 5);
                graphics.drawString(xLabel, x - metrics.stringWidth(xLabel) / 2, area.bottom() + 22 * scale);
            }
        }
        if (boxplot) {
            for (int i = 0; i < data.model().getPointCount(); i++) {
                String label = data.model().getLabel(i);
                int x = area.left() + (int) Math.round((data.model().getX(i) - data.minX())
                        / (data.maxX() - data.minX()) * area.width());
                graphics.drawString(label, x - metrics.stringWidth(label) / 2, area.bottom() + 22 * scale);
            }
        }
        String xTitle = example.xLabel();
        graphics.drawString(xTitle, area.left() + (area.width() - metrics.stringWidth(xTitle)) / 2,
                area.bottom() + 48 * scale);
        graphics.drawString(example.yLabel(), area.left(), 17 * scale);
    }

    private static void drawRadarLabels(Graphics2D graphics, ChartModel model, ChartTheme theme,
                                        PlotArea area, int scale) {
        FontMetrics metrics = graphics.getFontMetrics();
        double cx = area.left() + area.width() / 2.0;
        double cy = area.top() + area.height() / 2.0;
        double radius = PolarGeometry.radius(new ArberRect(0, 0, area.width(), area.height()));
        graphics.setColor(awt(theme.getAxisLabelColor()));
        for (int i = 0; i < model.getPointCount(); i++) {
            double angle = i * Math.PI * 2 / model.getPointCount() - Math.PI / 2;
            double cosine = Math.cos(angle);
            String label = model.getLabel(i);
            double x = cx + cosine * (radius + 16 * scale);
            double y = cy + Math.sin(angle) * (radius + 16 * scale);
            if (cosine < -0.1) x -= metrics.stringWidth(label);
            else if (cosine <= 0.1) x -= metrics.stringWidth(label) / 2.0;
            graphics.drawString(label, (float) x, (float) (y + (metrics.getAscent() - metrics.getDescent()) / 2.0));
        }
        graphics.setFont(graphics.getFont().deriveFont(10f * scale));
        for (int ring = 1; ring <= 5; ring++) {
            graphics.drawString(Integer.toString(ring * 20), (float) (cx + 5 * scale),
                    (float) (cy - radius * ring / 5.0 + 4 * scale));
        }
        String note = "Mean scores · 0–100";
        graphics.drawString(note, (float) (cx - graphics.getFontMetrics().stringWidth(note) / 2.0),
                area.bottom() + 30 * scale);
    }

    private static void drawColorScale(Graphics2D graphics, ChartModel model, ChartTheme theme,
                                       PlotArea area, int scale) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < model.getPointCount(); i++) {
            minimum = Math.min(minimum, model.getWeight(i));
            maximum = Math.max(maximum, model.getWeight(i));
        }
        int left = area.right() + 20 * scale;
        int barWidth = 12 * scale;
        for (int y = 0; y < area.height(); y++) {
            // Match the renderer's 256-entry lookup table, including its segment boundaries.
            float t = (float) Math.floor((1.0 - y / (double) (area.height() - 1)) * 255) / 255;
            ArberColor color = t < 0.33f
                    ? ColorUtils.interpolate(theme.getSeriesColor(0), theme.getSeriesColor(1), t / 0.33f)
                    : t < 0.66f
                    ? ColorUtils.interpolate(theme.getSeriesColor(1), theme.getSeriesColor(2), (t - 0.33f) / 0.33f)
                    : ColorUtils.interpolate(theme.getSeriesColor(2), theme.getSeriesColor(3), (t - 0.66f) / 0.34f);
            graphics.setColor(awt(color));
            graphics.fillRect(left, area.top() + y, barWidth, 1);
        }
        graphics.setColor(awt(theme.getAxisLabelColor()));
        graphics.drawString("Response", left, 17 * scale);
        for (int tick = 0; tick <= 4; tick++) {
            double fraction = tick / 4.0;
            int y = area.top() + (int) Math.round((1 - fraction) * (area.height() - 1));
            String label = number(minimum + fraction * (maximum - minimum), (maximum - minimum) / 4);
            graphics.drawString(label, left + barWidth + 7 * scale,
                    y + (graphics.getFontMetrics().getAscent() - graphics.getFontMetrics().getDescent()) / 2);
        }
    }

    private static String number(double value, double spacing) {
        int decimals = Math.max(0, Math.min(4, (int) Math.ceil(-Math.log10(spacing)) + 1));
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static Color awt(ArberColor color) {
        return new Color(color.argb(), true);
    }

    private record PlotArea(int left, int top, int width, int height) {
        int right() {
            return left + width;
        }

        int bottom() {
            return top + height;
        }
    }
}
