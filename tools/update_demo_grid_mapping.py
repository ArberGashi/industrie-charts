#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FACTORY = ROOT / 'arbercharts-demo/src/main/java/com/arbergashi/charts/rendererpanels/DemoPanelFactory.java'
OUTPUT = ROOT / 'docs/DEMO_GRID_MAPPING.md'

HEADER = """# Demo Grid Mapping (Core Grid Layers)

> **Auto-generated** from `DemoPanelFactory.resolveGridLayer`. Do not edit manually.
> Run `python3 tools/update_demo_grid_mapping.py` to refresh.

This document records the authoritative demo grid mapping used by `DemoPanelFactory`. Each demo panel consumes a core grid layer based on domain-specific best practice.
"""


def extract_titles(method: str, text: str) -> set[str]:
    m = re.search(rf'private static boolean {method}\(String title\) \{{([\s\S]*?)\n\s*\}}', text)
    if not m:
        return set()
    block = m.group(1)
    return set(re.findall(r'"([^"]+)"', block))


def main() -> int:
    factory_text = FACTORY.read_text(encoding='utf-8')

    titles = sorted({
        m.group(1)
        for m in re.finditer(r'case "([^"]+)" -> [A-Za-z0-9_]+\.create\(\);', factory_text)
    })

    financial = extract_titles('isFinancialChart', factory_text)
    medical = extract_titles('isMedicalChart', factory_text)
    analysis = extract_titles('isAnalysisChart', factory_text)
    statistical = extract_titles('isStatisticalChart', factory_text)

    log = {"FFT", "Spectrogram", "Medical Spectrogram"}
    polar = {"Radar Chart", "Polar Chart", "Polar Line", "Nightingale Rose", "Wind Rose", "Dependency Wheel", "Polar Advanced"}
    smith = {"Smith Chart"}
    geo = {"Geo Tactical"}
    iso = {"Isometric Blueprint"}
    ternary = {"Ternary Phase", "Ternary Contour", "Ternary Plot"}

    mapping: dict[str, str] = {}
    for t in titles:
        if t in log:
            mapping[t] = 'LogarithmicGridLayer'
        elif t in smith:
            mapping[t] = 'SmithChartGridLayer'
        elif t in geo:
            mapping[t] = 'GeoGridLayer'
        elif t in iso:
            mapping[t] = 'IsometricGridLayer'
        elif t in ternary:
            mapping[t] = 'TernaryGridLayer'
        elif t in polar:
            mapping[t] = 'PolarGridLayer'
        elif t in medical:
            mapping[t] = 'MedicalGridLayer'
        elif t in financial:
            mapping[t] = 'FinancialGridLayer'
        elif t in analysis:
            mapping[t] = 'AnalysisGridLayer'
        elif t in statistical:
            mapping[t] = 'StatisticalGridLayer'
        else:
            mapping[t] = 'DefaultGridLayer'

    from collections import defaultdict
    groups: dict[str, list[str]] = defaultdict(list)
    for title, grid in mapping.items():
        groups[grid].append(title)

    order = [
        'MedicalGridLayer',
        'LogarithmicGridLayer',
        'FinancialGridLayer',
        'AnalysisGridLayer',
        'StatisticalGridLayer',
        'PolarGridLayer',
        'SmithChartGridLayer',
        'GeoGridLayer',
        'IsometricGridLayer',
        'TernaryGridLayer',
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
