package com.arbergashi.charts.api;

import java.util.List;
/**
 * Minimal JSON serializer for audit events.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class ViewportAuditJson {
    private ViewportAuditJson() {
    }

    public static String toJson(List<ViewportAuditEvent> events) {
        return toJson(events, false);
    }

    public static String toJson(ViewportAuditTrail trail) {
        if (trail == null) {
            return toJson((List<ViewportAuditEvent>) null, false);
        }
        return toJson(trail.events(), trail.isTruncated());
    }

    public static String toJson(List<ViewportAuditEvent> events, boolean truncated) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"audit_truncated\":").append(truncated).append(",\"events\":[");
        if (events != null) {
            boolean first = true;
            for (ViewportAuditEvent e : events) {
                if (!first) sb.append(',');
                first = false;
                appendEvent(sb, e);
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void appendEvent(StringBuilder sb, ViewportAuditEvent e) {
        sb.append("{\"type\":\"").append(e.getType()).append('"');
        sb.append(",\"timestamp\":\"").append(e.getTimestamp()).append('"');
        sb.append(",\"minX\":").append(e.getMinX());
        sb.append(",\"maxX\":").append(e.getMaxX());
        sb.append(",\"minY\":").append(e.getMinY());
        sb.append(",\"maxY\":").append(e.getMaxY());
        sb.append(",\"metadata\":{");
        boolean first = true;
        if (e.getMetadata() != null) {
            for (var entry : e.getMetadata().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(entry.getKey())).append('"')
                        .append(":\"").append(escape(String.valueOf(entry.getValue()))).append('"');
            }
        }
        sb.append("}}");
    }

    private static String escape(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
