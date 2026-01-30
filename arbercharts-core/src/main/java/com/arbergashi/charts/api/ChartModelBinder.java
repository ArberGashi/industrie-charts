package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import java.util.Collection;
import java.util.function.ToDoubleFunction;
/**
 * Utility for binding plain Java objects (POJOs) into chart models.
 *
 * <p>This helper is intentionally minimal: it maps a collection into a {@link DefaultChartModel}
 * by extracting X/Y values via the provided functions. It does not perform aggregation,
 * sorting, or validation beyond iterating the given collection.</p>
 *
 * <p><b>Framework note:</b> For large datasets, consider pre-processing your data (e.g., sorting,
 * decimation, filtering) before binding to avoid unnecessary allocations and to keep rendering
 * responsive.</p>
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartModelBinder {

    private ChartModelBinder() {}

    /**
     * Binds a collection of POJOs to a DefaultChartModel.
     *
     * @param data The collection of POJOs.
     * @param name The name of the series.
     * @param xExtractor Function to extract X value.
     * @param yExtractor Function to extract Y value.
     * @param <T> The POJO type.
     * @return A DefaultChartModel populated with data.
     */
    public static <T> DefaultChartModel bind(Collection<T> data, String name, 
                                            ToDoubleFunction<T> xExtractor, 
                                            ToDoubleFunction<T> yExtractor) {
        DefaultChartModel model = new DefaultChartModel(name);
        for (T item : data) {
            model.setPoint(xExtractor.applyAsDouble(item), yExtractor.applyAsDouble(item), 0, null);
        }
        return model;
    }
}
