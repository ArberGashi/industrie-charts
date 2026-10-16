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

Run quality gate:

```bash
mvn -pl arbercharts-core -Pquality-gate validate
```

Run demo:

```bash
java --add-modules jdk.incubator.vector -jar arbercharts-demo/target/arbercharts-demo-2.2.0.jar
```

## Data Model Contract

- Always treat `getPointCount()` as logical size.
- Arrays returned by `getXData()` / `getYData()` may exceed logical size.
- For realtime streams, use `CircularChartModel`.

## Platform Focus

- Swing desktop UI
- Spring Boot / headless backend rendering

## Additional References

- [Quick Start](QUICK_START.md)
- [Renderer Catalog](RENDERER_CATALOG.md)
- [Licensing](LICENSING.md)
- [Swing Workbench](../arbercharts-demo/docs/WORKBENCH.md)
- [Spring Boot Workbench](../arbercharts-spring-boot-demo/README.md)
- [GitHub Issues](https://github.com/ArberGashi/industrie-charts/issues)
