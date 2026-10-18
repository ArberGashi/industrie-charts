#!/usr/bin/env python3
"""Validate Charts JARs, Maven POMs, Central bundles and GitHub distributions.

Usage: python3 tools/check_release_artifacts.py --version 2.5.0 FILE_OR_DIRECTORY [...]
Only project artifacts use the repository's canonical LICENSE and NOTICE. Bundled
third-party JARs and preserved dependency notices retain their own licensing text.
Central bundles must contain exactly the parent POM and four library modules,
including binary/source/Javadoc JARs, armored signatures and all four checksums.
Signature presence is checked, not cryptographic validity. This does not verify
remote publication status or runtime behavior.
"""

import argparse
from collections import Counter
import hashlib
import io
from pathlib import Path, PurePosixPath
import re
import shlex
import sys
import xml.etree.ElementTree as ET
import zipfile
import zlib


ROOT = Path(__file__).resolve().parents[1]
GROUP = 'com.arbergashi'
PARENT = 'arber-charts-parent'
CENTRAL_LIBRARIES = ('arbercharts-core', 'arbercharts-server-bridge',
                     'arbercharts-spring-boot-starter', 'arbercharts-swing-bridge')
CHECKSUMS = ('md5', 'sha1', 'sha256', 'sha512')
MIT_NAME = 'MIT License'
MIT_URL = 'https://opensource.org/licenses/MIT'
LAUNCHERS = {'run-demo.sh', 'run-demo.command', 'run-demo.bat'}
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
OWN_METADATA = re.compile(r'^(?:BOOT-INF/classes/)?META-INF/maven/com\.arbergashi/([^/]+)/(pom\.xml|pom\.properties)$')
# Matches only notice files preserved by preserve-bundled-dependency-notices,
# under a dependency-specific directory; other META-INF content stays checked.
THIRD_PARTY_NOTICE = re.compile(
    r'^(?:BOOT-INF/classes/)?META-INF/third-party/[^/]+/(?:[^/]+/)*'
    r'(?i:LICENSE|NOTICE|COPYING|COPYRIGHT)[^/]*$')
FORBIDDEN_DOCUMENT = re.compile(r'(?:BINARY-LICENSE|LICENSE-FAQ|LICENSING|SLA(?:[_-][A-Z0-9]+)*|PRICING)(?:\.(?:md|txt|html|pdf))?$', re.I)
# Keep these specific to retired project claims, not legitimate third-party terms
# such as permission for commercial use or inclusion in a closed-source product.
STALE_TEXT = re.compile(
    rb'binary[ -]first|source[ -]access|proprietary and confidential|paid support subscription'
    rb'|commercial\s+support,\s*closed-source\s+product\s+delivery', re.I)
TEXT_SUFFIXES = {'.md', '.txt', '.html', '.java', '.xml', '.pom', '.properties', '.json', '.yml', '.yaml', '.sh', '.bat', '.command'}
OPTION_FILES = {'options', 'javadoc-options-javadoc-resources.xml'}
MAX_REPORTED_ERRORS = 30
MAX_ARCHIVE_BYTES = 512 * 1024 * 1024


def project_artifacts():
    parent = ET.parse(ROOT / 'pom.xml').getroot()
    artifacts = {parent.findtext('m:artifactId', namespaces=NS)}
    for module in parent.findall('m:modules/m:module', NS):
        pom = ET.parse(ROOT / module.text / 'pom.xml').getroot()
        artifacts.add(pom.findtext('m:artifactId', namespaces=NS))
    if None in artifacts:
        raise ValueError('Project POMs must declare artifactId')
    return artifacts


