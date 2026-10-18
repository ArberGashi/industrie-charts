package com.arbergashi.charts.platform.swing.spatial;

import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SwingSpatialBatchConsumerTest {
    @Test
    void fillsWithTheCurrentBuilderStyleAndPreservesGraphicsState() {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            Paint originalPaint = new GradientPaint(0, 0, Color.GREEN, 80, 80, Color.BLUE);
            graphics.setPaint(originalPaint);
            Color originalColor = graphics.getColor();
            graphics.translate(3, 4);
            graphics.setClip(new Rectangle(10, 10, 20, 20));
            AffineTransform transform = graphics.getTransform();
            Rectangle clip = graphics.getClipBounds();
            SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
            builder.setStyleKey(SpatialStyleDescriptor.pack(0xFFFF0000, 1f, 0, 0));
            SwingSpatialBatchConsumer consumer = new SwingSpatialBatchConsumer(builder)
                    .setGraphics(graphics).setColor(Color.BLUE);

            consumer.fillQuad(0, 0, 60, 0, 60, 60, 0, 60);

            assertEquals(0xFFFF0000, image.getRGB(20, 20), "Fills must use the renderer's style, not the preceding grid color");
            assertEquals(0, image.getRGB(5, 5), "The host clip must still apply");
            assertSame(originalPaint, graphics.getPaint());
            assertEquals(originalColor, graphics.getColor());
            assertEquals(transform, graphics.getTransform());
            assertEquals(clip, graphics.getClipBounds());
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void fillsUnstyledQuadsWithTheConfiguredSeriesColor() {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.GREEN);
            SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder().setStyleKey(0L);
            SwingSpatialBatchConsumer consumer = new SwingSpatialBatchConsumer(builder)
                    .setGraphics(graphics).setColor(Color.BLUE);
            consumer.fillQuad(5, 5, 30, 5, 30, 30, 5, 30);
            assertEquals(Color.BLUE.getRGB(), image.getRGB(15, 15));
            assertEquals(Color.GREEN, graphics.getColor());
        } finally {
            graphics.dispose();
        }
    }
}
