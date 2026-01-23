package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Delaunay-like renderer (approximation): connects each point to nearby neighbors using a spatial grid
 * or a KD-tree for large datasets. This reduces O(n^2) behavior while keeping the rendering path allocation-light.
 * For very large datasets, it precomputes edges off-EDT and caches them to avoid jank.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class DelaunayRenderer extends BaseRenderer {

    // background precomputation
    private static final ExecutorService BACKGROUND = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private static final long COMPUTE_COOLDOWN_NANOS = 250_000_000L; // 250ms

    static {
        RendererRegistry.register("delaunay", new RendererDescriptor("delaunay", "renderer.delaunay", "/icons/delaunay.svg"), DelaunayRenderer::new);
    }

    private KDTree buildKdTree(double[] sxs, double[] sys) {
        return new KDTree(sxs, sys);
    }

    private transient final AtomicBoolean computing = new AtomicBoolean(false);
    // Reusable mapping buffer for allocation-free pixel mapping.
    private final double[] pix = new double[2];
    /**
     * Monotonic stamp to ensure only the newest background result becomes visible.
     * Guards against the following race: key A compute starts, then key B compute starts and finishes,
     * then key A finishes later and overwrites the cache.
     */
    private transient final AtomicLong computeStamp = new AtomicLong();
    private transient double[] xs;
    private transient double[] ys;
    private transient volatile int[] cachedEdgeA;
    private transient volatile int[] cachedEdgeB;
    private transient volatile int cachedKey = 0;
    // Reusable buffers for grid-based neighbor search (avoid ArrayList / per-point allocations)
    private transient int[] gridHead;
    private transient int[] gridNext;
    private transient double[] bestD;
    private transient int[] bestIdx;
    // Throttle background recomputation to avoid p95 spikes from frequent submits.
    private transient volatile long lastComputeNanos;

    public DelaunayRenderer() {
        super("delaunay");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;
        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ys", n);
        }

        // map points to pixel coordinates (allocation-free)
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), pix);
            double px = pix[0];
            double py = pix[1];
            xs[i] = px;
            ys[i] = py;
            if (px < minX) minX = px;
            if (py < minY) minY = py;
            if (px > maxX) maxX = px;
            if (py > maxY) maxY = py;
        }

        Stroke prev = g2.getStroke();
        Color prevColor = g2.getColor();
        g2.setStroke(getSeriesStroke());
        Color baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            g2.setColor(baseColor);
        }

        final int k = 6;
        Rectangle clip = g2.getClipBounds();
        int kEffective = k;
        if (n > 50_000) kEffective = 3;
        if (n > 200_000) kEffective = 2;

        // For very large datasets, prefer using a precomputed edge set (built off-EDT)
        int sampleLimit = 5000;
        Rectangle2D b = context.plotBounds();
        // Key should be stable and cheap: avoid hashing Color objects.
        int key = Objects.hash(n, (int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());

        if (n > 800) {
            if (cachedEdgeA != null && cachedKey == key) {
                // draw cached edges
                int[] ea = cachedEdgeA;
                int[] eb = cachedEdgeB;
                int ec = Math.min(ea.length, eb.length);
                for (int ei = 0; ei < ec; ei++) {
                    int a = ea[ei];
                    int b1 = eb[ei];
                    if ((a | b1) < 0 || a >= n || b1 >= n) continue;
                    double x1 = xs[a], y1 = ys[a], x2 = xs[b1], y2 = ys[b1];
                    if (clip != null) {
                        if ((x1 < clip.getX() && x2 < clip.getX()) || (x1 > clip.getX() + clip.getWidth() && x2 > clip.getX() + clip.getWidth())
                                || (y1 < clip.getY() && y2 < clip.getY()) || (y1 > clip.getY() + clip.getHeight() && y2 > clip.getY() + clip.getHeight())) {
                            continue;
                        }
                    }
                    double dx = x1 - x2, dy = y1 - y2;
                    if (dx * dx + dy * dy > 1e7) continue;
                    if (isMultiColor()) {
                        Color edge = themeSeries(context, a);
                        if (edge == null) edge = baseColor;
                        g2.setColor(edge);
                    }
                    g2.draw(getLine(x1, y1, x2, y2));
                }
                g2.setColor(prevColor);
                g2.setStroke(prev);
                return;
            }

            // Trigger background compute only if cache is missing, not already computing, and cooldown elapsed.
            if (cachedKey != key) {
                long now = System.nanoTime();
                if (!computing.get() && (now - lastComputeNanos) > COMPUTE_COOLDOWN_NANOS && computing.compareAndSet(false, true)) {
                    lastComputeNanos = now;
                    final int keyFinal = key;
                    final long stamp = computeStamp.incrementAndGet();

                    // Snapshot inputs for the background computation *now*.
                    final int step;
                    final int m;
                    if (n > sampleLimit) {
                        step = Math.max(1, n / sampleLimit);
                        m = (n + step - 1) / step;
                    } else {
                        step = 1;
                        m = n;
                    }

                    final double[] sxs = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "delaunay.snap.sxs", m);
                    final double[] sys = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "delaunay.snap.sys", m);
                    final int[] originalIdx = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "delaunay.snap.idx", m);

                    int si = 0;
                    for (int i = 0; i < n; i += step) {
                        if (si >= m) break;
                        sxs[si] = xs[i];
                        sys[si] = ys[i];
                        originalIdx[si] = i;
                        si++;
                    }

                    final int siFinal = si;

                    BACKGROUND.submit(() -> computeDelaunayBackground(sxs, sys, originalIdx, siFinal, stamp, keyFinal, k));
                }
            }
        }

            if (n > sampleLimit) {
            // create an index sample of size sampleLimit evenly
            int step = Math.max(1, n / sampleLimit);
            int m = (n + step - 1) / step;
            double[] sxs = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "delaunay.sample.sxs", m);
            double[] sys = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "delaunay.sample.sys", m);
            int[] originalIdx = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "delaunay.sample.idx", m);
            int si = 0;
            for (int i = 0; i < n; i += step) {
                sxs[si] = xs[i];
                sys[si] = ys[i];
                originalIdx[si] = i;
                si++;
            }
            KDTree tree = buildKdTree(sxs, sys);
            for (int i = 0; i < si; i++) {
                int[] neighbors = tree.kNearest(i, kEffective + 1);
                if (neighbors == null) continue;
                for (int idx : neighbors) {
                    if (idx <= i) continue;
                    int a = originalIdx[i];
                    int b1 = originalIdx[idx];
                    if (isMultiColor()) {
                        Color edge = themeSeries(context, a);
                        if (edge == null) edge = baseColor;
                        g2.setColor(edge);
                    }
                    g2.draw(getLine(xs[a], ys[a], xs[b1], ys[b1]));
                }
            }
        } else {
            // grid-based neighbor search (optimized with primitive linked lists)
            double width = Math.max(1.0, maxX - minX);
            double height = Math.max(1.0, maxY - minY);
            double cellSize = Math.sqrt((width * height) / (double) n) * 1.5;
            if (cellSize < 10.0) cellSize = 10.0;

            int cols = Math.max(1, (int) Math.ceil(width / cellSize));
            int rows = Math.max(1, (int) Math.ceil(height / cellSize));
            int cellCount = cols * rows;

            if (gridHead == null || gridHead.length < cellCount) {
                gridHead = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "delaunay.gridHead", cellCount);
            }
            // reset heads
            Arrays.fill(gridHead, 0, cellCount, -1);

            if (gridNext == null || gridNext.length < n) {
                gridNext = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "delaunay.gridNext", n);
            }

            // Build linked lists: push each point onto its cell bucket
            for (int i = 0; i < n; i++) {
                int cx = (int) ((xs[i] - minX) / cellSize);
                int cy = (int) ((ys[i] - minY) / cellSize);
                if (cx < 0) cx = 0;
                else if (cx >= cols) cx = cols - 1;
                if (cy < 0) cy = 0;
                else if (cy >= rows) cy = rows - 1;
                int cell = cy * cols + cx;
                gridNext[i] = gridHead[cell];
                gridHead[cell] = i;
            }

            if (bestD == null || bestD.length < k) {
                bestD = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "delaunay.bestD", k);
                bestIdx = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "delaunay.bestIdx", k);
            }

            for (int i = 0; i < n; i++) {
                int cx = (int) ((xs[i] - minX) / cellSize);
                int cy = (int) ((ys[i] - minY) / cellSize);
                if (cx < 0) cx = 0;
                else if (cx >= cols) cx = cols - 1;
                if (cy < 0) cy = 0;
                else if (cy >= rows) cy = rows - 1;

                for (int bi = 0; bi < k; bi++) {
                    bestD[bi] = Double.MAX_VALUE;
                    bestIdx[bi] = -1;
                }

                int window = 1;
                for (int oy = Math.max(0, cy - window); oy <= Math.min(rows - 1, cy + window); oy++) {
                    int rowBase = oy * cols;
                    for (int ox = Math.max(0, cx - window); ox <= Math.min(cols - 1, cx + window); ox++) {
                        int idxPt = gridHead[rowBase + ox];
                        while (idxPt >= 0) {
                            if (idxPt != i) {
                                double dx = xs[idxPt] - xs[i];
                                double dy = ys[idxPt] - ys[i];
                                double d2 = dx * dx + dy * dy;
                                for (int bj = 0; bj < k; bj++) {
                                    if (d2 < bestD[bj]) {
                                        for (int s = k - 1; s > bj; s--) {
                                            bestD[s] = bestD[s - 1];
                                            bestIdx[s] = bestIdx[s - 1];
                                        }
                                        bestD[bj] = d2;
                                        bestIdx[bj] = idxPt;
                                        break;
                                    }
                                }
                            }
                            idxPt = gridNext[idxPt];
                        }
                    }
                }

                for (int bi = 0; bi < k; bi++) {
                    int j = bestIdx[bi];
                    if (j <= i) continue;
                    double x1 = xs[i], y1 = ys[i], x2 = xs[j], y2 = ys[j];
                    if (clip != null) {
                        if ((x1 < clip.getX() && x2 < clip.getX()) || (x1 > clip.getX() + clip.getWidth() && x2 > clip.getX() + clip.getWidth())
                                || (y1 < clip.getY() && y2 < clip.getY()) || (y1 > clip.getY() + clip.getHeight() && y2 > clip.getY() + clip.getHeight())) {
                            continue;
                        }
                    }
                    if (isMultiColor()) {
                        Color edge = themeSeries(context, i);
                        if (edge == null) edge = baseColor;
                        g2.setColor(edge);
                    }
                    g2.draw(getLine(x1, y1, x2, y2));
                }
            }
        }

        g2.setColor(prevColor);
        g2.setStroke(prev);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }

    public DelaunayRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }

    // --- Simple KD-tree implementation for 2D points for k-NN queries ---
    private void computeDelaunayBackground(final double[] sxsBuf, final double[] sysBuf, final int[] originalIdxBuf, final int siFinal, final long stamp, final int keyFinal, final int k) {
        try {
            // Copy snapshots into thread-local arrays (allocations happen off-EDT)
            final double[] sxs = Arrays.copyOf(sxsBuf, siFinal);
            final double[] sys = Arrays.copyOf(sysBuf, siFinal);
            final int[] originalIdx = Arrays.copyOf(originalIdxBuf, siFinal);

            KDTree tree = new KDTree(sxs, sys);

            int maxEdges = Math.max(1, siFinal * k);
            int[] ea = new int[maxEdges];
            int[] eb = new int[maxEdges];
            int ec = 0;

            for (int i = 0; i < siFinal; i++) {
                int[] neigh = tree.kNearest(i, k + 1);
                if (neigh == null) continue;
                for (int idx : neigh) {
                    if (idx <= i) continue;
                    if (ec >= maxEdges) break;
                    ea[ec] = originalIdx[i];
                    eb[ec] = originalIdx[idx];
                    ec++;
                }
            }

            if (ec < ea.length) {
                ea = Arrays.copyOf(ea, ec);
                eb = Arrays.copyOf(eb, ec);
            }

            // Publish only if still newest.
            if (stamp == computeStamp.get()) {
                cachedEdgeA = ea;
                cachedEdgeB = eb;
                cachedKey = keyFinal;
            }
        } catch (Throwable ex) {
            // ignore
        } finally {
            computing.set(false);
        }
    }
    private static final class KDTree {
        private final int n;
        private final int[] idx; // permutation of indices in tree order
        private final double[] xs, ys;
        private final Node root;

        KDTree(double[] xs, double[] ys) {
            this.n = xs.length;
            this.xs = xs;
            this.ys = ys;
            this.idx = new int[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            this.root = build(0, n, 0);
        }

        private static double dist2(double ax, double ay, double bx, double by) {
            double dx = ax - bx;
            double dy = ay - by;
            return dx * dx + dy * dy;
        }

        private static void swap(int[] arr, int a, int b) {
            int t = arr[a];
            arr[a] = arr[b];
            arr[b] = t;
        }

        private Node build(int from, int to, int depth) {
            if (from >= to) return null;
            int dim = depth % 2;
            int mid = (from + to) >>> 1;
            nthElement(idx, from, to - 1, mid, dim);
            Node node = new Node(idx[mid]);
            node.left = build(from, mid, depth + 1);
            node.right = build(mid + 1, to, depth + 1);
            return node;
        }

        // find k nearest neighbors (return indices array) to point at index 'query'
        int[] kNearest(int queryIndex, int k) {
            if (n == 0) return null;
            // The renderer uses small k (<=7). Use a primitive bounded buffer to avoid PriorityQueue/Stream allocations.
            if (k <= 8) {
                double qx = xs[queryIndex], qy = ys[queryIndex];
                SmallKNN knn = new SmallKNN(k);
                searchSmall(root, qx, qy, 0, knn, queryIndex);
                return knn.toArraySorted();
            }

            double qx = xs[queryIndex], qy = ys[queryIndex];
            BoundedMaxHeap heap = new BoundedMaxHeap(k);
            search(root, qx, qy, 0, heap, queryIndex);
            return heap.toArray();
        }

        private void searchSmall(Node node, double qx, double qy, int depth, SmallKNN knn, int skipIdx) {
            if (node == null) return;
            double px = xs[node.i], py = ys[node.i];
            if (node.i != skipIdx) knn.offer(node.i, dist2(qx, qy, px, py));
            int dim = depth % 2;
            double delta = (dim == 0) ? (qx - px) : (qy - py);
            Node first = (delta <= 0) ? node.left : node.right;
            Node second = (first == node.left) ? node.right : node.left;
            if (first != null) searchSmall(first, qx, qy, depth + 1, knn, skipIdx);
            if (second != null) {
                double bestDist2 = knn.maxDistance();
                if (knn.size < knn.cap || delta * delta < bestDist2) {
                    searchSmall(second, qx, qy, depth + 1, knn, skipIdx);
                }
            }
        }

        private void search(Node node, double qx, double qy, int depth, BoundedMaxHeap heap, int skipIdx) {
            if (node == null) return;
            double px = xs[node.i], py = ys[node.i];
            if (node.i != skipIdx) heap.offer(node.i, dist2(qx, qy, px, py));
            int dim = depth % 2;
            double delta = (dim == 0) ? (qx - px) : (qy - py);
            Node first = (delta <= 0) ? node.left : node.right;
            Node second = (first == node.left) ? node.right : node.left;
            if (first != null) search(first, qx, qy, depth + 1, heap, skipIdx);
            if (second != null) {
                double bestDist2 = heap.maxDistance();
                if (heap.size() < heap.capacity() || delta * delta < bestDist2) {
                    search(second, qx, qy, depth + 1, heap, skipIdx);
                }
            }
        }

        /**
         * In-place quickselect so that arr[k] is the element that would be at that position
         * if the range were sorted by the selected dimension.
         */
        private void nthElement(int[] arr, int left, int right, int k, int dim) {
            int l = left;
            int r = right;
            while (l < r) {
                int pivotIndex = (l + r) >>> 1;
                pivotIndex = partition(arr, l, r, pivotIndex, dim);
                if (k == pivotIndex) {
                    return;
                } else if (k < pivotIndex) {
                    r = pivotIndex - 1;
                } else {
                    l = pivotIndex + 1;
                }
            }
        }

        private int partition(int[] arr, int left, int right, int pivotIndex, int dim) {
            int pivotValue = arr[pivotIndex];
            double pivotCoord = coord(pivotValue, dim);
            swap(arr, pivotIndex, right);
            int store = left;
            for (int i = left; i < right; i++) {
                int v = arr[i];
                if (coord(v, dim) < pivotCoord) {
                    swap(arr, store, i);
                    store++;
                }
            }
            swap(arr, right, store);
            return store;
        }

        private double coord(int index, int dim) {
            return (dim == 0) ? xs[index] : ys[index];
        }

        /**
         * Fixed-size nearest neighbor buffer for small k.
         * Keeps entries sorted by distance descending at insertion time (O(k)).
         */
        private static final class SmallKNN {
            final int cap;
            final int[] idx;
            final double[] dist;
            int size;

            SmallKNN(int cap) {
                this.cap = Math.max(0, cap);
                this.idx = new int[this.cap];
                this.dist = new double[this.cap];
                this.size = 0;
            }

            void offer(int i, double d) {
                if (cap == 0) return;
                if (size < cap) {
                    idx[size] = i;
                    dist[size] = d;
                    size++;
                    // bubble up to keep dist descending
                    for (int p = size - 1; p > 0; p--) {
                        if (dist[p] > dist[p - 1]) {
                            swap(p, p - 1);
                        } else {
                            break;
                        }
                    }
                    return;
                }

                // buffer full: only insert if better than current worst (which is dist[0] because descending)
                if (d >= dist[0]) return;

                idx[0] = i;
                dist[0] = d;
                // push down to restore descending order
                for (int p = 0; p < cap - 1; p++) {
                    if (dist[p] < dist[p + 1]) {
                        swap(p, p + 1);
                    } else {
                        break;
                    }
                }
            }

            double maxDistance() {
                return size == 0 ? Double.POSITIVE_INFINITY : dist[0];
            }

            int[] toArraySorted() {
                int[] out = Arrays.copyOf(idx, size);
                Arrays.sort(out);
                return out;
            }

            private void swap(int a, int b) {
                int ti = idx[a];
                idx[a] = idx[b];
                idx[b] = ti;
                double td = dist[a];
                dist[a] = dist[b];
                dist[b] = td;
            }
        }

        private static final class Node {
            int i;
            Node left, right;

            Node(int i) {
                this.i = i;
            }
        }

        // small bounded max-heap of (index, distance)
        private static final class BoundedMaxHeap {
            private final int cap;
            private final PriorityQueue<Entry> pq;

            BoundedMaxHeap(int cap) {
                this.cap = cap;
                this.pq = new PriorityQueue<>((a, b) -> Double.compare(b.dist, a.dist));
            }

            void offer(int idx, double dist) {
                if (cap <= 0) return;
                if (pq.size() < cap) {
                    pq.add(new Entry(idx, dist));
                    return;
                }
                Entry head = pq.peek();
                if (head != null && dist < head.dist) {
                    pq.poll();
                    pq.add(new Entry(idx, dist));
                }
            }

            int size() {
                return pq.size();
            }

            int capacity() {
                return cap;
            }

            double maxDistance() {
                return pq.isEmpty() ? Double.POSITIVE_INFINITY : pq.peek().dist;
            }

            int[] toArray() {
                // Avoid streams allocations.
                int sz = pq.size();
                int[] out = new int[sz];
                int i = 0;
                for (Entry e : pq) out[i++] = e.idx;
                Arrays.sort(out);
                return out;
            }

            record Entry(int idx, double dist) {
            }
        }
    }
}
