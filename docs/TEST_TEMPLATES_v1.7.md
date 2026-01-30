# Test Templates (v1.7.0-LTS)

This document provides minimal, copy‑ready templates for verification workstreams. These are scaffolds only; they do not modify the core.

## Property‑Based Tests (jqwik) — Circular Model
```java
@Property
void circularModelSnapshotsAreConsistent(@ForAll("series") List<Double> values) {
    CircularChartModel model = new CircularChartModel(1024, 2);
    for (int i = 0; i < values.size(); i++) {
        model.add(i, new double[]{i, values.get(i)});
    }
    // Snapshot invariants: counts, bounds, monotonic x
    int n = model.getPointCount();
    Assertions.assertTrue(n >= 0);
    double[] xs = model.getXData();
    double[] ys = model.getYData();
    int count = Math.min(n, Math.min(xs.length, ys.length));
    for (int i = 1; i < count; i++) {
        Assertions.assertTrue(xs[i] >= xs[i - 1]);
    }
}

@Provide
Arbitrary<List<Double>> series() {
    return Arbitraries.doubles().between(-1000, 1000)
        .list().ofMinSize(10).ofMaxSize(5000);
}
```

## Concurrency Tests (JCStress) — Reader/Writer
```java
@JCStressTest
@Outcome(id = "true", expect = Expect.ACCEPTABLE, desc = "Consistent snapshot")
@State
public class CircularSnapshotConsistencyTest {
    private final CircularChartModel model = new CircularChartModel(1024, 2);

    @Actor
    public void writer() {
        for (int i = 0; i < 1000; i++) {
            model.add(i, new double[]{i, i * 0.5});
        }
    }

    @Actor
    public void reader(ZZ_Result r) {
        double[] xs = model.getXData();
        double[] ys = model.getYData();
        int count = Math.min(model.getPointCount(), Math.min(xs.length, ys.length));
        boolean monotonic = true;
        for (int i = 1; i < count; i++) {
            if (xs[i] < xs[i - 1]) {
                monotonic = false;
                break;
            }
        }
        r.r1 = monotonic;
    }
}
```

## Visual Regression (Renderer Snapshots)
```java
@Test
void renderMatchesGoldenImage() {
    ChartModel model = TestModels.financial(5000);
    ChartRenderer renderer = new CandlestickRenderer();
    BufferedImage image = renderService.renderToImage(model, new Dimension(960, 540), ChartThemes.getDarkTheme(), renderer);
    ApprovalTests.verify(new BufferedImageWriter(image));
}
```

## Zero‑Allocation Checks
```text
Run: mvn -Pguidelines-check test
Ensures: drawData() methods contain no `new` allocations.
```

## Notes
- These templates are intentionally minimal.
- Extend with domain‑specific invariants (OHLC consistency, medical calibration).
