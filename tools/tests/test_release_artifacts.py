"""Release-content regressions using small archives, without Maven or a JDK."""

from contextlib import redirect_stderr, redirect_stdout
import hashlib
import importlib.util
import io
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
from unittest import mock
import zipfile


TOOLS = Path(__file__).resolve().parents[1]
VERSION = '2.5.0'
OLD_VERSION = '2.2.0'
CORE = 'arbercharts-core'
BOOT = 'arbercharts-spring-boot-demo'
STARTER = 'arbercharts-spring-boot-starter'
PARENT = 'arber-charts-parent'
SWING = 'arbercharts-demo'
LIBRARIES = (CORE, 'arbercharts-server-bridge', STARTER, 'arbercharts-swing-bridge')
ABOUT_DELIVERY = 'Support: Commercial support, closed-source product delivery'
LICENSE = b'MIT License\nCopyright (c) 2026 Fixture Authors\n'
NOTICE = b'Fixture project. Third-party dependencies retain their own licenses.\n'


def pom(artifact, version=VERSION, extra=''):
    """A valid project POM with an intentionally different third-party version."""
    return f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.arbergashi</groupId><artifactId>{artifact}</artifactId>
  <version>{version}</version>
  <licenses><license><name>MIT License</name>
    <url>https://opensource.org/licenses/MIT</url></license></licenses>
  <dependencies><dependency><groupId>org.example</groupId>
    <artifactId>external</artifactId><version>1.4.9</version>
  </dependency></dependencies>{extra}
