package com.arbergashi.charts.visualverifier.dto;

/**
 * Result of a visual regression comparison.
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
public record ComparisonResult(
        String renderer,
        Status status,
        boolean matches,
        double matchPercentage,
        int totalPixels,
        int diffPixels,
        String message,
        byte[] diffImage
) {
    /**
     * Comparison status.
     */
    public enum Status {
        /** Images match within tolerance */
        MATCH,
        /** Images differ beyond tolerance */
        MISMATCH,
        /** No baseline exists */
        NO_BASELINE,
        /** Image dimensions don't match */
        SIZE_MISMATCH,
        /** Error during comparison */
        ERROR
    }

    /**
     * Creates a result indicating no baseline exists.
     */
    public static ComparisonResult noBaseline(String renderer) {
        return new ComparisonResult(renderer, Status.NO_BASELINE, false, 0, 0, 0,
                "No baseline snapshot exists. Current render saved as baseline.", null);
    }

    /**
     * Creates a result indicating a size mismatch.
     */
    public static ComparisonResult sizeMismatch(String renderer, int baseW, int baseH, int currW, int currH) {
        return new ComparisonResult(renderer, Status.SIZE_MISMATCH, false, 0, 0, 0,
                String.format("Size mismatch: baseline=%dx%d, current=%dx%d", baseW, baseH, currW, currH), null);
    }

    /**
     * Creates an error result.
     */
    public static ComparisonResult error(String renderer, String message) {
        return new ComparisonResult(renderer, Status.ERROR, false, 0, 0, 0, message, null);
    }

    /**
     * Builder for ComparisonResult.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String renderer;
        private boolean matches;
        private double matchPercentage;
        private int totalPixels;
        private int diffPixels;
        private byte[] diffImage;

        public Builder renderer(String renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder matches(boolean matches) {
            this.matches = matches;
            return this;
        }

        public Builder matchPercentage(double matchPercentage) {
            this.matchPercentage = matchPercentage;
            return this;
        }

        public Builder totalPixels(int totalPixels) {
            this.totalPixels = totalPixels;
            return this;
        }

        public Builder diffPixels(int diffPixels) {
            this.diffPixels = diffPixels;
            return this;
        }

        public Builder diffImage(byte[] diffImage) {
            this.diffImage = diffImage;
            return this;
        }

        public ComparisonResult build() {
            Status status = matches ? Status.MATCH : Status.MISMATCH;
            String message = matches
                    ? String.format("Match: %.2f%% (%d pixels)", matchPercentage * 100, totalPixels)
                    : String.format("Mismatch: %.2f%% match, %d pixels differ", matchPercentage * 100, diffPixels);
            return new ComparisonResult(renderer, status, matches, matchPercentage, totalPixels, diffPixels, message, diffImage);
        }
    }
}

