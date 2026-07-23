# ArberCharts v2.1.0

**Release Date:** July 24, 2026

## 🎯 Headless Rendering Overhaul

2.1.0 makes server-side rendering a first-class citizen. A new smoke test
renders every cataloged renderer headless - it uncovered and drove out a
series of long-standing issues:

- **45 renderers drew fully transparent** through `ServerRenderService`:
  `BaseRenderer` never assigned its active context, so the theme fallback
  could not fire. One systemic fix healed Bar, Scatter, Bubble, the whole
  analysis family, and more.
- **Spatial pipeline:** renderers without an explicit spatial style were
  drawn with a transparent color (style key 0 now falls back to the theme);
  `LineSpatialRenderer` and `Candlestick3DRenderer` projected raw data and
  collapsed or exploded off-canvas - both now normalize into unit space.
- **Registry collision:** both `CandlestickHollowRenderer` variants
  self-registered under `candlestick_hollow` and crashed when both classes
  loaded. The financial variant is canonical; the specialized one is
  deprecated (removal in 3.0). The specialized `VectorFieldRenderer`
  tombstone is removed.
- **Point &amp; Figure** drew every box off-screen at a constant point; the
  chart is rebuilt with real columns, price levels, and X/O glyphs.
- **AdaptiveFunctionRenderer** draws its first frame synchronously instead
  of always rendering blank in single-frame scenarios.

## 🖼 View-Window Render API

`ServerRenderService` gains overloads with explicit view windows:

- Y window for oscillator indicators (MACD, Stochastic, ADX, ATR) that draw
  in their own value domain.
- Full X/Y window for fixed time windows and forward-looking overlays -
  predictive renderers draw beyond the last data point and are now visible
  headless.

## 🧱 Deterministic Registry

`RendererBootstrap.ensureRegistered()` loads all self-registering renderers
in a fixed order; registry content no longer depends on class-loading order.
The server bridge bootstraps automatically.

## ✅ Test Coverage

- Renderer smoke test across the full catalog (155 dynamic tests): id
  collisions, broken static initializers, render exceptions, blank output.
- SIMD/scalar parity suite continues to guard the Vector API fallback.
- CI runs the core suite with and without the Vector API module.

## 🔧 Maintenance

- FlatLaf updated to 3.7.2.
- Repository slimmed: generated site, binary JARs, and obsolete documents
  removed; docs consolidated (MIT-coherent support pricing, `RELEASING.md`).
- 361 stale `@version` Javadoc tags removed; tag indentation normalized.
- Dead code removed (`NativeLibraryLoader`).
- 156 renderers.

## Artifacts

Published to Maven Central under `com.arbergashi`:
`arbercharts-core`, `arbercharts-server-bridge`, `arbercharts-spring-boot-starter`,
`arbercharts-swing-bridge` (and `arber-charts-parent`).
