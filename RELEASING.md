# Releasing ArberCharts to Maven Central

## Prerequisites

- Central Portal token in `~/.m2/settings.xml` under server id `central`.
- GnuPG with a usable signing key; the public key must be published on
  `keyserver.ubuntu.com`. No passphrase interaction should be required
  (key without passphrase, or passphrase preset in `gpg-agent`).

## Steps

1. Set the release version (no SNAPSHOT):

   ```bash
   mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   ```

2. Full verification build:

   ```bash
   mvn clean verify
   mvn -pl arbercharts-core test -Darbercharts.test.jvmargs=   # scalar fallback pass
   ```

3. Deploy. IMPORTANT: list the publishable modules explicitly. The
   central-publishing plugin uploads from the last module in the reactor;
   `arbercharts-demo` and `arbercharts-starter` set `skipPublishing=true`,
   and if one of them runs last, the entire upload is silently skipped.

   ```bash
   mvn clean deploy -Pcentral-release \
     -pl .,arbercharts-core,arbercharts-server-bridge,arbercharts-spring-boot-starter,arbercharts-swing-bridge
   ```

4. Verify on Central (allow a few minutes for sync):

   ```bash
   curl -s https://repo1.maven.org/maven2/com/arbergashi/arbercharts-core/maven-metadata.xml
   ```

5. Commit, tag `vX.Y.Z`, push, then bump to the next `-SNAPSHOT` version.

## Notes

- Versions on Central are immutable - mistakes require a new version, never a
  re-upload.
- Release notes: add `RELEASE_NOTES_X.Y.Z.md` and link it from
  `RELEASE_NOTES.md`.
