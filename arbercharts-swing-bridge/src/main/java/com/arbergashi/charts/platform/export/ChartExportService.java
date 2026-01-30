package com.arbergashi.charts.platform.export;

import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.platform.swing.util.ChartEngine;
import com.arbergashi.charts.util.ChartScale;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
/**
 * Exports charts rendered by {@link ArberChartPanel} into images.
 *
 * <p>This class intentionally avoids any Swing dialogs or framework I18N.
 * UI concerns (file chooser, notifications) belong to the demo application.
 *
 * <h2>Supported formats</h2>
 * <ul>
 *   <li>PNG (always available via JDK)</li>
 *   <li>PDF (requires Apache PDFBox on the classpath)</li>
 *   <li>SVG (requires JFree SVG on the classpath)</li>
 * </ul>
 *
 * <p>Core policy: PDF/SVG integrations are optional and loaded via reflection.
 * The host application decides which export formats are available by adding dependencies.
 *
 * @author Arber Gashi
 * @version 0.9.0
 * @since 0.9.0
 */
public final class ChartExportService {

    private ChartExportService() {
    }

    /**
     * Renders the given chart panel into a PNG file.
     *
     * <p>This method is safe to use in headless environments.
     *
     * <p>Notes:
     * <ul>
     *   <li>If the panel is not yet displayed (width/height = 0), its preferred size is used.</li>
     *   <li>Parent directories are created automatically.</li>
     * </ul>
     *
     * @param panel      the chart panel to export
     * @param outputFile the output file (PNG)
     * @throws IllegalArgumentException if panel or outputFile is null
     * @throws RuntimeException         if writing the image fails
     */
    public static void exportPng(ArberChartPanel panel, File outputFile) {
        ExportGeometry geo = ExportGeometry.from(panel, outputFile);

        BufferedImage img = new BufferedImage(geo.getScaledWidth(), geo.getScaledHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            ChartEngine.prepareGraphics(g2, false);
            g2.scale(geo.getScale(), geo.getScale());

            // printAll() includes children (tooltip/crosshair). This is desirable for exports.
            panel.printAll(g2);

            writePng(img, geo.getOutputFile());
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Renders the given chart panel into an SVG file.
     *
     * <p>Requires dependency:
     * <pre>
     * org.jfree:jfreesvg
     * </pre>
     *
     * @param panel      the chart panel to export
     * @param outputFile the output file (SVG)
     * @throws IllegalArgumentException if panel or outputFile is null
     * @throws RuntimeException         if required SVG backend is missing or writing fails
     */
    public static void exportSvg(ArberChartPanel panel, File outputFile) {
        ExportGeometry geo = ExportGeometry.from(panel, outputFile);

        try {
            // JFreeSVG: org.jfree.svg.SVGGraphics2D(int width, int height)
            ClassLoader cl = ChartExportService.class.getClassLoader();
            Class<?> svgG2Class = Class.forName("org.jfree.svg.SVGGraphics2D", false, cl);
            Constructor<?> ctor = svgG2Class.getConstructor(int.class, int.class);
            Object svgG2 = ctor.newInstance(geo.scaledWidth, geo.scaledHeight);

            if (!(svgG2 instanceof Graphics2D g2)) {
                throw new IllegalStateException("SVG backend does not provide Graphics2D");
            }

            ChartEngine.prepareGraphics(g2, false);
            g2.scale(geo.scale, geo.scale);
            panel.printAll(g2);

            // JFreeSVG: String getSVGElement()
            Method getSvgElement = svgG2Class.getMethod("getSVGElement");
            String svg = (String) getSvgElement.invoke(svgG2);

            Files.writeString(geo.outputFile.toPath(), svg, StandardCharsets.UTF_8);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SVG export backend not found. Add dependency 'org.jfree:jfreesvg'.", e);
        } catch (Exception e) {
            throw new RuntimeException("SVG export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Renders the given chart panel into a PDF file.
     *
     * <p>Requires dependency:
     * <pre>
     * org.apache.pdfbox:pdfbox
     * </pre>
     *
     * @param panel      the chart panel to export
     * @param outputFile the output file (PDF)
     * @throws IllegalArgumentException if panel or outputFile is null
     * @throws RuntimeException         if required PDF backend is missing or writing fails
     */
    public static void exportPdf(ArberChartPanel panel, File outputFile) {
        ExportGeometry geo = ExportGeometry.from(panel, outputFile);

        try {
            ClassLoader cl = ChartExportService.class.getClassLoader();

            // PDFBox: PDDocument doc = new PDDocument();
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument", false, cl);
            Object doc = pdDocumentClass.getConstructor().newInstance();

            // PDPage page = new PDPage(new PDRectangle(width, height));
            Class<?> pdRectangleClass = Class.forName("org.apache.pdfbox.pdmodel.common.PDRectangle", false, cl);
            Constructor<?> rectCtor = pdRectangleClass.getConstructor(float.class, float.class);
            Object rect = rectCtor.newInstance((float) geo.scaledWidth, (float) geo.scaledHeight);

            Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage", false, cl);
            Constructor<?> pageCtor = pdPageClass.getConstructor(pdRectangleClass);
            Object page = pageCtor.newInstance(rect);

            Method addPage = pdDocumentClass.getMethod("addPage", pdPageClass);
            addPage.invoke(doc, page);

            // PDPageContentStream cs = new PDPageContentStream(doc, page)
            Class<?> pdPageContentStreamClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream", false, cl);
            Constructor<?> csCtor = pdPageContentStreamClass.getConstructor(pdDocumentClass, pdPageClass);
            Object contentStream = csCtor.newInstance(doc, page);

            // Render to BufferedImage, then embed as image into PDF (fast, simple, robust)
            BufferedImage img = new BufferedImage(geo.scaledWidth, geo.scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            try {
                ChartEngine.prepareGraphics(g2, false);
                g2.scale(geo.scale, geo.scale);
                panel.printAll(g2);
            } finally {
                g2.dispose();
            }

            // PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img)
            Class<?> losslessFactoryClass = Class.forName("org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory", false, cl);
            Method createFromImage = losslessFactoryClass.getMethod("createFromImage", pdDocumentClass, BufferedImage.class);
            Object pdImage = createFromImage.invoke(null, doc, img);

            // contentStream.drawImage(pdImage, 0, 0)
            Method drawImage = pdPageContentStreamClass.getMethod(
                    "drawImage",
                    Class.forName("org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject", false, cl),
                    float.class,
                    float.class
            );
            drawImage.invoke(contentStream, pdImage, 0f, 0f);

            Method close = pdPageContentStreamClass.getMethod("close");
            close.invoke(contentStream);

            Method save = pdDocumentClass.getMethod("save", File.class);
            save.invoke(doc, geo.outputFile);

            Method docClose = pdDocumentClass.getMethod("close");
            docClose.invoke(doc);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PDF export backend not found. Add dependency 'org.apache.pdfbox:pdfbox'.", e);
        } catch (Exception e) {
            throw new RuntimeException("PDF export failed: " + e.getMessage(), e);
        }
    }

    private static void writePng(BufferedImage img, File outputFile) throws IOException {
        if (!ImageIO.write(img, "png", outputFile)) {
            throw new IOException("No PNG writer available");
        }
    }

    private static File requireParentDirs(File outputFile) {
        if (outputFile == null) throw new IllegalArgumentException("outputFile must not be null");

        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Export failed: could not create directory " + parent);
        }
        return outputFile;
    }

    private static final class ExportGeometry {
        private File outputFile;
        private int panelWidth;
        private int panelHeight;
        private double scale;
        private int scaledWidth;
        private int scaledHeight;

        private ExportGeometry(File outputFile, int panelWidth, int panelHeight, double scale, int scaledWidth, int scaledHeight) {
            this.outputFile = outputFile;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
            this.scale = scale;
            this.scaledWidth = scaledWidth;
            this.scaledHeight = scaledHeight;
        }

        static ExportGeometry from(ArberChartPanel panel, File out) {
            if (panel == null) throw new IllegalArgumentException("panel must not be null");

            File outFile = requireParentDirs(out);

            // If the panel is not realized, use preferred size to avoid exporting a 1x1 image.
            int panelW = panel.getWidth();
            int panelH = panel.getHeight();
            if (panelW <= 0 || panelH <= 0) {
                Dimension pref = panel.getPreferredSize();
                if (pref != null) {
                    panelW = Math.max(panelW, pref.width);
                    panelH = Math.max(panelH, pref.height);
                }
            }

            double scale = Math.max(0.1, ChartScale.scale(1.0));
            int width = Math.max(1, (int) Math.round(panelW * scale));
            int height = Math.max(1, (int) Math.round(panelH * scale));

            return new ExportGeometry(outFile, panelW, panelH, scale, width, height);
        }

        File getOutputFile() {
            return outputFile;
        }

        int getPanelWidth() {
            return panelWidth;
        }

        int getPanelHeight() {
            return panelHeight;
        }

        double getScale() {
            return scale;
        }

        int getScaledWidth() {
            return scaledWidth;
        }

        int getScaledHeight() {
            return scaledHeight;
        }
    }
}
