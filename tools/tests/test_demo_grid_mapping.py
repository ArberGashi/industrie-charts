"""Keep grid documentation faithful to ordered, conditional demo selection."""

from pathlib import Path
import sys
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from update_demo_grid_mapping import parse_rules


SOURCE = '''
    private void configureChart(RendererCatalogEntry entry, ArberChartPanel panel) {
        String category = entry.category();
        String simple = entry.simpleName();
        if ("medical".equals(category)) {
            panel.setGridLayer(isEcgShowcase(simple) ? new MedicalGridLayer()
                    : new com.arbergashi.charts.render.grid.DefaultGridLayer());
        } else if ("financial".equals(category)) {
            panel.setGridLayer(new FinancialGridLayer());
        } else if ("security".equals(category) || "Candlestick3DRenderer".equals(simple)) {
            panel.setGridLayer(new IsometricGridLayer());
        }
        if ("medical".equals(category)) configureMedicalShowcase(simple, panel);
        else configureAxisForCategory(category, panel);
    }

    private static boolean isEcgShowcase(String simple) {
        return switch (simple) {
            case "CalibrationRenderer", "ECGRenderer" -> true;
            default -> false;
        };
    }
'''


class DemoGridMappingTests(unittest.TestCase):
    def test_medical_grid_requires_both_medical_category_and_ecg_name(self):
        rules = parse_rules(SOURCE)
        for category, name, expected in (
                ('medical', 'ECGRenderer', 'MedicalGridLayer'),
                ('medical', 'CalibrationRenderer', 'MedicalGridLayer'),
                ('medical', 'EEGRenderer', 'DefaultGridLayer'),
                ('medical', 'VCGRenderer', 'DefaultGridLayer'),
                ('security', 'ECGRenderer', 'IsometricGridLayer')):
            with self.subTest(category=category, name=name):
                fields = {'category': category, 'simple': name}
                self.assertEqual(next(rule.grid for rule in rules if rule.matches(fields)), expected)

    def test_earlier_category_rule_wins_over_later_renderer_rule(self):
        fields = {'category': 'financial', 'simple': 'Candlestick3DRenderer'}
        self.assertEqual(next(rule.grid for rule in parse_rules(SOURCE) if rule.matches(fields)),
                         'FinancialGridLayer')

    def test_changed_helper_cannot_silently_produce_a_partial_mapping(self):
        for helper in ('case "ECGRenderer", "ECGRenderer" -> true;',
                       'case "ECGRenderer" -> false;',
                       'case "ECGRenderer" -> anotherCondition();',
                       'case -> true;'):
            with self.subTest(helper=helper):
                changed = SOURCE.replace('case "CalibrationRenderer", "ECGRenderer" -> true;', helper)
                with self.assertRaises(ValueError):
                    parse_rules(changed)

    def test_unknown_selection_expression_is_rejected(self):
        changed = SOURCE.replace('isEcgShowcase(simple) ?', 'isEcgShowcase(simple) && enabled ?')
        with self.assertRaisesRegex(ValueError, 'Unsupported grid constructor'):
            parse_rules(changed)

    def test_extra_grid_override_is_rejected(self):
        changed = SOURCE.replace('if ("medical".equals(category)) configureMedicalShowcase',
                                 'panel.setGridLayer(new CustomGridLayer());\n'
                                 '        if ("medical".equals(category)) configureMedicalShowcase')
        with self.assertRaises(ValueError):
            parse_rules(changed)


if __name__ == '__main__':
    unittest.main()
