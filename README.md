# ArberCharts 2.0.0 (Binary Release)

![License](https://img.shields.io/badge/License-MIT-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

ArberCharts ist eine Java-25 Charting-Library fuer performante Datenvisualisierung auf dem JVM-Stack.
Dieses Repository enthaelt das oeffentliche Binary-Release (JARs), Startskripte und Vorschau-Assets fuer GitHub.

## Kurzbeschreibung

- Fokus auf High-Performance Rendering fuer Charts und technische Visualisierungen
- Bereitstellung als Binary-Artefakte (kein Source-Release in diesem Repository)
- Demo-App zum direkten Starten und Testen der Visualisierungen

## Bildvorschau (Renderer)

![Candlestick Chart](site/images/candlestick.png)
![Heatmap Chart](site/images/heatmap.png)
![Sankey Chart](site/images/sankey.png)
![Voronoi Chart](site/images/voronoi.png)
![Parallel Coordinates Chart](site/images/parallelcoordinates.png)
![Sunburst Chart](site/images/sunburst.png)

## Included Files

- `binaries/arbercharts-demo-2.0.0.jar`
- `binaries/arbercharts-core-2.0.0.jar`
- `binaries/arbercharts-starter-2.0.0.jar`
- `binaries/run-demo.sh`
- `binaries/run-demo.bat`
- `binaries/SHA256SUMS.txt`
- `site/images/` preview images
- `LICENSE`
- `NOTICE`
- `BINARY-LICENSE.md`

## Run Demo

```bash
java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -jar binaries/arbercharts-demo-2.0.0.jar
```

## Spring Boot Quick Start (Maven Central)

ArberCharts ist auf Maven Central verfuegbar. Fuer serverseitiges Rendering
(PNG-Export in REST-APIs, Reports, Batch-Jobs) genuegt der Spring-Boot-Starter:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.0.1</version>
</dependency>
```

Der Starter konfiguriert automatisch einen headless `ServerRenderService`
(Session-Pool, optional Micrometer-Metriken). Ab 2.0.1 sind keine JVM-Flags
noetig: Die Vector API (SIMD) wird zur Laufzeit erkannt; ohne
`--add-modules jdk.incubator.vector` greift transparent der skalare Fallback.

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

Konfiguration ueber `application.properties`:

```properties
arbercharts.server.pool-size=4
arbercharts.server.metrics-enabled=true
```

## Licensing

ArberCharts binary artifacts are distributed under the MIT License.
See `LICENSE`, `NOTICE`, and `BINARY-LICENSE.md`.

## Support

gashi@pro-business.ch
https://www.arbergashi.com
