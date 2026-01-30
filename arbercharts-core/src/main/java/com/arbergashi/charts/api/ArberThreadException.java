package com.arbergashi.charts.api;
/**
 * Thrown when an operation is executed on the wrong thread.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class ArberThreadException extends IllegalStateException {
    public ArberThreadException(String message) {
        super(message);
    }
}
