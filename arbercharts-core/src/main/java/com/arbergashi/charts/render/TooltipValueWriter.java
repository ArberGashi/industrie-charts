package com.arbergashi.charts.render;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.FastNumberFormatter;
/**
 * Zero-allocation tooltip value writer.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class TooltipValueWriter {
    private TooltipValueWriter() {
    }

    public static void appendAxisValue(StringBuilder sb, AxisConfig axis, double value) {
        int decimals = ChartAssets.getInt("Chart.tooltip.decimals", 3);
        FastNumberFormatter.appendFixed(sb, value, decimals);
        String unit = axis != null ? axis.getUnitSuffix() : "";
        if (unit != null && !unit.isEmpty()) {
            sb.append(' ').append(unit);
        }
    }
}
