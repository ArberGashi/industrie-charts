#!/usr/bin/env python3
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# --- Move map (Source FQN -> Target FQN)
MOVE_MAP = {
    # platform.swing
    "com.arbergashi.charts.ui.ArberChartPanel": "com.arbergashi.charts.platform.swing.ArberChartPanel",
    "com.arbergashi.charts.ui.SwingThemeListener": "com.arbergashi.charts.platform.swing.SwingThemeListener",
    "com.arbergashi.charts.ui.legend.DockedLegendPanel": "com.arbergashi.charts.platform.swing.legend.DockedLegendPanel",
    # platform.export
    "com.arbergashi.charts.ui.ChartExportService": "com.arbergashi.charts.platform.export.ChartExportService",
    "com.arbergashi.charts.ui.ChartExportHandler": "com.arbergashi.charts.platform.export.ChartExportHandler",
    # platform.ui
    "com.arbergashi.charts.ui.ChartTooltip": "com.arbergashi.charts.platform.ui.ChartTooltip",
    "com.arbergashi.charts.ui.TooltipBackgroundRenderer": "com.arbergashi.charts.platform.ui.TooltipBackgroundRenderer",
    "com.arbergashi.charts.ui.TooltipContentRenderer": "com.arbergashi.charts.platform.ui.TooltipContentRenderer",
    "com.arbergashi.charts.ui.TooltipSnapStrategy": "com.arbergashi.charts.platform.ui.TooltipSnapStrategy",
    "com.arbergashi.charts.ui.TooltipMetrics": "com.arbergashi.charts.platform.ui.TooltipMetrics",
    "com.arbergashi.charts.ui.TooltipPaintContext": "com.arbergashi.charts.platform.ui.TooltipPaintContext",
    "com.arbergashi.charts.ui.DefaultTooltipBackgroundRenderer": "com.arbergashi.charts.platform.ui.DefaultTooltipBackgroundRenderer",
    "com.arbergashi.charts.ui.DefaultTooltipContentRenderer": "com.arbergashi.charts.platform.ui.DefaultTooltipContentRenderer",
    "com.arbergashi.charts.ui.DataPointSnapStrategy": "com.arbergashi.charts.platform.ui.DataPointSnapStrategy",
    "com.arbergashi.charts.ui.HighPrecisionCrosshair": "com.arbergashi.charts.platform.ui.HighPrecisionCrosshair",
    "com.arbergashi.charts.ui.crosshair.CrosshairSyncController": "com.arbergashi.charts.platform.ui.crosshair.CrosshairSyncController",
    # engine.forensic
    "com.arbergashi.charts.ui.ForensicFrameVault": "com.arbergashi.charts.engine.forensic.ForensicFrameVault",
    # render.grid
    "com.arbergashi.charts.ui.grid.GridLayer": "com.arbergashi.charts.render.grid.GridLayer",
    "com.arbergashi.charts.ui.grid.PanelAwareGridLayer": "com.arbergashi.charts.render.grid.PanelAwareGridLayer",
    "com.arbergashi.charts.ui.grid.GridLODManager": "com.arbergashi.charts.render.grid.GridLODManager",
    "com.arbergashi.charts.ui.grid.DefaultGridLayer": "com.arbergashi.charts.render.grid.DefaultGridLayer",
    "com.arbergashi.charts.ui.grid.MedicalGridLayer": "com.arbergashi.charts.render.grid.MedicalGridLayer",
    "com.arbergashi.charts.ui.grid.FinancialGridLayer": "com.arbergashi.charts.render.grid.FinancialGridLayer",
    "com.arbergashi.charts.ui.grid.AnalysisGridLayer": "com.arbergashi.charts.render.grid.AnalysisGridLayer",
    "com.arbergashi.charts.ui.grid.StatisticalGridLayer": "com.arbergashi.charts.render.grid.StatisticalGridLayer",
    "com.arbergashi.charts.ui.grid.LogarithmicGridLayer": "com.arbergashi.charts.render.grid.LogarithmicGridLayer",
    "com.arbergashi.charts.ui.grid.PolarGridLayer": "com.arbergashi.charts.render.grid.PolarGridLayer",
    "com.arbergashi.charts.ui.grid.TernaryGridLayer": "com.arbergashi.charts.render.grid.TernaryGridLayer",
    "com.arbergashi.charts.ui.grid.SmithChartGridLayer": "com.arbergashi.charts.render.grid.SmithChartGridLayer",
    "com.arbergashi.charts.ui.grid.GeoGridLayer": "com.arbergashi.charts.render.grid.GeoGridLayer",
    "com.arbergashi.charts.ui.grid.IsometricGridLayer": "com.arbergashi.charts.render.grid.IsometricGridLayer",
    # render.legend (impl)
    "com.arbergashi.charts.ui.legend.InteractiveLegendOverlay": "com.arbergashi.charts.render.legend.InteractiveLegendOverlay",
    "com.arbergashi.charts.ui.legend.DefaultLegendLayoutEngine": "com.arbergashi.charts.render.legend.DefaultLegendLayoutEngine",
    "com.arbergashi.charts.ui.legend.DefaultLegendMarkerRenderer": "com.arbergashi.charts.render.legend.DefaultLegendMarkerRenderer",
    "com.arbergashi.charts.ui.legend.DefaultLegendTextRenderer": "com.arbergashi.charts.render.legend.DefaultLegendTextRenderer",
    "com.arbergashi.charts.ui.legend.DefaultLegendIconRenderer": "com.arbergashi.charts.render.legend.DefaultLegendIconRenderer",
    "com.arbergashi.charts.ui.legend.DefaultLegendBackgroundRenderer": "com.arbergashi.charts.render.legend.DefaultLegendBackgroundRenderer",
    "com.arbergashi.charts.ui.legend.DefaultLegendFontResolver": "com.arbergashi.charts.render.legend.DefaultLegendFontResolver",
    "com.arbergashi.charts.ui.legend.LegendPaintContext": "com.arbergashi.charts.render.legend.LegendPaintContext",
    "com.arbergashi.charts.ui.legend.LegendChartContext": "com.arbergashi.charts.render.legend.LegendChartContext",
    "com.arbergashi.charts.ui.legend.LegendLayoutEngine": "com.arbergashi.charts.render.legend.LegendLayoutEngine",
    "com.arbergashi.charts.ui.legend.LegendMarkerRenderer": "com.arbergashi.charts.render.legend.LegendMarkerRenderer",
    "com.arbergashi.charts.ui.legend.LegendBackgroundRenderer": "com.arbergashi.charts.render.legend.LegendBackgroundRenderer",
    "com.arbergashi.charts.ui.legend.LegendTextRenderer": "com.arbergashi.charts.render.legend.LegendTextRenderer",
    "com.arbergashi.charts.ui.legend.LegendIconRenderer": "com.arbergashi.charts.render.legend.LegendIconRenderer",
    "com.arbergashi.charts.ui.legend.LegendFontResolver": "com.arbergashi.charts.render.legend.LegendFontResolver",
    "com.arbergashi.charts.ui.legend.LegendMetrics": "com.arbergashi.charts.render.legend.LegendMetrics",
    "com.arbergashi.charts.ui.legend.DefaultLegendMetrics": "com.arbergashi.charts.render.legend.DefaultLegendMetrics",
    "com.arbergashi.charts.ui.legend.LegendFonts": "com.arbergashi.charts.render.legend.LegendFonts",
    # domain.legend
    "com.arbergashi.charts.ui.legend.LegendModel": "com.arbergashi.charts.domain.legend.LegendModel",
    "com.arbergashi.charts.ui.legend.LegendSeriesRow": "com.arbergashi.charts.domain.legend.LegendSeriesRow",
    "com.arbergashi.charts.ui.legend.LegendLayout": "com.arbergashi.charts.domain.legend.LegendLayout",
    "com.arbergashi.charts.ui.legend.LegendLayoutResult": "com.arbergashi.charts.domain.legend.LegendLayoutResult",
    "com.arbergashi.charts.ui.legend.LegendRowLayout": "com.arbergashi.charts.domain.legend.LegendRowLayout",
    "com.arbergashi.charts.ui.legend.LegendConfig": "com.arbergashi.charts.domain.legend.LegendConfig",
    "com.arbergashi.charts.ui.legend.LegendValueFormatter": "com.arbergashi.charts.domain.legend.LegendValueFormatter",
    "com.arbergashi.charts.ui.legend.LayerVisibilityModel": "com.arbergashi.charts.domain.legend.LayerVisibilityModel",
    "com.arbergashi.charts.ui.legend.LegendPlacement": "com.arbergashi.charts.domain.legend.LegendPlacement",
    "com.arbergashi.charts.ui.legend.LegendDockSide": "com.arbergashi.charts.domain.legend.LegendDockSide",
    "com.arbergashi.charts.ui.legend.LegendPosition": "com.arbergashi.charts.domain.legend.LegendPosition",
    "com.arbergashi.charts.ui.legend.LegendDensity": "com.arbergashi.charts.domain.legend.LegendDensity",
    "com.arbergashi.charts.ui.legend.LegendMarkerStyle": "com.arbergashi.charts.domain.legend.LegendMarkerStyle",
    "com.arbergashi.charts.ui.legend.TrendValue": "com.arbergashi.charts.domain.legend.TrendValue",
    "com.arbergashi.charts.ui.legend.LegendActionListener": "com.arbergashi.charts.domain.legend.LegendActionListener",
}

