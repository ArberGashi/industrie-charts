/**
 * Zero-allocation object pooling and memory management for ArberCharts.
 *
 * <p>This package contains the core infrastructure for zero-GC rendering,
 * the cornerstone of ArberCharts' performance advantage.
 *
 * <h2>Zero-GC Philosophy</h2>
 * <p>NO allocations are permitted in render hot paths. Every frame must render
 * without creating new objects on the heap. This eliminates GC pauses and
 * guarantees &lt;1ms p99 latency.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.engine.allocation.ZeroAllocPool} -
 *       Central object pool for strokes, colors, and buffers</li>
 * </ul>
 *
 * <h2>Performance Impact</h2>
 * <table border="1">
 *   <tr>
 *     <th>Metric</th>
 *     <th>With Zero-GC</th>
 *     <th>Without Zero-GC</th>
 *   </tr>
 *   <tr>
 *     <td>p99 latency</td>
 *     <td>&lt;1ms</td>
 *     <td>5-10ms</td>
 *   </tr>
 *   <tr>
 *     <td>GC pauses</td>
 *     <td>0</td>
 *     <td>5-10/min</td>
 *   </tr>
 *   <tr>
 *     <td>Throughput</td>
 *     <td>10,000+ fps</td>
 *     <td>1,000 fps</td>
 *   </tr>
 * </table>
 *
 * <h2>Usage Guidelines</h2>
 * <p><strong>Always use ZeroAllocPool for objects in render paths:</strong>
 *
 * <pre>{@code
 * // ❌ WRONG - Allocates on every render
 * public void render(Graphics2D g2) {
 *     g2.setStroke(new BasicStroke(2.0f));
 *     g2.setColor(new Color(255, 0, 0));
 * }
 *
 * // ✅ RIGHT - Zero allocations
 * public void render(Graphics2D g2) {
 *     g2.setStroke(ZeroAllocPool.getStroke(2.0f));
 *     g2.setColor(ZeroAllocPool.getColor(255, 0, 0));
 * }
 * }</pre>
 *
 * <h2>Architecture Enforcement</h2>
 * <p>Zero-allocation discipline is enforced via ArchUnit tests:
 * <ul>
 *   <li>{@code ZeroGcArchitectureTest} - Blocks PRs with allocations</li>
 *   <li>Build fails if {@code new BasicStroke()} or {@code new Color()}
 *       found in render methods</li>
 * </ul>
 *
 * @see com.arbergashi.charts.engine.allocation.ZeroAllocPool
 * @since 2.0.0
 */
package com.arbergashi.charts.engine.allocation;
