package com.arbergashi.charts.uielements;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NavigationTree extends JTree {

    private final List<NavItem> allNavItems = new ArrayList<>();

    public NavigationTree() {
        setModel(createTreeModel());

        setRootVisible(false);
        setShowsRootHandles(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(new NavigationTreeCellRenderer());
        int pad = UIScale.scale(5);
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        // Expand all rows initially
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    public List<NavItem> getAllNavItems() {
        return allNavItems;
    }


    @Override
    public void updateUI() {
        super.updateUI();
        TreeCellRenderer renderer = getCellRenderer();
        if (renderer instanceof NavigationTreeCellRenderer) {
            ((NavigationTreeCellRenderer) renderer).clearCache();
        }
    }

    private DefaultMutableTreeNode createLeaf(String title, String icon) {
        NavItem item = new NavItem(title, icon);
        allNavItems.add(item);
        return new DefaultMutableTreeNode(item);
    }

    private DefaultTreeModel createTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        allNavItems.clear();

        // Standard Charts
        DefaultMutableTreeNode standardNode = new DefaultMutableTreeNode(new NavItem("Standard", "icons/standard.svg"));
        standardNode.add(createLeaf("Line Chart", "icons/line.svg"));
        standardNode.add(createLeaf("Bar Chart", "icons/bar.svg"));
        standardNode.add(createLeaf("Stacked Bar", "icons/bar.svg"));
        standardNode.add(createLeaf("Grouped Bar", "icons/bar.svg"));
        standardNode.add(createLeaf("Area Chart", "icons/area.svg"));
        standardNode.add(createLeaf("Step Area", "icons/area.svg"));
        standardNode.add(createLeaf("Baseline Area", "icons/area.svg"));
        standardNode.add(createLeaf("Range Area", "icons/area.svg"));
        standardNode.add(createLeaf("Scatter Plot", "icons/scatter.svg"));
        standardNode.add(createLeaf("Bubble Chart", "icons/bubble.svg"));
        root.add(standardNode);

        // Circular Charts
        DefaultMutableTreeNode circularNode = new DefaultMutableTreeNode(new NavItem("Circular", "icons/pie.svg"));
        circularNode.add(createLeaf("Pie Chart", "icons/pie.svg"));
        circularNode.add(createLeaf("Donut Chart", "icons/donut.svg"));
        circularNode.add(createLeaf("Semi Donut", "icons/donut.svg"));
        circularNode.add(createLeaf("Polar Chart", "icons/polar.svg"));
        circularNode.add(createLeaf("Polar Line", "icons/polar.svg"));
        circularNode.add(createLeaf("Radar Chart", "icons/radar.svg"));
        circularNode.add(createLeaf("Nightingale Rose", "icons/nightingale_rose.svg"));
        circularNode.add(createLeaf("Radial Bar", "icons/radial_bar.svg"));
        circularNode.add(createLeaf("Radial Stacked", "icons/radial_bar.svg"));
        circularNode.add(createLeaf("Gauge", "icons/gauge.svg"));
        circularNode.add(createLeaf("Gauge Bands", "icons/gauge.svg"));
        root.add(circularNode);

        // Financial Charts
        DefaultMutableTreeNode financialNode = new DefaultMutableTreeNode(new NavItem("Financial", "icons/financial.svg"));
        financialNode.add(createLeaf("Candlestick", "icons/candlestick.svg"));
        financialNode.add(createLeaf("Hollow Candlestick", "icons/candlestick_hollow.svg"));
        financialNode.add(createLeaf("High Low", "icons/highlow.svg"));
        financialNode.add(createLeaf("Heikin Ashi", "icons/heikinashi.svg"));
        financialNode.add(createLeaf("Renko", "icons/renko.svg"));
        financialNode.add(createLeaf("Waterfall", "icons/waterfall.svg"));
        financialNode.add(createLeaf("Kagi", "icons/kagi.svg"));
        financialNode.add(createLeaf("Point & Figure", "icons/pointandfigure.svg"));
        financialNode.add(createLeaf("Volume", "icons/bar.svg"));
        // Indicators
        financialNode.add(createLeaf("MACD", "icons/financial.svg"));
        financialNode.add(createLeaf("Stochastic", "icons/financial.svg"));
        financialNode.add(createLeaf("ADX", "icons/financial.svg"));
        financialNode.add(createLeaf("ATR", "icons/financial.svg"));
        financialNode.add(createLeaf("Bollinger Bands", "icons/bollinger.svg"));
        financialNode.add(createLeaf("Parabolic SAR", "icons/financial.svg"));
        financialNode.add(createLeaf("Ichimoku", "icons/financial.svg"));
        financialNode.add(createLeaf("Fibonacci", "icons/fibonacci.svg"));
        financialNode.add(createLeaf("Pivot Points", "icons/financial.svg"));
        financialNode.add(createLeaf("OBV", "icons/obv.svg"));
        root.add(financialNode);

        // Medical Charts
        DefaultMutableTreeNode medicalNode = new DefaultMutableTreeNode(new NavItem("Medical", "icons/medical.svg"));
        medicalNode.add(createLeaf("ECG", "icons/ecg.svg"));
        medicalNode.add(createLeaf("EEG", "icons/eeg.svg"));
        medicalNode.add(createLeaf("EMG", "icons/emg.svg"));
        medicalNode.add(createLeaf("PPG", "icons/ppg.svg"));
        medicalNode.add(createLeaf("Spirometry", "icons/spirometry.svg"));
        medicalNode.add(createLeaf("Capnography", "icons/capnography.svg"));
        medicalNode.add(createLeaf("NIRS", "icons/nirs.svg"));
        medicalNode.add(createLeaf("Ventilator", "icons/ventilator_waveform.svg"));
        medicalNode.add(createLeaf("IBP", "icons/ibp.svg"));
        medicalNode.add(createLeaf("Ultrasound M-Mode", "icons/ultrasound_m-mode.svg"));
        medicalNode.add(createLeaf("VCG", "icons/vcg.svg"));
        medicalNode.add(createLeaf("EOG", "icons/eog.svg"));
        medicalNode.add(createLeaf("Medical Sweep", "icons/medical.svg"));
        medicalNode.add(createLeaf("Sweep EKG", "icons/sweep_erase_ekg.svg"));
        medicalNode.add(createLeaf("Heart Rate Variability", "icons/ecg.svg"));
        root.add(medicalNode);

        // Statistical Charts
        DefaultMutableTreeNode statisticalNode = new DefaultMutableTreeNode(new NavItem("Statistical", "icons/statistical.svg"));
        statisticalNode.add(createLeaf("Box Plot", "icons/boxplot.svg"));
        statisticalNode.add(createLeaf("Violin Plot", "icons/violin.svg"));
        statisticalNode.add(createLeaf("Histogram", "icons/histogram.svg"));
        statisticalNode.add(createLeaf("KDE", "icons/statistical.svg"));
        statisticalNode.add(createLeaf("QQ Plot", "icons/statistical.svg"));
        statisticalNode.add(createLeaf("ECDF", "icons/ecdf.svg"));
        statisticalNode.add(createLeaf("Error Bar", "icons/errorbar.svg"));
        statisticalNode.add(createLeaf("Statistical Error Bar", "icons/errorbar.svg"));
        statisticalNode.add(createLeaf("Confidence Interval", "icons/confidence.svg"));
        statisticalNode.add(createLeaf("Band", "icons/band.svg"));
        statisticalNode.add(createLeaf("Ridge Line", "icons/ridgeline.svg"));
        statisticalNode.add(createLeaf("Hexbin", "icons/hexbin.svg"));
        root.add(statisticalNode);

        // Specialized Charts
        DefaultMutableTreeNode specializedNode = new DefaultMutableTreeNode(new NavItem("Specialized", "icons/specialized.svg"));
        specializedNode.add(createLeaf("Sunburst", "icons/sunburst.svg"));
        specializedNode.add(createLeaf("Sankey", "icons/sankey.svg"));
        specializedNode.add(createLeaf("Chord Diagram", "icons/chord.svg"));
        specializedNode.add(createLeaf("Chernoff Faces", "icons/chernoff.svg"));
        specializedNode.add(createLeaf("Joyplot", "icons/joyplot.svg"));
        specializedNode.add(createLeaf("Lollipop", "icons/lollipop.svg"));
        specializedNode.add(createLeaf("Heatmap", "icons/heatmap.svg"));
        specializedNode.add(createLeaf("Streamgraph", "icons/streamgraph.svg"));
        specializedNode.add(createLeaf("Voronoi", "icons/voronoi.svg"));
        specializedNode.add(createLeaf("Delaunay", "icons/delaunay.svg"));
        specializedNode.add(createLeaf("Dependency Wheel", "icons/dependency.svg"));
        specializedNode.add(createLeaf("Parallel Coordinates", "icons/parallel.svg"));
        specializedNode.add(createLeaf("Marimekko", "icons/specialized.svg"));
        specializedNode.add(createLeaf("Alluvial", "icons/alluvial.svg"));
        specializedNode.add(createLeaf("Wind Rose", "icons/wind_rose.svg"));
        specializedNode.add(createLeaf("Bullet Chart", "icons/bullet.svg"));
        specializedNode.add(createLeaf("Network", "icons/network.svg"));
        specializedNode.add(createLeaf("Arc Diagram", "icons/arc.svg"));
        specializedNode.add(createLeaf("Dendrogram", "icons/dendrogram.svg"));
        specializedNode.add(createLeaf("Pareto", "icons/pareto.svg"));
        specializedNode.add(createLeaf("Ternary Phase", "icons/ternary.svg"));
        specializedNode.add(createLeaf("Ternary Contour", "icons/ternarycontour.svg"));
        specializedNode.add(createLeaf("Gantt", "icons/gantt.svg"));
        specializedNode.add(createLeaf("Gantt Resource", "icons/gantt.svg"));
        specializedNode.add(createLeaf("Control Chart", "icons/controlchart.svg"));
        specializedNode.add(createLeaf("Horizon", "icons/horizon.svg"));
        root.add(specializedNode);

        // Analysis Charts
        DefaultMutableTreeNode analysisNode = new DefaultMutableTreeNode(new NavItem("Analysis", "icons/analysis.svg"));
        analysisNode.add(createLeaf("FFT", "icons/fft.svg"));
        analysisNode.add(createLeaf("Spectrogram", "icons/spectrogram.svg"));
        analysisNode.add(createLeaf("Vector Field", "icons/vector_field.svg"));
        analysisNode.add(createLeaf("Regression", "icons/regression_line.svg"));
        analysisNode.add(createLeaf("Polynomial Regression", "icons/polynomial_regression.svg"));
        analysisNode.add(createLeaf("Autocorrelation", "icons/autocorrelation.svg"));
        analysisNode.add(createLeaf("Change Point", "icons/change_point.svg"));
        analysisNode.add(createLeaf("Outlier Detection", "icons/outlier_detection.svg"));
        analysisNode.add(createLeaf("Slope", "icons/slope.svg"));
        analysisNode.add(createLeaf("Adaptive Function", "icons/adaptive_function.svg"));
        root.add(analysisNode);

        return new DefaultTreeModel(root);
    }

    public static class NavItem {
        private final String title;
        private final String iconPath;

        public NavItem(String title, String iconPath) {
            this.title = title;
            this.iconPath = iconPath;
        }

        public String getTitle() {
            return title;
        }

        // Return the relative classpath resource path as used originally (no leading slash)
        public String getIconPath() {
            return iconPath;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private static class NavigationTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final Icon FALLBACK_ICON = new Icon() {
            private final int size = UIScale.scale(16);

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                // Draw a subtle placeholder matching current foreground
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(UIManager.getColor("Label.foreground"));
                    g2.fillOval(x, y, size, size);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };

        /**
         * Cache tinted icons per (path + rgb + size) to avoid re-parsing SVGs every repaint.
         * Cleared automatically on Look&Feel changes via {@link #clearCache()}.
         */
        private final Map<String, Icon> iconCache = new ConcurrentHashMap<>();

        public void clearCache() {
            iconCache.clear();
        }

        private static Color iconTint(JTree tree, boolean selected) {
            // Prefer the actual Tree text foreground to match FlatLaf selection/disabled colors.
            if (selected) {
                Color c = UIManager.getColor("Tree.selectionForeground");
                if (c != null) return c;
            }
            Color fg = tree.getForeground();
            if (fg != null) return fg;
            Color labelFg = UIManager.getColor("Label.foreground");
            return labelFg != null ? labelFg : Color.BLACK;
        }

        private Icon loadTintedIcon(JTree tree, String path, boolean selected) {
            int s = UIScale.scale(16);
            Color tint = iconTint(tree, selected);
            int rgb = tint.getRGB();
            String key = path + "|" + s + "|" + rgb;

            return iconCache.computeIfAbsent(key, __ -> createTintedIcon(path, s, tint));
        }

        private static Icon createTintedIcon(String path, int size, Color tint) {
            try {
                FlatSVGIcon svg = new FlatSVGIcon(path, size, size);
                svg.setColorFilter(new FlatSVGIcon.ColorFilter(color -> tint));
                return svg;
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof NavItem item) {
                    setText(item.getTitle());

                    Icon icon = loadTintedIcon(tree, item.getIconPath(), sel);
                    setIcon(icon != null ? icon : FALLBACK_ICON);
                    if (!leaf) {
                        Font base = UIManager.getFont("Label.font");
                        if (base == null) base = getFont();
                        float size = base.getSize2D();
                        Font inter = new Font("Inter Medium", Font.PLAIN, Math.round(size));
                        setFont(inter.deriveFont(Font.PLAIN, size));
                    }
                }
            }
            return this;
        }
    }
}
