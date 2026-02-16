package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * <h1>MarimekkoRenderer - Marimekko/Mosaic Chart</h1>
 *
 * <p>Professional Marimekko chart renderer for visualizing two-dimensional categorical data.
 * Both width and height of rectangles represent proportions.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Two-Dimensional:</b> Width = category size, Height = subcategory proportion</li>
 *   <li><b>Proportional Areas:</b> Rectangle area represents total contribution</li>
 *   <li><b>Market Analysis:</b> Perfect for market share and segment analysis</li>
 *   <li><b>Color-Coded:</b> Different colors for subcategories</li>
 * </ul>
 *
 * <h2>Data Mapping:</h2>
 * <pre>
 * ChartPoint fields:
 *   x       → Category index
 *   y       → Value/proportion
 *   weight  → Category width (optional, defaults to equal)
 *   label   → Subcategory name
 * </pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Market segmentation analysis</li>
 *   <li>Product portfolio visualization</li>
 *   <li>Revenue breakdown by region and product</li>
 *   <li>Customer demographics</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class MarimekkoRenderer extends BaseRenderer {

    public MarimekkoRenderer() {
        super("marimekko");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ChartTheme theme = getResolvedTheme(context);
        // Group by category (category = x)
        java.util.Map<Double, java.util.List<Integer>> categories = com.arbergashi.charts.tools.RendererAllocationCache.getMap(this, "marimekko.categories");
        for (int i = 0; i < count; i++) {
            double key = model.getX(i);
            categories.computeIfAbsent(key, k -> com.arbergashi.charts.tools.RendererAllocationCache.getList(this, "marimekko.cat." + k)).add(i);
        }

        // Calculate category widths and totals
        Map<Double, Double> categoryWidths = getCalculatedCategoryWidths(categories, model);
        double totalWidth = 0.0;
        for (Double v : categoryWidths.values()) totalWidth += v;

        // Color palette for subcategories
        ArberColor baseColor = getSeriesColor(model);
        ArberColor[] colors = generateColorPalette(getMaxSubcategories(categories), theme, baseColor);

        ArberRect bounds = context.getPlotBounds();
        double currentX = bounds.x();

        for (Map.Entry<Double, List<Integer>> entry : categories.entrySet()) {
            double categoryKey = entry.getKey();
            List<Integer> subcategories = entry.getValue();

            double categoryWidth = (categoryWidths.get(categoryKey) / totalWidth) * bounds.width();

            // Calculate total for this category
            double categoryTotal = 0.0;
            for (int idx : subcategories) categoryTotal += model.getY(idx);

            // Draw stacked rectangles
            double currentY = bounds.y();

            for (int i = 0; i < subcategories.size(); i++) {
                int subIdx = subcategories.get(i);
                double proportion = model.getY(subIdx) / categoryTotal;
                double rectHeight = proportion * bounds.height();

                ArberColor fillColor = colors[i % colors.length];
                canvas.setColor(fillColor);
                canvas.fillRect((float) currentX, (float) currentY, (float) categoryWidth, (float) rectHeight);

                canvas.setColor(ColorUtils.adjustBrightness(fillColor, 0.7f));
                canvas.setStroke(ChartScale.scale(1.0f));
                canvas.drawRect((float) currentX, (float) currentY, (float) categoryWidth, (float) rectHeight);

                currentY += rectHeight;
            }

            currentX += categoryWidth;
        }
    }

    private Map<Double, Double> getCalculatedCategoryWidths(Map<Double, List<Integer>> categories, ChartModel model) {
        Map<Double, Double> widths = new LinkedHashMap<>();
        for (Map.Entry<Double, List<Integer>> entry : categories.entrySet()) {
            double total = 0.0;
            for (int idx : entry.getValue()) total += model.getY(idx);
            // Use weight of first element if available, otherwise use total value
            double width = (entry.getValue().isEmpty() ? 0.0 : model.getWeight(entry.getValue().get(0))) > 0 ?
                    model.getWeight(entry.getValue().get(0)) : total;
            widths.put(entry.getKey(), width);
        }
        return widths;
    }

    private int getMaxSubcategories(Map<Double, List<Integer>> categories) {
        return categories.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);
    }

    private ArberColor[] generateColorPalette(int count, ChartTheme theme, ArberColor baseColor) {
        ArberColor[] palette = new ArberColor[count];
        for (int i = 0; i < count; i++) {
            palette[i] = isMultiColor() ? theme.getSeriesColor(i) : baseColor;
        }
        return palette;
    }
}