class Validator:
    def __init__(self, version):
        self.version = version
        self.artifacts = project_artifacts()
        self.notices = {name: (ROOT / name).read_bytes() for name in ('LICENSE', 'NOTICE')}
        self.counts = Counter()
        self.errors = []
        self.error_count = 0

    def error(self, label, message):
        self.error_count += 1
        if len(self.errors) < MAX_REPORTED_ERRORS:
            detail = f'{label}: {message}'
            self.errors.append(detail if len(detail) <= 900 else detail[:897] + '...')

    def filename_identity(self, name):
        name = PurePosixPath(name).name.removeprefix('original-')
        for artifact in sorted(self.artifacts, key=len, reverse=True):
            if not name.startswith(artifact + '-'):
                continue
            suffix = PurePosixPath(name).suffix
            remainder = name[len(artifact) + 1:-len(suffix)]
            classifier = ''
            for candidate in ('sources', 'javadoc'):
                if remainder.endswith('-' + candidate):
                    remainder = remainder[:-len(candidate) - 1]
                    classifier = candidate
                    break
            return artifact, remainder, classifier
        return None

    def check_version(self, value, label, description, allow_property=False):
        allowed = {self.version}
        if allow_property:
            allowed.update({'${project.version}', '${pom.version}', '${project.parent.version}'})
        if value not in allowed:
            self.error(label, f'{description} is {value!r}, expected {self.version}')

    def check_repository_path(self, name, label):
        match = re.search(r'(?:^|/)com/arbergashi/([^/]+)/([^/]+)/([^/]+)$', name)
        if match and match.group(1) in self.artifacts:
            artifact, version, filename = match.groups()
            self.check_version(version, label, 'Maven repository directory version')
            if not filename.startswith(artifact + '-' + self.version + '.') and not filename.startswith(artifact + '-' + self.version + '-'):
                self.error(label, 'filename does not match its Maven repository coordinates')

    def check_text(self, name, data, label):
        path = PurePosixPath(name)
        third_party_notice = THIRD_PARTY_NOTICE.fullmatch(name) is not None
        if not third_party_notice and FORBIDDEN_DOCUMENT.fullmatch(path.name):
            self.error(label, 'obsolete commercial or duplicate licensing document')
        own_class = name.removeprefix('BOOT-INF/classes/').startswith('com/arbergashi/charts/') and path.suffix == '.class'
        if not third_party_notice and (path.suffix.lower() in TEXT_SUFFIXES or path.name in self.notices or own_class):
            match = STALE_TEXT.search(data)
            if match:
                self.error(label, f'obsolete delivery wording: {match.group().decode("ascii")}')
        if 'native' in path.parts and path.suffix in {'.so', '.dylib', '.dll'} and re.match(r'(?:lib)?arbercharts', path.name):
            self.error(label, 'obsolete bundled native starter library')
        native_prefix = 'META-INF/native-image/com.arbergashi.charts/spring-boot-starter/'
        if name.removeprefix('BOOT-INF/classes/') in {
            native_prefix + 'jni-config.json', native_prefix + 'resource-config.json'
        }:
            self.error(label, 'obsolete Spring Boot starter JNI/native resource metadata')

    def check_pom(self, data, label, expected_artifact=None):
        try:
            pom = ET.fromstring(data)
        except ET.ParseError as error:
            self.error(label, f'invalid POM XML: {error}')
            return
        group = pom.findtext('m:groupId', namespaces=NS) or pom.findtext('m:parent/m:groupId', namespaces=NS)
        artifact = pom.findtext('m:artifactId', namespaces=NS)
        if group != GROUP or artifact not in self.artifacts:
            self.error(label, f'expected project POM, found {group}:{artifact}')
            return
        self.counts['POMs'] += 1
        if expected_artifact is not None and artifact != expected_artifact:
            self.error(label, f'artifactId {artifact!r} does not match {expected_artifact!r}')
        version = pom.findtext('m:version', namespaces=NS) or pom.findtext('m:parent/m:version', namespaces=NS)
        self.check_version(version, label, 'project version')
        if pom.findtext('m:parent/m:groupId', namespaces=NS) == GROUP:
            self.check_version(pom.findtext('m:parent/m:version', namespaces=NS), label, 'parent version')
        licenses = pom.findall('m:licenses/m:license', NS)
        inherits_license = (pom.findtext('m:parent/m:groupId', namespaces=NS) == GROUP
                            and pom.findtext('m:parent/m:artifactId', namespaces=NS) == PARENT)
        if not licenses and not inherits_license:
            self.error(label, 'project POM must declare or inherit the canonical MIT license')
        for license in licenses:
            if (license.findtext('m:name', namespaces=NS) != MIT_NAME
                    or license.findtext('m:url', namespaces=NS) != MIT_URL):
                self.error(label, 'project POM license must match the canonical MIT name and URL')
        for dependency in pom.findall('.//m:dependency', NS):
            if dependency.findtext('m:groupId', namespaces=NS) == GROUP:
                dependency_id = dependency.findtext('m:artifactId', namespaces=NS)
                if dependency_id in self.artifacts:
                    value = dependency.findtext('m:version', namespaces=NS)
                    if value is not None:
                        self.check_version(value, label, f'{dependency_id} dependency version', allow_property=True)
        for plugin in pom.findall('.//m:plugin', NS):
            if plugin.findtext('m:artifactId', namespaces=NS) == 'maven-javadoc-plugin':
                for locale in plugin.findall('.//m:locale', NS):
                    if not re.fullmatch(r'en(?:[_-][A-Za-z0-9]+)*', locale.text or ''):
                        self.error(label, f'Javadoc plugin locale {locale.text!r} is not English')
        self.check_text('pom.xml', data, label)

    def check_properties(self, data, label, expected_artifact):
        values = {}
        for line in data.decode('utf-8').splitlines():
            if '=' in line and not line.lstrip().startswith(('#', '!')):
                key, value = line.split('=', 1)
                values[key.strip()] = value.strip()
        if values.get('groupId') != GROUP or values.get('artifactId') != expected_artifact:
            self.error(label, 'Maven properties do not match the project artifact path')
        self.check_version(values.get('version'), label, 'Maven properties version')

    def check_options(self, data, label):
        self.counts['Javadoc option files'] += 1
        text = data.decode('utf-8')
        if label.endswith('.xml'):
            try:
                root = ET.fromstring(text)
            except ET.ParseError as error:
                self.error(label, f'invalid Javadoc options XML: {error}')
                return
            locales = [element.text or '' for element in root.iter()
                       if element.tag.rsplit('}', 1)[-1] == 'locale']
        else:
            tokens = shlex.split(text)
            locales = []
            for index, token in enumerate(tokens):
                if token == '-locale':
                    locales.append(tokens[index + 1] if index + 1 < len(tokens) else '')
        for locale in locales:
            if not re.fullmatch(r'en(?:[_-][A-Za-z0-9]+)*', locale):
                self.error(label, f'Javadoc locale {locale!r} is not English')

    def check_notices(self, archive, prefix, label):
        for name, expected in self.notices.items():
            member = prefix + name
            if member not in archive.namelist():
                self.error(label, f'missing canonical {member}')
            elif archive.read(member) != expected:
                self.error(f'{label}!/{member}', f'does not exactly match repository {name}')

    def check_manifest(self, data, label, runtime):
        text = data.decode('utf-8').replace('\r\n', '\n')
        main = text.split('\n\n', 1)[0].replace('\n ', '')
        values = dict(line.split(': ', 1) for line in main.splitlines() if ': ' in line)
        value = values.get('Implementation-Version')
        if runtime or value is not None:
            self.check_version(value, label, 'Implementation-Version')
        for key, expected in (('License', MIT_NAME), ('License-URL', MIT_URL)):
            if (runtime or key in values) and values.get(key) != expected:
                self.error(label, f'{key} must be {expected!r}')

    def check_archive(self, data, name, label, depth=0, nested=False, structural_only=False):
        if depth > 4 or len(data) > MAX_ARCHIVE_BYTES:
            self.error(label, 'archive exceeds validation depth or size limit')
            return
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                members = [info for info in archive.infolist() if not info.is_dir()]
                names = [info.filename for info in members]
                if not names:
                    self.error(label, 'empty archive')
                    return
                if len(names) != len(set(names)):
                    self.error(label, 'duplicate archive member names')
                if any(n.startswith('/') or '\\' in n or '..' in n.split('/') for n in names):
                    self.error(label, 'unsafe or ambiguous archive member path')
                if any(info.file_size > MAX_ARCHIVE_BYTES for info in members) or sum(info.file_size for info in members) > 4 * MAX_ARCHIVE_BYTES:
                    self.error(label, 'expanded archive exceeds validation size limit')
                    return
                invalid = archive.testzip()
                if invalid:
                    self.error(label, f'CRC mismatch in {invalid}')
                    return
                if structural_only:
                    self.check_foreign_contents(archive, names, label, depth)
                    return
                if name.lower().endswith('.jar'):
                    identity = self.filename_identity(name)
                    own_metadata = [(n, OWN_METADATA.fullmatch(n)) for n in names if OWN_METADATA.fullmatch(n)]
                    if identity is None and not own_metadata:
                        if nested:
                            self.counts['third-party JARs checked'] += 1
                            self.check_foreign_contents(archive, names, label, depth)
                        else:
                            self.error(label, 'not a recognized project JAR')
                        return
                    self.check_jar(archive, names, own_metadata, identity, label, depth)
                else:
                    self.check_zip(archive, names, name, label, depth)
        except (zipfile.BadZipFile, OSError, ValueError, RuntimeError, UnicodeError, zlib.error) as error:
            self.error(label, f'cannot validate archive: {error}')

    def check_foreign_contents(self, archive, names, label, depth):
        # Dependency text keeps its own licensing; every nested archive still
        # receives the same structural, size, path and CRC checks.
        for member in names:
            if member.lower().endswith(('.jar', '.zip')):
                self.check_archive(archive.read(member), member, label + '!/' + member,
                                   depth + 1, nested=True, structural_only=True)

    def check_central_inventory(self, archive, names, label, prefix='', allowed_sidecars=()):
        payloads = set()
        for artifact in (PARENT, *CENTRAL_LIBRARIES):
            base = f'{prefix}com/arbergashi/{artifact}/{self.version}/{artifact}-{self.version}'
            payloads.add(base + '.pom')
            if artifact != PARENT:
                payloads.update(base + suffix for suffix in ('.jar', '-sources.jar', '-javadoc.jar'))
        expected = {name + suffix for name in payloads
                    for suffix in ('', '.asc', *(f'.{algorithm}' for algorithm in CHECKSUMS))}
        present = set(names)
        for missing in sorted(expected - present):
            self.error(label, f'missing required Central file {missing}')
        for extra in sorted(present - expected - set(allowed_sidecars)):
            self.error(label + '!/' + extra, 'unexpected file or module in Central bundle')
        for member in sorted(payloads & present):
            data = archive.read(member)
            for algorithm in CHECKSUMS:
                checksum = member + '.' + algorithm
                if checksum in present:
                    actual = archive.read(checksum).strip().lower()
                    expected_hash = hashlib.new(algorithm, data, usedforsecurity=False).hexdigest().encode('ascii')
                    if actual != expected_hash:
                        self.error(label + '!/' + checksum, f'{algorithm} checksum does not match payload')
            signature = member + '.asc'
            if signature in present:
                content = archive.read(signature).strip()
                begin = b'-----BEGIN PGP SIGNATURE-----'
                end = b'-----END PGP SIGNATURE-----'
                if (not content.startswith(begin) or not content.endswith(end)
                        or not content[len(begin):-len(end)].strip()):
                    self.error(label + '!/' + signature, 'missing armored OpenPGP signature content')

    def check_launcher(self, name, data, available_names, label):
        if PurePosixPath(name).name not in LAUNCHERS:
            return
        expected = f'arbercharts-demo-{self.version}.jar'
        neighbor = str(PurePosixPath(name).with_name(expected))
        if neighbor not in available_names:
            self.error(label, f'launcher requires neighboring {expected}')
        text = data.decode('utf-8')
        if '@DEMO_JAR@' in text or '@PROJECT_VERSION@' in text:
            self.error(label, 'unresolved launcher build token')
        windows = name.endswith('.bat')
        assignments = []
        arguments = []
        for line in text.splitlines():
            line = line.strip()
            if not line or line.startswith('#') or (windows and re.match(r'(?i)rem(?:\s|$)', line)):
                continue
            if windows:
                assignment = re.fullmatch(r'(?i)set\s+"?JAR=(.*?)"?', line)
                if assignment:
                    assignments.append(assignment.group(1))
            elif line.startswith('JAR='):
                values = shlex.split(line[4:], comments=True)
                assignments.extend(values if len(values) == 1 else ['<ambiguous assignment>'])
            tokens = shlex.split(line, comments=not windows)
            if tokens and tokens[0].lstrip('@') == 'exec':
                tokens = tokens[1:]
            if tokens and tokens[0].lstrip('@') == 'java' and '-jar' in tokens:
                index = tokens.index('-jar') + 1
                arguments.append(tokens[index] if index < len(tokens) else '')
        variables = {'%JAR%'} if windows else {'$JAR', '${JAR}'}
        allowed_paths = {expected, '%DIR%' + expected} if windows else {
            expected, '$DIR/' + expected, '${DIR}/' + expected}
        if len(arguments) != 1:
            self.error(label, 'launcher must contain one explicit java -jar command')
        elif arguments[0] in variables:
            if len(assignments) != 1 or assignments[0] not in allowed_paths:
                self.error(label, f'launcher JAR assignment must select {expected}')
        elif arguments[0] not in allowed_paths:
            self.error(label, f'launcher java -jar target must select {expected}')

    def check_jar(self, archive, names, metadata, identity, label, depth):
        if identity is None:
            self.error(label, 'project Maven metadata is present under an unrecognized JAR filename')
            classifier = ''
        else:
            artifact, version, classifier = identity
            self.check_version(version, label, 'JAR filename version')
        self.counts[(classifier or 'runtime') + ' JARs'] += 1
        runtime = not classifier
        self.check_notices(archive, 'META-INF/', label)
        manifest = 'META-INF/MANIFEST.MF'
        if manifest not in names:
            self.error(label, 'missing JAR manifest')
        else:
            self.check_manifest(archive.read(manifest), label + '!/' + manifest, runtime)
        if identity and classifier != 'javadoc':
            expected = f'META-INF/maven/{GROUP}/{artifact}/'
            for suffix in ('pom.xml', 'pom.properties'):
                if expected + suffix not in names:
                    self.error(label, f'missing own Maven metadata {expected + suffix}')
        for member, match in metadata:
            artifact_id, kind = match.groups()
            if artifact_id not in self.artifacts:
                self.error(label + '!/' + member, 'unknown project artifactId')
            elif kind == 'pom.xml':
                self.check_pom(archive.read(member), label + '!/' + member, artifact_id)
            else:
                self.check_properties(archive.read(member), label + '!/' + member, artifact_id)
        for member in names:
            member_label = label + '!/' + member
            if member.endswith('.jar'):
                self.check_archive(archive.read(member), member, member_label, depth + 1, nested=True)
                continue
            suffix = PurePosixPath(member).suffix.lower()
            own_class = member.removeprefix('BOOT-INF/classes/').startswith('com/arbergashi/charts/') and suffix == '.class'
            if suffix in TEXT_SUFFIXES or PurePosixPath(member).name in self.notices or own_class:
                content = archive.read(member)
                self.check_text(member, content, member_label)
                if classifier == 'javadoc' and suffix == '.html':
                    languages = re.findall(rb'<html\b[^>]*\blang=["\']([^"\']+)', content, re.I)
                    if any(not re.fullmatch(rb'en(?:-[A-Za-z0-9]+)*', value, re.I) for value in languages):
                        self.error(member_label, 'generated Javadoc HTML is not marked English')
            else:
                self.check_text(member, b'', member_label)
            if PurePosixPath(member).name in OPTION_FILES:
                self.check_options(archive.read(member), member_label)

    def check_zip(self, archive, names, name, label, depth):
        self.counts['ZIP bundles'] += 1
        basename = PurePosixPath(name).name
        if basename.startswith('industrie-charts-') and not basename.endswith('-' + self.version + '.zip'):
            self.error(label, 'distribution ZIP filename does not match the intended version')
        central = (basename == 'central-bundle.zip'
                   or any(re.fullmatch(r'com/arbergashi/[^/]+/[^/]+/[^/]+', n) for n in names))
        if central:
            self.check_central_inventory(archive, names, label)
        else:
            top = {name.split('/', 1)[0] for name in names}
            prefix = next(iter(top)) + '/' if len(top) == 1 and all('/' in n for n in names) else ''
            if prefix.startswith('industrie-charts-') and not prefix.endswith('-' + self.version + '/'):
                self.error(label, f'distribution root {prefix!r} does not match {self.version}')
            self.check_notices(archive, prefix, label)
            if basename.startswith('industrie-charts-libraries-'):
                root = f'industrie-charts-libraries-{self.version}/'
                self.check_central_inventory(
                    archive, names, label, root + 'repository/',
                    {root + sidecar for sidecar in ('README.txt', 'LICENSE', 'NOTICE')})
        project_counts = ('POMs', 'runtime JARs', 'sources JARs', 'javadoc JARs')
        previous_count = sum(self.counts[key] for key in project_counts)
        for member in names:
            member_label = label + '!/' + member
            self.check_repository_path(member, member_label)
            if member.endswith(('.jar', '.zip')):
                self.check_archive(archive.read(member), member, member_label, depth + 1, nested=True)
            elif member.endswith('.pom') or PurePosixPath(member).name == 'pom.xml':
                identity = self.filename_identity(member)
                self.check_pom(archive.read(member), member_label, identity[0] if identity else None)
                if identity:
                    self.check_version(identity[1], member_label, 'POM filename version')
            else:
                path = PurePosixPath(member)
                content = archive.read(member) if path.suffix in TEXT_SUFFIXES or path.name in self.notices else b''
                self.check_text(member, content, member_label)
                self.check_launcher(member, content, names, member_label)
        if sum(self.counts[key] for key in project_counts) == previous_count:
            self.error(label, 'bundle contains no project JARs or POMs')

    def check_file(self, path):
        label = str(path)
        try:
            if path.stat().st_size > MAX_ARCHIVE_BYTES:
                self.error(label, 'file exceeds validation size limit')
                return
            data = path.read_bytes()
            self.check_repository_path(path.as_posix(), label)
            if path.suffix.lower() in {'.jar', '.zip'}:
                self.check_archive(data, path.name, label)
            elif path.suffix == '.pom' or path.name == 'pom.xml':
                identity = self.filename_identity(path.name)
                self.check_pom(data, label, identity[0] if identity else None)
                if identity:
                    self.check_version(identity[1], label, 'POM filename version')
            elif path.name in OPTION_FILES:
                self.check_options(data, label)
            elif path.name in self.notices:
                self.counts['notice files'] += 1
                if data != self.notices[path.name]:
                    self.error(label, f'does not exactly match repository {path.name}')
                self.check_text(path.name, data, label)
            elif path.suffix.lower() in TEXT_SUFFIXES or FORBIDDEN_DOCUMENT.fullmatch(path.name) or path.suffix in {'.so', '.dylib', '.dll'}:
                self.counts['sidecar documents/resources'] += 1
                self.check_text(path.as_posix(), data, label)
                if path.name in LAUNCHERS:
                    neighbors = {entry.name for entry in path.parent.iterdir() if entry.is_file()}
                    self.check_launcher(path.name, data, neighbors, label)
            else:
                self.error(label, 'expected a project JAR, POM, ZIP or Javadoc options file')
        except (OSError, ValueError, UnicodeError) as error:
            self.error(label, f'cannot read artifact: {error}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True, help='exact intended Charts release version')
    parser.add_argument('paths', nargs='+', type=Path, metavar='FILE_OR_DIRECTORY')
    args = parser.parse_args()
    try:
        validator = Validator(args.version)
    except (OSError, ValueError, ET.ParseError) as error:
        parser.exit(2, f'Cannot load canonical project metadata: {error}\n')
    files = set()
    for path in args.paths:
        if path.is_dir():
            found = {p.resolve() for p in path.rglob('*') if p.is_file() and
                     (p.suffix.lower() in {'.jar', '.pom', '.zip', '.md', '.txt', '.so', '.dylib', '.dll', '.sh', '.bat', '.command'}
                      or p.name in OPTION_FILES | {'pom.xml'}
                      or FORBIDDEN_DOCUMENT.fullmatch(p.name))}
            # Top-level distribution notices are ours; generated Javadoc legal/
            # directories can contain unrelated JDK and JavaScript licenses.
            found.update((path / name).resolve() for name in validator.notices if (path / name).is_file())
            if not found:
                validator.error(str(path), 'no artifacts found')
            files.update(found)
        elif path.is_file():
            files.add(path.resolve())
        else:
            validator.error(str(path), 'input does not exist')
    for path in sorted(files):
        validator.check_file(path)
    summary = ', '.join(f'{count} {kind}' for kind, count in sorted(validator.counts.items())) or 'no artifacts'
    if validator.error_count:
        print(f'Release artifact checks failed for {args.version}: {validator.error_count} violation(s); {summary}.', file=sys.stderr)
        for error in validator.errors:
            print('- ' + error, file=sys.stderr)
        omitted = validator.error_count - len(validator.errors)
        if omitted:
            print(f'- {omitted} further violation(s) omitted.', file=sys.stderr)
        return 1
    print(f'Release artifact checks passed for {args.version}: {summary}.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
