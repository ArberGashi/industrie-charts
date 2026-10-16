package com.arbergashi.charts.demo;

import com.arbergashi.charts.render.analysis.VectorFieldRenderer;

/** Uses one held simulation time for every arrow in a demo frame. */
final class ShowcaseVectorFieldRenderer extends VectorFieldRenderer {
    private final SwirlField field;

    ShowcaseVectorFieldRenderer() {
        this(new SwirlField());
    }

    private ShowcaseVectorFieldRenderer(SwirlField field) {
        super(field);
        this.field = field;
        setGridResolution(16);
    }

    void setElapsedSeconds(double seconds) {
        field.elapsedSeconds = seconds;
    }

    private static final class SwirlField implements VectorFieldFunction {
        private double elapsedSeconds;

        @Override
        public boolean compute(double x, double y, double[] out) {
            double time = elapsedSeconds;
            double pulse = 0.75 + 0.25 * Math.sin(time * 0.9);
            out[0] = (Math.cos(y * 0.24 + time * 1.1) + Math.sin(x * 0.13 - time * 0.7)) * pulse;
            out[1] = (Math.sin(x * 0.24 - time * 0.95) - Math.cos(y * 0.13 + time * 0.8)) * pulse;
            return true;
        }
    }
}
