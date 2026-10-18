# Concurrency model

This document describes the **2.5.0** model implementations and host ownership
rules. `ChartModel` does not define a universal lock-free or atomic-snapshot
guarantee. Choose the implementation and access pattern for the application.

## Numeric data access

| Implementation | Writer behavior | Reader behavior |
| --- | --- | --- |
| `DefaultChartModel` | Numeric mutations synchronize on the internal data lock. Capacity growth allocates new arrays. | Individual numeric getters synchronize on the same lock. Array getters return defensive copies. Separate calls do not form a transaction. |
| `CircularChartModel` | `setPoint` serializes writers with an atomic spin lock and overwrites the oldest points once the ring is full. A single producer avoids writer contention. | Per-point numeric reads use slot sequence guards and retry while a slot is being changed. `readPoint(index, buffer)` reads X, Y, minimum, maximum and weight from one stable slot. |

For `CircularChartModel`, allocate a five-element buffer per reader and reuse it:

```java
double[] point = new double[5];
if (model.readPoint(index, point)) {
    double x = point[0];
    double y = point[1];
    double minimum = point[2];
    double maximum = point[3];
    double weight = point[4];
}
```

The slot guard becomes odd before the writer updates fields and even after the
release publication. Readers compare acquire reads of the guard around the field
reads. They retry when the guard changes or is odd. This protects the numeric
tuple, but does not freeze the entire ring or preserve a logical index while the
ring advances. Separate getter calls can observe different updates. Retry loops
do not provide a bounded completion time under contention.

The ring's array getters return shared, reusable snapshot buffers. They are not
immutable, and building a buffer reads slots individually rather than taking one
atomic snapshot of the whole series. Do not modify or retain these arrays as a
stable historical frame, or request them concurrently from multiple readers.
For a stable full-frame view, coordinate ingestion and capture at the application
boundary, then render an independently owned model. Always respect
`getPointCount()` and the available array lengths.

Configure model names, colors, label handling and listener dispatch before
publishing a model to workers. Coordinate `clear()` and configuration changes
with ongoing work; the ring's serialized `setPoint` path does not make every
method a concurrent transaction. Other specialized models have their own
implementation constraints.

## Listeners and Swing

The default and circular models use a `CopyOnWriteArrayList` for listeners.
Notifications normally run on the calling thread. `setDispatchOnEdt(true)` alone
does not discover Swing or move execution to the event dispatch thread: both
that flag and a dispatch executor must be configured. A Swing application can
supply `SwingUtilities::invokeLater` from the bridge/application layer.

If the executor rejects a notification, the models log a warning and notify on
the caller thread. A listener that touches Swing must therefore preserve its own
EDT boundary. Listener exceptions are logged and isolated from other listeners.
`CircularChartModel.setPoint` publishes its update stamp while holding the writer
lock, then releases the lock before notifying listeners. A listener can append to
the same model, and another writer can progress while a callback is running.
With synchronous dispatch, notifications from different writers can overlap or
arrive out of mutation order; listeners must coordinate their own mutable state.
Keep callbacks short, avoid unbounded notification recursion, and remove listeners
when their owner is disposed.

Create and modify Swing components on the EDT. Publish background data through
the chosen model contract; thread-safe data ingestion does not make Swing
components safe to access from a worker.

## Rendering ownership

Canvas instances, plot contexts and renderer instances can contain mutable
scratch state. Give them one rendering session at a time. `ServerRenderService`
manages its context pool, but callers still own the model and any custom renderer
passed into a request. Avoid sharing a mutable renderer across concurrent calls.
Close the service when its application shuts down.

`getUpdateStamp()` supports cache invalidation. It is not a lock or a transaction
boundary for a sequence of reads. Likewise, reusable buffers reduce allocations
but do not establish a universal zero-allocation rendering guarantee.

## Verification

`CircularChartModelTest` covers reentrant listener writes, progress of another
writer during notification and logical timestamp indexing across ring wrap.
`CircularChartModelPropertyTest` covers bounded model behavior.
`DefaultSignalChartModelTest` checks logical channel and timestamp access across
wrap and growth. These focused regressions do not establish an atomic snapshot
contract for concurrent readers. `ConcurrentModelTest` contains opt-in
concurrent-writer stress cases:

```bash
mvn -B -pl arbercharts-core -Pstress -Dtest=ConcurrentModelTest test
```

The stress tests are skipped in a default test run. They are workload checks,
not a formal Java Memory Model proof or a JCStress suite. See the
[verification strategy](VERIFICATION_STRATEGY.md) for the remaining coverage and
performance limits.
