/**
 * AWT object caches and scratch buffers for desktop integrations.
 *
 * <p>This package is provided by {@code arbercharts-swing-bridge} since 2.2.0.
 * Its package name and public signatures are retained for existing desktop
 * callers. Platform-neutral Core code must not depend on these AWT utilities.</p>
 *
 * <p>{@link com.arbergashi.charts.engine.allocation.ZeroAllocPool} keeps separate
 * stroke, color and array storage for each thread. Cache misses, lookup keys
 * and growing buffers can allocate. Cache reuse alone does not establish a
 * zero-allocation or latency guarantee; use measurements for those claims.</p>
 *
 * <p>Scratch arrays may be reused by subsequent calls on the same thread.
 * Consume them immediately and do not retain their contents across render passes.</p>
 *
 * @since 2.0.0
 */
package com.arbergashi.charts.engine.allocation;
