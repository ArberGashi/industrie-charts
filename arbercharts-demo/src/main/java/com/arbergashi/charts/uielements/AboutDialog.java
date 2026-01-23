package com.arbergashi.charts.uielements;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * About dialog for the ArberCharts Demo application.
 * <p>
 * This dialog is meant for end users and evaluators and explains what the demo shows.
 */
public final class AboutDialog {

    private AboutDialog() {
    }

    /**
     * Shows the About dialog.
     *
     * @param owner parent window
     */
    public static void show(Window owner) {
        JDialog dialog = new JDialog(owner, "About ArberCharts", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        int pad = UIScale.scale(16);
        JPanel root = new JPanel(new BorderLayout(UIScale.scale(14), UIScale.scale(14)));
        root.setBorder(new EmptyBorder(pad, pad, pad, pad));

        // Header
        JPanel headerCard = createCardPanel();
        headerCard.setLayout(new BorderLayout(UIScale.scale(14), 0));

        int iconSize = UIScale.scale(64);
        FlatSVGIcon appIcon = new FlatSVGIcon("icons/appicon.svg", iconSize, iconSize);
        appIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> color));
        JLabel iconLabel = new JLabel(appIcon);
        headerCard.add(iconLabel, BorderLayout.WEST);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel name = new JLabel("ArberCharts");
        name.setFont(name.getFont().deriveFont(Font.BOLD, UIScale.scale(20f)));

        JLabel subtitle = new JLabel("Industry-grade chart rendering platform");
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));

        titles.add(name);
        titles.add(Box.createVerticalStrut(UIScale.scale(2)));
        titles.add(subtitle);

        headerCard.add(titles, BorderLayout.CENTER);

        JPanel versionBlock = new JPanel();
        versionBlock.setOpaque(false);
        versionBlock.setLayout(new BoxLayout(versionBlock, BoxLayout.Y_AXIS));
        JLabel versionLabel = new JLabel("Version");
        versionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        JLabel versionValue = new JLabel(resolveVersion());
        versionValue.setFont(versionValue.getFont().deriveFont(Font.BOLD, UIScale.scale(14f)));
        versionBlock.add(versionLabel);
        versionBlock.add(Box.createVerticalStrut(UIScale.scale(2)));
        versionBlock.add(versionValue);
        headerCard.add(versionBlock, BorderLayout.EAST);

        root.add(headerCard, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(UIScale.scale(14), UIScale.scale(14)));
        content.setOpaque(false);

        JPanel highlightsCard = createCardPanel();
        highlightsCard.setLayout(new BorderLayout(0, UIScale.scale(8)));
        JLabel highlightsTitle = new JLabel("Highlights");
        highlightsTitle.setFont(highlightsTitle.getFont().deriveFont(Font.BOLD, UIScale.scale(13f)));
        highlightsCard.add(highlightsTitle, BorderLayout.NORTH);

        JTextArea highlightsText = new JTextArea(
                "- 139 renderers across standard, analysis, financial, and medical.\n" +
                "- Precision grids, crosshair, legend, and tooltips.\n" +
                "- FlatLaf light/dark themes with HiDPI scaling.\n" +
                "- Zero-GC renderer paths tuned for performance.\n" +
                "- Export-ready vectors and ultra-high resolution outputs."
        );
        highlightsText.setEditable(false);
        highlightsText.setOpaque(false);
        highlightsText.setLineWrap(true);
        highlightsText.setWrapStyleWord(true);
        highlightsText.setBorder(null);
        highlightsText.setFont(UIManager.getFont("Label.font"));
        highlightsCard.add(highlightsText, BorderLayout.CENTER);

        JPanel infoCard = createCardPanel();
        infoCard.setLayout(new BorderLayout(0, UIScale.scale(8)));
        JLabel infoTitle = new JLabel("System");
        infoTitle.setFont(infoTitle.getFont().deriveFont(Font.BOLD, UIScale.scale(13f)));
        infoCard.add(infoTitle, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridBagLayout());
        info.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, UIScale.scale(6), UIScale.scale(10));
        c.anchor = GridBagConstraints.WEST;

        String appVersion = resolveVersion();
        String java = "JDK 25+";
        String os = System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "");
        String theme = FlatLaf.isLafDark() ? "Dark" : "Light";
        String scale = String.format("%.2fx", UIScale.getUserScaleFactor());

        addInfoRow(info, c, "Application", "ArberCharts Demo");
        addInfoRow(info, c, "Framework", "ArberCharts Core");
        addInfoRow(info, c, "Version", appVersion);
        addInfoRow(info, c, "Theme", theme);
        addInfoRow(info, c, "UI Scale", scale);
        addInfoRow(info, c, "Java", java);
        addInfoRow(info, c, "Spring Boot", "Supported (server-side rendering)");
        addInfoRow(info, c, "OS", os.trim());

        infoCard.add(info, BorderLayout.CENTER);

        JPanel cards = new JPanel(new GridLayout(1, 2, UIScale.scale(14), 0));
        cards.setOpaque(false);
        cards.add(highlightsCard);
        cards.add(infoCard);

        content.add(cards, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        // Buttons
        JButton close = new JButton("Close");
        close.addActionListener(__ -> dialog.dispose());

        JLabel footer = new JLabel("Copyright (c) Arber Gashi. All rights reserved.");
        footer.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel footerRow = new JPanel(new BorderLayout());
        footerRow.setOpaque(false);
        footerRow.add(footer, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(close);
        footerRow.add(buttons, BorderLayout.EAST);
        root.add(footerRow, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(close);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(UIScale.scale(640), UIScale.scale(360)));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void addInfoRow(JPanel panel, GridBagConstraints c, String label, String value) {
        JLabel key = new JLabel(label + ":");
        key.setForeground(UIManager.getColor("Label.disabledForeground"));
        GridBagConstraints left = (GridBagConstraints) c.clone();
        left.gridx = 0;
        panel.add(key, left);

        JLabel val = new JLabel(value);
        GridBagConstraints right = (GridBagConstraints) c.clone();
        right.gridx = 1;
        panel.add(val, right);

        c.gridy++;
    }

    private static String resolveVersion() {
        Package pkg = AboutDialog.class.getPackage();
        if (pkg != null) {
            String v = pkg.getImplementationVersion();
            if (v != null && !v.isBlank()) return v;
        }
        return "dev";
    }

    private static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        Color bg = UIManager.getColor("TitlePane.background");
        if (bg == null) bg = UIManager.getColor("Panel.background");
        if (bg == null) bg = panel.getBackground();
        panel.setBackground(bg);

        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = UIManager.getColor("Separator.foreground");
        if (border == null) border = new Color(0, 0, 0, 50);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(UIScale.scale(12), UIScale.scale(12), UIScale.scale(12), UIScale.scale(12))
        ));
        return panel;
    }
}
