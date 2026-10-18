# Releasing ArberCharts to Maven Central

The current release line is **2.2.0**. A `verify` build creates local artifacts only; publishing to Maven Central and creating a GitHub release are separate release actions.

## Build environment

- Use the existing **Eclipse Temurin JDK 25** and **Maven 3.9 or newer**. CI uses Temurin 25 on Linux, Windows and macOS. No Oracle JDK or replacement Java installation is needed for the JVM release.
- Keep dependency and plugin versions in the parent POM. The release baseline includes Spring Boot 4.1.1 and JUnit 6.1.3; review [Dependency updates](docs/DEPENDENCY_UPDATES.md) before changing it.
- Native-image builds are a separate toolchain and validation step; see [GraalVM audit](docs/GRAALVM_AUDIT.md). Do not treat an existing native binary as proof of a current-source native build.

Publishing additionally requires a Central Portal token in `~/.m2/settings.xml` under server ID `central`, plus a usable GnuPG signing key and configured signing agent. Keep credentials and signing secrets outside the repository.

## Prepare and verify

1. Review the release scope, [migration notes](docs/RENDERING_ARCHITECTURE.md#migration-from-210) and [verification limits](docs/VERIFICATION_STRATEGY.md). Record unresolved [rendering audit](docs/audits/2026-10-16-rendering/README.md) findings accurately; a green build is not complete visual acceptance. Keep the current tree free of superseded release instructions, commercial documents and generated artifacts. Run `python3 tools/check_repository.py` before packaging.

2. Set the intended release version across the reactor:

   ```sh
   mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   ```

3. Run a fresh full build, the scalar-fallback tests and static quality gate:

   ```sh
   mvn -B -Djava.awt.headless=true clean verify
   mvn -B -pl arbercharts-core -Djava.awt.headless=true test -Darbercharts.test.jvmargs=
   mvn -B -Pquality-gate validate
   ```

   The normal build attaches fresh binary, source and English Javadoc JARs. Check the generated Javadoc and relevant demo output before preparing the release. Architecture tests use the actual compiled Core location; an alternate output directory can be checked with:

   ```sh
   mvn -B -pl arbercharts-core test \
     -Dtest=CoreSovereigntyTest,ZeroGcArchitectureTest \
     -Darbercharts.buildDirSuffix=-architecture
   ```

4. Inspect the staged demo distributions for the selected `${project.version}`:

   ```text
   dist/releases/jvm/${project.version}/
   dist/releases/spring-boot/${project.version}/
   ```

   Each directory contains its current application JAR. The Swing distribution includes launch scripts naming that exact JAR; the Spring Boot distribution includes its README with startup instructions. Start the Swing demo on a graphical desktop and the Spring Boot demo locally; verify the intended version and behavior. Do not select a JAR by an ambiguous wildcard or reuse an older distribution from another version directory.

5. Update the current release notes and review the final diff, artifact contents and verification results. Keep earlier release documentation in Git history. Preserve dated evidence supporting the current release. Check that freshly built JARs contain the current `META-INF/LICENSE` and `META-INF/NOTICE`, and that release bundles include these same notices. Never replace already published binaries under the same version to repair embedded metadata; publish those corrections with a new version.

## Publish the selected modules

Only run deployment as part of an authorized release. List the publishable modules explicitly:

```sh
mvn -B -Djava.awt.headless=true clean deploy -Pcentral-release \
  -pl .,arbercharts-core,arbercharts-server-bridge,arbercharts-spring-boot-starter,arbercharts-swing-bridge
```

`arbercharts-demo`, `arbercharts-starter` and `arbercharts-spring-boot-demo` are application/demo modules and are excluded from Central publication. They configure `skipPublishing=true`. The Central publishing plugin aggregates at the end of the selected reactor; including a skipped application as the final module can prevent the expected upload. Keep the explicit module list above.

After deployment, verify the release in Central and allow for metadata synchronization:

```sh
curl -fsS https://repo1.maven.org/maven2/com/arbergashi/arbercharts-core/maven-metadata.xml
```

Confirm the uploaded binary, sources, Javadoc and signatures for each published module. Commit and tag the approved release as `vX.Y.Z`, publish the intended GitHub assets, then bump the development version according to the release workflow. Deployment, tagging and pushing are not performed by the verification commands.

Central versions are immutable: a faulty publication requires a new version. Add the versioned release notes and link them from [RELEASE_NOTES.md](RELEASE_NOTES.md).
