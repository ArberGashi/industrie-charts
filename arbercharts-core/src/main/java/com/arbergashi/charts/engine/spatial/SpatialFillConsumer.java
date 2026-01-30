package com.arbergashi.charts.engine.spatial;

/**
 * Optional consumer for filling simple quadrilaterals in screen space.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface SpatialFillConsumer {
    /**
     * Fills a quad given its 2D projected coordinates.
     *
     * @param x1 first x
     * @param y1 first y
     * @param x2 second x
     * @param y2 second y
     * @param x3 third x
     * @param y3 third y
     * @param x4 fourth x
     * @param y4 fourth y
     */
    void fillQuad(double x1, double y1,
                  double x2, double y2,
                  double x3, double y3,
                  double x4, double y4);
}
