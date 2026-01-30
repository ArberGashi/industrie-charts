package com.arbergashi.charts.util;
/**
 * Small predictive helpers kept allocation-free for render-time use.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PredictiveMath {

    private PredictiveMath() {
    }

    public static double smoothDelta(double previous, double delta, double smoothing) {
        double s = MathUtils.clamp(smoothing, 0.0, 0.999);
        if (!Double.isFinite(previous)) {
            return delta;
        }
        double gain = 1.0 - s;
        return previous * s + delta * gain;
    }

    public static double extrapolate(double value, double deltaPerTick, int lookaheadTicks) {
        int ticks = Math.max(1, lookaheadTicks);
        return value + deltaPerTick * ticks;
    }
}

