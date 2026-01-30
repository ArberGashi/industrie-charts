package com.arbergashi.charts.platform.swing.legend;

import com.arbergashi.charts.api.ChartFocus;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.domain.legend.LegendActionListener;
import com.arbergashi.charts.domain.legend.LegendDensity;
import com.arbergashi.charts.domain.legend.LegendDockSide;
import com.arbergashi.charts.render.legend.InteractiveLegendOverlay;
import com.arbergashi.charts.render.legend.LegendChartContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;

import javax.swing.*;
import java.awt.*;
/**
 * Docked legend container.
 *
 * <p>This wraps {@link InteractiveLegendOverlay} but uses normal Swing layout
 * so the legend can live outside the chart canvas.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class DockedLegendPanel extends JPanel {

    private static final String KEY_DOCK_DENSITY = "Chart.legend.dock.density"; // compact|dense

    private static final String KEY_DOCK_PREF_W = "Chart.legend.dock.preferredWidth";
    private static final String KEY_DOCK_PREF_H = "Chart.legend.dock.preferredHeight";
    private static final String KEY_DOCK_MIN_W = "Chart.legend.dock.minWidth";
    private static final String KEY_DOCK_MIN_H = "Chart.legend.dock.minHeight";
    private static final String KEY_DOCK_MAX_W = "Chart.legend.dock.maxWidth";
    private static final String KEY_DOCK_MAX_H = "Chart.legend.dock.maxHeight";

    private static final String KEY_DOCK_PREF_W_RIGHT = "Chart.legend.dock.right.preferredWidth";
    private static final String KEY_DOCK_PREF_W_LEFT = "Chart.legend.dock.left.preferredWidth";
    private static final String KEY_DOCK_PREF_H_TOP = "Chart.legend.dock.top.preferredHeight";
    private static final String KEY_DOCK_PREF_H_BOTTOM = "Chart.legend.dock.bottom.preferredHeight";

    private final InteractiveLegendOverlay legend;
    private LegendDockSide side = LegendDockSide.RIGHT;

    public DockedLegendPanel(LegendChartContext chart, ChartTheme theme) {
        super(new BorderLayout());
        setOpaque(false);
        legend = new InteractiveLegendOverlay(chart, theme != null ? theme : ChartThemes.getDarkTheme());

        add(legend, BorderLayout.CENTER);
    }

    /**
     * Sets the docking side that controls sizing defaults and layout behavior.
     *
     * @param side docking side (defaults to right when null)
     */
    public DockedLegendPanel setDockSide(LegendDockSide side) {
        this.side = (side != null) ? side : LegendDockSide.RIGHT;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Returns the current docking side.
     *
     * @return dock side
     */
    public LegendDockSide getDockSide() {
        return side;
    }

    /**
     * Sets the legend theme used by the embedded overlay component.
     *
     * @param theme chart theme to apply
     */
    public DockedLegendPanel setTheme(ChartTheme theme) {
        legend.setTheme(theme);
        return this;
    }

    /**
     * Installs an action listener for legend interactions.
     *
     * @param actions action listener (null disables interactions)
     */
    public DockedLegendPanel setLegendActionListener(LegendActionListener actions) {
        legend.setLegendActionListener(actions);
        return this;
    }

    /**
     * Updates the focus state used for live value rendering.
     *
     * @param focus focus payload (null resets to {@link ChartFocus#EMPTY})
     */
    public DockedLegendPanel setFocus(ChartFocus focus) {
        legend.setFocus(focus);
        return this;
    }

    /**
     * Returns the legend component used for rendering.
     *
     * @return interactive legend overlay component
     */
    public JComponent getLegendComponent() {
        return legend;
    }

    /**
     * Sets a resolver callback used to determine series visibility.
     *
     * @param resolver visibility resolver (null means all visible)
     */
    public DockedLegendPanel setVisibilityResolver(java.util.function.Predicate<String> resolver) {
        legend.setVisibilityResolver(resolver);
        return this;
    }

    private static LegendDensity getDensity() {
        String raw = ChartAssets.getString(KEY_DOCK_DENSITY, "compact");
        if (raw == null) return LegendDensity.COMPACT;
        return raw.equalsIgnoreCase("dense") ? LegendDensity.DENSE : LegendDensity.COMPACT;
    }

    @Override
    public Dimension getMinimumSize() {
        // Defaults are density-aware; properties override.
        LegendDensity density = getDensity();
        int defMinW = (density == LegendDensity.DENSE) ? 220 : 180;
        int defMinH = (density == LegendDensity.DENSE) ? 140 : 120;

        int minW = Math.round(ChartScale.scale(ChartAssets.getInt(KEY_DOCK_MIN_W, defMinW)));
        int minH = Math.round(ChartScale.scale(ChartAssets.getInt(KEY_DOCK_MIN_H, defMinH)));
        return new Dimension(minW, minH);
    }

    @Override
    public Dimension getPreferredSize() {
        LegendDensity density = getDensity();

        // Global density-aware defaults.
        int defPrefW = (density == LegendDensity.DENSE) ? 300 : 240;
        int defPrefH = (density == LegendDensity.DENSE) ? 200 : 160;

        // Side-specific defaults: right/left want width, top/bottom want height.
        int defRightW = (density == LegendDensity.DENSE) ? 320 : 260;
        int defLeftW  = (density == LegendDensity.DENSE) ? 300 : 250;
        int defTopH   = (density == LegendDensity.DENSE) ? 160 : 120;
        int defBottomH= (density == LegendDensity.DENSE) ? 160 : 120;

        int prefW = ChartAssets.getInt(KEY_DOCK_PREF_W, defPrefW);
        int prefH = ChartAssets.getInt(KEY_DOCK_PREF_H, defPrefH);

        // Side-specific overrides (properties win over our defaults)
        if (side == LegendDockSide.RIGHT) {
            prefW = ChartAssets.getInt(KEY_DOCK_PREF_W_RIGHT, defRightW);
        } else if (side == LegendDockSide.LEFT) {
            prefW = ChartAssets.getInt(KEY_DOCK_PREF_W_LEFT, defLeftW);
        } else if (side == LegendDockSide.TOP) {
            prefH = ChartAssets.getInt(KEY_DOCK_PREF_H_TOP, defTopH);
        } else if (side == LegendDockSide.BOTTOM) {
            prefH = ChartAssets.getInt(KEY_DOCK_PREF_H_BOTTOM, defBottomH);
        }

        int w = Math.round(ChartScale.scale(prefW));
        int h = Math.round(ChartScale.scale(prefH));

        Dimension min = getMinimumSize();
        Dimension max = getMaximumSize();

        w = Math.max(min.width, w);
        h = Math.max(min.height, h);

        if (max.width > 0) w = Math.min(max.width, w);
        if (max.height > 0) h = Math.min(max.height, h);

        return new Dimension(w, h);
    }

    @Override
    public Dimension getMaximumSize() {
        int maxW = ChartAssets.getInt(KEY_DOCK_MAX_W, 0);
        int maxH = ChartAssets.getInt(KEY_DOCK_MAX_H, 0);
        // 0 means "no limit" (Swing convention for our policy). Return huge.
        int w = (maxW > 0) ? Math.round(ChartScale.scale(maxW)) : Integer.MAX_VALUE;
        int h = (maxH > 0) ? Math.round(ChartScale.scale(maxH)) : Integer.MAX_VALUE;
        return new Dimension(w, h);
    }
}
