package com.ps3dec.ui.panels;

import com.ps3dec.service.DecryptService;
import com.ps3dec.service.DkeySearchService;
import com.ps3dec.model.DkeyResult;
import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.ui.dialogs.ProgressDialog;
import com.ps3dec.ui.models.BatchItem;
import com.ps3dec.ui.models.BatchTableModel;
import com.ps3dec.ui.util.DndHelper;
import com.ps3dec.ui.util.UIUtils;
import com.ps3dec.util.AppPreferences;
import com.ps3dec.util.I18n;
import com.ps3dec.util.Theme;
import com.ps3dec.util.OSUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for the Batch ISO conversion tab.
 */
public class BatchTabPanel extends JPanel {

    private final JFrame parent;
    private final LogPanel logPanel;
    private final DecryptService decryptService;
    private final DkeySearchService dkeySearchService;

    private JLabel labelBatchFolder, labelBatchDest;
    private JTextField folderField, batchOutputField;
    private JTable batchTable;
    private BatchTableModel batchTableModel;
    private JButton btnStartBatch;

    public BatchTabPanel(JFrame parent, LogPanel logPanel) {
        this.parent = parent;
        this.logPanel = logPanel;
        this.decryptService = new DecryptService();
        this.dkeySearchService = new DkeySearchService();

        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        initComponents();
        setupDragAndDrop();
    }

    private void initComponents() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Source Folder
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        labelBatchFolder = UIFactory.createLabel(I18n.get("label.batch_folder"));
        controls.add(labelBatchFolder, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        folderField = UIFactory.createTextField(I18n.get("placeholder.batch"));
        folderField.setEditable(false);
        String savedBatch = AppPreferences.getBatchDir();
        if (!savedBatch.isEmpty()) {
            folderField.setText(savedBatch);
            File savedDir = new File(savedBatch);
            if (savedDir.isDirectory()) {
                SwingUtilities.invokeLater(() -> scanFolderForBatch(savedDir));
            }
        }
        controls.add(folderField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        controls.add(UIFactory.createBrowseButton(e -> {
            File folder = UIUtils.showFolderChooser(parent, I18n.get("label.batch_folder"));
            if (folder != null) {
                folderField.setText(folder.getAbsolutePath());
                AppPreferences.setBatchDir(folder.getAbsolutePath());
                if (batchOutputField.getText().trim().isEmpty()) {
                    batchOutputField.setText(folder.getAbsolutePath());
                }
                scanFolderForBatch(folder);
            }
        }), gbc);

        // Destination Folder
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        labelBatchDest = UIFactory.createLabel(I18n.get("label.dest_folder"));
        controls.add(labelBatchDest, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        batchOutputField = UIFactory.createTextField(I18n.get("placeholder.dest"));
        batchOutputField.setText(AppPreferences.getOutputDir());
        controls.add(batchOutputField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        controls.add(UIFactory.createBrowseButton(e -> {
            File folder = UIUtils.showFolderChooser(parent, I18n.get("label.dest_folder"));
            if (folder != null) {
                batchOutputField.setText(folder.getAbsolutePath());
                AppPreferences.setOutputDir(folder.getAbsolutePath());
            }
        }), gbc);

        add(controls, BorderLayout.NORTH);

        // Batch table
        batchTable = new JTable();
        batchTable.setRowHeight(30);
        batchTable.setBackground(Theme.BG_FIELD);
        batchTable.setForeground(Theme.TEXT_PRIMARY);
        batchTable.getTableHeader().setBackground(new Color(55, 57, 72));
        batchTable.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        batchTable.getTableHeader().setFont(new Font(Font.DIALOG, Font.BOLD, 12));

        batchTableModel = new BatchTableModel(new ArrayList<>(), new ArrayList<>());
        batchTable.setModel(batchTableModel);

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        btnStartBatch = UIFactory.createActionButton(I18n.get("btn.start_batch"), Color.WHITE, Theme.ACCENT, e -> runBatchDecrypt());
        btnStartBatch.setPreferredSize(null);
        btnStartBatch.setMargin(new Insets(0, 24, 0, 24));
        bottom.add(btnStartBatch);
        add(bottom, BorderLayout.SOUTH);
    }

    private void setupDragAndDrop() {
        DndHelper.setup(folderField, f -> {
            File folder = f.isDirectory() ? f : f.getParentFile();
            folderField.setText(folder.getAbsolutePath());
            AppPreferences.setBatchDir(folder.getAbsolutePath());
            if (batchOutputField.getText().trim().isEmpty()) {
                batchOutputField.setText(folder.getAbsolutePath());
            }
            scanFolderForBatch(folder);
        });
        DndHelper.setup(batchOutputField, f -> batchOutputField.setText(f.getAbsolutePath()));
    }

    private void scanFolderForBatch(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        List<File> isos = new ArrayList<>();
        List<File> keys = new ArrayList<>();

        for (File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".iso")) {
                    isos.add(f);
                } else if (name.endsWith(".key") || name.endsWith(".dkey") || name.endsWith(".rap")) {
                    keys.add(f);
                }
            }
        }

        List<BatchItem> items = new ArrayList<>();
        List<BatchItem> missingItems = new ArrayList<>();
        for (File iso : isos) {
            String isoName = iso.getName();
            String isoBase = isoName.substring(0, isoName.lastIndexOf('.'));

            File matchedKey = null;
            for (File key : keys) {
                String keyName = key.getName();
                String keyBase = keyName.contains(".") ? keyName.substring(0, keyName.lastIndexOf('.')) : keyName;
                if (keyBase.equalsIgnoreCase(isoBase)) {
                    matchedKey = key;
                    break;
                }
            }

            String status = matchedKey != null ? "READY" : "PENDING";
            BatchItem item = new BatchItem(iso, matchedKey, status);
            items.add(item);
            if (matchedKey == null) {
                missingItems.add(item);
            }
        }

        batchTableModel = new BatchTableModel(items, keys);
        batchTable.setModel(batchTableModel);
        setupTableRenderers(keys);

        if (!missingItems.isEmpty()) {
            searchMissingKeysOnline(missingItems);
        }
    }

