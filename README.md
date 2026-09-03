# Industrie Charts

![License](https://img.shields.io/badge/License-MIT-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![Maven Central](https://img.shields.io/maven-central/v/com.arbergashi/arbercharts-core)

Industrie Charts is a Java 25 charting library for high-performance data visualisation on the JVM:
156 renderers, zero-GC render paths, SIMD-accelerated where available, and runnable without special JVM flags.

## In short

- Focus on high-performance rendering for charts and technical visualisations
- Server-side headless rendering (Spring Boot) and Swing desktop integration
- The Vector API (SIMD) is detected at runtime; without `--add-modules jdk.incubator.vector`
  the scalar fallback takes over transparently with identical results

## Preview (renderers)

![Candlestick Chart](docs/images/candlestick.png)
![Heatmap Chart](docs/images/heatmap.png)
![Sankey Chart](docs/images/sankey.png)
![Voronoi Chart](docs/images/voronoi.png)
![Parallel Coordinates Chart](docs/images/parallelcoordinates.png)
![Sunburst Chart](docs/images/sunburst.png)

## Installation (Maven Central)

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

For Swing applications add `arbercharts-swing-bridge`; for server rendering use
`arbercharts-server-bridge` or the Spring Boot starter directly (see below).

## Spring Boot quick start

For server-side rendering (PNG export in REST APIs, reports, batch jobs) the Spring Boot starter is all you need:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

The starter configures a headless `ServerRenderService` automatically
(session pool, optional Micrometer metrics). No JVM flags are required.

```java
@RestController
public class ChartController {
    private final ServerRenderService renderService;

    public ChartController(ServerRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping(value = "/chart.png", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] chart() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Demo");
        double price = 100.0;
        for (int i = 0; i < 500; i++) {
            double close = price + Math.sin(i * 0.08) * 2.5;
            model.setOHLC(i, price, Math.max(price, close) + 1.2, Math.min(price, close) - 1.2, close);
            price = close;
        }
        return renderService.renderToPng(model, new Dimension(800, 400));
    }
}
```

Configuration via `application.properties`:

```properties
arbercharts.server.pool-size=4
arbercharts.server.metrics-enabled=true
```

## Build and run the demo

```bash
mvn -pl arbercharts-demo -am package
java -jar arbercharts-demo/target/arbercharts-demo-*.jar
```

The demo enables the runtime flags it needs by itself (restart mechanism in the launcher).
Ready-made demo JARs are attached to the GitHub releases.

## Documentation

- [Quick Start](docs/QUICK_START.md)
- [User Guide](docs/USER_GUIDE.md)
- [Renderer Catalog](docs/RENDERER_CATALOG.md)
- [Release Notes](RELEASE_NOTES.md)

## Licensing

Industrie Charts artifacts are released under the MIT license.
See `LICENSE`, `NOTICE` and `BINARY-LICENSE.md`.
Commercial support and SLAs: see [docs/PRICING.md](docs/PRICING.md).

## Support

gashi@pro-business.ch
https://www.arbergashi.com
