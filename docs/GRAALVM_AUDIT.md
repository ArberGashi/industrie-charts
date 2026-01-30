# GraalVM Audit (arbercharts-core)

Status: generated configs for native-image shared library preparation.

## Scope
- Target artifact: **arbercharts-core** only.
- Native-image goal: **shared library** (`.so/.dylib/.dll`).
- Config location: `arbercharts-core/src/main/resources/META-INF/native-image/com.arbergashi/charts-core/`.
- Output: **audit report + reflect/resource configs**.

## Reflection Findings
### ServiceLoader
- `com.arbergashi.charts.core.rendering.ArberBridgeFactory` uses `ServiceLoader`.
- Resource config includes `META-INF/services/.*` to allow provider discovery at runtime.

### Explicit reflection (Class.forName / reflection calls)
1) `com.arbergashi.charts.api.forensic.PlaybackFactory`
   - `com.arbergashi.charts.engine.forensic.DeterministicPlaybackManager`
   - `com.arbergashi.charts.engine.forensic.ChronosPlaybackDrive`
   - `com.arbergashi.charts.engine.forensic.StreamPlaybackDriveImpl`
   - Configured with `allDeclaredConstructors=true` (conditional on typeReachable).

2) `com.arbergashi.charts.api.ViewportAuditExtractor`
   - Optional PDFBox usage via reflection:
     - `org.apache.pdfbox.pdmodel.PDDocument`
     - `org.apache.pdfbox.pdmodel.PDDocumentInformation`
   - Configured with method lists, conditional on typeReachable (safe when PDFBox absent).

### MethodHandles
- `com.arbergashi.charts.model.CircularChartModel` uses `MethodHandles` for VarHandle. No explicit reflection config required.

## Resource Findings
### Resource bundles
- Base bundle: `i18n.charts`
- Included in `resource-config.json`.

### Properties
- `i18n/charts.properties`
- `i18n/charts_en.properties`

## Generated Configs
- `reflect-config.json`
- `resource-config.json`

## Notes / Recommendations
- If additional reflective entry points are introduced, update `reflect-config.json`.
- Keep ServiceLoader provider files in bridge modules; resource config pattern already includes `META-INF/services/.*`.
- Consider removing `arbercharts-core/src/main/resources/.DS_Store` to keep native resources clean.
