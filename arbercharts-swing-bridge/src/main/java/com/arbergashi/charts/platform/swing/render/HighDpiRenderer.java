package com.arbergashi.charts.platform.swing.render;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

/**
 * Scale conversion helpers using one shared, cached display scale.
 *
 * <p>{@link #getScaleFactor(Component)} derives the larger X/Y scale from a
 * component's default graphics transform when the cache is empty. The cached
 * value is then shared across components until {@link #resetCache()} is called.
 * This helper does not monitor display changes, maintain per-monitor state or
 * render SVG icons. Font and stroke helpers perform explicit numeric scaling;
 * they do not guarantee pixel-identical output across displays.
 *
 * <p>Do not apply the scale again to a graphics context that already contains
 * its device transform, such as a Swing paint context. For an application-owned
 * image with an initially unscaled graphics context, the helper can convert
 * logical drawing units to the image's allocated pixel dimensions:
 * <pre>{@code
 * int width = HighDpiRenderer.toDevicePixels(400, component);
 * int height = HighDpiRenderer.toDevicePixels(200, component);
 * var image = new java.awt.image.BufferedImage(
 *     width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
 * Graphics2D graphics = image.createGraphics();
 * try {
 *     HighDpiRenderer.applyScaling(graphics, component);
 *     graphics.drawLine(0, 0, 100, 100);
 * } finally {
 *     graphics.dispose();
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @see java.awt.Graphics2D#getTransform()
 * @see java.awt.GraphicsConfiguration#getDefaultTransform()
 */
public final class HighDpiRenderer {

    /**
     * Shared scale factor reused until explicitly reset.
     */
    private static volatile Double cachedScaleFactor = null;

    private HighDpiRenderer() {
        // Utility class
    }

    /**
     * Returns the shared cached scale, deriving it from the component when absent.
     *
     * <p>Uses the larger X/Y value of the default graphics transform. If no
     * configuration is available or lookup fails, caches 1.0. The result is not
     * specific to later components and is not refreshed when a window moves.
     * The library requires Java 25.
     *
     * @param component the component to check
     * @return scale factor (1.0, 1.25, 1.5, 2.0, 2.5, etc.)
     */
    public static double getScaleFactor(Component component) {
        // Reuse the shared scale until resetCache() clears it
        if (cachedScaleFactor != null) {
            return cachedScaleFactor;
        }

        try {
            GraphicsConfiguration gc = component.getGraphicsConfiguration();
            if (gc != null) {
                AffineTransform transform = gc.getDefaultTransform();
                double scaleX = transform.getScaleX();
                double scaleY = transform.getScaleY();

                // Use maximum of X and Y scaling
                double scale = Math.max(scaleX, scaleY);

                // Cache the result
                cachedScaleFactor = scale;
                return scale;
            }
        } catch (Exception e) {
            // Use the default scale when the graphics configuration cannot be read
        }

        // Default: no scaling
        cachedScaleFactor = 1.0;
        return 1.0;
    }

    /**
     * Multiplies the current graphics transform by the cached scale.
     *
     * <p>Use only when the host requires this additional logical-to-pixel
     * conversion. A Swing paint context can already contain a device scale;
     * applying this method again would multiply it a second time. Preserve the
     * host context by drawing through a copy and disposing that copy afterward.
     *
     * @param g2 the graphics context
     * @param component the component being painted
     */
    public static void applyScaling(Graphics2D g2, Component component) {
        double scale = getScaleFactor(component);
        if (scale != 1.0) {
            g2.scale(scale, scale);
        }
    }

    /**
     * Scales a font size for High-DPI displays.
     *
     * @param baseFontSize the base font size (at 100%)
     * @param component the component context
     * @return scaled font size
     */
    public static float scaleFontSize(float baseFontSize, Component component) {
        double scale = getScaleFactor(component);
        return (float) (baseFontSize * scale);
    }

    /**
     * Scales a line width for High-DPI displays.
     *
     * <p>Multiplies by the cached scale and clamps the result to at least 1.0.
     * The result is a width in drawing units; its device-pixel footprint also
     * depends on the graphics transform and rasterization.
     *
     * @param baseWidth the base line width (at 100%)
     * @param component the component context
     * @return scaled line width
     */
    public static float scaleLineWidth(float baseWidth, Component component) {
        double scale = getScaleFactor(component);
        float scaled = (float) (baseWidth * scale);

        // Minimum one drawing unit
        return Math.max(scaled, 1.0f);
    }

    /**
     * Rounds a coordinate to the nearest pixel boundary for the cached scale.
     *
     * <p>The calculation is {@code round(coordinate * scale) / scale}. Actual
     * stroke alignment also depends on line width and the graphics transform.
     *
     * @param coordinate logical coordinate
     * @param component the component context
     * @return snapped coordinate
     */
    public static double snapToPixel(double coordinate, Component component) {
        double scale = getScaleFactor(component);
        return Math.round(coordinate * scale) / scale;
    }

    /**
     * Converts logical pixels to device pixels.
     *
     * @param logicalPixels logical pixel count
     * @param component the component context
     * @return device pixel count
     */
    public static int toDevicePixels(int logicalPixels, Component component) {
        double scale = getScaleFactor(component);
        return (int) Math.round(logicalPixels * scale);
    }

    /**
     * Converts device pixels to logical pixels.
     *
     * @param devicePixels device pixel count
     * @param component the component context
     * @return logical pixel count
     */
    public static int toLogicalPixels(int devicePixels, Component component) {
        double scale = getScaleFactor(component);
        return (int) Math.round(devicePixels / scale);
    }

    /**
     * Checks if the display is High-DPI (scale > 1.0).
     *
     * @param component the component context
     * @return true if High-DPI, false otherwise
     */
    public static boolean isHighDpi(Component component) {
        return getScaleFactor(component) > 1.0;
    }

    /**
     * Gets a user-friendly description of the current scale.
     *
     * @param component the component context
     * @return description such as "200% (Retina/4K)" or "100% (Standard)"
     */
    public static String getScaleDescription(Component component) {
        double scale = getScaleFactor(component);
        int percent = (int) Math.round(scale * 100);

        if (scale == 1.0) {
            return "100% (Standard)";
        } else if (scale == 2.0) {
            return "200% (Retina/4K)";
        } else {
            return String.format("%d%% (%.1fx)", percent, scale);
        }
    }

    /**
     * Creates a High-DPI compatible stroke.
     *
     * @param baseWidth base width at 100% scale
     * @param component the component context
     * @return scaled BasicStroke
     */
    public static BasicStroke createScaledStroke(float baseWidth, Component component) {
        float scaledWidth = scaleLineWidth(baseWidth, component);
        return new BasicStroke(scaledWidth);
    }

    /**
     * Creates a High-DPI compatible font.
     *
     * @param baseFont base font at 100% scale
     * @param component the component context
     * @return scaled Font
     */
    public static Font createScaledFont(Font baseFont, Component component) {
        float scaledSize = scaleFontSize(baseFont.getSize2D(), component);
        return baseFont.deriveFont(scaledSize);
    }

    /**
     * Resets the cached scale factor.
     *
     * <p>The next lookup recomputes one shared value. Hosts must coordinate
     * cache resets with their display lifecycle; resetting does not create
     * independent scale values for windows on different monitors.
     */
    public static void resetCache() {
        cachedScaleFactor = null;
    }
}

