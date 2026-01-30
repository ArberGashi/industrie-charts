package com.arbergashi.charts.api;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
/**
 * In-memory append-only audit trail.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class ViewportAuditTrail implements ViewportAuditLogger {
    private static final int DEFAULT_CAPACITY = 10_000;
    private final int capacity;
    private final Deque<ViewportAuditEvent> events;
    private boolean truncated;

    public ViewportAuditTrail() {
        this(DEFAULT_CAPACITY);
    }

    public ViewportAuditTrail(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.events = new ArrayDeque<>(Math.min(capacity, 256));
    }

    @Override
    public synchronized void onEvent(ViewportAuditEvent event) {
        if (event != null) {
            if (events.size() >= capacity) {
                events.pollFirst();
                truncated = true;
            }
            events.addLast(event);
        }
    }

    public synchronized List<ViewportAuditEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized boolean isTruncated() {
        return truncated;
    }

    /**
     * Compacts consecutive PAN events into a single start/end record.
     */
    public synchronized void compact() {
        if (events.isEmpty()) return;
        List<ViewportAuditEvent> compacted = new ArrayList<>(events.size());
        ViewportAuditEvent panStart = null;
        ViewportAuditEvent lastPan = null;

        for (ViewportAuditEvent event : events) {
            if (event.getType() == ViewportAuditEventType.PAN) {
                if (panStart == null) {
                    panStart = event;
                }
                lastPan = event;
                continue;
            }

            if (panStart != null && lastPan != null) {
                compacted.add(compactPan(panStart, lastPan));
                panStart = null;
                lastPan = null;
            }
            compacted.add(event);
        }

        if (panStart != null && lastPan != null) {
            compacted.add(compactPan(panStart, lastPan));
        }

        events.clear();
        for (ViewportAuditEvent event : compacted) {
            if (events.size() >= capacity) {
                events.pollFirst();
                truncated = true;
            }
            events.addLast(event);
        }
    }

    private ViewportAuditEvent compactPan(ViewportAuditEvent start, ViewportAuditEvent end) {
        Map<String, Object> metadata = Map.of(
                "panStartMinX", Double.toString(start.getMinX()),
                "panStartMaxX", Double.toString(start.getMaxX()),
                "panStartMinY", Double.toString(start.getMinY()),
                "panStartMaxY", Double.toString(start.getMaxY()),
                "panEndMinX", Double.toString(end.getMinX()),
                "panEndMaxX", Double.toString(end.getMaxX()),
                "panEndMinY", Double.toString(end.getMinY()),
                "panEndMaxY", Double.toString(end.getMaxY())
        );
        Instant timestamp = start.getTimestamp();
        return new ViewportAuditEvent(
                ViewportAuditEventType.PAN,
                timestamp,
                end.getMinX(),
                end.getMaxX(),
                end.getMinY(),
                end.getMaxY(),
                metadata
        );
    }
}
