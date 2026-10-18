# Architecture policy

These rules apply to the **2.2.0** module layout. The
[rendering architecture](RENDERING_ARCHITECTURE.md) describes integration and
migration details.

## Module ownership

- `arbercharts-core` owns models, themes, geometry, renderers and the
  `ArberCanvas`/`PlotContext` contracts. Its production classes must not depend
  on any `java.desktop` package, a desktop or server bridge, Spring, Micrometer
  or a UI framework.
- `arbercharts-swing-bridge` owns Swing components, Java2D integration, desktop
  interaction and export. `arbercharts-server-bridge` owns headless Java2D
  rendering and image encoding. Both depend on Core.
- `arbercharts-spring-boot-starter` owns Spring configuration and server-service
  integration. The demo applications compose the corresponding host APIs.
- Renderer algorithms use Core contracts. Add a frontend by implementing host
  and canvas responsibilities, without adding frontend types to renderer APIs.

Headless rendering removes the need for a display server, not the server
bridge's dependency on `java.desktop`. Keep functional rendering tests runnable
without a display, and validate desktop interaction separately on a real display.

## Allocation and state ownership

Reuse geometry buffers and formatting state where practical. Keep avoidable
allocations out of repeated rendering loops, and measure the allocation behavior
of the workload being changed. Buffer growth, cache misses, fonts, export and
backend operations can allocate; structural rules alone are not a performance
guarantee.

Renderer and canvas instances can own mutable scratch state. Hosts must manage
session ownership, lifecycle and thread access. See the
[concurrency model](CONCURRENCY_MODEL.md).

## Configuration

`ChartAssets` is a process-wide registry. Configure its defaults during
application startup. Request-specific or tenant-specific behavior belongs in
the model, `PlotContext`, `ChartTheme` and renderer contracts, rather than
mutating global defaults during a render. Document the scope and expected
lifetime of new configuration keys.

## Enforcement

`CoreSovereigntyTest` checks all compiled production Core classes and its Maven
dependency declarations. `ZeroGcArchitectureTest` checks renderer placement
and selected allocation patterns. Both use the Java 25 class-file API and JDK
dependency analysis; the project no longer uses ArchUnit.

Run the focused checks from the repository root:

```bash
mvn -B -pl arbercharts-core -Dtest=CoreSovereigntyTest,ZeroGcArchitectureTest test
```

Run the full quality gate and functional tests before accepting a change:

```bash
mvn -B -Pquality-gate -Djava.awt.headless=true verify
```

Review generated and manually written changes against these same contracts.
The [verification strategy](VERIFICATION_STRATEGY.md) explains what each check
covers and what still needs workload or visual validation.
