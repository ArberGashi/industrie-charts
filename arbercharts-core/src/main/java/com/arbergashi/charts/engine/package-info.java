/**
 * Core engine utilities for scheduling, synchronization, and deterministic replay.
 *
 * <p>Provides low-level infrastructure used by renderers and models. The engine layer is
 * optimized for predictable latency and minimal allocations.</p>
 *
 * <p>Concurrency: Engine components may rely on VarHandle/atomic semantics to provide
 * lock-free coordination. See docs/CONCURRENCY_MODEL.md for ordering guarantees.</p>
 *
 * @author Arber Gashi
 * @version 1.7.0
 * @since 2026-01-30
 */
package com.arbergashi.charts.engine;
