# ArberCharts 2.2.0 User Guide

ArberCharts 2.2.0 is a Java charting framework for desktop and server workloads.

## Requirements

- Eclipse Temurin JDK 25 from Adoptium
- Maven 3.9+

## Modules

- `arbercharts-core`: rendering engine, models, renderer catalog
- `arbercharts-swing-bridge`: Swing integration and interactive panel
- `arbercharts-server-bridge`: headless image rendering for backend services
- `arbercharts-spring-boot-starter`: Spring Boot auto-configuration
- `arbercharts-starter`: bundled JVM artifact built locally; excluded from Maven Central publishing
- `arbercharts-demo`: executable Swing workbench
- `arbercharts-spring-boot-demo`: executable browser workbench backed by the server bridge

Use the same **2.2.0** version for Core and every bridge in an application. Core
does not depend on `java.desktop`; Swing and server raster rendering do. A
headless server runtime therefore still needs that JDK module.

## Quick Start

For a Swing application, add the bridge, which includes Core as a dependency:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.2.0</version>
</dependency>
```

Build from the repository root:

```bash
mvn clean package
```

Run the quality gate and tests across all modules:

```bash
mvn -B -Pquality-gate -Djava.awt.headless=true verify
```

Run the Swing demo on a graphical desktop:

```bash
java -jar arbercharts-demo/target/arbercharts-demo-2.2.0.jar
```

Run the browser demo, then open `http://localhost:8080`:

```bash
java -Djava.awt.headless=true -jar arbercharts-spring-boot-demo/target/arbercharts-spring-boot-demo-2.2.0.jar
```

The optional `--add-modules jdk.incubator.vector` JVM argument enables vector
acceleration where supported. The applications also support the scalar path.

## Data Model Contract

- Always treat `getPointCount()` as logical size.
- Arrays returned by `getXData()` / `getYData()` may exceed logical size.
- For realtime streams, use `CircularChartModel`.
- Threading and snapshot guarantees depend on the model implementation. Read the
  [concurrency model](CONCURRENCY_MODEL.md) before sharing models between threads.

## Platform Focus

- Swing desktop UI
- Spring Boot / headless backend rendering

## Additional References

- [Quick Start](QUICK_START.md)
- [Renderer Catalog](RENDERER_CATALOG.md)
- [License](../LICENSE)
- [Rendering Architecture](RENDERING_ARCHITECTURE.md)
- [Verification Strategy](VERIFICATION_STRATEGY.md)
- [Swing Workbench](../arbercharts-demo/docs/WORKBENCH.md)
- [Spring Boot Workbench](../arbercharts-spring-boot-demo/README.md)
- [GitHub Issues](https://github.com/ArberGashi/industrie-charts/issues)
