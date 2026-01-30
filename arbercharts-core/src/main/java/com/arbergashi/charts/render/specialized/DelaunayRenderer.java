package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Delaunay-like renderer (approximation): connects each point to nearby neighbors using a spatial grid
 * or a KD-tree for large datasets. Headless implementation using ArberCanvas only.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class DelaunayRenderer extends BaseRenderer {

    private static final ExecutorService BACKGROUND = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private static final long COMPUTE_COOLDOWN_NANOS = 250_000_000L;

    static {
        RendererRegistry.register("delaunay", new RendererDescriptor("delaunay", "renderer.delaunay", "/icons/delaunay.svg"), DelaunayRenderer::new);
    }

    private transient final AtomicBoolean computing = new AtomicBoolean(false);
    private final double[] pix = new double[2];
    private transient final AtomicLong computeStamp = new AtomicLong();
    private transient double[] xs;
    private transient double[] ys;
    private transient volatile int[] cachedEdgeA;
    private transient volatile int[] cachedEdgeB;
    private transient volatile int cachedKey = 0;
    private transient int[] gridHead;
    private transient int[] gridNext;
    private transient double[] bestD;
    private transient int[] bestIdx;
    private transient volatile long lastComputeNanos;

    public DelaunayRenderer() {
        super("delaunay");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;
        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ys", n);
        }

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

        canvas.setStroke(getSeriesStrokeWidth());
        ArberColor baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            canvas.setColor(baseColor);
        }

        int k = 6;
        if (n > 50_000) k = 3;
        if (n > 200_000) k = 2;
        final int kFinal = k;

        int sampleLimit = 5000;
        ArberRect b = context.getPlotBounds();
        int key = java.util.Objects.hash(n, (int) b.x(), (int) b.y(), (int) b.width(), (int) b.height());

        if (n > 800) {
            if (cachedEdgeA != null && cachedKey == key) {
                drawCachedEdges(canvas, context, baseColor, n);
                return;
            }
            if (cachedKey != key) {
                long now = System.nanoTime();
                if (!computing.get() && (now - lastComputeNanos) > COMPUTE_COOLDOWN_NANOS && computing.compareAndSet(false, true)) {
                    lastComputeNanos = now;
                    final int keyFinal = key;
                    final long stamp = computeStamp.incrementAndGet();

                    final int step;
                    final int m;
                    if (n > sampleLimit) {
                        step = Math.max(1, n / sampleLimit);
                        m = (n + step - 1) / step;
                    } else {
                        step = 1;
                        m = n;
                    }

                    final double[] sxs = RendererAllocationCache.getDoubleArray(this, "delaunay.snap.sxs", m);
                    final double[] sys = RendererAllocationCache.getDoubleArray(this, "delaunay.snap.sys", m);
                    final int[] originalIdx = RendererAllocationCache.getIntArray(this, "delaunay.snap.idx", m);

                    int si = 0;
                    for (int i = 0; i < n; i += step) {
                        if (si >= m) break;
                        sxs[si] = xs[i];
                        sys[si] = ys[i];
                        originalIdx[si] = i;
                        si++;
                    }

                    final int siFinal = si;
                    BACKGROUND.submit(() -> getComputedDelaunayBackground(sxs, sys, originalIdx, siFinal, stamp, keyFinal, kFinal));
                }
            }
        }

        if (n > sampleLimit) {
            int step = Math.max(1, n / sampleLimit);
            int m = (n + step - 1) / step;
            double[] sxs = RendererAllocationCache.getDoubleArray(this, "delaunay.sample.sxs", m);
            double[] sys = RendererAllocationCache.getDoubleArray(this, "delaunay.sample.sys", m);
            int[] originalIdx = RendererAllocationCache.getIntArray(this, "delaunay.sample.idx", m);
            int si = 0;
            for (int i = 0; i < n; i += step) {
                sxs[si] = xs[i];
                sys[si] = ys[i];
                originalIdx[si] = i;
                si++;
            }
            KDTree tree = buildKDTree(sxs, sys);
            for (int i = 0; i < si; i++) {
                int[] neighbors = tree.kNearest(i, k + 1);
                if (neighbors == null) continue;
                for (int idx : neighbors) {
                    if (idx <= i) continue;
                    int a = originalIdx[i];
                    int b1 = originalIdx[idx];
                    if (isMultiColor()) {
                        ArberColor edge = themeSeries(context, a);
                        if (edge == null) edge = baseColor;
                        canvas.setColor(edge);
                    }
                    drawLine(canvas, xs[a], ys[a], xs[b1], ys[b1]);
                }
            }
        } else {
            double width = Math.max(1.0, maxX - minX);
            double height = Math.max(1.0, maxY - minY);
            double cellSize = Math.sqrt((width * height) / (double) n) * 1.5;
            if (cellSize < 10.0) cellSize = 10.0;

            int cols = Math.max(1, (int) Math.ceil(width / cellSize));
            int rows = Math.max(1, (int) Math.ceil(height / cellSize));
            int cellCount = cols * rows;

            if (gridHead == null || gridHead.length < cellCount) {
                gridHead = RendererAllocationCache.getIntArray(this, "delaunay.gridHead", cellCount);
            }
            Arrays.fill(gridHead, 0, cellCount, -1);

            if (gridNext == null || gridNext.length < n) {
                gridNext = RendererAllocationCache.getIntArray(this, "delaunay.gridNext", n);
            }

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
                bestD = RendererAllocationCache.getDoubleArray(this, "delaunay.bestD", k);
                bestIdx = RendererAllocationCache.getIntArray(this, "delaunay.bestIdx", k);
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
                    if (isMultiColor()) {
                        ArberColor edge = themeSeries(context, i);
                        if (edge == null) edge = baseColor;
                        canvas.setColor(edge);
                    }
                    drawLine(canvas, xs[i], ys[i], xs[j], ys[j]);
                }
            }
        }
    }

    private void drawCachedEdges(ArberCanvas canvas, PlotContext context, ArberColor baseColor, int n) {
        int[] ea = cachedEdgeA;
        int[] eb = cachedEdgeB;
        int ec = Math.min(ea.length, eb.length);
        for (int ei = 0; ei < ec; ei++) {
            int a = ea[ei];
            int b1 = eb[ei];
            if ((a | b1) < 0 || a >= n || b1 >= n) continue;
            if (isMultiColor()) {
                ArberColor edge = themeSeries(context, a);
                if (edge == null) edge = baseColor;
                canvas.setColor(edge);
            }
            drawLine(canvas, xs[a], ys[a], xs[b1], ys[b1]);
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "delaunay.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "delaunay.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }

    public DelaunayRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }

    private void getComputedDelaunayBackground(final double[] sxsBuf, final double[] sysBuf, final int[] originalIdxBuf, final int siFinal, final long stamp, final int keyFinal, final int k) {
        try {
            final double[] sxs = Arrays.copyOf(sxsBuf, siFinal);
            final double[] sys = Arrays.copyOf(sysBuf, siFinal);
            final int[] originalIdx = Arrays.copyOf(originalIdxBuf, siFinal);

            KDTree tree = buildKDTree(sxs, sys);

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

    private static KDTree buildKDTree(double[] xs, double[] ys) {
        return new KDTree(xs, ys);
    }

    private static final class KDTree {
        private final int n;
        private final int[] idx;
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

        int[] kNearest(int queryIndex, int k) {
            if (n == 0) return null;
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
                    bubbleUp();
                    return;
                }
                if (d >= dist[0]) return;
                idx[0] = i;
                dist[0] = d;
                bubbleDown();
            }

            int[] toArraySorted() {
                int[] out = new int[size];
                System.arraycopy(idx, 0, out, 0, size);
                return out;
            }

            double maxDistance() {
                return (size == 0) ? Double.POSITIVE_INFINITY : dist[0];
            }

            private void bubbleUp() {
                int i = size - 1;
                while (i > 0) {
                    int p = (i - 1) >>> 1;
                    if (dist[i] <= dist[p]) break;
                    swap(idx, i, p);
                    double t = dist[i]; dist[i] = dist[p]; dist[p] = t;
                    i = p;
                }
            }

            private void bubbleDown() {
                int i = 0;
                while (true) {
                    int l = (i << 1) + 1;
                    int r = l + 1;
                    if (l >= size) break;
                    int m = (r < size && dist[r] > dist[l]) ? r : l;
                    if (dist[i] >= dist[m]) break;
                    swap(idx, i, m);
                    double t = dist[i]; dist[i] = dist[m]; dist[m] = t;
                    i = m;
                }
            }
        }

        private static final class BoundedMaxHeap {
            private final int cap;
            private final int[] idx;
            private final double[] dist;
            private int size;

            BoundedMaxHeap(int cap) {
                this.cap = Math.max(1, cap);
                this.idx = new int[this.cap];
                this.dist = new double[this.cap];
                this.size = 0;
            }

            int capacity() {
                return cap;
            }

            int size() {
                return size;
            }

            void offer(int i, double d) {
                if (size < cap) {
                    idx[size] = i;
                    dist[size] = d;
                    size++;
                    bubbleUp();
                    return;
                }
                if (d >= dist[0]) return;
                idx[0] = i;
                dist[0] = d;
                bubbleDown();
            }

            double maxDistance() {
                return (size == 0) ? Double.POSITIVE_INFINITY : dist[0];
            }

            int[] toArray() {
                int[] out = new int[size];
                System.arraycopy(idx, 0, out, 0, size);
                return out;
            }

            private void bubbleUp() {
                int i = size - 1;
                while (i > 0) {
                    int p = (i - 1) >>> 1;
                    if (dist[i] <= dist[p]) break;
                    swap(idx, i, p);
                    double t = dist[i]; dist[i] = dist[p]; dist[p] = t;
                    i = p;
                }
            }

            private void bubbleDown() {
                int i = 0;
                while (true) {
                    int l = (i << 1) + 1;
                    int r = l + 1;
                    if (l >= size) break;
                    int m = (r < size && dist[r] > dist[l]) ? r : l;
                    if (dist[i] >= dist[m]) break;
                    swap(idx, i, m);
                    double t = dist[i]; dist[i] = dist[m]; dist[m] = t;
                    i = m;
                }
            }
        }

        private static final class Node {
            final int i;
            Node left;
            Node right;
            Node(int i) { this.i = i; }
        }
    }
}
