/**
 * Public core API contracts for ArberCharts.
 *
 * <p>Defines the stable interfaces for themes, plot context, animation profiles, and render hints.
 * This package is UI-framework agnostic and safe to use across bridge implementations.</p>
 *
 * <p>Thread-safety: API types are designed for concurrent reads; mutable implementations must
 * document their own concurrency behavior.</p>
 *
 * <p>Zero-allocation: APIs are optimized for hot-path usage and avoid allocations in render loops.</p>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-30
 */
package com.arbergashi.charts.api;
