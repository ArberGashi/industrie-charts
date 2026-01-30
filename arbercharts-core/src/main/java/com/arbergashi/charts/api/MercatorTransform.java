package com.arbergashi.charts.api;
/**
 * Mercator projection transformer for geographic charts.
 *
 * <p>Uses PlotContext min/max as longitude/latitude bounds in degrees.</p>
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class MercatorTransform implements CoordinateTransformer {
    private static final double MAX_LAT = 85.0;

    @Override
    public void mapToPixel(PlotContext context, double lon, double lat, double[] out) {
        double minLon = context.getMinX();
        double maxLon = context.getMaxX();
        double minLat = clampLat(context.getMinY());
        double maxLat = clampLat(context.getMaxY());

        double xRatio = (lon - minLon) / (maxLon - minLon);
        double mercMin = mercatorY(minLat);
        double mercMax = mercatorY(maxLat);
        double yRatio = (mercatorY(clampLat(lat)) - mercMin) / (mercMax - mercMin);

        double bx = context.getPlotBounds().x();
        double by = context.getPlotBounds().y();
        double bw = context.getPlotBounds().width();
        double bh = context.getPlotBounds().height();

        out[0] = bx + xRatio * bw;
        out[1] = by + bh - yRatio * bh;
    }

    @Override
    public void mapToData(PlotContext context, double pixelX, double pixelY, double[] out) {
        double bx = context.getPlotBounds().x();
        double by = context.getPlotBounds().y();
        double bw = context.getPlotBounds().width();
        double bh = context.getPlotBounds().height();

        double minLon = context.getMinX();
        double maxLon = context.getMaxX();
        double minLat = clampLat(context.getMinY());
        double maxLat = clampLat(context.getMaxY());

        double xRatio = (pixelX - bx) / bw;
        double yRatio = (by + bh - pixelY) / bh;

        double lon = minLon + xRatio * (maxLon - minLon);
        double mercMin = mercatorY(minLat);
        double mercMax = mercatorY(maxLat);
        double mercY = mercMin + yRatio * (mercMax - mercMin);
        double lat = inverseMercatorY(mercY);

        out[0] = lon;
        out[1] = lat;
    }

    private static double mercatorY(double lat) {
        double rad = Math.toRadians(lat);
        return Math.log(Math.tan(Math.PI / 4.0 + rad / 2.0));
    }

    private static double inverseMercatorY(double y) {
        return Math.toDegrees(2.0 * Math.atan(Math.exp(y)) - Math.PI / 2.0);
    }

    private static double clampLat(double lat) {
        if (lat > MAX_LAT) return MAX_LAT;
        if (lat < -MAX_LAT) return -MAX_LAT;
        return lat;
    }
}
