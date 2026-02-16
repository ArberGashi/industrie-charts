#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'arbercharts-demo/src/main/resources/data/renderer-catalog.txt'
OUTPUT = ROOT / 'docs/DEMO_GRID_MAPPING.md'

HEADER = """# Demo Grid Mapping (Core Grid Layers)

> **Auto-generated** from `arbercharts-demo` (`RendererCatalog` + `DemoApplication.configureGrid`). Do not edit manually.
> Run `python3 tools/update_demo_grid_mapping.py` to refresh.

This document records the demo grid mapping used by the Swing demo application.
"""


def parse_catalog(text: str) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if len(parts) != 2:
            continue
        category = parts[0].strip().lower()
        class_name = parts[1].strip()
        if not category or not class_name:
            continue
        out.append((category, class_name))
    return out


def simple_name(class_name: str) -> str:
    i = class_name.rfind(".")
    return class_name[i + 1:] if i >= 0 else class_name


def main() -> int:
    catalog_text = CATALOG.read_text(encoding="utf-8")
    entries = parse_catalog(catalog_text)
    if not entries:
        raise SystemExit(f"Renderer catalog is empty: {CATALOG}")

    smith_renderers = {"SmithChartRenderer", "VSWRCircleRenderer"}

    mapping: dict[str, str] = {}
    for category, class_name in entries:
        name = simple_name(class_name)
        if category == "medical":
            mapping[name] = "MedicalGridLayer"
        elif name in smith_renderers:
            mapping[name] = "SmithChartGridLayer"
        else:
            mapping[name] = "DefaultGridLayer"

    from collections import defaultdict
    groups: dict[str, list[str]] = defaultdict(list)
    for name, grid in mapping.items():
        groups[grid].append(name)

    order = [
        'MedicalGridLayer',
        'SmithChartGridLayer',
        'DefaultGridLayer',
    ]

    lines = [HEADER.rstrip(), ""]
    for grid in order:
        if grid not in groups:
            continue
        lines.append(f"## {grid}")
        for title in sorted(groups[grid]):
            lines.append(f"- {title}")
        lines.append("")

    OUTPUT.write_text("\n".join(lines).rstrip() + "\n", encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
