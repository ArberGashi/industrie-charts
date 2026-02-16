package com.arbergashi.charts.api.forensic;
/**
 * Buffering strategy for live stream ingestion.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public enum StreamBufferStrategy {
    DROP_OLDEST,
    DROP_NEWEST,
    COALESCE,
    BLOCK
}
