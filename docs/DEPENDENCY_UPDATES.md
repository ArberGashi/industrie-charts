# Dependency and build updates for 2.5.0

The dependency review for **2.5.0** was refreshed on **2026-10-18** against
Maven Central metadata and upstream documentation. It covered the parent and
all seven module POMs, including 43 dependency and plugin coordinates. Only
stable releases were selected; snapshots, milestones, release candidates and
alpha/beta versions were excluded even when Central's `release` field listed
one of them. Version declarations remain in the [parent POM](../pom.xml).

The development and release toolchain remains the existing **Eclipse Temurin
25** installation. Spring Boot 4.1.1 supports Java 17 through 26 according to its
[system requirements](https://docs.spring.io/spring-boot/system-requirements.html).
Version metadata establishes release availability; the repository's build and
runtime checks establish compatibility with this project.

## Selected dependencies

| Family | Selected stable version | Integration decision and primary source |
| --- | --- | --- |
| Spring Boot | [4.1.1](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/maven-metadata.xml) | Current stable BOM, starter dependencies and executable-JAR plugin. |
| JUnit | [6.1.3](https://repo.maven.apache.org/maven2/org/junit/junit-bom/maven-metadata.xml) | Align the entire family before Boot's defaults. |
| jqwik | [1.10.2](https://repo.maven.apache.org/maven2/net/jqwik/jqwik/maven-metadata.xml) | Property tests use the selected JUnit Platform. |
| Micrometer | [1.17.1](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-bom/maven-metadata.xml) | Import its family BOM; matches Boot 4.1.1. |
| SLF4J | [2.0.20](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-bom/maven-metadata.xml) | Align the logging API family before Boot's defaults. |
| Logback Classic / Core | [1.6.5](https://logback.qos.ch/news.html) | Upgrade both modules together from Boot's 1.5.38 default. |
| FlatLaf / Extras | [3.7.2](https://github.com/JFormDesigner/FlatLaf/releases/tag/3.7.2) | Already current; both remain confined to the Swing demo. |
| Inter font bundle | [4.1](https://repo.maven.apache.org/maven2/com/formdev/flatlaf-fonts-inter/maven-metadata.xml) | Already current; its version is independent of FlatLaf. |
| JSVG | [2.2.0](https://repo.maven.apache.org/maven2/com/github/weisj/jsvg/maven-metadata.xml) | Already current; overrides the 2.1.0 runtime dependency declared by FlatLaf Extras 3.7.2. |
| JMH | [1.37](https://repo.maven.apache.org/maven2/org/openjdk/jmh/jmh-core/maven-metadata.xml) | Still the latest stable release; use it where the actual benchmarks reside. |
| GraalVM nativeimage API | [25.4.4.1.1](https://repo.maven.apache.org/maven2/org/graalvm/sdk/nativeimage/maven-metadata.xml) | Provided scope for Core's native entry-point declarations. |

The [Boot 4.1.1 BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)
still selects Logback 1.5.38. Logback now classifies 1.5.x as legacy; 1.6.x requires
Java 11 or later and SLF4J 2.0.x. The parent explicitly manages both `logback-core`
and `logback-classic` at 1.6.5. The upgrade needs a fresh Boot startup and HTTP
smoke check with logging enabled because 1.6.0 removed deprecated APIs. The
repository contains no custom Janino-based Logback configuration. See the
[Logback release notes](https://logback.qos.ch/news.html) and
[removed APIs](https://logback.qos.ch/notes/release_1.6.0.txt).

The JUnit BOM aligns Jupiter, Platform, Commons and Launcher. This matters because
Boot and jqwik declare different Platform baselines. The existing jqwik
properties run during the functional build; compatibility is not inferred from
version numbers alone. See [JUnit build support](https://docs.junit.org/6.1.3/running-tests/build-support.html)
and [jqwik release notes](https://jqwik.net/release-notes.html).

Core's native entry points use the provided API; the broader `graal-sdk`
aggregate is not needed. This compile-time dependency does not replace Temurin.
Producing a native binary requires a separate toolchain and validation.

## Maven plugins

These are the latest stable plugin versions found in the review. Each version
links to its authoritative Maven Central metadata:

| Plugin | Version |
| --- | --- |
| `org.apache.maven.plugins:maven-compiler-plugin` | [3.16.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-surefire-plugin` | [3.6.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-surefire-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-source-plugin` | [3.4.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-source-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-javadoc-plugin` | [3.12.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-javadoc-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-jar-plugin` | [3.5.1](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-jar-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-clean-plugin` | [3.5.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-clean-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-install-plugin` | [3.2.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-install-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-site-plugin` | [3.22.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-site-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-deploy-plugin` | [3.2.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-deploy-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-resources-plugin` | [3.5.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-resources-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-checkstyle-plugin` | [3.6.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-checkstyle-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-enforcer-plugin` | [3.6.3](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-enforcer-plugin/maven-metadata.xml) |
| `org.codehaus.mojo:exec-maven-plugin` | [3.6.4](https://repo.maven.apache.org/maven2/org/codehaus/mojo/exec-maven-plugin/maven-metadata.xml) |
| `org.sonatype.central:central-publishing-maven-plugin` | [0.12.0](https://repo.maven.apache.org/maven2/org/sonatype/central/central-publishing-maven-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-gpg-plugin` | [3.2.8](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-gpg-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-shade-plugin` | [3.6.2](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-shade-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-antrun-plugin` | [3.2.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-antrun-plugin/maven-metadata.xml) |
| `org.apache.maven.plugins:maven-dependency-plugin` | [3.11.0](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-dependency-plugin/maven-metadata.xml) |
| `org.springframework.boot:spring-boot-maven-plugin` | [4.1.1](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-maven-plugin/maven-metadata.xml) |

The web demo uses the same Boot version for its packaging plugin and BOM. Maven 4
prerelease plugins are excluded; the existing Maven 3.9 toolchain is retained.

The Checkstyle plugin's bundled engine is older than the plugin itself, so it is
overridden explicitly with [Checkstyle 14.3.0](https://repo.maven.apache.org/maven2/com/puppycrawl/tools/checkstyle/maven-metadata.xml),
also the latest stable engine at review time. This follows the
[official plugin instructions](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html).
The quality gate retains Enforcer dependency convergence, upper-bound and plugin
version checks. Surefire uses `excludedGroups`/`groups` for the `perf` tag;
selecting tagged JUnit tests does not itself launch JMH benchmarks.

Java 25 requires annotation processors to be activated explicitly. The Boot
configuration processor is assigned to the starter's main compilation, and the
JMH generator to the Swing bridge's test compilation, where the benchmarks
reside. Both use explicit processor paths instead of ordinary dependencies.
Core's unused JMH dependencies and runner have been removed. See
[Boot metadata generation](https://docs.spring.io/spring-boot/specification/configuration-metadata/annotation-processor.html)
and [Maven's annotation processor configuration](https://maven.apache.org/plugins/maven-compiler-plugin-4.x/examples/annotation-processor.html).

For 2.5.0, the Swing demo and local shaded starter enable the parent-managed
Dependency Plugin execution during `prepare-package`. It copies matching license, notice,
copyright and copying files from bundled third-party runtime JARs into separate
artifact directories under `META-INF/third-party/`. This preserves the dependency
texts alongside the project's canonical `META-INF/LICENSE` and `META-INF/NOTICE`
instead of replacing their terms with the project's MIT license. The plugin
version and execution are declared in the [parent POM](../pom.xml). The Spring
Boot executable keeps its dependency JARs, including their notices, nested inside
the application archive.

## Fresh artifacts and verification

`mvn -B -Djava.awt.headless=true clean verify` rebuilds all modules, source JARs,
English Javadocs and both executable demos. Distributions are staged under
`dist/releases/jvm/<version>/` and `dist/releases/spring-boot/<version>/`.
Launchers refer to the exact JAR from the same build instead of selecting an
older wildcard match. Historical distributions are not treated as current output.

The [verification strategy](VERIFICATION_STRATEGY.md) covers the architecture,
canvas contracts, properties, HTTP/PNG integration and scalar fallback checks.
Validate the exact packaged files with `tools/check_release_artifacts.py` as
described in [Releasing](../RELEASING.md), and inspect the retained third-party
texts in the shaded archives before publication.
The dependency update does not publish artifacts or validate native-image output.
The `central-release` profile still requires an explicit release action.

This review checks the declared Maven dependencies and build plugins, selected
logging transitive dependencies, and their source-level usage. It is not a
complete transitive dependency or vulnerability audit. CI action versions and
native-image execution are outside its scope. The links above replace the
obsolete pre-integration inventory; build and release verification remain
separate from this version review.
