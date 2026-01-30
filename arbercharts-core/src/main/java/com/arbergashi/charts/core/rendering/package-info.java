/**
 * Headless rendering abstractions (ArberCanvas, matrices, buffers).
 *
 * <p>Defines the contract for platform-agnostic rendering. Bridge implementations map these
 * abstractions to Swing, Compose, native, or server backends.</p>
 *
 * <p>Zero-allocation: rendering interfaces are designed for reuse in hot paths.</p>
 *
 * @author Arber Gashi
 * @version 1.7.0
 * @since 2026-01-30
 */
package com.arbergashi.charts.core.rendering;
