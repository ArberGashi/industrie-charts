package com.arbergashi.charts.demo;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;

/** Keeps the two demo side panels mounted while allocating space to the active chart. */
final class DemoToolWindowShell extends JPanel {
    private final JComponent catalog;
    private final JComponent editor;
    private final JComponent tools;
    private final JPanel leftStripe;
    private final JPanel rightStripe;
    private final JSplitPane catalogSplit;
    private final JSplitPane toolsSplit;
    private final Action catalogAction;
    private final Action toolsAction;
    private final Runnable focusEditor;
    private double catalogWidth = 292;
    private double toolsWidth = 260;
    private boolean toolsOpenedLast;
    private Boolean requestedTools;
    private boolean allocating;

    DemoToolWindowShell(JComponent catalog, JComponent editor, JComponent tools,
                        JPanel leftStripe, JPanel rightStripe, Action catalogAction, Action toolsAction,
                        Runnable focusEditor) {
        super(new BorderLayout());
        this.catalog = catalog;
        this.editor = editor;
        this.tools = tools;
        this.leftStripe = leftStripe;
        this.rightStripe = rightStripe;
        this.catalogAction = catalogAction;
        this.toolsAction = toolsAction;
        this.focusEditor = focusEditor;
        catalogSplit = split("catalogSplit", catalog, editor, 0);
        toolsSplit = split("toolsSplit", catalogSplit, tools, 1);
        catalogSplit.setMinimumSize(new Dimension(0, 0));
        add(leftStripe, BorderLayout.WEST);
        add(toolsSplit, BorderLayout.CENTER);
        add(rightStripe, BorderLayout.EAST);
        catalogSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            if (!allocating && catalog.isVisible() && catalogSplit.getWidth() > 0) {
                catalogWidth = Math.max(0, catalogSplit.getDividerLocation()) / UIScale.getUserScaleFactor();
                revalidate();
            }
        });
        toolsSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            if (!allocating && tools.isVisible() && toolsSplit.getWidth() > 0) {
                toolsWidth = Math.max(0, toolsSplit.getWidth() - UIScale.scale(toolsSplit.getDividerSize())
                        - toolsSplit.getDividerLocation()) / UIScale.getUserScaleFactor();
                revalidate();
            }
        });
        setVisiblePanels(true, false);
    }

    boolean isCatalogVisible() {
        return catalog.isVisible();
    }

    boolean isToolsVisible() {
        return tools.isVisible();
    }

    void setCatalogVisible(boolean visible) {
        if (visible) {
            toolsOpenedLast = false;
            requestedTools = false;
        }
        setVisiblePanels(visible, tools.isVisible());
        revalidate();
        doLayout();
        repaint();
    }

    void setToolsVisible(boolean visible) {
        if (visible) {
            toolsOpenedLast = true;
            requestedTools = true;
        }
        setVisiblePanels(catalog.isVisible(), visible);
        revalidate();
        doLayout();
        repaint();
    }

    @Override
    public void doLayout() {
        if (catalogSplit == null || allocating) return;
        allocating = true;
        try {
            int width = Math.max(0, getWidth() - leftStripe.getPreferredSize().width
                    - rightStripe.getPreferredSize().width - getInsets().left - getInsets().right);
            int divider = UIScale.scale(6);
            int minimumEditor = Math.max(UIScale.scale(420), editor.getLayout().minimumLayoutSize(editor).width);
            int minimumSide = UIScale.scale(160);
            boolean showCatalog = catalog.isVisible();
            boolean showTools = tools.isVisible();
            if (width > 0 && showCatalog && showTools && width < minimumEditor + 2 * minimumSide + 2 * divider) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                boolean preferTools = requestedTools != null ? requestedTools
                        : focus != null && SwingUtilities.isDescendingFrom(focus, catalog)
                        ? false : focus != null && SwingUtilities.isDescendingFrom(focus, tools) || toolsOpenedLast;
                showCatalog = !preferTools;
                showTools = preferTools;
                setVisiblePanels(showCatalog, showTools);
            }
            if (width > 0 && (showCatalog || showTools) && width < minimumEditor + minimumSide + divider) {
                showCatalog = false;
                showTools = false;
                setVisiblePanels(false, false);
            }
            catalog.setMinimumSize(new Dimension(Math.min(minimumSide, width), 0));
            tools.setMinimumSize(new Dimension(Math.min(minimumSide, width), 0));
            editor.setMinimumSize(new Dimension(Math.min(minimumEditor, width), 0));
            // FlatLaf scales the logical divider-size property in its UI delegate.
            catalogSplit.setDividerSize(showCatalog ? 6 : 0);
            toolsSplit.setDividerSize(showTools ? 6 : 0);
            super.doLayout();

            int panelBudget = Math.max(0, width - minimumEditor
                    - (showCatalog ? divider : 0) - (showTools ? divider : 0));
            int left = showCatalog ? UIScale.scale((int) Math.round(catalogWidth)) : 0;
            int right = showTools ? UIScale.scale((int) Math.round(toolsWidth)) : 0;
            if (showCatalog && showTools && left + right > panelBudget) {
                int extra = Math.max(0, panelBudget - 2 * minimumSide);
                int leftExtra = Math.max(0, left - minimumSide);
                int rightExtra = Math.max(0, right - minimumSide);
                left = minimumSide + (int) Math.round(extra * (double) leftExtra / Math.max(1, leftExtra + rightExtra));
                right = panelBudget - left;
            } else if (showCatalog && !showTools) {
                left = Math.min(left, panelBudget);
            } else if (showTools && !showCatalog) {
                right = Math.min(right, panelBudget);
            }
            toolsSplit.setDividerLocation(Math.max(0, width - right - (showTools ? divider : 0)));
            toolsSplit.doLayout();
            catalogSplit.setDividerLocation(left);
            catalogSplit.doLayout();
        } finally {
            requestedTools = null;
            allocating = false;
        }
    }

    void applyPalette(DemoPalette palette) {
        setBackground(palette.contentBackground());
        leftStripe.setBackground(palette.windowBackground());
        rightStripe.setBackground(palette.windowBackground());
        catalogSplit.setBackground(palette.windowBackground());
        toolsSplit.setBackground(palette.windowBackground());
    }

    private void setVisiblePanels(boolean showCatalog, boolean showTools) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean hiddenFocus = focus != null && ((!showCatalog && SwingUtilities.isDescendingFrom(focus, catalog))
                || (!showTools && SwingUtilities.isDescendingFrom(focus, tools)));
        catalog.setVisible(showCatalog);
        tools.setVisible(showTools);
        catalogAction.putValue(Action.SELECTED_KEY, showCatalog);
        toolsAction.putValue(Action.SELECTED_KEY, showTools);
        if (hiddenFocus) SwingUtilities.invokeLater(focusEditor);
    }

    private static JSplitPane split(String name, JComponent left, JComponent right, double resizeWeight) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setName(name);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setResizeWeight(resizeWeight);
        return split;
    }
}
