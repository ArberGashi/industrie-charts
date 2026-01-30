/**
 * Data model layer for ArberCharts.
 *
 * <p>Provides zero-allocation models for financial, medical, and analytical data. Implementations
 * often expose primitive arrays and require consumers to respect {@code getPointCount()}.</p>
 *
 * <p>Thread-safety: Several models support lock-free reads with concurrent writers; each model
 * documents its own guarantees and limits. See docs/CONCURRENCY_MODEL.md for core semantics.</p>
 *
 * @author Arber Gashi
 * @version 1.7.0
 * @since 2026-01-30
 */
package com.arbergashi.charts.model;
