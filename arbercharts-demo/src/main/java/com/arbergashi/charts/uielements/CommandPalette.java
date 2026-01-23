package com.arbergashi.charts.uielements;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class CommandPalette extends JDialog {

    private final JTextField searchField;
    private final JList<NavigationTree.NavItem> resultList;
    private final DefaultListModel<NavigationTree.NavItem> listModel;
    private final List<NavigationTree.NavItem> allItems;
    private final Consumer<NavigationTree.NavItem> onSelected;

    public CommandPalette(Window owner, List<NavigationTree.NavItem> items, Consumer<NavigationTree.NavItem> onSelected) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.allItems = items;
        this.onSelected = onSelected;

        setUndecorated(true);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 350));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search Chart (Arrow keys to navigate, Enter to select)");
        searchField.putClientProperty("JTextField.showClearButton", true);
        FlatSVGIcon searchIcon = new FlatSVGIcon("icons/search.svg", UIScale.scale(16), UIScale.scale(16));
        Color fg = UIManager.getColor("TextField.foreground");
        if (fg != null) {
            searchIcon.setColorFilter(new FlatSVGIcon.ColorFilter(_ -> fg));
        }
        searchField.putClientProperty("JTextField.leadingIcon", searchIcon);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                new EmptyBorder(10, 10, 10, 10)
        ));
        searchField.setFont(searchField.getFont().deriveFont(16f));

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setCellRenderer(new PaletteCellRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);

        add(searchField, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Styling
        getRootPane().setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));

        initListeners();
        filter("");
        
        pack();
        setLocationRelativeTo(owner);
    }

    private void initListeners() {
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    resultList.setSelectedIndex(Math.min(resultList.getSelectedIndex() + 1, listModel.size() - 1));
                    resultList.ensureIndexIsVisible(resultList.getSelectedIndex());
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    resultList.setSelectedIndex(Math.max(resultList.getSelectedIndex() - 1, 0));
                    resultList.ensureIndexIsVisible(resultList.getSelectedIndex());
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectCurrent();
                } else {
                    filter(searchField.getText());
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCurrent();
                }
            }
        });
    }

    private void filter(String query) {
        listModel.clear();
        String lowerQuery = query.toLowerCase();
        for (NavigationTree.NavItem item : allItems) {
            if (item.getTitle().toLowerCase().contains(lowerQuery)) {
                listModel.addElement(item);
            }
        }
        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
    }

    private void selectCurrent() {
        NavigationTree.NavItem selected = resultList.getSelectedValue();
        if (selected != null) {
            onSelected.accept(selected);
            dispose();
        }
    }

    public static void show(Window owner, List<NavigationTree.NavItem> items, Consumer<NavigationTree.NavItem> onSelected) {
        CommandPalette palette = new CommandPalette(owner, items, onSelected);
        palette.setVisible(true);
    }

    private static class PaletteCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof NavigationTree.NavItem item) {
                label.setText(item.getTitle());
                if (item.getIconPath() != null) {
                    int s = UIScale.scale(16);
                    FlatSVGIcon icon = new FlatSVGIcon(item.getIconPath(), s, s);
                    Color fg = isSelected ? list.getSelectionForeground() : list.getForeground();
                    if (fg != null) {
                        icon.setColorFilter(new FlatSVGIcon.ColorFilter(_ -> fg));
                    }
                    label.setIcon(icon);
                }
                label.setBorder(new EmptyBorder(UIScale.scale(5), UIScale.scale(10), UIScale.scale(5), UIScale.scale(10)));
            }
            return label;
        }
    }
}
