package com.arbergashi.charts.api;

/**
 * Immutable audit event representing a viewport action.
  * @author Arber Gashi
  * @version 2.0.0
  * @since 2026-01-30
 */
public final class ViewportAuditEvent {
    private ViewportAuditEventType type;
    private java.time.Instant timestamp;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private java.util.Map<String, Object> metadata;

    public ViewportAuditEvent(ViewportAuditEventType type,
                              java.time.Instant timestamp,
                              double minX,
                              double maxX,
                              double minY,
                              double maxY,
                              java.util.Map<String, Object> metadata) {
        this.type = type;
        this.timestamp = timestamp;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.metadata = metadata;
    }

    public ViewportAuditEventType getType() {
        return type;
    }

    public ViewportAuditEvent setType(ViewportAuditEventType type) {
        this.type = type;
        return this;
    }

    public java.time.Instant getTimestamp() {
        return timestamp;
    }

    public ViewportAuditEvent setTimestamp(java.time.Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public double getMinX() {
        return minX;
    }

    public ViewportAuditEvent setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    public double getMaxX() {
        return maxX;
    }

    public ViewportAuditEvent setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    public double getMinY() {
        return minY;
    }

    public ViewportAuditEvent setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    public double getMaxY() {
        return maxY;
    }

    public ViewportAuditEvent setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    public ViewportAuditEvent setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
}
