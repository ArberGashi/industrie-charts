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
package com.arbergashi.charts.visualverifier.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object for tooltip responses.
 *
 * <p>This DTO encapsulates tooltip information returned by the tooltip
 * API endpoint, including display text and positioning hints.</p>
 *
 * <h2>ZERO-GC Compliance</h2>
 * <p>This record is designed to be lightweight and immutable,
 * minimizing garbage collection pressure in frequent tooltip updates.</p>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TooltipResponse(
        /**
         * The main tooltip title.
         */
        String title,

        /**
         * The formatted value to display.
         */
        String value,

        /**
         * Additional information or description.
         */
        String description,

        /**
         * The X coordinate for tooltip positioning.
         */
        int x,

        /**
         * The Y coordinate for tooltip positioning.
         */
        int y,

        /**
         * Whether the tooltip should be visible.
         */
        boolean visible,

        /**
         * Optional series name for multi-series charts.
         */
        String seriesName,

        /**
         * Optional data index within the series.
         */
        Integer dataIndex
) {
    /**
     * Creates an empty/hidden tooltip response.
     *
     * @return a tooltip response with visible set to false
     */
    public static TooltipResponse hidden() {
        return new TooltipResponse(null, null, null, 0, 0, false, null, null);
    }

    /**
     * Creates a simple tooltip response with title and value.
     *
     * @param title the tooltip title
     * @param value the formatted value
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @return a visible tooltip response
     */
    public static TooltipResponse of(String title, String value, int x, int y) {
        return new TooltipResponse(title, value, null, x, y, true, null, null);
    }

    /**
     * Creates a detailed tooltip response with all information.
     *
     * @param title       the tooltip title
     * @param value       the formatted value
     * @param description additional description
     * @param x           the X coordinate
     * @param y           the Y coordinate
     * @param seriesName  the series name
     * @param dataIndex   the data index
     * @return a fully populated tooltip response
     */
    public static TooltipResponse detailed(String title, String value, String description,
                                           int x, int y, String seriesName, int dataIndex) {
        return new TooltipResponse(title, value, description, x, y, true, seriesName, dataIndex);
    }
}

