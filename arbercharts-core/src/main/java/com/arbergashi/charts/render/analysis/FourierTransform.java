package com.arbergashi.charts.render.analysis;

import java.util.Arrays;

/** Package-local radix-2 transform shared by the frequency renderer and its public helper. */
final class FourierTransform {
    private FourierTransform() {
    }

    static int paddedLength(int count) {
        if (count <= 0) return 0;
        if (count > (1 << 30)) throw new IllegalArgumentException("Signal is too large");
        return count == 1 ? 1 : Integer.highestOneBit(count - 1) << 1;
    }

    static void compute(double[] input, int count, double[] real, double[] imaginary, int length) {
        System.arraycopy(input, 0, real, 0, count);
        Arrays.fill(real, count, length, 0.0);
        Arrays.fill(imaginary, 0, length, 0.0);
        for (int i = 1, j = 0; i < length; i++) {
            int bit = length >>> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>>= 1;
            }
            j ^= bit;
            if (i < j) {
                double value = real[i];
                real[i] = real[j];
                real[j] = value;
            }
        }
        for (int size = 2; size <= length; size <<= 1) {
            int half = size >>> 1;
            double angle = -2.0 * Math.PI / size;
            double stepReal = Math.cos(angle);
            double stepImaginary = Math.sin(angle);
            for (int start = 0; start < length; start += size) {
                double wr = 1.0;
                double wi = 0.0;
                for (int j = 0; j < half; j++) {
                    int a = start + j;
                    int b = a + half;
                    double tr = wr * real[b] - wi * imaginary[b];
                    double ti = wr * imaginary[b] + wi * real[b];
                    real[b] = real[a] - tr;
                    imaginary[b] = imaginary[a] - ti;
                    real[a] += tr;
                    imaginary[a] += ti;
                    double nextReal = wr * stepReal - wi * stepImaginary;
                    wi = wr * stepImaginary + wi * stepReal;
                    wr = nextReal;
                }
            }
            if (size == length) break;
        }
    }

    static double amplitude(double[] real, double[] imaginary, int bin, int length, int count) {
        double factor = bin == 0 || bin == length / 2 ? 1.0 : 2.0;
        return Math.hypot(real[bin], imaginary[bin]) * factor / count;
    }

    static double samplingInterval(double[] x, double[] y, int count) {
        if (count < 2) return Double.NaN;
        double step = (x[count - 1] - x[0]) / (count - 1);
        if (!Double.isFinite(step) || step <= 0.0) return Double.NaN;
        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) return Double.NaN;
            if (i > 0) {
                double delta = x[i] - x[i - 1];
                if (!Double.isFinite(delta) || delta <= 0.0) return Double.NaN;
                // Absolute timestamps lose precision, but their ULP must never
                // make duplicate, reversed or materially uneven intervals valid.
                double rounding = 2.0 * Math.max(Math.ulp(x[i]), Math.ulp(x[i - 1]));
                double tolerance = Math.min(step * 1e-3, Math.max(step * 1e-6, rounding));
                if (Math.abs(delta - step) > tolerance) return Double.NaN;
            }
        }
        return step;
    }
}