STUB_HEADER = """\
/**
 * @deprecated Part of the ArberCharts High-Integrity Architecture Doctrine.
 * Please migrate to the new platform/render/domain structure.
 * This stub will be removed in v1.7.0 LTS.
 */
@Deprecated
"""

def iter_java_files():
    for root in (
        "arbercharts-core/src/main/java",
        "arbercharts-core/src/test/java",
        "arbercharts-demo/src/main/java",
    ):
        base = ROOT / root
        if not base.exists():
            continue
        for p in base.rglob("*.java"):
            yield p

def rewrite_imports():
    pattern = re.compile(r"(import\s+)([\w\.]+);")
    for p in iter_java_files():
        text = p.read_text(encoding="utf-8")
        def repl(m):
            fq = m.group(2)
            if fq in MOVE_MAP:
                return m.group(1) + MOVE_MAP[fq] + ";"
            return m.group(0)
        new_text = pattern.sub(repl, text)
        if new_text != text:
            p.write_text(new_text, encoding="utf-8")

def write_stub(source_fq, target_fq):
    src_pkg, src_cls = source_fq.rsplit(".", 1)
    tgt_pkg, tgt_cls = target_fq.rsplit(".", 1)
    pkg_path = ROOT / "arbercharts-core/src/main/java" / Path(*src_pkg.split("."))
    pkg_path.mkdir(parents=True, exist_ok=True)
    stub_file = pkg_path / f"{src_cls}.java"
    body = f"""package {src_pkg};

{STUB_HEADER}
public class {src_cls} extends {tgt_pkg}.{tgt_cls} {{
    public {src_cls}() {{
        super();
    }}
}}
"""
    stub_file.write_text(body, encoding="utf-8")

def generate_stubs():
    for src, dst in MOVE_MAP.items():
        write_stub(src, dst)

if __name__ == "__main__":
    rewrite_imports()
    generate_stubs()
    print("Refactor import rewrite + stubs generated.")
