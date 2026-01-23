package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.Optional;

/**
 * Professional, zero-allocation box plot renderer.
 *
 * <p>Uses a layer cache (VolatileImage/BufferedImage) keyed by model update stamp
 * and plot bounds to avoid per-frame Java2D allocations for static data.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class BoxPlotRenderer extends BaseRenderer {

    private transient VolatileImage layerCache;
    private transient BufferedImage layerFallback;
    private transient GraphicsConfiguration cacheConfig;
    private transient int cacheWidth = -1;
    private transient int cacheHeight = -1;
    private transient long cacheStamp = Long.MIN_VALUE;
    private transient int cacheThemeKey;
    private transient int cacheHintsKey;
    private transient int cacheLayerIndex = Integer.MIN_VALUE;
    private transient boolean cacheMultiColor;
    private transient float cachedBoxStrokeWidth = -1.0f;
    private transient Stroke cachedBoxStroke;
    private transient float cachedMedianStrokeWidth = -1.0f;
    private transient Stroke cachedMedianStroke;

    public BoxPlotRenderer() {
        super("boxplot");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        Rectangle2D bounds = context.plotBounds();
        int width = (int) Math.ceil(bounds.getWidth());
        int height = (int) Math.ceil(bounds.getHeight());
        if (width <= 0 || height <= 0) return;

        ChartTheme theme = resolveTheme(context);
        long stamp = model.getUpdateStamp();
        int themeKey = System.identityHashCode(theme);
        int hintsKey = System.identityHashCode(context.renderHints());
        int layerIndex = getLayerIndex();
        boolean multiColor = isMultiColor();

        boolean cacheValid = stamp == cacheStamp
                && width == cacheWidth
                && height == cacheHeight
                && themeKey == cacheThemeKey
                && hintsKey == cacheHintsKey
                && layerIndex == cacheLayerIndex
                && multiColor == cacheMultiColor
                && (layerCache != null || layerFallback != null);

        if (!cacheValid || !drawCachedLayer(g2, bounds)) {
            renderLayer(g2, model, context, bounds, width, height, themeKey, hintsKey, layerIndex, multiColor, stamp);
            drawCachedLayer(g2, bounds);
        }
    }

    private boolean drawCachedLayer(Graphics2D g2, Rectangle2D bounds) {
        int x = (int) Math.round(bounds.getX());
        int y = (int) Math.round(bounds.getY());
        if (layerCache != null) {
            if (layerCache.contentsLost()) {
                return false;
            }
            g2.drawImage(layerCache, x, y, null);
            return true;
        }
        if (layerFallback != null) {
            g2.drawImage(layerFallback, x, y, null);
            return true;
        }
        return false;
    }

    private void renderLayer(Graphics2D g2, ChartModel model, PlotContext context,
                             Rectangle2D bounds, int width, int height,
                             int themeKey, int hintsKey, int layerIndex, boolean multiColor, long stamp) {
        if (g2 != null && g2.getDeviceConfiguration() != null) {
            GraphicsConfiguration gc = g2.getDeviceConfiguration();
            if (layerCache == null || cacheWidth != width || cacheHeight != height || cacheConfig != gc) {
                layerCache = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
                cacheConfig = gc;
            }
            do {
                int validation = layerCache.validate(gc);
                if (validation == VolatileImage.IMAGE_INCOMPATIBLE) {
                    layerCache = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
                }
                Graphics2D layerGraphics = layerCache.createGraphics();
                try {
                    ChartRenderHints hints = context.renderHints();
                    if (hints != null) {
                        hints.applyTo(layerGraphics);
                    }
                    Composite oldComposite = layerGraphics.getComposite();
                    layerGraphics.setComposite(AlphaComposite.Clear);
                    layerGraphics.fillRect(0, 0, width, height);
                    layerGraphics.setComposite(oldComposite);
                    layerGraphics.setClip(0, 0, width, height);
                    layerGraphics.translate(-bounds.getX(), -bounds.getY());
                    drawBoxPlot(layerGraphics, model, context);
                } finally {
                    layerGraphics.dispose();
                }
            } while (layerCache.contentsLost());
        } else {
            if (layerFallback == null || cacheWidth != width || cacheHeight != height) {
                layerFallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D layerGraphics = layerFallback.createGraphics();
            try {
                ChartRenderHints hints = context.renderHints();
                if (hints != null) {
                    hints.applyTo(layerGraphics);
                }
                Composite oldComposite = layerGraphics.getComposite();
                layerGraphics.setComposite(AlphaComposite.Clear);
                layerGraphics.fillRect(0, 0, width, height);
                layerGraphics.setComposite(oldComposite);
                layerGraphics.setClip(0, 0, width, height);
                layerGraphics.translate(-bounds.getX(), -bounds.getY());
                drawBoxPlot(layerGraphics, model, context);
            } finally {
                layerGraphics.dispose();
            }
        }

        cacheWidth = width;
        cacheHeight = height;
        cacheStamp = stamp;
        cacheThemeKey = themeKey;
        cacheHintsKey = hintsKey;
        cacheLayerIndex = layerIndex;
        cacheMultiColor = multiColor;
    }

    private void drawBoxPlot(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ChartTheme theme = resolveTheme(context);

        BufferedImage[] gradients = (BufferedImage[]) RendererAllocationCache.getArray(this, "boxplot.gradients", BufferedImage.class, n);
        int[] lastColors = RendererAllocationCache.getIntArray(this, "boxplot.gradColors", n);
        int[] lastWidths = RendererAllocationCache.getIntArray(this, "boxplot.gradWidths", n);
        int[] lastHeights = RendererAllocationCache.getIntArray(this, "boxplot.gradHeights", n);
        Color[] lowColors = (Color[]) RendererAllocationCache.getArray(this, "boxplot.gradLow", Color.class, n);
        Color[] highColors = (Color[]) RendererAllocationCache.getArray(this, "boxplot.gradHigh", Color.class, n);
        Color[] outlierColors = (Color[]) RendererAllocationCache.getArray(this, "boxplot.outlierColors", Color.class, n);
        int[] outlierKeys = RendererAllocationCache.getIntArray(this, "boxplot.outlierKeys", n);

        final double barWidth = ChartScale.scale(30.0);
        final double whiskerCap = ChartScale.scale(14.0);
        final float strokeWidth = ChartScale.scale(1.5f);

        g2.setStroke(getBoxStroke(strokeWidth));
        final Rectangle2D viewBounds = g2.getClipBounds() != null ? g2.getClipBounds() : context.plotBounds();

        for (int i = 0; i < n; i++) {
            final double xVal = model.getX(i);
            final double median = model.getY(i);
            final double min = model.getValue(i, 3); // min
            final double max = model.getValue(i, 4); // max
            final double iqr = model.getValue(i, 2); // weight = iqr

            double[] buf = pBuffer();
            context.mapToPixel(xVal, median, buf);
            double x = buf[0];
            if (x < viewBounds.getMinX() - barWidth || x > viewBounds.getMaxX() + barWidth) {
                continue;
            }

            context.mapToPixel(xVal, max, buf);
            double pixMaxY = buf[1];
            context.mapToPixel(xVal, min, buf);
            double pixMinY = buf[1];

            double halfIqr = (iqr > 0) ? (iqr / 2.0) : 0.0;
            context.mapToPixel(xVal, median + halfIqr, buf);
            double pixQ3Y = buf[1];
            context.mapToPixel(xVal, median - halfIqr, buf);
            double pixQ1Y = buf[1];

            // Each box plot gets a distinct color from the theme palette
            Color boxColor = seriesOrBase(model, context, i);

            g2.setColor(boxColor);
            int xLine = (int) Math.round(x);
            int yMax = (int) Math.round(pixMaxY);
            int yQ3 = (int) Math.round(pixQ3Y);
            int yMin = (int) Math.round(pixMinY);
            int yQ1 = (int) Math.round(pixQ1Y);
            g2.drawLine(xLine, yMax, xLine, yQ3);
            g2.drawLine(xLine, yMin, xLine, yQ1);

            double capHalf = whiskerCap / 2.0;
            int capLeft = (int) Math.round(x - capHalf);
            int capRight = (int) Math.round(x + capHalf);
            g2.drawLine(capLeft, yMax, capRight, yMax);
            g2.drawLine(capLeft, yMin, capRight, yMin);

            double boxTopY = Math.min(pixQ3Y, pixQ1Y);
            double boxHeight = Math.abs(pixQ1Y - pixQ3Y);
            if (boxHeight < 1) boxHeight = 1;

            int xi = (int) Math.round(x - barWidth / 2.0);
            int yi = (int) Math.round(boxTopY);
            int wi = (int) Math.round(barWidth);
            int hi = (int) Math.round(boxHeight);
            BufferedImage grad = cachedGradientImage(g2, boxColor, i, wi, hi, gradients, lastColors, lastWidths, lastHeights,
                    lowColors, highColors);
            g2.drawImage(grad, xi, yi, null);
            g2.setColor(boxColor);
            int ow = Math.max(0, wi - 1);
            int oh = Math.max(0, hi - 1);
            g2.drawRect(xi, yi, ow, oh);

            g2.setColor(themeForeground(context));
            g2.setStroke(getMedianStroke(strokeWidth));
            int medianY = (int) Math.round(buf[1]);
            int medianLeft = (int) Math.round(x - barWidth / 2.0);
            int medianRight = (int) Math.round(x + barWidth / 2.0);
            g2.drawLine(medianLeft, medianY, medianRight, medianY);

            if (model instanceof BoxPlotOutlierModel outlierModel) {
                double[] outliers = outlierModel.getOutliers(i);
                if (outliers != null && outliers.length > 0) {
                    double r = ChartScale.scale(2.5f);
                    g2.setColor(cachedOutlierColor(boxColor, i, outlierColors, outlierKeys));
                    for (double outlier : outliers) {
                        context.mapToPixel(xVal, outlier, buf);
                        int size = (int) Math.round(r * 2.0);
                        int outX = (int) Math.round(buf[0] - r);
                        int outY = (int) Math.round(buf[1] - r);
                        g2.fillRect(outX, outY, size, size);
                    }
                }
            }
        }
    }

    private BufferedImage cachedGradientImage(Graphics2D g2, Color base, int index, int width, int height,
                                              BufferedImage[] gradients,
                                              int[] lastColors,
                                              int[] lastWidths,
                                              int[] lastHeights,
                                              Color[] lowColors,
                                              Color[] highColors) {
        int key = (base != null) ? base.getRGB() : 0;
        if (width < 1) width = 1;
        if (height < 1) height = 1;
        if (gradients[index] == null || lastColors[index] != key || lastWidths[index] != width || lastHeights[index] != height) {
            lowColors[index] = ColorUtils.withAlpha(base, 0.10f);
            highColors[index] = ColorUtils.withAlpha(base, 0.80f);
            gradients[index] = createCompatibleImage(g2, width, height);
            int[] row = RendererAllocationCache.getIntArray(this, "boxplot.gradRow", width);
            int lr = lowColors[index].getRed();
            int lg = lowColors[index].getGreen();
            int lb = lowColors[index].getBlue();
            int la = lowColors[index].getAlpha();
            int hr = highColors[index].getRed();
            int hg = highColors[index].getGreen();
            int hb = highColors[index].getBlue();
            int ha = highColors[index].getAlpha();
            float denom = (height - 1);
            for (int y = 0; y < height; y++) {
                float t = denom > 0 ? (y / denom) : 0f;
                int r = (int) (lr + (hr - lr) * t);
                int g = (int) (lg + (hg - lg) * t);
                int b = (int) (lb + (hb - lb) * t);
                int a = (int) (la + (ha - la) * t);
                int argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                java.util.Arrays.fill(row, argb);
                gradients[index].setRGB(0, y, width, 1, row, 0, width);
            }
            lastColors[index] = key;
            lastWidths[index] = width;
            lastHeights[index] = height;
        }
        return gradients[index];
    }

    private Color cachedOutlierColor(Color base, int index, Color[] outlierColors, int[] outlierKeys) {
        int key = (base != null) ? base.getRGB() : 0;
        if (outlierColors[index] == null || outlierKeys[index] != key) {
            outlierColors[index] = ColorUtils.withAlpha(base, 0.90f);
            outlierKeys[index] = key;
        }
        return outlierColors[index];
    }

    private BufferedImage createCompatibleImage(Graphics2D g2, int w, int h) {
        GraphicsConfiguration gc = (g2 != null) ? g2.getDeviceConfiguration() : null;
        if (gc != null) {
            return gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private Stroke getBoxStroke(float width) {
        if (cachedBoxStroke == null || cachedBoxStrokeWidth != width) {
            cachedBoxStroke = new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
            cachedBoxStrokeWidth = width;
        }
        return cachedBoxStroke;
    }

    private Stroke getMedianStroke(float width) {
        if (cachedMedianStroke == null || cachedMedianStrokeWidth != width) {
            cachedMedianStroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            cachedMedianStrokeWidth = width;
        }
        return cachedMedianStroke;
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }
}
