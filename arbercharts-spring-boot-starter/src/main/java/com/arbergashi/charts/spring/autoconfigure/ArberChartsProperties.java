package com.arbergashi.charts.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arber.charts")
public final class ArberChartsProperties {
    /**
     * Size of the render session pool.
     */
    private int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors());

    /**
     * Enable per-render timing metrics at DEBUG level.
     */
    private boolean metricsEnabled = true;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = Math.max(1, poolSize);
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
}
