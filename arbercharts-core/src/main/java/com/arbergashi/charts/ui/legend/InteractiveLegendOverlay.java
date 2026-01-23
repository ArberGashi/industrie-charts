package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.api.ChartFocus;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TradingView-style interactive legend overlay.
 *
 * <p>Features:
 * <ul>
 *   <li>Column layout (name + values)</li>
 *   <li>Live values from the current {@link ChartFocus}</li>
 *   <li>Visibility toggles (click name)</li>
 *   <li>Settings action area (right side)</li>
 * </ul>
 *
 * <p>This component deliberately stays lightweight: it renders text/boxes itself and
 * delegates actions via {@link LegendActionListener}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class InteractiveLegendOverlay extends JComponent {

    private static final String KEY_POS = "Chart.legend.position"; // e.g. "top-left", "bottom-right"
    private static final String KEY_SOLO_ENABLED = "Chart.legend.soloEnabled";

    private final LegendChartContext chart;
    private ChartTheme theme;
    private LegendActionListener actions;

    private ChartFocus focus = ChartFocus.EMPTY;

    private final RoundRectangle2D.Float box = new RoundRectangle2D.Float();

    // hit regions for interaction
    private final List<RowHit> rowHits = new ArrayList<>();
    private String hoveredId;

    private java.util.function.Predicate<String> visibilityResolver = _ -> true;

    private static final class RowHit {
        final String id;
        final Rectangle nameBounds;
        final Rectangle settingsBounds;

        RowHit(String id, Rectangle nameBounds, Rectangle settingsBounds) {
            this.id = id;
            this.nameBounds = nameBounds;
            this.settingsBounds = settingsBounds;
        }
    }

    public InteractiveLegendOverlay(LegendChartContext chart, ChartTheme theme) {
        this.chart = chart;
        this.theme = theme != null ? theme : ChartThemes.defaultDark();
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                RowHit hit = findHit(e.getPoint());
                if (hit == null || actions == null) return;

                if (hit.settingsBounds.contains(e.getPoint())) {
                    actions.openSeriesSettings(hit.id);
                } else if (hit.nameBounds.contains(e.getPoint())) {
                    boolean soloEnabled = ChartAssets.getBoolean(KEY_SOLO_ENABLED, true);
                    if (soloEnabled && e.isAltDown()) {
                        actions.soloSeries(hit.id);
                    } else {
                        actions.toggleSeries(hit.id);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredId != null) {
                    hoveredId = null;
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                RowHit hit = findHit(e.getPoint());
                String next = (hit != null) ? hit.id : null;
                if ((hoveredId == null && next != null) || (hoveredId != null && !hoveredId.equals(next))) {
                    hoveredId = next;
                    repaint();
                }
            }
        });
    }

    /**
     * Updates the legend theme for colors and typography.
     *
     * @param theme chart theme (falls back to default when null)
     */
    public void setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.defaultDark();
        repaint();
    }

    /**
     * Installs the action handler used for toggle/solo/settings interactions.
     *
     * @param actions action listener (may be null to disable interactivity)
     */
    public void setLegendActionListener(LegendActionListener actions) {
        this.actions = actions;
    }

    /**
     * Updates the focus state used to display live series values.
     *
     * @param focus focus payload (null resets to {@link ChartFocus#EMPTY})
     */
    public void setFocus(ChartFocus focus) {
        this.focus = (focus != null) ? focus : ChartFocus.EMPTY;
        repaint();
    }

    /**
     * Sets a resolver callback used to determine whether a series is visible.
     *
     * <p>If not set, all series are assumed visible.</p>
     */
    public void setVisibilityResolver(java.util.function.Predicate<String> resolver) {
        this.visibilityResolver = (resolver != null) ? resolver : (_ -> true);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        ChartModel model = chart.getModel();
        if (model == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Font base = theme.getBaseFont();
            if (base == null) base = UIManager.getFont("Label.font");
            if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

            float fontScale = ChartAssets.getFloat("Chart.legend.fontScale", 1.0f);
            Font font = base.deriveFont(Font.PLAIN, Math.max(9f, ChartScale.uiFontSize(base, 11f) * fontScale));
            Font bold = font.deriveFont(Font.BOLD);

            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int padding = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.legend.padding", 8f)));
            int rowGap = Math.round(ChartScale.scale(3f));
            int iconSize = Math.round(ChartScale.scale(ChartAssets.getFloat("Chart.legend.iconSize", 9f)));
            int colGap = Math.round(ChartScale.scale(10f));
            int settingsW = Math.round(ChartScale.scale(14f));

            // Build rows from renderers
            List<LegendSeriesRow> rows = buildRows(model);
            if (rows.isEmpty()) return;

            // Column measurement
            int nameColW = 0;
            int valuesColW = 0;

            for (LegendSeriesRow r : rows) {
                nameColW = Math.max(nameColW, fm.stringWidth(r.name()));
                String values = formatValues(r.values());
                valuesColW = Math.max(valuesColW, fm.stringWidth(values));
            }

            int rowH = fm.getHeight() + rowGap;
            int boxW = padding * 2 + iconSize + Math.round(ChartScale.scale(6f)) + nameColW + colGap + valuesColW + settingsW;
            int maxBoxW = Math.max(0, getWidth() - padding * 2);
            if (maxBoxW > 0) boxW = Math.min(boxW, maxBoxW);

            int maxRows = Math.max(1, (getHeight() - padding * 2) / rowH);
            boolean overflow = rows.size() > maxRows;
            int visibleRows = overflow ? Math.max(1, maxRows - 1) : rows.size();
            int boxH = padding * 2 + (overflow ? (visibleRows + 1) : visibleRows) * rowH;

            LegendPosition pos = LegendPosition.parse(ChartAssets.getString(KEY_POS, "TOP_LEFT"), LegendPosition.TOP_LEFT);
            Rectangle legendBounds = pos.place(new Rectangle(0, 0, getWidth(), getHeight()), new Dimension(boxW, boxH), new Insets(padding, padding, padding, padding));
            int x = legendBounds.x;
            int y = legendBounds.y;

            // Background
            Color bg = ColorUtils.withAlpha(theme.getBackground(), 0.82f);
            g2.setColor(bg);
            int arc = Math.round(ChartScale.scale(10f));
            box.setRoundRect(x, y, boxW, boxH, arc, arc);
            g2.fill(box);

            g2.setColor(ColorUtils.withAlpha(theme.getAxisLabelColor(), 0.25f));
            g2.draw(box);

            // Rows
            rowHits.clear();
            int cy = y + padding + fm.getAscent();
            int nameX = x + padding + iconSize + Math.round(ChartScale.scale(6f));
            int contentW = boxW - padding * 2 - iconSize - Math.round(ChartScale.scale(6f)) - settingsW;
            int minNameW = Math.round(ChartScale.scale(60f));
            int valuesCap = Math.min(valuesColW, Math.max(0, contentW - minNameW - colGap));
            int nameCap = Math.max(0, contentW - valuesCap - colGap);
            int valuesX = nameX + nameCap + colGap;
            int settingsX = x + boxW - padding - settingsW;

            int limit = visibleRows;
            for (int i = 0; i < limit; i++) {
                LegendSeriesRow row = rows.get(i);
                String nameText = truncateToWidth(row.name(), nameCap, fm);
                String values = truncateToWidth(formatValues(row.values()), valuesCap, fm);

                if (row.id() != null && row.id().equals(hoveredId)) {
                    Color hover = ColorUtils.withAlpha(theme.getAccentColor(), 0.12f);
                    int rowY = cy - fm.getAscent();
                    g2.setColor(hover);
                    g2.fillRect(x + padding / 2, rowY, boxW - padding, rowH);
                }

                // marker
                g2.setColor(row.color());
                g2.fillOval(x + padding, cy - iconSize + Math.round(ChartScale.scale(2f)), iconSize, iconSize);

                // name (dim if hidden)
                g2.setFont(bold);
                g2.setColor(row.visible() ? theme.getForeground() : ColorUtils.withAlpha(theme.getForeground(), 0.45f));
                g2.drawString(nameText, nameX, cy);

                // values
                g2.setFont(font);
                g2.setColor(ColorUtils.withAlpha(theme.getForeground(), 0.85f));
                g2.drawString(values, valuesX, cy);

                // settings glyph
                g2.setColor(ColorUtils.withAlpha(theme.getAxisLabelColor(), 0.85f));
                g2.drawString("⚙", settingsX, cy);

                Rectangle nameBounds = new Rectangle(nameX, cy - fm.getAscent(), Math.min(nameCap, fm.stringWidth(nameText)), fm.getHeight());
                Rectangle settingsBounds = new Rectangle(settingsX - 2, cy - fm.getAscent(), settingsW + 4, fm.getHeight());
                rowHits.add(new RowHit(row.id(), nameBounds, settingsBounds));

                cy += rowH;
            }

            if (overflow) {
                int remaining = rows.size() - visibleRows;
                String moreText = "+" + remaining + " more";
                g2.setFont(font);
                g2.setColor(ColorUtils.withAlpha(theme.getAxisLabelColor(), 0.9f));
                g2.drawString(moreText, nameX, cy);
            }
        } finally {
            g2.dispose();
        }
    }

    private RowHit findHit(Point p) {
        for (RowHit h : rowHits) {
            if (h.nameBounds.contains(p) || h.settingsBounds.contains(p)) return h;
        }
        return null;
    }

    private List<LegendSeriesRow> buildRows(ChartModel model) {
        List<LegendSeriesRow> rows = new ArrayList<>();

        for (BaseRenderer r : chart.getRenderers()) {
            if (!r.isLegendRequired()) continue;

            String id = r.getId();
            String name = r.getName() != null ? r.getName() : id;
            Color color = r.getLegendColor(model);

            boolean visible = visibilityResolver == null || visibilityResolver.test(id);

            Map<String, Object> values = new LinkedHashMap<>();
            if (focus != null && focus.isActive() && focus.index() >= 0) {
                // Renderer may contribute values.
                try {
                    // Context is optional; consumers may ignore it.
                    com.arbergashi.charts.api.PlotContext ctx = null;
                    values.putAll(r.getFocusValues(focus.index(), model, ctx));
                } catch (Exception ignored) {
                    // Keep legend robust even for third-party renderers.
                }
            }

            rows.add(new LegendSeriesRow(id, name, color, visible, true, values));
        }

        return rows;
    }


    private static String formatValues(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : values.entrySet()) {
            if (!first) sb.append("  ");
            sb.append(e.getKey()).append(": ").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static String truncateToWidth(String text, int maxWidth, FontMetrics fm) {
        if (text == null) return "";
        if (maxWidth <= 0 || fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = fm.stringWidth(ellipsis);
        int limit = Math.max(0, maxWidth - ellipsisW);
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) > limit) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
