package com.arbergashi.charts.api;

import java.time.Instant;
import java.util.Map;
/**
 * Append-only audit sink for viewport and filter changes.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface ViewportAuditLogger {

    void onEvent(ViewportAuditEvent event);

    default void onZoom(double minX, double maxX, double minY, double maxY) {
        onEvent(new ViewportAuditEvent(ViewportAuditEventType.ZOOM, Instant.now(), minX, maxX, minY, maxY, Map.of()));
    }

    default void onPan(double minX, double maxX, double minY, double maxY, double deltaX) {
        onEvent(new ViewportAuditEvent(ViewportAuditEventType.PAN, Instant.now(), minX, maxX, minY, maxY,
                Map.of("deltaX", Double.toString(deltaX))));
    }

    default void onReset(double minX, double maxX, double minY, double maxY) {
        onEvent(new ViewportAuditEvent(ViewportAuditEventType.RESET, Instant.now(), minX, maxX, minY, maxY, Map.of()));
    }

    default void onAxisConfig(String axisId, String details) {
        onEvent(new ViewportAuditEvent(ViewportAuditEventType.AXIS_CONFIG, Instant.now(),
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                Map.of("axis", axisId, "details", details)));
    }

    default void onFilterChange(String filterId, String state) {
        onEvent(new ViewportAuditEvent(ViewportAuditEventType.FILTER_CHANGE, Instant.now(),
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                Map.of("filter", filterId, "state", state)));
    }
}
