package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
/**
 * Polynomial regression renderer (degree 2).
 *
 * <p>Fits a quadratic curve {@code y = a + b*x + c*x^2} using normal equations and draws it as a smooth curve.</p>
 *
 * <p><b>Performance:</b> Fitting is O(n) with a fixed-size 3x3 solve; rendering is O(m) where m is the number of segments.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PolynomialRegressionRenderer extends BaseRenderer {

    // Zero-Allocation Buffers
    private final double[] pBuffer = new double[2];
    private final double[] solveBuffer = new double[3];

    public PolynomialRegressionRenderer() {
        super("polynomialRegression");
    }

    private static boolean solve3x3(double[][] A, double[] b, double[] result) {
        // Gaussian elimination (fixed 3x3).
        double a00 = A[0][0], a01 = A[0][1], a02 = A[0][2];
        double a10 = A[1][0], a11 = A[1][1], a12 = A[1][2];
        double a20 = A[2][0], a21 = A[2][1], a22 = A[2][2];
        double b0 = b[0], b1 = b[1], b2 = b[2];

        if (Math.abs(a00) < 1e-12) return false;
        double inv00 = 1.0 / a00;
        a01 *= inv00;
        a02 *= inv00;
        b0 *= inv00;

        // Eliminate row 1
        double f10 = a10;
        a11 -= f10 * a01;
        a12 -= f10 * a02;
        b1 -= f10 * b0;

        // Eliminate row 2
        double f20 = a20;
        a21 -= f20 * a01;
        a22 -= f20 * a02;
        b2 -= f20 * b0;

        // Pivot row 1
        if (Math.abs(a11) < 1e-12) return false;
        double inv11 = 1.0 / a11;
        a12 *= inv11;
        b1 *= inv11;

        // Eliminate row 2
        double f21 = a21;
        a22 -= f21 * a12;
        b2 -= f21 * b1;

        // Pivot row 2
        if (Math.abs(a22) < 1e-12) return false;
        double inv22 = 1.0 / a22;
        b2 *= inv22;

        // Back substitute
        double c = b2;
        double bb = b1 - a12 * c;
        double aa = b0 - a02 * c - a01 * bb;

        result[0] = aa;
        result[1] = bb;
        result[2] = c;
        return true;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 3) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // Normal equations for degree-2 least squares.
        double s1 = 0, s2 = 0, s3 = 0, s4 = 0;
        double t0 = 0, t1 = 0, t2 = 0;

        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double y = yData[i];
            double x2 = x * x;
            s1 += x;
            s2 += x2;
            s3 += x2 * x;
            s4 += x2 * x2;
            t0 += y;
            t1 = Math.fma(x, y, t1);
            t2 = Math.fma(x2, y, t2);
        }

        double[][] A = {
                {(double) count, s1, s2},
                {s1, s2, s3},
                {s2, s3, s4}
        };
        double[] B = {t0, t1, t2};

        if (!solve3x3(A, B, solveBuffer)) return;
        double a = solveBuffer[0], b = solveBuffer[1], c = solveBuffer[2];

        double xMin = context.getMinX();
        double xMax = context.getMaxX();

        int segments = 64;
        double step = (xMax - xMin) / segments;
        if (step == 0) return;

        float lw = ChartAssets.getFloat("chart.analysis.lineWidth", 2.0f);
        canvas.setStroke(ChartScale.scale(lw));
        int points = segments + 1;
        float[] xs = RendererAllocationCache.getFloatArray(this, "poly.line.x", points);
        float[] ys = RendererAllocationCache.getFloatArray(this, "poly.line.y", points);
        int outCount = 0;

        for (int i = 0; i <= segments; i++) {
            double x = xMin + i * step;
            double y = a + b * x + c * x * x;
            context.mapToPixel(x, y, pBuffer);
            xs[outCount] = (float) pBuffer[0];
            ys[outCount] = (float) pBuffer[1];
            outCount++;
        }

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;
        if (isMultiColor() && accent != base) {
            canvas.setColor(accent);
            canvas.drawPolyline(xs, ys, outCount);
        }
        canvas.setColor(base);
        canvas.drawPolyline(xs, ys, outCount);
    }
}
