# Spring Boot chart workbench

A runnable browser demo for the ArberCharts 2.5.0 server backend, built with Spring Boot 4.1.1 and Java 25. It uses a compact IDE-inspired layout with an example navigator, chart preview, dataset inspector and render details. The design uses original assets and does not depend on an IDE installation or external web resources.

Use the installed **Eclipse Temurin 25 (Adoptium)** JDK. No additional JDK installation is needed when Temurin is already available. Check the runtime used by Maven with `mvn -v`.

## Build and run

From the repository root:

```shell
mvn -pl arbercharts-spring-boot-demo -am clean verify
java -Djava.awt.headless=true --add-modules jdk.incubator.vector -jar arbercharts-spring-boot-demo/target/arbercharts-spring-boot-demo-2.5.0.jar
```

Open [the local workbench](http://127.0.0.1:8080). The server binds to `127.0.0.1` by default. Override the port with `--server.port=8081` or `SERVER_PORT=8081`. Stop the process with Ctrl+C; Spring closes the renderer session pool on shutdown.

Packaging also stages the executable JAR and this README in `dist/releases/spring-boot/2.5.0/`. From that directory:

```shell
java -Djava.awt.headless=true --add-modules jdk.incubator.vector -jar arbercharts-spring-boot-demo-2.5.0.jar
```

The vector module is optional; omitting `--add-modules jdk.incubator.vector` uses the framework's scalar fallback. The executable JAR contains its Spring Boot dependencies and needs no application server. This demo is excluded from Maven Central publishing.

## Explore

- Choose from eleven curated examples: line, area, step, bar, scatter, candlestick, donut, radar, heatmap, box plot and kernel density.
- Change the input size, preview size or light/dark theme. The input label explains whether the value means observations, measurements per group or a grid-cell budget. These controls trigger real HTTP renders.
- Download the current PNG with its labels and scales, or copy the render URL from **HTTP request**.
- Inspect the actual runtime version, image dimensions, response size and measured round-trip time.

All datasets are deterministic and synthetic. The candlestick example contains generated OHLC values, not market data. Each request creates its own model and renderer because renderer instances contain mutable drawing state. The demo uses the shared Core renderer classes through `ServerRenderService`. Coordinate-specific labels, legends and Cartesian grids belong to this demo's host layer. Donut charts have category labels and a center total; radar charts use the renderer's radial grid and host-supplied dimension labels. Neither receives Cartesian axes. Renderer corrections and their regression evidence are documented separately in the repository's rendering audit.

The browser supports keyboard navigation, narrow screens, reduced motion and system font scaling. Fit-panel previews request doubled raster dimensions on high-density displays, within the same output limits. A fixed export size describes the actual PNG dimensions.

## Additional renderer examples

| Example ID | What the image shows | Input meaning |
| --- | --- | --- |
| `donut` | Five synthetic energy-source shares, category labels and the observation total | Observations are distributed across five fixed category proportions; integer category counts sum to the requested total. |
| `radar` | Six named metrics, a shared 0–100 radial scale and a filled profile | The requested number of measurements is averaged for each metric; six vertices are always shown. |
| `heatmap` | Two response peaks over a 10 × 6 mm field, with a quantitative color scale | A square grid uses `floor(sqrt(samples))²` cells, never exceeding the selected cell budget. Each cell samples the field at its center. |
| `boxplot` | Six batches, quartiles, median marks, whiskers and separate outliers | The requested number of observations is generated per batch. Quartiles use linear interpolation; whiskers end at the most extreme observations inside the 1.5 IQR fences. |
| `kde` | A Gaussian density estimate of a synthetic two-population mixture | Each requested sample is one measured observation. Bandwidth is fixed at 0.18 measurement units; the horizontal domain includes the kernel tails, and the vertical axis shows probability density. |

The existing line, area, step, bar, scatter and candlestick examples retain their sample-based controls. All examples create fresh mutable state for each HTTP request. The palette uses distinct category colors; the heatmap has a sequential response palette and an explicit low-to-high scale.

## Workbench documentation gallery

The repository README uses actual **1280 × 720 browser captures**
of the complete workbench. Each image includes the chart, renderer navigator,
settings and dataset inspector. They were captured on 18 October 2026 from the
verified 2.5.0 executable JAR, after the model, renderer and backend corrections.
The [capture manifest](docs/screenshots/workbench-gallery/manifest.json) records
the source executable checksum, page state, image dimensions and file checksums.

| Example | Light workbench | Dark workbench |
| --- | --- | --- |
| Candlestick | [JPEG](docs/screenshots/workbench-gallery/candlestick-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/candlestick-dark.jpg) |
| Heatmap | [JPEG](docs/screenshots/workbench-gallery/heatmap-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/heatmap-dark.jpg) |
| Donut | [JPEG](docs/screenshots/workbench-gallery/donut-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/donut-dark.jpg) |
| Radar | [JPEG](docs/screenshots/workbench-gallery/radar-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/radar-dark.jpg) |
| Box plot | [JPEG](docs/screenshots/workbench-gallery/boxplot-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/boxplot-dark.jpg) |
| Kernel density | [JPEG](docs/screenshots/workbench-gallery/kde-light.jpg) | [JPEG](docs/screenshots/workbench-gallery/kde-dark.jpg) |

To refresh the gallery, run the exact staged artifact, open each recorded page
at 1280 × 720, select **Fit panel**, and wait for **Rendered successfully** before
capturing the viewport. Check the selected renderer, theme and download request.
Review each capture without altering the application UI, then update the
capture manifest. The measured response times may differ
between captures; the chart datasets are deterministic.

## Standalone chart exports

Separate PNG exports are available for reports and renderer comparisons. A 1600 × 900 output with `scale=2` provides a sharp image at an 800 × 450 display size, including readable labels. These example URLs use the default application port; adjust the port if the local process uses an override:

- [Donut, light](http://127.0.0.1:8080/api/charts/donut.png?width=1600&height=900&scale=2&samples=256&theme=light)
- [Radar, light](http://127.0.0.1:8080/api/charts/radar.png?width=1600&height=900&scale=2&samples=96&theme=light)
- [Heatmap, light](http://127.0.0.1:8080/api/charts/heatmap.png?width=1600&height=900&scale=2&samples=1024&theme=light)
- [Box plot, light](http://127.0.0.1:8080/api/charts/boxplot.png?width=1600&height=900&scale=2&samples=96&theme=light)
- [Kernel density, light](http://127.0.0.1:8080/api/charts/kde.png?width=1600&height=900&scale=2&samples=256&theme=light)

Change `theme=light` to `theme=dark` for the matching dark image. The checked-in
[image manifest](docs/screenshots/renderers/manifest.json) records the source JAR,
runtime, exact requests and image checksums for the 2.5.0 exports. To regenerate
the six chart examples in both themes from the current
local application, run from the repository root:

```shell
python3 scripts/export-demo-gallery.py \
    --base-url http://127.0.0.1:8080 \
    --artifact dist/releases/spring-boot/2.5.0/arbercharts-spring-boot-demo-2.5.0.jar
```

The script validates the running renderer catalog, PNG format and dimensions before
writing local images and their manifest. Review the images before updating the
repository. Fixed inputs are reproducible on the same runtime and font
environment; font rasterization can differ between operating systems.

| Example | Light | Dark |
| --- | --- | --- |
| Candlestick | [PNG](docs/screenshots/renderers/candlestick-light.png) | [PNG](docs/screenshots/renderers/candlestick-dark.png) |
| Heatmap | [PNG](docs/screenshots/renderers/heatmap-light.png) | [PNG](docs/screenshots/renderers/heatmap-dark.png) |
| Donut | [PNG](docs/screenshots/renderers/donut-light.png) | [PNG](docs/screenshots/renderers/donut-dark.png) |
| Radar | [PNG](docs/screenshots/renderers/radar-light.png) | [PNG](docs/screenshots/renderers/radar-dark.png) |
| Box plot | [PNG](docs/screenshots/renderers/boxplot-light.png) | [PNG](docs/screenshots/renderers/boxplot-dark.png) |
| Kernel density | [PNG](docs/screenshots/renderers/kde-light.png) | [PNG](docs/screenshots/renderers/kde-dark.png) |

## HTTP API

| Endpoint | Response |
| --- | --- |
| `GET /` | Browser workbench |
| `GET /api/examples` | Curated examples and runtime metadata |
| `GET /api/charts/{id}.png` | PNG rendered by the server bridge |
| `GET /actuator/health` | Application health |

Example:

```shell
curl 'http://127.0.0.1:8080/api/charts/line.png?width=1280&height=720&samples=256&theme=light&download=true' --output line.png
```

| Parameter | Default | Accepted values |
| --- | --- | --- |
| `width` | `960` | 320–2560 output pixels; at least `320 × scale` |
| `height` | `540` | 240–1440 output pixels; at least `240 × scale` |
| `samples` | `256` | 16–2048 |
| `theme` | `dark` | `dark`, `light` |
| `scale` | `1` | `1`, `2`; scales demo labels and margins for dense previews |
| `download` | `false` | Adds an attachment filename when `true` |

Unknown examples return 404. Invalid dimensions, themes and sample counts return 400. At most four requests render concurrently; additional requests return 429 so a burst cannot allocate unbounded image buffers. The frontend exposes errors with a retry action. Images are not cached, and no submitted data is stored.

The application is a local demo without authentication. Its configuration deliberately exposes only the health actuator endpoint. Review authentication, access controls and deployment limits before changing the loopback bind address for a shared deployment.

## Verification

`ChartWorkbenchHttpTest` starts the real application on a random loopback port. It checks the workbench assets and catalog, all eleven PNG renderers in both themes, download dimensions and headers, malformed and oversized input, and concurrent deterministic rendering. Additional image checks verify donut category shares, the filled radar profile, heatmap orientation and color-scale endpoints, all boxplot median marks, and density-curve coordinates against an independent Gaussian sum. The five additional examples are also exercised at both input limits and the smallest logical preview with `scale=2`. These tests use the real server bridge and PNG decoding, without renderer mocks.

`DemoExamplesTest` checks data semantics separately, including fixed category totals, six radial dimensions, heatmap cell centers and budgets, typed statistical models, request-owned renderer instances, and a hand-calculated quartile/outlier fixture.

```shell
mvn -pl arbercharts-spring-boot-demo -am test
```

The asynchronous browser state also has dependency-free regression tests using
Node's built-in test runner. With an existing Node.js installation, run from the
repository root:

```shell
node --test arbercharts-spring-boot-demo/src/test/frontend/workbench.test.mjs
```

These tests cover catalog request deduplication, initialization failure and retry,
empty catalogs, stale image responses, download availability during failed
or pending renders, image decoding failure, object URL cleanup, request
cancellation, coordinate-specific accessibility text and the heatmap cell-budget
control. They execute the production script with controlled network responses and
a DOM fixture whose element IDs come from the actual HTML. They do not replace
live browser layout and PNG checks. Node.js is not required to build or run the
executable Java demo.

The independent renderer and UI presentation audit remains the source for future rendering corrections. This demo does not claim that all framework renderers or all axis layouts have been visually perfected.

Final packaged verification used the staged 2.5.0 executable JAR with SHA-256
`a9ec87c7effd37647d80fb72656cb27810384552fd767927481aadf1d5512c66`.
The HTTP smoke check exercised the running application over its real loopback
socket: all 22 example/theme combinations decoded at 800 × 480 with chart content,
12 invalid or unknown requests received the expected error response, and 44
parallel repeat renders matched the corresponding PNG bytes. A download response
decoded at 1280 × 720 with the expected attachment filename. The catalog reported
Spring Boot 4.1.1, Java 25 and Eclipse Adoptium; application health remained `UP`.

The twelve standalone exports were then requested again from that process at
1600 × 900 with `scale=2`. Each PNG was fully decoded and checked for dimensions,
theme, chart content and its manifest checksum. The changed radar exports were
visually reviewed for the filled profile, radial scale and all six dimension
labels. Deterministic exports can remain byte-identical across builds; the
manifest records the actual executable used for this export pass.

The workbench screenshots were captured from the same final executable: both
themes at 1280 × 720 and the narrow layout at 502 × 766. The final browser session
opened all six gallery examples in both themes, used the theme toggle on the Line
example, checked complete chart images and their download links, and confirmed
the radar's 0-100 scale in the inspector. The browser reported no warnings or
errors. Search, empty-search results and changes through the sample and fixed-size
controls were not repeated in this final capture session.
The capture manifest records all fifteen browser views and their checksums:

- [Dark workbench](docs/screenshots/workbench-dark.jpg)
- [Light workbench](docs/screenshots/workbench-light.jpg)
- [Narrow layout](docs/screenshots/workbench-mobile.jpg)
