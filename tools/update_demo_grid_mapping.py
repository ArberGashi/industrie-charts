#!/usr/bin/env python3
"""Document the Swing demo's base grid selection without duplicating its rules."""

import argparse
from collections import defaultdict
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'arbercharts-demo/src/main/resources/data/renderer-catalog.txt'
DEMO = ROOT / 'arbercharts-demo/src/main/java/com/arbergashi/charts/demo/DemoApplication.java'
PANEL = ROOT / 'arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java'
OUTPUT = ROOT / 'docs/DEMO_GRID_MAPPING.md'


def parse_catalog(text: str) -> list[tuple[str, str]]:
    entries = []
    seen = set()
    for line_number, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        parts = line.split('|')
        if len(parts) != 2 or not all(part.strip() for part in parts):
            raise ValueError(f'Invalid catalog entry on line {line_number}')
        category, class_name = (part.strip() for part in parts)
        category = category.lower()
        if class_name in seen:
            raise ValueError(f'Duplicate catalog class: {class_name}')
        seen.add(class_name)
        entries.append((category, class_name))
    if not entries:
        raise ValueError('Renderer catalog is empty')
    return entries


def parse_rules(source: str) -> list[tuple[list[tuple[str, str]], str]]:
    method = re.search(r'private void configureChart\([^\n]*\)\s*\{(.*?)\n    \}', source, re.S)
    if method is None:
        raise ValueError('Cannot locate DemoApplication.configureChart')
    body = re.sub(r'//[^\n]*', '', method.group(1))
    selection = re.fullmatch(
        r'\s*String category\s*=\s*entry\.category\(\);\s*'
        r'String simple\s*=\s*entry\.simpleName\(\);(?P<rules>.*?)'
        r'\s*configureAxisForCategory\(entry\.category\(\),\s*panel\);\s*',
        body, re.S)
    if selection is None:
        raise ValueError('Grid selection structure changed; update this generator')
    branches = selection.group('rules')
    branch_pattern = re.compile(
        r'(?P<else>else\s+)?if\s*\((?P<condition>.*?)\)\s*\{\s*'
        r'panel\.setGridLayer\(new (?P<grid>\w+)\(\)\);\s*\}', re.S)
    rules = []
    position = 0
    for branch in branch_pattern.finditer(branches):
        if branches[position:branch.start()].strip() or bool(branch.group('else')) != bool(rules):
            raise ValueError('Expected one ordered if/else-if grid selection chain')
        conditions = []
        for term in branch.group('condition').split('||'):
            comparison = re.fullmatch(r'\s*"([^"\n]+)"\.equals\((category|simple)\)\s*', term)
            if comparison is None:
                raise ValueError(f'Unsupported grid condition: {term.strip()}')
            value, field = comparison.groups()
            conditions.append((field, value))
        rules.append((conditions, branch.group('grid')))
        position = branch.end()
    if not rules or branches[position:].strip():
        raise ValueError('Incomplete or unsupported grid selection; refusing stale output')
    return rules


def render_document() -> str:
    entries = parse_catalog(CATALOG.read_text(encoding='utf-8'))
    rules = parse_rules(DEMO.read_text(encoding='utf-8'))
    defaults = re.findall(r'this\.gridLayer\s*=\s*new (\w+)\(\);', PANEL.read_text(encoding='utf-8'))
    if len(defaults) != 1:
        raise ValueError('Cannot determine the unique ArberChartPanel default grid')
    default_grid = defaults[0]
    version = ET.parse(ROOT / 'pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
    if not version:
        raise ValueError('Parent POM has no release version')

    groups = defaultdict(list)
    for category, class_name in entries:
        fields = {'category': category, 'simple': class_name.rsplit('.', 1)[-1]}
        grid = next((grid for conditions, grid in rules
                     if any(fields[field] == value for field, value in conditions)), default_grid)
        groups[grid].append((category, class_name))

    lines = [
        f'# Demo grid mapping ({version})', '',
        '> Generated from the renderer catalog, `DemoApplication.configureChart` and the',
        '> `ArberChartPanel` default. Run `python3 tools/update_demo_grid_mapping.py` to refresh;',
        '> use `--check` to verify without writing.', '',
        f'The Swing workbench assigns the following base grid layers to all {len(entries)} catalog entries.',
        'Rules are evaluated in source order. Category rules take precedence over later renderer-name',
        'rules: for example, the financial `Candlestick3DRenderer` uses `FinancialGridLayer`.',
        'Axis settings and showcase presets control visibility separately; a selected layer does',
        'not imply that every grid line is visible. These are Swing workbench choices, not a',
        'required grid for a renderer in another application.', '',
    ]
    order = list(dict.fromkeys([grid for _, grid in rules] + [default_grid]))
    for grid in order:
        if grid not in groups:
            continue
        lines.extend([f'## {grid}', '', '| Category | Catalog class |', '| --- | --- |'])
        for category, class_name in sorted(groups[grid]):
            lines.append(f'| {category} | `{class_name}` |')
        lines.append('')
    return '\n'.join(lines).rstrip() + '\n'


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='fail when the checked-in document is stale')
    args = parser.parse_args()
    try:
        document = render_document()
    except ValueError as error:
        parser.exit(1, f'{error}\n')
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8') != document:
            parser.exit(1, 'Demo grid mapping is stale; run tools/update_demo_grid_mapping.py\n')
        print('Demo grid mapping matches the current Swing configuration.')
    else:
        OUTPUT.write_text(document, encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
