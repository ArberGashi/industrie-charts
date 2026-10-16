package com.arbergashi.charts.render.medical;

/** Shared erase-gap calculations for physical ring-buffer positions. */
final class MedicalSweepSupport {
    private MedicalSweepSupport() {}

    /** Keeps the newest sample visible even when the configured gap exceeds the buffer. */
    static boolean isInGap(int index, int head, int capacity, int gapWidth) {
        int distance = index - head;
        if (distance < 0) distance += capacity;
        int width = Math.min(Math.max(0, gapWidth), Math.max(0, capacity - 1));
        return distance < width;
    }
}
