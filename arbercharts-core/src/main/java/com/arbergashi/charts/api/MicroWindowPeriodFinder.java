package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;

/**
 * Fast, allocation-free period scout over a very small trailing window.
 */
final class MicroWindowPeriodFinder {
    private final int windowSize;
    private final double minPeriod;
    private final double maxPeriod;

    MicroWindowPeriodFinder(int windowSize, double minPeriod, double maxPeriod) {
        this.windowSize = Math.max(6, windowSize);
        this.minPeriod = minPeriod;
        this.maxPeriod = maxPeriod;
    }

    Result find(ChartModel model, int count) {
        int start = Math.max(0, count - windowSize);
        double prevX = Double.NaN;
        double prevY = Double.NaN;
        double prevCross = Double.NaN;
        double lastCross = Double.NaN;
        boolean lastCrossPositiveSlope = true;
        for (int i = start; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            if (Double.isFinite(prevX)) {
                double dx = x - prevX;
                boolean signChange = (prevY <= 0.0 && y > 0.0) || (prevY >= 0.0 && y < 0.0);
                if (signChange) {
                    double denom = y - prevY;
                    if (Math.abs(denom) > 1e-12) {
                        double t = -prevY / denom;
                        double crossX = prevX + dx * t;
                        prevCross = lastCross;
                        lastCross = crossX;
                        lastCrossPositiveSlope = denom > 0.0;
                    }
                }
            }
            prevX = x;
            prevY = y;
        }
        double period = Double.NaN;
        if (Double.isFinite(prevCross) && Double.isFinite(lastCross)) {
            // Consecutive sign changes represent half a period.
            double p = Math.abs(lastCross - prevCross) * 2.0;
            if (p >= minPeriod && p <= maxPeriod) period = p;
        }
        return new Result(period, lastCross, lastCrossPositiveSlope);
    }

    static final class Result {
        private double period;
        private double anchorX;
        private boolean positiveSlopeCrossing;

        Result(double period, double anchorX, boolean positiveSlopeCrossing) {
            this.period = period;
            this.anchorX = anchorX;
            this.positiveSlopeCrossing = positiveSlopeCrossing;
        }

        double getPeriod() {
            return period;
        }

        void setPeriod(double period) {
            this.period = period;
        }

        double getAnchorX() {
            return anchorX;
        }

        void setAnchorX(double anchorX) {
            this.anchorX = anchorX;
        }

        boolean isPositiveSlopeCrossing() {
            return positiveSlopeCrossing;
        }

        void setPositiveSlopeCrossing(boolean positiveSlopeCrossing) {
            this.positiveSlopeCrossing = positiveSlopeCrossing;
        }

        boolean isPeriodAvailable() {
            return Double.isFinite(period);
        }
    }
}
