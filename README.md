# ArberCharts 2.0.0

![Zero-GC](https://img.shields.io/badge/Zero--GC-Guaranteed-brightgreen) ![Java 25](https://img.shields.io/badge/Java-25-orange) ![Renderers](https://img.shields.io/badge/Renderers-157-blue) ![License](https://img.shields.io/badge/License-MIT-blue)

ArberCharts is an enterprise-grade Java charting framework for mission-critical systems.
It delivers deterministic rendering, lock-free streaming models, and a zero-GC rendering pipeline.

## Highlights

- Java 25 baseline
- 157 production renderers
- Swing desktop bridge
- Spring Boot starter
- Headless server rendering
- Demo application for catalog walkthroughs and customer presentations

## Maven

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

Or use modules directly:

- `arbercharts-core`
- `arbercharts-swing-bridge`
- `arbercharts-server-bridge`
- `arbercharts-spring-boot-starter`

## Documentation

All public documentation is under `docs/`:

- `docs/README.md`
- `docs/USER_GUIDE.md`
- `docs/RENDERER_CATALOG.md`
- `docs/QUICK_START.md`
- `docs/RELEASE_NOTES_2.0.0.md`
- `docs/LICENSING.md`

## Demo

```bash
mvn -pl arbercharts-demo -am package
java --enable-native-access=ALL-UNNAMED -jar arbercharts-demo/target/arbercharts-demo-2.0.0.jar
```

## License

ArberCharts runtime artifacts are distributed under the MIT License.
You may use ArberCharts in commercial and non-commercial projects, including closed-source end products,
as long as MIT copyright and license notice obligations are preserved on redistribution.

Commercial model:
- No runtime royalty for end products
- Paid support, SLA, and services

See `LICENSE`, `BINARY-LICENSE.md`, and `NOTICE`.

## Support

gashi@pro-business.ch
https://www.arbergashi.com
