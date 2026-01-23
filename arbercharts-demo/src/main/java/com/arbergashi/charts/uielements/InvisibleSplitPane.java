package com.arbergashi.charts.uielements;

import javax.swing.*;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.Serial;

/**
 * @author Arber Gashi
 * @version 1.0
 * @since 2024-01-28
 */
public class InvisibleSplitPane extends JSplitPane {

    @Serial
    private static final long serialVersionUID = -4035902767549566260L;

    public InvisibleSplitPane() {
        super();
    }

    public InvisibleSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent,
                              Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
    }

    public InvisibleSplitPane(int newOrientation, boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
    }

    public InvisibleSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
    }

    @Override
    public void updateUI() {
        SplitPaneUI ui = new InvisibleSplitPaneUI();
        setUI(ui);
        revalidate();
    }

    private static class InvisibleSplitPaneUI extends BasicSplitPaneUI {

        public InvisibleSplitPaneUI() {
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            splitPane.setBorder(null);
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            BasicSplitPaneDivider d = new BasicSplitPaneDivider(this) {
                @Serial
                private static final long serialVersionUID = 225334791139486944L;

                @Override
                public void paint(Graphics g) {
                    // Empty paint to make it invisible
                }
            };
            d.setBorder(null);
            return d;
        }
    }
}
