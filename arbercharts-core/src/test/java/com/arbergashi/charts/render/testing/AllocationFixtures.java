package com.arbergashi.charts.render.testing;

import java.util.function.Supplier;

/** Test-only constructor calls for the bytecode allocation rules. */
public final class AllocationFixtures {
    private AllocationFixtures() {}

    public static final class CachedBuilders {
        private static final StringBuilder SHARED = new StringBuilder(16);
        private final StringBuilder builder = new StringBuilder(32);

        public void render() {
            builder.setLength(0);
            SHARED.setLength(0);
        }
    }

    public static final class PerCallBuilders {
        public StringBuilder render() {
            return new StringBuilder();
        }

        public StringBuilder drawData() {
            return new StringBuilder(16);
        }

        public StringBuilder helper() {
            return new StringBuilder("label");
        }

        public Supplier<StringBuilder> constructorReference() {
            return StringBuilder::new;
        }
    }

    public static final class DesktopObjects {
        public Object[] render() {
            return new Object[] {
                new java.awt.BasicStroke(2.0f),
                new java.awt.Color(1, 2, 3),
                new java.awt.geom.Point2D.Double(1.0, 2.0)
            };
        }
    }
}
