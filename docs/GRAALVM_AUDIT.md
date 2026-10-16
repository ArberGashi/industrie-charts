# GraalVM metadata audit

This describes native-image metadata ownership in the **2.2.0** tree. The normal JVM build uses Eclipse Temurin 25. GraalVM SDK dependencies and metadata do not require replacing the installed JDK, and their presence does not prove that a native library has been built or validated.

## Core scope

The native API targets `arbercharts-core` as a shared library (`.so`, `.dylib` or `.dll`). Core metadata remains under:

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

`ViewportAuditExtractor` moves to `arbercharts-swing-bridge` while retaining the package `com.arbergashi.charts.api`. Its ImageIO and optional PDFBox integration no longer belong to the Core artifact. The associated PDFBox reflection entries now reside in:

```text
arbercharts-swing-bridge/src/main/resources/META-INF/native-image/com.arbergashi/arbercharts-swing-bridge/reflect-config.json
```

The entries describe the current reflective calls to `PDDocument.load(File)`, `getDocumentInformation()`, `close()`, and `PDDocumentInformation.getCustomMetadataValue(String)`. They are conditional on the PDFBox types being reachable. This records the current implementation; it is not a compatibility claim for every PDFBox version. An application enabling PDF metadata extraction must validate its selected optional PDFBox version and the resulting native executable separately.

## Validation limits

The [architecture checks](VERIFICATION_STRATEGY.md#jdk-based-architecture-checks) verify that production Core bytecode has no `java.desktop` or bridge dependency. The server bridge still uses Java2D and ImageIO, even in headless mode. These are JVM dependency assertions, not native-image execution tests.

A native release still needs a build with the intended native-image toolchain, target-platform loading tests, C entry-point and command-stream checks, and explicit resource/provider/reflection coverage. Existing binary files or a successful JVM build do not establish that an embedded native library matches the current source revision. No native-image build or new native deployment is claimed by this metadata update.

See [Rendering architecture](RENDERING_ARCHITECTURE.md) for module ownership and migration details, and [Dependency updates](DEPENDENCY_UPDATES.md) for the current SDK and build-tool baseline.
