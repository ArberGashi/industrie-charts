# Releasing ArberCharts to Maven Central

The current release line is **2.5.0**. A `verify` build creates local artifacts only; publishing to Maven Central and creating a GitHub release are separate release actions.

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

   Set `project.build.outputTimestamp` in the parent POM to the release date in
   UTC and retain it throughout verification and bundle preparation. Maven's
   archive plugins and Spring Boot use this value for stable archive timestamps.
   Compare the final library bytes with the libraries embedded in the tested
   demo; a repeated packaging step must not silently replace tested payloads.

3. Run a fresh signed build with the full quality gate, followed by scalar-fallback tests:

   ```sh
   mvn -B -Pcentral-release,quality-gate -Djava.awt.headless=true clean verify
   mvn -B -pl arbercharts-core -Djava.awt.headless=true test -Darbercharts.test.jvmargs=
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

## Prepare the signed Central bundle

Prepare a signed bundle for the five publishable modules without uploading it:

```sh
mvn -B -Djava.awt.headless=true deploy -Pcentral-release \
  -DskipUpload=true \
  -pl .,arbercharts-core,arbercharts-server-bridge,arbercharts-spring-boot-starter,arbercharts-swing-bridge
```

`skipUpload=true` stops the Central publishing plugin after local bundle
preparation. This step requires the configured signing key but does not publish
the release. The resulting bundle is
`target/central-publishing/central-bundle.zip`.

`arbercharts-demo`, `arbercharts-starter` and `arbercharts-spring-boot-demo` are
application/demo modules and are excluded from Central publication. They
configure `skipPublishing=true`. The Central publishing plugin aggregates at the
end of the selected reactor; including a skipped application as the final module
can prevent the expected bundle preparation. Keep the explicit module list above.

Inspect the exact signed bundle and both staged demo distributions before any
upload:

```sh
python3 tools/check_release_artifacts.py --version 2.5.0 \
  target/central-publishing/central-bundle.zip \
  dist/releases/jvm/2.5.0/ \
  dist/releases/spring-boot/2.5.0/
```

Apply the same check to the final GitHub library and demo ZIP bundles once they
are assembled. Review their file lists, current license notices, versioned
contents, signatures and checksums. Preserve the validated files unchanged for
upload; rebuilding or repackaging them requires another artifact check.

## Publish and verify

Publishing requires release authorization. Once that authorization is in place,
upload the exact validated `central-bundle.zip` with the Publisher API in
`USER_MANAGED` mode. Record the deployment identifier, wait for Central to report
`VALIDATED`, then request publication for that deployment through the API. This
workflow does not require a separate browser confirmation. Distinguish upload,
validation, publishing and public availability in the release record.

The `central-release` profile defaults to `autoPublish=false` and
`waitUntil=validated`. A Maven deployment without `skipUpload` therefore uploads
for validation rather than immediately publishing. The procedure above keeps
bundle creation, artifact inspection, upload and publication explicit. Use
credentials from the configured credential store; never put tokens into source
files, release notes or diagnostic output.

After Central completes publication, confirm the uploaded binaries, sources,
Javadoc and signatures for every published module. Check the versioned paths as
well as repository metadata, allowing for metadata synchronization:

```sh
curl -fsS https://repo1.maven.org/maven2/com/arbergashi/arbercharts-core/maven-metadata.xml
```

Commit and tag the approved source as `v2.5.0`. Publish the validated GitHub
library bundle, both executable demo JARs, both demo ZIP bundles, checksum file
and its signature. Compare public downloads with the local checksums. Packaging,
repository checks and bundle preparation do not themselves publish a release.

Central versions are immutable: a faulty publication requires a new version.
Add that version's release notes and link them from [RELEASE_NOTES.md](RELEASE_NOTES.md).
