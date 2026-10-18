# GraalVM metadata audit

This describes native-image metadata ownership in the **2.2.0** tree. The normal JVM build uses Eclipse Temurin 25. GraalVM SDK dependencies and metadata do not require replacing the installed JDK, and their presence does not prove that a native library has been built or validated.

## Core scope

Core retains the source-level `ArberNativeExport` entry points for a GraalVM shared
library (`.so`, `.dylib` or `.dll`). They cover chart handles, data updates, viewport
setup and rendering through a host canvas or command buffer. The host must provide
and validate the native integration. Core metadata remains under:

```text
arbercharts-core/src/main/resources/META-INF/native-image/com.arbergashi/charts-core/
```

`reflect-config.json` retains conditional constructor metadata for the playback implementations selected reflectively by `PlaybackFactory`:

- `com.arbergashi.charts.engine.forensic.DeterministicPlaybackManager`
- `com.arbergashi.charts.engine.forensic.ChronosPlaybackDrive`
- `com.arbergashi.charts.engine.forensic.StreamPlaybackDriveImpl`

`resource-config.json` includes the `i18n.charts` bundle, `i18n/*.properties`, and `META-INF/services/*`. Provider descriptors belong to the module implementing a provider. `ArberBridgeFactory` discovers canvas provider metadata, while `SimpleChart.show()` discovers `ChartDisplayProvider`; provider discovery does not create a canvas automatically.

`CircularChartModel` uses `MethodHandles` to create VarHandles. This is separate from the explicit reflective construction covered by the playback configuration.

## Desktop export metadata

`ViewportAuditExtractor` belongs to `arbercharts-swing-bridge`, in the package `com.arbergashi.charts.api`. Its ImageIO and optional PDFBox integration are outside the Core artifact. The associated PDFBox reflection entries reside in:

```text
arbercharts-swing-bridge/src/main/resources/META-INF/native-image/com.arbergashi/arbercharts-swing-bridge/reflect-config.json
```

The entries describe the current reflective calls to `PDDocument.load(File)`, `getDocumentInformation()`, `close()`, and `PDDocumentInformation.getCustomMetadataValue(String)`. They are conditional on the PDFBox types being reachable. This records the current implementation; it is not a compatibility claim for every PDFBox version. An application enabling PDF metadata extraction must validate its selected optional PDFBox version and the resulting native executable separately.

## Validation limits

The server bridge and Spring Boot starter render through the JVM's Java2D and
ImageIO APIs. Their normal integration does not load a bundled JNI library or
require GraalVM. Use Eclipse Temurin 25 for the JVM build and both demo applications.

The [architecture checks](VERIFICATION_STRATEGY.md#jdk-based-architecture-checks) verify that production Core bytecode has no `java.desktop` or bridge dependency. The server bridge still uses Java2D and ImageIO, even in headless mode. These are JVM dependency assertions, not native-image execution tests.

A native release needs a build with the intended native-image toolchain, target-platform loading tests, C entry-point and command-stream checks, and explicit resource/provider/reflection coverage. A successful JVM build does not establish that a native library matches the current source revision. The 2.2.0 JVM release and this metadata inventory do not claim native-image execution validation.

See [Rendering architecture](RENDERING_ARCHITECTURE.md) for module ownership and migration details, and [Dependency updates](DEPENDENCY_UPDATES.md) for the current SDK and build-tool baseline.
