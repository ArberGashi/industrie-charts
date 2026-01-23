package com.arbergashi.charts.jmh;

/**
 * Simple JMH runner for ad-hoc benchmark execution.
 */
public final class RunJmh {
    private RunJmh() {
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
