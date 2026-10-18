#!/usr/bin/env python3
"""Check current release metadata, local documentation links and source-tree hygiene."""

from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parent.parent
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
LINK = re.compile(r"!?\[[^\]\n]*\]\(<?([^\s)>]+)>?(?:\s+\"[^\"]*\")?\)")
HTML_LINK = re.compile(r"(?:src|srcset|href)=[\"']([^\"']+)[\"']")
LEGACY_DOCUMENT = re.compile(r"(?:SLA(?:_[A-Z0-9]+)*|PRICING|BINARY-LICENSE|LICENSE-FAQ|LICENSING)\.md$", re.I)
# Reject the retired About-dialog delivery claim without banning ordinary
# technical support references or the MIT permission for commercial use.
STALE_TERMS = re.compile(
    r"binary-first|source-access|PROPRIETARY AND CONFIDENTIAL"
    r"|commercial\s+support,\s*closed-source\s+product\s+delivery", re.I)
PROJECT_URL = "https://github.com/ArberGashi/industrie-charts/"


def main():
    pom = ET.parse(ROOT / "pom.xml").getroot()
    version = pom.findtext("m:version", namespaces=NS)
    names = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT,
    ).decode("utf-8").split("\0")
    files = sorted({name for name in names if name and (ROOT / name).is_file()})
    errors = []
    links = 0

    for name in files:
        path = Path(name)
        parts = path.parts
        if (".jqwik-database" in parts or "__pycache__" in parts
                or any(p == "target" or p.startswith("target-") for p in parts)
                or name.startswith(("docs/site/", "docs/javadoc/", "site/", "binaries/", "dist/"))
                or path.suffix in {".class", ".jar", ".zip", ".dylib", ".dll", ".so", ".pyc", ".pyo"}):
            errors.append(f"{name}: generated artifact must not be committed")
        if LEGACY_DOCUMENT.fullmatch(path.name):
            errors.append(f"{name}: obsolete commercial or duplicate licensing document")
        if path.name.startswith("RELEASE_NOTES_") and path.name != f"RELEASE_NOTES_{version}.md":
            errors.append(f"{name}: superseded release notes belong in Git history")

        if path.suffix in {".md", ".java", ".xml"} or name in {"LICENSE", "NOTICE"}:
            text = (ROOT / path).read_text(encoding="utf-8")
            # Repository guidance may name prohibited terms; product documents may not assert them.
            if path.name != "AGENTS.md" and STALE_TERMS.search(text):
                errors.append(f"{name}: stale private-delivery or proprietary wording")
        else:
            continue

        if path.suffix != ".md":
            continue
        for declared in re.findall(r"<version>([^<]+)</version>", text):
            if declared != version and not declared.startswith("${"):
                errors.append(f"{name}: Maven example uses {declared}, expected {version}")
        for target in LINK.findall(text) + HTML_LINK.findall(text):
            for prefix in (PROJECT_URL + "blob/main/", PROJECT_URL + "tree/main/"):
                if target.startswith(prefix):
                    target = "/" + target[len(prefix):]
                    break
            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            relative = unquote(parsed.path)
            resolved = (ROOT / relative.lstrip("/") if relative.startswith("/")
                        else ROOT / path.parent / relative)
            links += 1
            if not resolved.exists():
                errors.append(f"{name}: missing local link target {target}")

    for module in pom.findall("m:modules/m:module", NS):
        module_pom = ROOT / module.text / "pom.xml"
        child = ET.parse(module_pom).getroot()
        actual = child.findtext("m:parent/m:version", namespaces=NS)
        if actual != version:
            errors.append(f"{module.text}/pom.xml: parent {actual}, expected {version}")

    catalog = (ROOT / "arbercharts-demo/src/main/resources/data/renderer-catalog.txt").read_text()
    documented = (ROOT / "docs/RENDERER_CATALOG.md").read_text()
    if not documented.startswith(f"# Renderer Catalog ({version})\n"):
        errors.append("docs/RENDERER_CATALOG.md: title does not match the current release")
    for entry in catalog.splitlines():
        if not entry.strip():
            continue
        _, qualified = entry.split("|", 1)
        source = "../arbercharts-core/src/main/java/" + qualified.replace(".", "/") + ".java"
        if documented.count("](" + source + ")") != 1:
            errors.append(f"docs/RENDERER_CATALOG.md: expected one source link for {qualified}")

    grid_check = subprocess.run(
        [sys.executable, str(ROOT / "tools/update_demo_grid_mapping.py"), "--check"],
        cwd=ROOT, capture_output=True, text=True,
    )
    if grid_check.returncode:
        errors.append("docs/DEMO_GRID_MAPPING.md: " + grid_check.stderr.strip())

    if errors:
        print("Repository checks failed:\n" + "\n".join(f"- {error}" for error in errors), file=sys.stderr)
        return 1
    print(f"Repository checks passed: version {version}, {len(files)} files, {links} local links.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
