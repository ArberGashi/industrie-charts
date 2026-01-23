package com.arbergashi.charts.uielements;

import com.arbergashi.charts.ui.ArberChartPanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PropertyGrid extends JPanel {
    private final JTable table;
    private final DefaultTableModel model;
    private ArberChartPanel targetPanel;

    public PropertyGrid() {
        setLayout(new BorderLayout());
        
        String[] columnNames = {"Property", "Value"};
        model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        
        table = new JTable(model);
        table.setRowHeight(24);
        table.setGridColor(UIManager.getColor("Component.borderColor"));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && targetPanel != null) {
                int row = e.getFirstRow();
                String key = (String) model.getValueAt(row, 0);
                Object value = model.getValueAt(row, 1);
                applyProperty(key, value);
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        
        JLabel titleLabel = new JLabel("Chart Properties");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        add(titleLabel, BorderLayout.NORTH);
        
        setPreferredSize(new Dimension(250, 0));
    }

    public void setTargetPanel(ArberChartPanel panel) {
        this.targetPanel = panel;
    }

    private void applyProperty(String key, Object value) {
        if (targetPanel == null) return;
        
        try {
            switch (key) {
                case "Legend" -> targetPanel.withLegend("Visible".equalsIgnoreCase(value.toString()));
                case "Tooltips" -> targetPanel.withTooltips("Enabled".equalsIgnoreCase(value.toString()));
                case "Background" -> {
                     // Simple color hex support could be added here
                }
            }
            targetPanel.repaint();
        } catch (Exception ignored) {
            // Property change failed - ignore silently
        }
    }

    public void addProperty(String name, String value) {
        model.addRow(new Object[]{name, value});
    }

    public void clear() {
        model.setRowCount(0);
    }
}
