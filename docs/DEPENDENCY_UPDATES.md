# Dependency and build updates for 2.2

Stable versions were checked against Maven Central and upstream documentation on
2026-10-16. The normal build and both JVM demos use the existing Eclipse Temurin
25 installation. No JDK installation or machine-wide configuration change is
part of this update. Temurin is available without commercial JDK subscription
fees under GPLv2 with the Classpath Exception. See the
[Adoptium FAQ](https://adoptium.net/docs/faq#is-eclipse-temurin-free-to-use).

## Selected dependencies

| Family | Version | Integration decision |
| --- | --- | --- |
| Spring Boot | 4.1.1 | Import the Boot BOM for the starter and runnable web demo. |
| JUnit | 6.1.3 | Import the entire family BOM before Boot's defaults. |
| jqwik | 1.10.2 | Verify property discovery and execution with the selected JUnit Platform. |
| Micrometer | 1.17.1 | Import its family BOM; matches Boot 4.1.1. |
| SLF4J | 2.0.20 | Import its family BOM before Boot's defaults. |
| FlatLaf / Extras | 3.7.2 | Already the latest stable versions; used by the Swing demo, removed from Core. |
| Inter font bundle | 4.1 | Own version property; independent of FlatLaf. |
| JSVG | 2.2.0 | Override Extras' older transitive version; verify actual demo SVG icons. |
| JMH | 1.37 | Already the latest stable version. |
| GraalVM nativeimage API | 25.4.4.1.1 | Provided scope; replaces the broader graal-sdk aggregate. |

The JUnit BOM aligns Jupiter, Platform, Commons and Launcher. This matters because
Boot and jqwik declare different Platform baselines. The existing jqwik
properties run during the functional build; compatibility is not inferred from
version numbers alone. See [JUnit build support](https://docs.junit.org/6.1.3/running-tests/build-support.html)
and [jqwik release notes](https://jqwik.net/release-notes.html).

Core imports only the optional `org.graalvm.nativeimage` API, so the SDK aggregate's
unused Polyglot and Collections dependencies are no longer needed. This
compile-time API does not change the JVM to GraalVM or Oracle JDK. Producing a
native binary is a separate build requiring its own toolchain and validation.
See the [GraalVM Community release notes](https://www.graalvm.org/release-notes/25.4/).

## Maven plugins

These stable Maven 3-compatible versions are centralized in the parent POM:

| Plugin | Version |
| --- | --- |
| `org.apache.maven.plugins:maven-compiler-plugin` | 3.16.0 |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.6.0 |
| `org.apache.maven.plugins:maven-source-plugin` | 3.4.0 |
| `org.apache.maven.plugins:maven-javadoc-plugin` | 3.12.0 |
| `org.apache.maven.plugins:maven-jar-plugin` | 3.5.1 |
| `org.apache.maven.plugins:maven-clean-plugin` | 3.5.0 |
| `org.apache.maven.plugins:maven-install-plugin` | 3.2.0 |
| `org.apache.maven.plugins:maven-site-plugin` | 3.22.0 |
| `org.apache.maven.plugins:maven-deploy-plugin` | 3.2.0 |
| `org.apache.maven.plugins:maven-resources-plugin` | 3.5.0 |
| `org.apache.maven.plugins:maven-checkstyle-plugin` | 3.6.0 |
| `org.apache.maven.plugins:maven-enforcer-plugin` | 3.6.3 |
| `org.codehaus.mojo:exec-maven-plugin` | 3.6.4 |
| `org.sonatype.central:central-publishing-maven-plugin` | 0.12.0 |
| `org.apache.maven.plugins:maven-gpg-plugin` | 3.2.8 |
| `org.apache.maven.plugins:maven-shade-plugin` | 3.6.2 |
| `org.apache.maven.plugins:maven-antrun-plugin` | 3.2.0 |

The new web demo also uses `spring-boot-maven-plugin` at the same version as the
Boot BOM. Maven 4 prerelease plugins are not used. The installed Maven 3.9 series
is sufficient; no Maven installation is required by these changes.

The Checkstyle plugin's bundled engine is older than the plugin itself, so it is
overridden explicitly with Checkstyle 14.3.0. This follows the
[official plugin instructions](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html).
The quality gate retains Enforcer dependency convergence, upper-bound and plugin
version checks. Surefire uses `excludedGroups`/`groups` for the `perf` tag.

## Fresh artifacts and verification

`mvn -B -Djava.awt.headless=true clean verify` rebuilds all modules, source JARs,
English Javadocs and both executable demos. Distributions are staged under
`dist/releases/jvm/<version>/` and `dist/releases/spring-boot/<version>/`.
Launchers refer to the exact JAR from the same build instead of selecting an
older wildcard match. Historical distributions are not treated as current output.

The [verification strategy](VERIFICATION_STRATEGY.md) covers the architecture,
canvas contracts, properties, HTTP/PNG integration and scalar fallback checks.
The dependency update does not publish artifacts or validate native-image output.
The `central-release` profile still requires an explicit release action.

## Research evidence

The [machine-readable research inventory](audits/2026-10-16-dependencies.json)
records each audited coordinate, baseline declaration, selected version and
primary Maven Central/upstream sources. It is the research snapshot before
integration, so its `not_performed` field describes the research step, not later
repository verification. It is a version/compatibility inventory, not a complete
vulnerability audit of all transitive dependencies. CI action versions were
outside this Maven dependency review.
