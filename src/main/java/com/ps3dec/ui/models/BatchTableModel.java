package com.ps3dec.ui.models;

import com.ps3dec.util.I18n;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.List;

public class BatchTableModel extends AbstractTableModel {
    private final List<BatchItem> items;
    private final List<File> allKeys;

    public BatchTableModel(List<BatchItem> items, List<File> allKeys) {
        this.items = items;
        this.allKeys = allKeys;
    }

    @Override
    public int getRowCount() { return items.size(); }

    @Override
    public int getColumnCount() { return 3; }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0: return I18n.get("table.col.file");
            case 1: return I18n.get("label.dkey");
            case 2: return I18n.get("table.col.status");
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1; // Só a chave é editável no dropdown
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        BatchItem item = items.get(rowIndex);
        switch (columnIndex) {
            case 0: return item.getIsoFile().getName();
            case 1: return item.getKeyFile() != null ? item.getKeyFile().getName() : "...";
            case 2: return item.getStatusText(); 
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 1 && aValue instanceof String) {
            String selectedKeyName = (String) aValue;
            for (File key : allKeys) {
                if (key.getName().equals(selectedKeyName)) {
                    items.get(rowIndex).setKeyFile(key);
                    updateRowStatus(rowIndex);
                    fireTableCellUpdated(rowIndex, columnIndex);
                    break;
                }
            }
        }
    }

    public void updateRowStatus(int rowIndex) {
        BatchItem item = items.get(rowIndex);
        if (item.getStatus().equals("DONE") || item.getStatus().equals("ERROR")) return;
        
        if (item.getKeyFile() == null) {
            item.setStatus("PENDING");
        } else {
            item.setStatus("READY");
        }
        fireTableCellUpdated(rowIndex, 2);
    }
    
    public BatchItem getItem(int row) {
        return items.get(row);
    }
    
    public List<BatchItem> getItems() {
        return items;
    }

    public List<File> getAllKeys() {
        return allKeys;
    }
}
