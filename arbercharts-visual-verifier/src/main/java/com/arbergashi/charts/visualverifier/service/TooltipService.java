/*
 * Copyright (c) 2024-2026 Arber Gashi. All rights reserved.
 *
 * This file is part of ArberCharts, a high-performance charting library
 * designed for real-time financial applications.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This source code is licensed under the ArberCharts Commercial License.
 * Unauthorized copying, modification, distribution, or use of this file,
 * via any medium, is strictly prohibited.
 *
 * For licensing inquiries, contact: license@arbercharts.com
 */
package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.visualverifier.dto.TooltipResponse;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

/**
 * Service for generating tooltip content based on chart coordinates.
 *
 * <p>This service analyzes the renderer type and mouse position to generate
 * appropriate tooltip content for the Visual Verifier UI.</p>
 *
 * <h2>ZERO-GC Compliance</h2>
 * <p>This service uses pooled formatters and reuses response objects
 * where possible to minimize garbage collection pressure.</p>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
public class TooltipService {

    // Thread-local formatter for ZERO-GC compliance
    private static final ThreadLocal<DecimalFormat> VALUE_FORMATTER =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00"));

    /**
     * Generates tooltip content for the specified chart position.
     *
     * @param className the fully qualified renderer class name
     * @param width     the chart width in pixels
     * @param height    the chart height in pixels
     * @param x         the mouse X coordinate
     * @param y         the mouse Y coordinate
     * @return the tooltip response with display content
     */
    public TooltipResponse generateTooltip(String className, int width, int height, int x, int y) {
        // Extract simple class name from fully qualified name
        String simpleClassName = extractSimpleClassName(className);

        // Calculate normalized position (0-1 range)
        double normalizedX = (double) x / width;
        double normalizedY = 1.0 - ((double) y / height); // Invert Y for chart coordinates

        // Generate tooltip based on renderer type
        return generateRendererSpecificTooltip(simpleClassName, normalizedX, normalizedY, x, y);
    }

    /**
     * Extracts the simple class name from a fully qualified class name.
     *
     * @param className the fully qualified class name
     * @return the simple class name
     */
    private String extractSimpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * Generates tooltip content specific to the renderer type.
     *
     * @param rendererName the simple renderer class name
     * @param normalizedX  the normalized X position (0-1)
     * @param normalizedY  the normalized Y position (0-1)
     * @param pixelX       the pixel X coordinate
     * @param pixelY       the pixel Y coordinate
     * @return the tooltip response
     */
    private TooltipResponse generateRendererSpecificTooltip(String rendererName,
                                                            double normalizedX,
                                                            double normalizedY,
                                                            int pixelX,
                                                            int pixelY) {
        DecimalFormat formatter = VALUE_FORMATTER.get();

        return switch (rendererName) {
            case "CandlestickRenderer", "OHLCRenderer" -> generateFinancialTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "LineRenderer", "AreaRenderer", "SplineRenderer" -> generateXYTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "BarRenderer", "StackedBarRenderer" -> generateBarTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "PieRenderer", "DonutRenderer", "NightingaleRoseRenderer" -> generateCircularTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "HeatmapRenderer" -> generateHeatmapTooltip(
                    normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "ScatterRenderer" -> generateScatterTooltip(
                    normalizedX, normalizedY, pixelX, pixelY, formatter);
            case "SunburstRenderer", "ChordDiagramRenderer", "TreemapRenderer" -> generateHierarchicalTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
            default -> generateGenericTooltip(
                    rendererName, normalizedX, normalizedY, pixelX, pixelY, formatter);
        };
    }

    private TooltipResponse generateFinancialTooltip(String rendererName, double normalizedX,
                                                     double normalizedY, int x, int y,
                                                     DecimalFormat formatter) {
        // Simulate OHLC data based on position
        double basePrice = 100 + normalizedX * 50;
        double open = basePrice + (Math.random() - 0.5) * 5;
        double high = Math.max(open, basePrice) + Math.random() * 3;
        double low = Math.min(open, basePrice) - Math.random() * 3;
        double close = low + Math.random() * (high - low);

        String title = rendererName.replace("Renderer", "");
        String value = String.format("O: %s  H: %s  L: %s  C: %s",
                formatter.format(open), formatter.format(high),
                formatter.format(low), formatter.format(close));
        String description = "Index: " + (int)(normalizedX * 100);

        return TooltipResponse.detailed(title, value, description, x, y, "Price", (int)(normalizedX * 100));
    }

    private TooltipResponse generateXYTooltip(String rendererName, double normalizedX,
                                              double normalizedY, int x, int y,
                                              DecimalFormat formatter) {
        double xValue = normalizedX * 100;
        double yValue = normalizedY * 100;

        String title = rendererName.replace("Renderer", "") + " Series";
        String value = String.format("X: %s, Y: %s", formatter.format(xValue), formatter.format(yValue));

        return TooltipResponse.of(title, value, x, y);
    }

    private TooltipResponse generateBarTooltip(String rendererName, double normalizedX,
                                               double normalizedY, int x, int y,
                                               DecimalFormat formatter) {
        int categoryIndex = (int)(normalizedX * 10);
        double value = normalizedY * 100;

        String title = "Category " + (categoryIndex + 1);
        String valueStr = "Value: " + formatter.format(value);

        return TooltipResponse.detailed(title, valueStr, rendererName.replace("Renderer", ""),
                x, y, "Series 1", categoryIndex);
    }

    private TooltipResponse generateCircularTooltip(String rendererName, double normalizedX,
                                                    double normalizedY, int x, int y,
                                                    DecimalFormat formatter) {
        // Calculate angle from center
        double centerX = 0.5;
        double centerY = 0.5;
        double angle = Math.atan2(normalizedY - centerY, normalizedX - centerX);
        double normalizedAngle = (angle + Math.PI) / (2 * Math.PI);

        int segmentIndex = (int)(normalizedAngle * 6) % 6;
        String[] segments = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta"};
        double percentage = 10 + segmentIndex * 5 + Math.random() * 10;

        String title = segments[segmentIndex];
        String value = formatter.format(percentage) + "%";

        return TooltipResponse.detailed(title, value, rendererName.replace("Renderer", ""),
                x, y, segments[segmentIndex], segmentIndex);
    }

    private TooltipResponse generateHeatmapTooltip(double normalizedX, double normalizedY,
                                                   int x, int y, DecimalFormat formatter) {
        int row = (int)(normalizedY * 10);
        int col = (int)(normalizedX * 10);
        double intensity = Math.sin(normalizedX * Math.PI) * Math.cos(normalizedY * Math.PI) * 100;

        String title = String.format("Cell [%d, %d]", row, col);
        String value = "Intensity: " + formatter.format(Math.abs(intensity));

        return TooltipResponse.of(title, value, x, y);
    }

    private TooltipResponse generateScatterTooltip(double normalizedX, double normalizedY,
                                                   int x, int y, DecimalFormat formatter) {
        double xValue = normalizedX * 100;
        double yValue = normalizedY * 100;
        double size = 5 + Math.random() * 20;

        String title = "Data Point";
        String value = String.format("(%s, %s) Size: %s",
                formatter.format(xValue), formatter.format(yValue), formatter.format(size));

        return TooltipResponse.of(title, value, x, y);
    }

    private TooltipResponse generateHierarchicalTooltip(String rendererName, double normalizedX,
                                                        double normalizedY, int x, int y,
                                                        DecimalFormat formatter) {
        String[] levels = {"Root", "Branch A", "Branch B", "Leaf 1", "Leaf 2", "Leaf 3"};
        int levelIndex = (int)((normalizedX + normalizedY) * 3) % levels.length;
        double value = 50 + Math.random() * 100;

        String title = levels[levelIndex];
        String valueStr = "Value: " + formatter.format(value);
        String description = "Depth: " + (levelIndex / 2);

        return TooltipResponse.detailed(title, valueStr, description, x, y,
                rendererName.replace("Renderer", ""), levelIndex);
    }

    private TooltipResponse generateGenericTooltip(String rendererName, double normalizedX,
                                                   double normalizedY, int x, int y,
                                                   DecimalFormat formatter) {
        String title = rendererName.replace("Renderer", "");
        String value = String.format("Position: (%s, %s)",
                formatter.format(normalizedX * 100), formatter.format(normalizedY * 100));

        return TooltipResponse.of(title, value, x, y);
    }
}

