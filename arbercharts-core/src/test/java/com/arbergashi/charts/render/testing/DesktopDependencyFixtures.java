package com.arbergashi.charts.render.testing;

/** Test-only violations outside the core package, guarding the module boundary rule. */
public final class DesktopDependencyFixtures {
    private DesktopDependencyFixtures() {}

    public static final class AwtReference {
        public java.awt.Color color;
    }

    public static final class ImageIoReference {
        public javax.imageio.ImageReader reader;
    }

    public static final class SoundReference {
        public javax.sound.sampled.Clip clip;
    }

    public static final class GenericReference {
        public java.util.List<java.awt.Color> colors;
    }

    @java.beans.JavaBean
    public static final class AnnotationReference {}
}
