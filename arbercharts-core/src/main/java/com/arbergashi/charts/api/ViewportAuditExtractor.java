package com.arbergashi.charts.api;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
/**
 * Extracts embedded audit metadata from PNG/PDF exports.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class ViewportAuditExtractor {
    private ViewportAuditExtractor() {
    }

    public static String extract(File file) throws IOException {
        if (file == null) return null;
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) {
            return extractFromPng(file);
        }
        if (name.endsWith(".pdf")) {
            return extractFromPdf(file);
        }
        return null;
    }

    public static String extractFromPng(File file) throws IOException {
        if (file == null) return null;
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
        if (!readers.hasNext()) return null;
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            reader.setInput(in, true, true);
            IIOMetadata meta = reader.getImageMetadata(0);
            if (meta == null) return null;
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_png_1.0");
            for (int i = 0; i < root.getLength(); i++) {
                if (root.item(i) instanceof IIOMetadataNode node && "iTXt".equals(node.getNodeName())) {
                    return extractItxt(node);
                }
            }
        } finally {
            reader.dispose();
        }
        return null;
    }

    private static String extractItxt(IIOMetadataNode itxtNode) {
        for (int i = 0; i < itxtNode.getLength(); i++) {
            if (itxtNode.item(i) instanceof IIOMetadataNode entry && "iTXtEntry".equals(entry.getNodeName())) {
                String keyword = entry.getAttribute("keyword");
                if ("ArberChartsAudit".equals(keyword)) {
                    return entry.getAttribute("text");
                }
            }
        }
        return null;
    }

    public static String extractFromPdf(File file) {
        if (file == null) return null;
        try {
            ClassLoader cl = ViewportAuditExtractor.class.getClassLoader();
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument", false, cl);
            Method load = pdDocumentClass.getMethod("load", File.class);
            Object doc = load.invoke(null, file);
            try {
                Method getInfo = pdDocumentClass.getMethod("getDocumentInformation");
                Object info = getInfo.invoke(doc);
                Method getCustom = info.getClass().getMethod("getCustomMetadataValue", String.class);
                Object value = getCustom.invoke(info, "ArberChartsAudit");
                return value != null ? value.toString() : null;
            } finally {
                Method close = pdDocumentClass.getMethod("close");
                close.invoke(doc);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
