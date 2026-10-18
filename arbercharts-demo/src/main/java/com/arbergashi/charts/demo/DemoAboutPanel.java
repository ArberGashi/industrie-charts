package com.arbergashi.charts.demo;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** About content shared by the desktop dialog and offscreen layout checks. */
final class DemoAboutPanel extends JPanel {
    private final JButton closeButton = new JButton("Close");

    DemoAboutPanel(String applicationName, String version, String copyright, int rendererCount,
                   boolean vectorAvailable, Icon icon, Runnable closeAction) {
        super(new BorderLayout());
        DemoPalette palette = DemoThemeSupport.currentPalette();
        setBackground(palette.windowBackground());

        JPanel identity = new JPanel(new BorderLayout(UIScale.scale(16), 0));
        identity.setOpaque(false);
        identity.setBorder(BorderFactory.createEmptyBorder(
                UIScale.scale(24), UIScale.scale(24), UIScale.scale(24), UIScale.scale(24)));
        JLabel mark = new JLabel(icon);
        mark.setVerticalAlignment(JLabel.TOP);
        identity.add(mark, BorderLayout.WEST);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        JLabel title = new JLabel(applicationName);
        title.setFont(title.getFont().deriveFont(Font.BOLD, UIScale.scale(24f)));
        title.setForeground(palette.foreground());
        heading.add(title);
        heading.add(Box.createVerticalStrut(UIScale.scale(4)));
        JLabel description = new JLabel("Chart workbench for Swing");
        description.setForeground(palette.muted());
        heading.add(description);
        heading.add(Box.createVerticalStrut(UIScale.scale(12)));
        JLabel release = new JLabel("Version " + version);
        release.setForeground(palette.accent());
        release.setFont(release.getFont().deriveFont(Font.BOLD));
        heading.add(release);
        identity.add(heading, BorderLayout.CENTER);
        add(identity, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(palette.surfaceBackground());
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, palette.border()),
                BorderFactory.createEmptyBorder(UIScale.scale(16), UIScale.scale(24),
                        UIScale.scale(16), UIScale.scale(24))));
        addDetail(details, 0, "Catalog", rendererCount + " renderers", palette);
        addDetail(details, 1, "Java", System.getProperty("java.version", "Unknown"), palette);
        addDetail(details, 2, "Runtime vendor", System.getProperty("java.vendor", "Unknown"), palette);
        addDetail(details, 3, "System", System.getProperty("os.name", "Unknown") + " "
                + System.getProperty("os.version", "") + " (" + System.getProperty("os.arch", "Unknown") + ")",
                palette);
        addDetail(details, 4, "Vector API", vectorAvailable ? "Available" : "Scalar fallback", palette);
        add(details, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(UIScale.scale(24), 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(
                UIScale.scale(16), UIScale.scale(24), UIScale.scale(24), UIScale.scale(24)));
        JPanel attribution = new JPanel();
        attribution.setLayout(new BoxLayout(attribution, BoxLayout.Y_AXIS));
        attribution.setOpaque(false);
        JLabel license = new JLabel("MIT License");
        license.setForeground(palette.foreground());
        attribution.add(license);
        attribution.add(Box.createVerticalStrut(UIScale.scale(4)));
        JLabel author = new JLabel(copyright);
        author.setFont(author.getFont().deriveFont(UIScale.scale(11f)));
        author.setForeground(palette.muted());
        attribution.add(author);
        footer.add(attribution, BorderLayout.CENTER);
        closeButton.putClientProperty(FlatClientProperties.STYLE, "minimumWidth: 90; margin: 8,16,8,16");
        closeButton.addActionListener(event -> closeAction.run());
        JPanel close = new JPanel();
        close.setLayout(new BoxLayout(close, BoxLayout.Y_AXIS));
        close.setOpaque(false);
        close.add(Box.createVerticalGlue());
        close.add(closeButton);
        close.add(Box.createVerticalGlue());
        footer.add(close, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    JButton closeButton() {
        return closeButton;
    }

    private static void addDetail(JPanel details, int row, String name, String value, DemoPalette palette) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(row == 0 ? 0 : UIScale.scale(12), 0, 0, UIScale.scale(24));
        JLabel label = new JLabel(name);
        label.setForeground(palette.muted());
        details.add(label, constraints);

        JLabel detail = new JLabel(value);
        detail.setForeground(palette.foreground());
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.insets.right = 0;
        details.add(detail, constraints);
    }
}