    private void setupTableRenderers(List<File> keys) {
        if (!keys.isEmpty()) {
            JComboBox<String> keyCombo = new JComboBox<>();
            keyCombo.addItem(I18n.get("batch.key_select"));
            for (File k : keys) keyCombo.addItem(k.getName());
            batchTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(keyCombo));
        }

        final String keyMissing     = I18n.get("item.status.not_found_online");
        final String keyError       = I18n.get("item.status.error");
        final String keyDone        = I18n.get("item.status.done");
        final String keyReady       = I18n.get("item.status.pending"); // Map back from getStatusText()
        final String keyProcessing  = I18n.get("item.status.decrypting");

        batchTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if (status.equals(keyMissing) || status.startsWith(keyError))
                    c.setForeground(new Color(255, 100, 100));
                else if (status.equals(keyDone) || status.contains("READY") || status.equals(keyReady))
                    c.setForeground(new Color(100, 255, 100));
                else if (status.equals(keyProcessing) || status.contains("SEARCH"))
                    c.setForeground(Theme.ACCENT);
                else
                    c.setForeground(Theme.TEXT_PRIMARY);
                return c;
            }
        });
    }

    private void searchMissingKeysOnline(List<BatchItem> missingItems) {
        new SwingWorker<Void, BatchItem>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (BatchItem item : missingItems) {
                    item.setStatus("ONLINE_SEARCH");
                    publish(item);

                    String titleId = decryptService.extractGameIdFromFileName(item.getIsoFile().getAbsolutePath());
                    if (titleId.isEmpty()) {
                        item.setStatus("NOT_FOUND");
                        publish(item);
                        continue;
                    }

                    try {
                        DkeyResult res = dkeySearchService.fetchAndParseSync(titleId);
                        if (res != null) {
                            File tempKey = File.createTempFile(titleId + "_", ".dkey");
                            tempKey.deleteOnExit();
                            try (FileOutputStream fos = new FileOutputStream(tempKey)) {
                                fos.write(res.getDkeyHex().getBytes(StandardCharsets.US_ASCII));
                            }
                            item.setKeyFile(tempKey);
                            item.setStatus("READY_ONLINE");
                            logPanel.log("\uD83D\uDD11 DKEY online (batch): " + titleId + " \u2192 " + res.getDkeyHex());
                        } else {
                            item.setStatus("NOT_FOUND");
                        }
                    } catch (Exception e) {
                        item.setStatus("SEARCH_ERROR");
                    }
                    publish(item);
                    Thread.sleep(1000);
                }
                return null;
            }

            @Override
            protected void process(List<BatchItem> chunks) {
                if (batchTableModel != null) batchTableModel.fireTableDataChanged();
            }
        }.execute();
    }

    private void runBatchDecrypt() {
        String outputBase = batchOutputField.getText().trim();
        if (outputBase.isEmpty()) outputBase = folderField.getText().trim();

        List<BatchItem> pending = new ArrayList<>();
        for (BatchItem item : batchTableModel.getItems()) {
            if (item.getKeyFile() != null && !item.getStatus().equals("DONE")) {
                pending.add(item);
            }
        }

        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(parent, I18n.get("batch.status.missing_key"), I18n.get("alert.error"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        processNextBatchItem(pending, 0, outputBase);
    }

    private void processNextBatchItem(List<BatchItem> queue, int index, String outputBase) {
        if (index >= queue.size()) {
            logPanel.log("\u2714 " + I18n.get("batch.status.done"));
            return;
        }

        BatchItem item = queue.get(index);
        item.setStatus("DECRYPTING");
        batchTableModel.fireTableDataChanged();

        String ps3decBin = OSUtils.getPs3decPath();
        String isoPath = item.getIsoFile().getAbsolutePath();
        String dkey = item.getKeyFile().getAbsolutePath();
        String isoName = item.getIsoFile().getName();
        String stemName = isoName.contains(".") ? isoName.substring(0, isoName.lastIndexOf('.')) : isoName;
        String outputFileName = stemName + "_decrypted.iso";

        ProgressDialog progress = new ProgressDialog(parent, isoName, item.getIsoFile().length(), () -> decryptService.cancel());

        decryptService.startDecryption(ps3decBin, isoPath, dkey, outputBase, outputFileName, 
            new DecryptService.DecryptListener() {
                @Override
                public void onProgress(int targetPct, String statusText, String elapsedStr) {
                    progress.updateProgress(targetPct, statusText, elapsedStr);
                }
                @Override
                public void onSuccess(String outputFilePath) {
                    item.setStatus("DONE");
                    progress.finishWithAnimation(() -> {
                        SwingUtilities.invokeLater(() -> processNextBatchItem(queue, index + 1, outputBase));
                    });
                }
                @Override
                public void onError(String errorMsg) {
                    item.setStatus("ERROR");
                    progress.dispose();
                    SwingUtilities.invokeLater(() -> processNextBatchItem(queue, index + 1, outputBase));
                }
                @Override
                public void onFinish() {
                    batchTableModel.fireTableDataChanged();
                }
            }, 
            line -> logPanel.log("  " + line)
        );

        progress.setVisible(true);
    }

    public void updateLocalizedText() {
        if (labelBatchFolder != null) labelBatchFolder.setText(I18n.get("label.batch_folder"));
        if (labelBatchDest != null) labelBatchDest.setText(I18n.get("label.dest_folder"));
        if (btnStartBatch != null) btnStartBatch.setText(I18n.get("btn.start_batch"));

        UIUtils.setPlaceholder(folderField, I18n.get("placeholder.batch"));
        UIUtils.setPlaceholder(batchOutputField, I18n.get("placeholder.dest"));

        if (batchTableModel != null) {
            batchTableModel.fireTableStructureChanged();
            setupTableRenderers(batchTableModel.getAllKeys());
        }
    }
}