</project>'''.encode('utf-8')


def archive(entries):
    result = io.BytesIO()
    with zipfile.ZipFile(result, 'w', zipfile.ZIP_STORED) as zipped:
        for name, content in entries.items():
            zipped.writestr(name, content)
    return result.getvalue()


def class_with_literal(text):
    """Minimal Java 25 class with a String constant; no compiler is needed."""
    def utf8(value):
        encoded = value.encode('utf-8')
        return b'\x01' + struct.pack('>H', len(encoded)) + encoded

    pool = (utf8('com/arbergashi/charts/demo/DemoApplication') + b'\x07\x00\x01'
            + utf8('java/lang/Object') + b'\x07\x00\x03'
            + utf8(text) + b'\x08\x00\x05')
    # Seven constant-pool slots including slot zero; public class, no members.
    return struct.pack('>IHHH', 0xCAFEBABE, 0, 69, 7) + pool + struct.pack('>HHHHHHH', 0x21, 2, 4, 0, 0, 0, 0)


def runtime_entries(artifact=CORE, version=VERSION):
    metadata = f'META-INF/maven/com.arbergashi/{artifact}/'
    return {
        'META-INF/MANIFEST.MF': (f'Manifest-Version: 1.0\r\nImplementation-Version: {version}\r\n'
                                 'License: MIT License\r\nLicense-URL: https://opensource.org/licenses/MIT\r\n\r\n').encode(),
        'META-INF/LICENSE': LICENSE,
        'META-INF/NOTICE': NOTICE,
        metadata + 'pom.xml': pom(artifact, version),
        metadata + 'pom.properties': f'groupId=com.arbergashi\nartifactId={artifact}\nversion={version}\n'.encode(),
        'com/arbergashi/charts/demo/DemoApplication.class': class_with_literal('Industrie Charts'),
    }


def foreign_jar():
    # Even the exact retired project sentence is allowed in a foreign artifact.
    return archive({
        'META-INF/LICENSE': ('Different vendor license. ' + ABOUT_DELIVERY).encode(),
        'META-INF/NOTICE': b'Third-party copyright, not the project NOTICE.',
        'META-INF/maven/org.example/external/pom.properties': b'groupId=org.example\nartifactId=external\nversion=1.4.9\n',
    })


def central_entries():
    """Complete independent fixture for the five publishable Maven modules."""
    entries = {}
    for artifact in (PARENT, *LIBRARIES):
        base = f'com/arbergashi/{artifact}/{VERSION}/{artifact}-{VERSION}'
        entries[base + '.pom'] = pom(artifact)
        if artifact != PARENT:
            for suffix in ('.jar', '-sources.jar', '-javadoc.jar'):
                entries[base + suffix] = archive(runtime_entries(artifact))
    for name, data in list(entries.items()):
        entries[name + '.asc'] = b'-----BEGIN PGP SIGNATURE-----\n\nAQ==\n-----END PGP SIGNATURE-----\n'
        for algorithm in ('md5', 'sha1', 'sha256', 'sha512'):
            entries[name + '.' + algorithm] = hashlib.new(algorithm, data, usedforsecurity=False).hexdigest().encode()
    return entries


def launcher(name, version=VERSION):
    jar = f'{SWING}-{version}.jar'
    if name.endswith('.bat'):
        return (f'@echo off\nset "DIR=%~dp0"\nset "JAR=%DIR%{jar}"\n'
                'java --add-modules jdk.incubator.vector -jar "%JAR%" %*\n').encode()
    return (f'#!/bin/sh\nDIR="$(dirname -- "$0")"\nJAR="$DIR/{jar}"\n'
            'exec java --add-modules jdk.incubator.vector -jar "$JAR" "$@"\n').encode()


class ReleaseFixture(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='charts-release-check-')
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / 'LICENSE').write_bytes(LICENSE)
        (self.root / 'NOTICE').write_bytes(NOTICE)
        modules = ''.join(f'<module>{module}</module>' for module in (*LIBRARIES, SWING, BOOT, 'arbercharts-starter'))
        (self.root / 'pom.xml').write_bytes(pom('arber-charts-parent', extra=f'<modules>{modules}</modules>'))
        for module in (*LIBRARIES, SWING, BOOT, 'arbercharts-starter'):
            (self.root / module).mkdir()
            (self.root / module / 'pom.xml').write_bytes(pom(module))
        tools = self.root / 'tools'
        tools.mkdir()
        self.cli = tools / 'check_release_artifacts.py'
        shutil.copyfile(TOOLS / self.cli.name, self.cli)

    def write(self, name, content):
        path = self.root / 'artifacts' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        return path

    def check(self, path, accepted=True, contains=None, version=VERSION):
        result = subprocess.run([sys.executable, str(self.cli), '--version', version, str(path)],
                                capture_output=True, text=True, timeout=15)
        output = result.stdout + result.stderr
        self.assertEqual(result.returncode, 0 if accepted else 1, output)
        self.assertNotIn('Traceback', output)
        if contains:
            self.assertIn(contains, output)
        return output

    def jar(self, entries=None, artifact=CORE, version=VERSION, directory=''):
        return self.write(f'{directory}{artifact}-{version}.jar', archive(
            runtime_entries(artifact, version) if entries is None else entries))

    def distribution(self, entries=None):
        prefix = f'industrie-charts-demo-fixture-{VERSION}/'
        contents = {'LICENSE': LICENSE, 'NOTICE': NOTICE,
                    f'{CORE}-{VERSION}.jar': archive(runtime_entries())}
        if entries is not None:
            contents = entries
        return self.write(f'industrie-charts-demo-fixture-{VERSION}.zip', archive(
            {prefix + name: data for name, data in contents.items()}))


class ReleaseArtifactTests(ReleaseFixture):
    def test_current_project_version_allows_foreign_versions_and_historical_since(self):
        entries = runtime_entries()
        entries['docs/history.md'] = b'The historical API documents @since 1.7.0.'
        self.check(self.jar(entries), contains='1 runtime JARs')

    def test_wrong_project_metadata_versions_are_rejected(self):
        metadata = f'META-INF/maven/com.arbergashi/{CORE}/'
        for field in ('filename', 'manifest', 'pom.xml', 'pom.properties'):
            with self.subTest(field=field):
                entries = runtime_entries()
                if field == 'manifest':
                    key = 'META-INF/MANIFEST.MF'
                elif field != 'filename':
                    key = metadata + field
                if field != 'filename':
                    entries[key] = entries[key].replace(VERSION.encode(), OLD_VERSION.encode())
                path = self.jar(entries, version=OLD_VERSION if field == 'filename' else VERSION)
                self.check(path, accepted=False, contains=f'expected {VERSION}')

    def test_expected_version_is_explicit_and_not_taken_from_fixture_pom(self):
        self.check(self.jar(), accepted=False, version=OLD_VERSION, contains=f'expected {OLD_VERSION}')

    def test_own_dependency_version_must_match_but_external_version_may_differ(self):
        entries = runtime_entries(BOOT)
        metadata = f'META-INF/maven/com.arbergashi/{BOOT}/pom.xml'
        own_dependency = f'<dependency><groupId>com.arbergashi</groupId><artifactId>{CORE}</artifactId><version>{OLD_VERSION}</version></dependency>'
        entries[metadata] = pom(BOOT).replace(b'</dependencies>', own_dependency.encode() + b'</dependencies>')
        self.check(self.jar(entries, artifact=BOOT), accepted=False, contains=f'{CORE} dependency version')

    def test_own_parent_version_must_match(self):
        entries = runtime_entries()
        parent = f'<parent><groupId>com.arbergashi</groupId><artifactId>arber-charts-parent</artifactId><version>{OLD_VERSION}</version></parent>'
        entries[f'META-INF/maven/com.arbergashi/{CORE}/pom.xml'] = pom(CORE).replace(
            b'</modelVersion>', b'</modelVersion>' + parent.encode())
        self.check(self.jar(entries), accepted=False, contains='parent version')

    def test_runtime_notices_must_exist_and_match_exactly(self):
        for name in ('LICENSE', 'NOTICE'):
            for mutation in ('missing', 'changed'):
                with self.subTest(name=name, mutation=mutation):
                    entries = runtime_entries()
                    if mutation == 'missing':
                        del entries['META-INF/' + name]
                    else:
                        entries['META-INF/' + name] += b'Additional delivery terms.'
                    self.check(self.jar(entries), accepted=False, contains=name)

    def test_distribution_root_notices_are_checked_as_well_as_inner_jar(self):
        self.check(self.distribution(), contains='1 ZIP bundles')
        for name in ('LICENSE', 'NOTICE'):
            with self.subTest(name=name):
                entries = {'LICENSE': LICENSE, 'NOTICE': NOTICE,
                           f'{CORE}-{VERSION}.jar': archive(runtime_entries())}
                del entries[name]
                self.check(self.distribution(entries), accepted=False, contains='missing canonical')

    def test_old_about_delivery_class_literal_is_rejected(self):
        for prefix in ('', 'BOOT-INF/classes/'):
            with self.subTest(prefix=prefix):
                entries = runtime_entries()
                entries[prefix + 'com/arbergashi/charts/demo/DemoApplication.class'] = class_with_literal(ABOUT_DELIVERY)
                self.check(self.jar(entries), accepted=False, contains='obsolete delivery wording')

    def test_general_support_and_mit_commercial_use_are_not_banned(self):
        entries = runtime_entries()
        entries['README.md'] = b'MIT permits commercial use and closed-source products. Technical support is available.'
        entries['com/arbergashi/charts/demo/DemoApplication.class'] = class_with_literal('Support: GitHub Issues')
        self.check(self.jar(entries))

    def test_nested_boot_project_artifacts_checked_and_foreign_notices_isolated(self):
        entries = runtime_entries(BOOT)
        entries[f'BOOT-INF/lib/{CORE}-{VERSION}.jar'] = archive(runtime_entries())
        entries['BOOT-INF/lib/external-1.4.9.jar'] = foreign_jar()
        self.check(self.jar(entries, artifact=BOOT), contains='1 third-party JARs checked')
        nested = runtime_entries()
        nested['META-INF/NOTICE'] += b'Changed project notice.'
        entries[f'BOOT-INF/lib/{CORE}-{VERSION}.jar'] = archive(nested)
        self.check(self.jar(entries, artifact=BOOT), accepted=False, contains=f'BOOT-INF/lib/{CORE}-{VERSION}.jar!/META-INF/NOTICE')

    def test_foreign_nested_archives_are_checked_for_crc_and_structure(self):
        payload = b'Unique foreign class payload for the CRC regression.'
        foreign = archive({'org/example/Example.class': payload})
        corrupted = foreign.replace(payload, b'X' + payload[1:], 1)
        with zipfile.ZipFile(io.BytesIO(corrupted)) as zipped:
            self.assertEqual(zipped.testzip(), 'org/example/Example.class')
        invalid = (
            ('direct CRC', corrupted, 'CRC mismatch'),
            ('invalid ZIP', b'not a ZIP archive', 'cannot validate archive'),
            ('unsafe member', archive({'../Example.class': payload}), 'unsafe or ambiguous'),
            ('nested JAR CRC', archive({'lib/inner-1.0.jar': corrupted}), 'CRC mismatch'),
            ('nested ZIP CRC', archive({'lib/inner-1.0.zip': corrupted}), 'CRC mismatch'),
        )
        for case, contents, expected in invalid:
            with self.subTest(case=case):
                entries = runtime_entries(BOOT)
                entries['BOOT-INF/lib/external-1.4.9.jar'] = contents
                self.check(self.jar(entries, artifact=BOOT), accepted=False, contains=expected)

    def test_project_pom_license_metadata_matches_mit_or_inherits_parent(self):
        for old, new in ((b'MIT License', b'Apache License, Version 2.0'),
                         (b'https://opensource.org/licenses/MIT', b'https://example.org/restrictive-license')):
            with self.subTest(new=new):
                entries = runtime_entries()
                member = f'META-INF/maven/com.arbergashi/{CORE}/pom.xml'
                entries[member] = entries[member].replace(old, new)
                self.check(self.jar(entries), accepted=False, contains='POM license must match')
        without_license = pom(CORE)
        start, end = without_license.index(b'<licenses>'), without_license.index(b'</licenses>')
        without_license = without_license[:start] + without_license[end + len(b'</licenses>'):]
        self.check(self.write(f'{CORE}-{VERSION}.pom', without_license),
                   accepted=False, contains='declare or inherit')
        parent = (f'<parent><groupId>com.arbergashi</groupId><artifactId>{PARENT}</artifactId>'
                  f'<version>{VERSION}</version></parent>').encode()
        inherited = without_license.replace(b'<modelVersion>4.0.0</modelVersion>',
                                            b'<modelVersion>4.0.0</modelVersion>' + parent)
        self.check(self.write(f'{CORE}-{VERSION}.pom', inherited))

    def test_project_manifest_license_metadata_cannot_contradict_canonical_notice(self):
        for classifier in ('', '-sources', '-javadoc'):
            for old, new in ((b'License: MIT License', b'License: Proprietary'),
                             (b'License-URL: https://opensource.org/licenses/MIT',
                              b'License-URL: https://example.org/restrictive-license')):
                with self.subTest(classifier=classifier, new=new):
                    entries = runtime_entries()
                    entries['META-INF/MANIFEST.MF'] = entries['META-INF/MANIFEST.MF'].replace(old, new)
                    self.check(self.write(f'{CORE}-{VERSION}{classifier}.jar', archive(entries)),
                               accepted=False, contains='must be')

    def test_preserved_shaded_dependency_notices_keep_foreign_licensing_text(self):
        foreign_notice = ('Copyright (c) Third-party Authors. Source-access terms. '
                          + ABOUT_DELIVERY).encode()
        for prefix in ('', 'BOOT-INF/classes/'):
            with self.subTest(prefix=prefix):
                entries = runtime_entries()
                dependency = prefix + 'META-INF/third-party/org.example.external-1.4.9-jar/'
                for name in ('LICENSE', 'META-INF/NOTICE.txt', 'licenses/COPYING.md',
                             'COPYRIGHT.txt', 'license.txt', 'META-INF/LICENSE-FAQ.md'):
                    entries[dependency + name] = foreign_notice
                self.check(self.jar(entries))

    def test_preserved_dependency_notices_do_not_relax_own_canonical_notices(self):
        for name in ('LICENSE', 'NOTICE'):
            with self.subTest(name=name):
                entries = runtime_entries()
                entries['META-INF/third-party/org.example.external-1.4.9-jar/NOTICE'] = ABOUT_DELIVERY.encode()
                entries['META-INF/' + name] += b'Altered project notice.'
                self.check(self.jar(entries), accepted=False,
                           contains=f'does not exactly match repository {name}')

    def test_foreign_notice_exemption_does_not_skip_other_meta_inf_content(self):
        for name in ('META-INF/README.md', 'META-INF/third-party/LICENSE',
                     'META-INF/third-party/org.example.external-1.4.9-jar/README.md'):
            with self.subTest(name=name):
                entries = runtime_entries()
                entries[name] = ABOUT_DELIVERY.encode()
                self.check(self.jar(entries), accepted=False, contains='obsolete delivery wording')

    def test_nested_project_version_cannot_hide_behind_current_boot_version(self):
        entries = runtime_entries(BOOT)
        entries[f'BOOT-INF/lib/{CORE}-{OLD_VERSION}.jar'] = archive(runtime_entries(version=OLD_VERSION))
        self.check(self.jar(entries, artifact=BOOT), accepted=False, contains='JAR filename version')

    def test_obsolete_documents_are_rejected(self):
        for name in ('META-INF/BINARY-LICENSE.md', 'docs/SLA.md', 'docs/PRICING.md'):
            with self.subTest(name=name):
                entries = runtime_entries()
                entries[name] = b'Obsolete document.'
                self.check(self.jar(entries), accepted=False, contains='obsolete commercial')

    def test_obsolete_starter_metadata_and_native_library_are_rejected(self):
        prefix = 'META-INF/native-image/com.arbergashi.charts/spring-boot-starter/'
        for name in (prefix + 'jni-config.json', prefix + 'resource-config.json',
                     'native/linux/x86_64/libarbercharts-core.so'):
            with self.subTest(name=name):
                entries = runtime_entries(STARTER)
                entries[name] = b'[]'
                self.check(self.jar(entries, artifact=STARTER), accepted=False, contains='obsolete')

    def test_corrupt_archives_are_rejected_without_tracebacks(self):
        self.check(self.write(f'{CORE}-{VERSION}.jar', b'not a ZIP archive'),
                   accepted=False, contains='cannot validate archive')
        entries = runtime_entries(BOOT)
        entries[f'BOOT-INF/lib/{CORE}-{VERSION}.jar'] = b'broken nested JAR'
        self.check(self.jar(entries, artifact=BOOT), accepted=False, contains='cannot validate archive')

    def test_archive_crc_corruption_is_rejected(self):
        data = archive(runtime_entries())
        # ZIP_STORED keeps these bytes literal; replacing a byte leaves the CRC stale.
        corrupted = data.replace(LICENSE, b'X' + LICENSE[1:], 1)
        self.assertNotEqual(data, corrupted)
        self.check(self.write(f'{CORE}-{VERSION}.jar', corrupted), accepted=False, contains='CRC')

    def test_sources_and_javadoc_require_canonical_notices_and_preserve_javadoc_legal_files(self):
        entries = runtime_entries()
        del entries['com/arbergashi/charts/demo/DemoApplication.class']
        entries['com/arbergashi/charts/Example.java'] = b'/** @since 1.7.0 */ class Example {}'
        self.check(self.write(f'{CORE}-{VERSION}-sources.jar', archive(entries)))
        javadoc = {'META-INF/MANIFEST.MF': b'Manifest-Version: 1.0\n\n',
                   'META-INF/LICENSE': LICENSE, 'META-INF/NOTICE': NOTICE,
                   'index.html': b'<html lang="en"><title>API</title></html>',
                   'legal/LICENSE': b'JDK and JavaScript licenses differ from the project license.'}
        self.check(self.write(f'{CORE}-{VERSION}-javadoc.jar', archive(javadoc)))
        for classifier, contents in (('sources', entries), ('javadoc', javadoc)):
            for name in ('LICENSE', 'NOTICE'):
                for mutation in ('missing', 'changed'):
                    with self.subTest(classifier=classifier, name=name, mutation=mutation):
                        changed = dict(contents)
                        if mutation == 'missing':
                            del changed['META-INF/' + name]
                        else:
                            changed['META-INF/' + name] = b'All rights reserved. Redistribution is forbidden.'
                        self.check(self.write(f'{CORE}-{VERSION}-{classifier}.jar', archive(changed)),
                                   accepted=False, contains=name)
        javadoc['index.html'] = b'<html lang="de"><title>API</title></html>'
        self.check(self.write(f'{CORE}-{VERSION}-javadoc.jar', archive(javadoc)),
                   accepted=False, contains='not marked English')

    def test_present_javadoc_locale_option_must_be_english(self):
        self.check(self.write('options', b'-locale\nen\n'))
        self.check(self.write('options', b'-locale\nde\n'), accepted=False, contains='not English')

    def test_central_repository_directory_version_is_checked(self):
        current = f'com/arbergashi/{CORE}/{VERSION}/{CORE}-{VERSION}.jar'
        entries = central_entries()
        self.check(self.write('central-bundle.zip', archive(entries)))
        stale = f'com/arbergashi/{CORE}/{OLD_VERSION}/{CORE}-{VERSION}.jar'
        entries[stale] = entries.pop(current)
        self.check(self.write('central-bundle.zip', archive(entries)),
                   accepted=False, contains='Maven repository directory version')

    def test_central_requires_exact_parent_and_four_library_inventory(self):
        complete = central_entries()
        self.assertEqual(len(complete), 102)
        self.check(self.write('central-bundle.zip', archive(complete)))
        # Each release file is mandatory, including parent metadata and every
        # classifier, signature and checksum; one intact library is not enough.
        core = f'com/arbergashi/{CORE}/{VERSION}/{CORE}-{VERSION}'
        parent = f'com/arbergashi/{PARENT}/{VERSION}/{PARENT}-{VERSION}'
        required = [parent + '.pom', *(core + suffix for suffix in
                    ('.pom', '.jar', '-sources.jar', '-javadoc.jar', '.jar.asc',
                     '.jar.md5', '.jar.sha1', '.jar.sha256', '.jar.sha512'))]
        for missing in required:
            with self.subTest(missing=missing):
                entries = dict(complete)
                del entries[missing]
                self.check(self.write('central-bundle.zip', archive(entries)),
                           accepted=False, contains='missing required Central file ' + missing)
        for artifact in (SWING, BOOT, 'arbercharts-starter'):
            with self.subTest(unpublishable=artifact):
                entries = dict(complete)
                entries[f'com/arbergashi/{artifact}/{VERSION}/{artifact}-{VERSION}.pom'] = pom(artifact)
                self.check(self.write('central-bundle.zip', archive(entries)),
                           accepted=False, contains='unexpected file or module in Central bundle')
        self.check(self.write('central-bundle.zip', archive({core + '.jar': complete[core + '.jar']})),
                   accepted=False, contains='missing required Central file')

    def test_central_checksums_cover_actual_payload_bytes_and_signatures_are_present(self):
        complete = central_entries()
        base = f'com/arbergashi/{CORE}/{VERSION}/{CORE}-{VERSION}.jar'
        for algorithm in ('md5', 'sha1', 'sha256', 'sha512'):
            with self.subTest(algorithm=algorithm):
                entries = dict(complete)
                entries[base + '.' + algorithm] = b'0' * len(entries[base + '.' + algorithm])
                self.check(self.write('central-bundle.zip', archive(entries)),
                           accepted=False, contains=algorithm + ' checksum does not match payload')
        for contents in (b'', b'not a signature',
                         b'-----BEGIN PGP SIGNATURE-----\n\n-----END PGP SIGNATURE-----'):
            with self.subTest(signature=contents):
                entries = dict(complete)
                entries[base + '.asc'] = contents
                self.check(self.write('central-bundle.zip', archive(entries)),
                           accepted=False, contains='missing armored OpenPGP signature content')

    def test_github_libraries_require_complete_signed_repository_inventory(self):
        root = f'industrie-charts-libraries-{VERSION}/'
        complete = {root + 'repository/' + name: data for name, data in central_entries().items()}
        complete.update({root + 'README.txt': b'Current signed library artifacts.\n',
                         root + 'LICENSE': LICENSE, root + 'NOTICE': NOTICE})
        name = f'industrie-charts-libraries-{VERSION}.zip'
        self.assertEqual(len(complete), 105)
        self.check(self.write(name, archive(complete)))
        for artifact in LIBRARIES:
            for classifier in ('sources', 'javadoc'):
                missing = (root + f'repository/com/arbergashi/{artifact}/{VERSION}/'
                           f'{artifact}-{VERSION}-{classifier}.jar')
                with self.subTest(missing=missing):
                    entries = dict(complete)
                    del entries[missing]
                    self.check(self.write(name, archive(entries)), accepted=False,
                               contains='missing required Central file ' + missing)
        for extra in (root + f'repository/com/arbergashi/{SWING}/{VERSION}/{SWING}-{VERSION}.pom',
                      root + 'unreviewed.txt'):
            with self.subTest(extra=extra):
                entries = dict(complete)
                entries[extra] = pom(SWING) if extra.endswith('.pom') else b'Unexpected sidecar.'
                self.check(self.write(name, archive(entries)), accepted=False,
                           contains='unexpected file or module in Central bundle')

    def test_pom_identity_must_match_repository_filename(self):
        wrong = pom(STARTER)
        self.check(self.write(f'{CORE}-{VERSION}.pom', wrong),
                   accepted=False, contains='does not match')
        entries = central_entries()
        path = f'com/arbergashi/{CORE}/{VERSION}/{CORE}-{VERSION}.pom'
        entries[path] = wrong
        # Recompute checksums so a metadata identity error must be detected even
        # when every archive checksum is internally consistent.
        for algorithm in ('md5', 'sha1', 'sha256', 'sha512'):
            entries[path + '.' + algorithm] = hashlib.new(algorithm, wrong, usedforsecurity=False).hexdigest().encode()
        self.check(self.write('central-bundle.zip', archive(entries)),
                   accepted=False, contains=f"artifactId '{STARTER}' does not match '{CORE}'")

    def test_demo_launchers_select_the_present_current_jar_in_zip_and_directory(self):
        for name in ('run-demo.sh', 'run-demo.command', 'run-demo.bat'):
            with self.subTest(name=name):
                entries = {'LICENSE': LICENSE, 'NOTICE': NOTICE,
                           f'{SWING}-{VERSION}.jar': archive(runtime_entries(SWING)),
                           name: launcher(name)}
                self.check(self.distribution(entries))
                directory = 'launchers/' + name + '/'
                self.jar(artifact=SWING, directory=directory)
                script = self.write(directory + name, launcher(name))
                self.check(script.parent)
                entries[name] = launcher(name, OLD_VERSION)
                self.check(self.distribution(entries), accepted=False, contains='launcher JAR assignment')
                script.write_bytes(entries[name])
                self.check(script.parent, accepted=False, contains='launcher JAR assignment')

    def test_demo_launcher_cannot_claim_a_current_jar_in_comments_or_a_missing_file(self):
        for name in ('run-demo.sh', 'run-demo.command', 'run-demo.bat'):
            with self.subTest(name=name):
                entries = {'LICENSE': LICENSE, 'NOTICE': NOTICE,
                           f'{SWING}-{VERSION}.jar': archive(runtime_entries(SWING))}
                comment = 'rem ' if name.endswith('.bat') else '# '
                entries[name] = (comment + f'java -jar {SWING}-{VERSION}.jar\n'
                                 f'java -jar {SWING}-{OLD_VERSION}.jar\n').encode()
                self.check(self.distribution(entries), accepted=False, contains='launcher java -jar target')
                entries[name] = launcher(name)
                del entries[f'{SWING}-{VERSION}.jar']
                entries[f'{CORE}-{VERSION}.jar'] = archive(runtime_entries())
                self.check(self.distribution(entries), accepted=False, contains='requires neighboring')

    def test_demo_launcher_rejects_unresolved_tokens_and_wrong_shell_variables(self):
        entries = {'LICENSE': LICENSE, 'NOTICE': NOTICE,
                   f'{SWING}-{VERSION}.jar': archive(runtime_entries(SWING))}
        for token in (b'@DEMO_JAR@', b'@PROJECT_VERSION@'):
            with self.subTest(token=token):
                entries['run-demo.sh'] = launcher('run-demo.sh').replace(f'{SWING}-{VERSION}.jar'.encode(), token)
                self.check(self.distribution(entries), accepted=False, contains='unresolved launcher build token')
        del entries['run-demo.sh']
        entries['run-demo.bat'] = launcher('run-demo.bat').replace(b'%JAR%', b'${JAR}')
        self.check(self.distribution(entries), accepted=False, contains='launcher java -jar target')

    def test_distribution_cannot_contain_only_third_party_artifacts(self):
        self.check(self.distribution({'LICENSE': LICENSE, 'NOTICE': NOTICE,
                                      'lib/external-1.4.9.jar': foreign_jar()}),
                   accepted=False, contains='no project JARs or POMs')

    def test_directory_inventory_rejects_loose_old_documents(self):
        path = self.jar()
        self.write('SLA.md', b'Old support agreement.')
        self.check(path.parent, accepted=False, contains='obsolete commercial')


class RepositoryWordingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        spec = importlib.util.spec_from_file_location('charts_repository_check_test', TOOLS / 'check_repository.py')
        cls.checker = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.checker)

    def run_repository_check(self, source_text):
        with tempfile.TemporaryDirectory(prefix='charts-source-check-') as temporary:
            root = Path(temporary)
            source = 'arbercharts-core/src/main/java/com/arbergashi/charts/Example.java'
            files = {
                'pom.xml': pom('arber-charts-parent'),
                'arbercharts-demo/src/main/resources/data/renderer-catalog.txt': b'standard|com.arbergashi.charts.Example\n',
                'docs/RENDERER_CATALOG.md': f'# Renderer Catalog ({VERSION})\n\n[Example](../{source})\n'.encode(),
                source: ('class Example { String about = "' + source_text + '"; }').encode(),
            }
            for name, content in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(content)
            stdout, stderr = io.StringIO(), io.StringIO()
            # Only Git inventory and the unrelated grid subprocess are isolated.
            # The source reading and rejection path run through the real main().
            with (mock.patch.object(self.checker, 'ROOT', root),
                  mock.patch.object(self.checker.subprocess, 'check_output', return_value='\0'.join(files).encode()),
                  mock.patch.object(self.checker.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, '', '')),
                  redirect_stdout(stdout), redirect_stderr(stderr)):
                result = self.checker.main()
            return result, stdout.getvalue() + stderr.getvalue()

    def test_known_about_claim_is_rejected_in_source(self):
        result, output = self.run_repository_check(ABOUT_DELIVERY)
        self.assertEqual(result, 1, output)
        self.assertIn('Example.java: stale private-delivery or proprietary wording', output)

    def test_general_support_and_commercial_use_are_allowed(self):
        for text in ('Technical support', 'MIT permits commercial use.',
                     'May be integrated into closed-source products.'):
            with self.subTest(text=text):
                result, output = self.run_repository_check(text)
                self.assertEqual(result, 0, output)


if __name__ == '__main__':
    unittest.main()
