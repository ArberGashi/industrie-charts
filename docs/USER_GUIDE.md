# ArberCharts 2.0.0 User Guide

ArberCharts 2.0.0 is a high-performance Java charting framework for desktop and server workloads.

## Requirements

- Java 25
- Maven 3.9+

## Modules

- `arbercharts-core`: rendering engine, models, renderer catalog
- `arbercharts-swing-bridge`: Swing integration and interactive panel
- `arbercharts-server-bridge`: headless image rendering for backend services
- `arbercharts-spring-boot-starter`: Spring Boot auto-configuration
- `arbercharts-starter`: bundled JVM starter artifact

## Quick Start

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

Build:

```bash
mvn clean package
```

Run demo:

```bash
java --enable-native-access=ALL-UNNAMED -jar arbercharts-demo/target/arbercharts-demo-2.0.0.jar
```

## Data Model Contract

- Always treat `getPointCount()` as logical size.
- Arrays returned by `getXData()` / `getYData()` may exceed logical size.
- For realtime streams, use `CircularChartModel`.

## Platform Focus

- Swing desktop UI
- Spring Boot / headless backend rendering

## Additional References

- `QUICK_START.md`
- `RENDERER_CATALOG.md`
- `LICENSING.md`
