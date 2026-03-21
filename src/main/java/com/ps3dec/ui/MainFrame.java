package com.ps3dec.ui;

import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.ui.dialogs.ProgressDialog;
import com.ps3dec.ui.dialogs.SuccessDialog;
import com.ps3dec.service.DecryptService;
import com.ps3dec.service.DkeySearchService;
import com.ps3dec.model.DkeyResult;
import com.ps3dec.util.OSUtils;
import com.ps3dec.util.Theme;
import com.ps3dec.ui.models.BatchItem;
import com.ps3dec.ui.models.BatchTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import com.ps3dec.util.AppPreferences;

public class MainFrame extends JFrame {

    // --- Single-tab fields ---
    private JTextField isoField;
    private JTextField dkeyField;
    private JTextField outputField;
    private DecryptService decryptService;
    private final DkeySearchService dkeySearchService = new DkeySearchService();
    /** Status label shown below the button bar in single view */
    private JLabel searchStatusLabel;

    // --- Batch fields ---
    private JTable batchTable;
    private BatchTableModel batchTableModel;
    private JTextField batchOutputField;

    // --- Layout ---
    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // --- Log console (Feature 5) ---
    private JTextArea logArea;
    private JPanel logPanel;
    private boolean logsVisible = false;

    // --- System Tray (Feature 4) ---
    private TrayIcon trayIcon;

    public MainFrame() {
        super("PS3 ISO Decryptor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(Theme.BG_DARK);
        decryptService = new DecryptService();
        initTray();
        initUI();
    }

    // ── System Tray setup ────────────────────────────────────────────────────
    private void initTray() {
        if (!SystemTray.isSupported()) return;
        try {
            java.net.URL iconUrl = getClass().getResource("/AppIcon.png");
            Image icon;
            if (iconUrl != null) {
                icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
            } else {
                // Fallback: plain coloured square
                BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D fg = fallback.createGraphics();
                fg.setColor(new Color(88, 101, 242));
                fg.fillRect(0, 0, 16, 16);
                fg.dispose();
                icon = fallback;
            }
            trayIcon = new TrayIcon(icon.getScaledInstance(16, 16, Image.SCALE_SMOOTH),
                    "PS3 ISO Decryptor");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception ignored) { }
    }

    private void notifyTray(String title, String message) {
        // On macOS, use osascript for proper Notification Center integration
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            try {
                String safeMsg = message.replace("\"", "'");
                String safeTitle = title.replace("\"", "'");
                Runtime.getRuntime().exec(new String[]{
                        "osascript", "-e",
                        "display notification \"" + safeMsg + "\" with title \"" + safeTitle + "\""
                });
                return;
            } catch (Exception ignored) { }
        }
        // Fallback: AWT SystemTray (Windows/Linux)
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    // ── Main UI ──────────────────────────────────────────────────────────────
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header with title + tab buttons
        JPanel header = new JPanel(new BorderLayout(0, 15));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JLabel titleLabel = new JLabel("PS3 ISO Decryptor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.NORTH);

        // Tab toggle buttons
        JPanel tabButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        tabButtons.setOpaque(false);
        JButton btnSingle = UIFactory.createActionButton("Única", Color.WHITE, Theme.ACCENT, null);
        JButton btnBatch  = UIFactory.createActionButton("Em Lote", Theme.TEXT_SECONDARY, new Color(55, 57, 72), null);
        tabButtons.add(btnSingle);
        tabButtons.add(btnBatch);
        header.add(tabButtons, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // Card content
        cardLayout  = new CardLayout();
        cardsPanel  = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        cardsPanel.add(createSingleView(), "SINGLE");
        cardsPanel.add(createBatchView(),  "BATCH");
        root.add(cardsPanel, BorderLayout.CENTER);

        // Bottom: log toggle + log panel
        JPanel southWrapper = new JPanel(new BorderLayout(0, 8));
        southWrapper.setOpaque(false);

        // Use array wrapper so the lambda can reference the button before it is fully assigned
        final JButton[] logToggleRef = new JButton[1];
        logToggleRef[0] = UIFactory.createActionButton("▾ Mostrar Logs", Theme.TEXT_SECONDARY,
                new Color(40, 42, 54), e -> toggleLogs(logToggleRef[0]));
        logToggleRef[0].setPreferredSize(new Dimension(160, 32));
        JPanel logToggleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logToggleRow.setOpaque(false);
        logToggleRow.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        logToggleRow.add(logToggleRef[0]);
        southWrapper.add(logToggleRow, BorderLayout.NORTH);

        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setBackground(new Color(18, 18, 28));
        logArea.setForeground(new Color(160, 255, 160));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));

        logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.add(logScroll, BorderLayout.CENTER);
        logPanel.setVisible(false);
        southWrapper.add(logPanel, BorderLayout.CENTER);

        root.add(southWrapper, BorderLayout.SOUTH);

        // Tab switching actions
        btnSingle.addActionListener(e -> {
            cardLayout.show(cardsPanel, "SINGLE");
            btnSingle.setBackground(Theme.ACCENT);
            btnSingle.setForeground(Color.WHITE);
            btnBatch.setBackground(new Color(55, 57, 72));
            btnBatch.setForeground(Theme.TEXT_SECONDARY);
            pack();
        });
        btnBatch.addActionListener(e -> {
            cardLayout.show(cardsPanel, "BATCH");
            btnBatch.setBackground(Theme.ACCENT);
            btnBatch.setForeground(Color.WHITE);
            btnSingle.setBackground(new Color(55, 57, 72));
            btnSingle.setForeground(Theme.TEXT_SECONDARY);
            pack();
        });

        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void toggleLogs(JButton toggleBtn) {
        logsVisible = !logsVisible;
        logPanel.setVisible(logsVisible);
        toggleBtn.setText(logsVisible ? "▴ Ocultar Logs" : "▾ Mostrar Logs");
        pack();
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ── Single conversion tab ────────────────────────────────────────────────
    private JPanel createSingleView() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);

        JPanel card = createCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ISO row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        card.add(UIFactory.createLabel("Arquivo ISO"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        isoField = UIFactory.createTextField("Selecione ou arraste o .iso aqui");
        enableFileDrop(isoField, true);  // isIso=true → salva em lastIsoPath
        card.add(isoField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseISO()), gbc);

        // DKEY row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        card.add(UIFactory.createLabel("Chave"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        dkeyField = UIFactory.createTextField("Arraste ou selecione o .dkey / .key");
        enableFileDrop(dkeyField, false);
        card.add(dkeyField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseDkey()), gbc);

        // Output row
        // Load saved preferences
        String savedIso = AppPreferences.getLastIsoPath();
        if (!savedIso.isEmpty() && new File(savedIso).exists()) isoField.setText(savedIso);

        String savedDkey = AppPreferences.getLastDkeyPath();
        if (!savedDkey.isEmpty()) {
            dkeyField.setText(savedDkey);
        } else if (!savedIso.isEmpty() && new File(savedIso).exists()) {
            // Se tem ISO mas não tem chave, busca online no startup
            javax.swing.SwingUtilities.invokeLater(this::searchDkeyOnline);
        }

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        card.add(UIFactory.createLabel("Destino"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        outputField = UIFactory.createTextField("Pasta de saída");
        outputField.setText(AppPreferences.getOutputDir());
        card.add(outputField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseOutput(outputField)), gbc);

        panel.add(card, BorderLayout.CENTER);

        // --- Bottom area: button bar + search status ---
        JPanel southArea = new JPanel(new BorderLayout(0, 6));
        southArea.setOpaque(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonBar.setOpaque(false);
        buttonBar.add(UIFactory.createActionButton("Site DKEY", Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> openWebsite()));
        buttonBar.add(UIFactory.createActionButton("ID do Jogo", Theme.TEXT_PRIMARY, new Color(65, 67, 85), e -> extractGameID()));
        buttonBar.add(UIFactory.createActionButton("🔍 Buscar Chave", new Color(200, 220, 255), new Color(50, 70, 120), e -> searchDkeyOnline()));
        buttonBar.add(UIFactory.createActionButton("Converter ISO", Color.WHITE, Theme.ACCENT, e -> runDecrypt()));
        southArea.add(buttonBar, BorderLayout.NORTH);

        searchStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        searchStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        searchStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        southArea.add(searchStatusLabel, BorderLayout.CENTER);

        panel.add(southArea, BorderLayout.SOUTH);
        return panel;
    }

    // ── Batch tab ────────────────────────────────────────────────────────────
    private JPanel createBatchView() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        controls.add(UIFactory.createLabel("Pasta"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        JTextField folderField = UIFactory.createTextField("Selecione ou arraste a pasta com ISOs e Chaves");
        folderField.setEditable(false);
        // Restore saved batch folder and auto-populate the table
        String savedBatch = AppPreferences.getBatchDir();
        if (!savedBatch.isEmpty()) {
            folderField.setText(savedBatch);
            File savedDir = new File(savedBatch);
            if (savedDir.isDirectory()) {
                // Defer until after the panel is fully constructed
                SwingUtilities.invokeLater(() -> scanFolderForBatch(savedDir));
            }
        }
        enableFolderDrop(folderField);
        controls.add(folderField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        controls.add(UIFactory.createBrowseButton(e -> chooseBatchFolder(folderField)), gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        controls.add(UIFactory.createLabel("Destino"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        batchOutputField = UIFactory.createTextField("Pasta de saída");
        batchOutputField.setText(AppPreferences.getOutputDir());
        controls.add(batchOutputField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        controls.add(UIFactory.createBrowseButton(e -> chooseOutput(batchOutputField)), gbc);

        panel.add(controls, BorderLayout.NORTH);

        // Batch table
        batchTable = new JTable();
        batchTable.setRowHeight(30);
        batchTable.setBackground(Theme.BG_FIELD);
        batchTable.setForeground(Theme.TEXT_PRIMARY);
        batchTable.getTableHeader().setBackground(new Color(55, 57, 72));
        batchTable.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        batchTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        batchTableModel = new BatchTableModel(new ArrayList<>(), new ArrayList<>());
        batchTable.setModel(batchTableModel);

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(UIFactory.createActionButton("Iniciar Lote", Color.WHITE, Theme.ACCENT, e -> runBatchDecrypt()));
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ── Drag & Drop helpers (Feature 1) ──────────────────────────────────────

    /** Enables file drag onto a text field. Pass isIso=true to also persist the path. */
    private void enableFileDrop(JTextField field, boolean isIso) {
        new DropTarget(field, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File f = files.get(0);
                        if (isIso) {
                            boolean isNewIso = !f.getAbsolutePath().equals(field.getText().trim());
                            field.setText(f.getAbsolutePath());
                            AppPreferences.setLastIsoPath(f.getAbsolutePath());
                            AppPreferences.setIsoDir(f.getParent());
                            if (dkeyField != null) {
                                if (isNewIso) {
                                    dkeyField.setText("");
                                    searchDkeyOnline();
                                } else if (dkeyField.getText().trim().isEmpty()) {
                                    searchDkeyOnline();
                                }
                            }
                        } else {
                            field.setText(f.getAbsolutePath());
                            AppPreferences.setLastDkeyPath(f.getAbsolutePath());
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    /** Enables folder drag onto a text field and triggers the batch scan. */
    private void enableFolderDrop(JTextField folderField) {
        new DropTarget(folderField, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File f = files.get(0);
                        File folder = f.isDirectory() ? f : f.getParentFile();
                        folderField.setText(folder.getAbsolutePath());
                        AppPreferences.setBatchDir(folder.getAbsolutePath());
                        if (batchOutputField.getText().trim().isEmpty()) {
                            batchOutputField.setText(folder.getAbsolutePath());
                        }
                        scanFolderForBatch(folder);
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    // ── Shared UI component ───────────────────────────────────────────────────
    private JPanel createCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Theme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
    }

    // ── File choosers ─────────────────────────────────────────────────────────
    private void chooseISO() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione o arquivo ISO");
        chooser.setCurrentDirectory(new File(AppPreferences.getIsoDir()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            boolean isNewIso = !f.getAbsolutePath().equals(isoField.getText().trim());
            
            isoField.setText(f.getAbsolutePath());
            AppPreferences.setLastIsoPath(f.getAbsolutePath());
            AppPreferences.setIsoDir(f.getParent());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(f.getParent());
            }
            if (isNewIso) {
                dkeyField.setText("");
                searchDkeyOnline();
            } else if (dkeyField.getText().trim().isEmpty()) {
                searchDkeyOnline();
            }
        }
    }

    private void chooseDkey() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione o arquivo .dkey / .key");
        chooser.setCurrentDirectory(new File(AppPreferences.getIsoDir()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            dkeyField.setText(selected.getAbsolutePath());
            AppPreferences.setLastDkeyPath(selected.getAbsolutePath());
        }
    }

    private void chooseOutput(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Pasta de saída");
        chooser.setCurrentDirectory(new File(targetField.getText()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            targetField.setText(path);
            AppPreferences.setOutputDir(path);
        }
    }

    private void chooseBatchFolder(JTextField folderField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Pasta contendo ISOs e Keys");
        String lastDir = AppPreferences.getBatchDir();
        chooser.setCurrentDirectory(new File(lastDir.isEmpty() ? System.getProperty("user.home") : lastDir));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            folderField.setText(folder.getAbsolutePath());
            AppPreferences.setBatchDir(folder.getAbsolutePath());
            if (batchOutputField.getText().trim().isEmpty()) {
                batchOutputField.setText(folder.getAbsolutePath());
            }
            scanFolderForBatch(folder);
        }
    }

    // ── Batch scan ────────────────────────────────────────────────────────────
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

            String status = matchedKey != null ? "Pronto" : "Faltando Chave";
            BatchItem item = new BatchItem(iso, matchedKey, status);
            items.add(item);
            
            if (matchedKey == null) {
                missingItems.add(item);
            }
        }

        batchTableModel = new BatchTableModel(items, keys);
        batchTable.setModel(batchTableModel);

        if (!keys.isEmpty()) {
            JComboBox<String> keyCombo = new JComboBox<>();
            keyCombo.addItem("Selecione a chave...");
            for (File k : keys) keyCombo.addItem(k.getName());
            batchTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(keyCombo));
        }

        batchTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                if (status.equals("Faltando Chave") || status.startsWith("Erro") || status.startsWith("Não achou")) c.setForeground(new Color(255, 100, 100));
                else if (status.equals("Concluído") || status.startsWith("Pronto"))     c.setForeground(new Color(100, 255, 100));
                else if (status.equals("Processando...") || status.startsWith("Buscando")) c.setForeground(Theme.ACCENT);
                else                                      c.setForeground(Theme.TEXT_PRIMARY);
                return c;
            }
        });

        if (!missingItems.isEmpty()) {
            searchMissingKeysOnline(missingItems);
        }
    }

    private void searchMissingKeysOnline(List<BatchItem> missingItems) {
        new SwingWorker<Void, BatchItem>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (BatchItem item : missingItems) {
                    item.setStatus("Buscando online...");
                    publish(item);
                    
                    String titleId = findGameId(item.getIsoFile().getAbsolutePath());
                    if (titleId.isEmpty()) {
                        item.setStatus("ID não encontrado");
                        publish(item);
                        continue;
                    }
                    
                    try {
                        com.ps3dec.model.DkeyResult res = dkeySearchService.fetchAndParseSync(titleId);
                        if (res != null) {
                            File tempKey = File.createTempFile(titleId + "_", ".dkey");
                            tempKey.deleteOnExit();
                            try (FileOutputStream fos = new FileOutputStream(tempKey)) {
                                fos.write(res.getDkeyHex().getBytes(StandardCharsets.US_ASCII));
                            }
                            item.setKeyFile(tempKey);
                            item.setStatus("Pronto (Online)");
                            appendLog("🔑 DKEY online em lote: " + titleId + " → " + res.getDkeyHex());
                        } else {
                            item.setStatus("Não achou online");
                        }
                    } catch (Exception e) {
                        item.setStatus("Erro na busca");
                    }
                    publish(item);
                    Thread.sleep(1000); // 1s polite delay so we don't bombard aldostools
                }
                return null;
            }

            @Override
            protected void process(List<BatchItem> chunks) {
                if (batchTableModel != null) {
                    batchTableModel.fireTableDataChanged();
                }
            }
        }.execute();
    }

    // ── Online DKEY search (Feature 2) ───────────────────────────────────────
    private void searchDkeyOnline() {
        // 1. Extract Game ID (try filename first, then scan ISO contents)
        String isoPath = isoField.getText().trim();
        if (isoPath.isEmpty()) {
            setSearchStatus("Selecione um arquivo ISO primeiro.", new Color(255, 180, 80));
            return;
        }

        String titleId = findGameId(isoPath);
        if (titleId.isEmpty()) {
            setSearchStatus("Game ID não encontrado no nome nem no conteúdo da ISO.", new Color(255, 120, 120));
            return;
        }

        // 2. Show loading indicator
        setSearchStatus("Buscando chave para " + titleId + "…", Theme.TEXT_SECONDARY);

        // 3. Search async
        dkeySearchService.searchAsync(titleId, new DkeySearchService.SearchCallback() {
            @Override
            public void onResult(DkeyResult result) {
                dkeyField.setText(result.getDkeyHex());
                AppPreferences.setLastDkeyPath(result.getDkeyHex()); // Saves the hex string to preferences
                setSearchStatus("✔ Chave encontrada: " + result.getGameName(), new Color(100, 220, 100));
                appendLog("🔑 DKEY online: " + result.getTitleId() + " → " + result.getDkeyHex());
            }

            @Override
            public void onNotFound(String id) {
                setSearchStatus("Chave não encontrada para " + id + ". Verifique o Game ID.", new Color(255, 150, 80));
            }

            @Override
            public void onError(String errorMsg) {
                setSearchStatus("Erro ao buscar: " + errorMsg, new Color(255, 100, 100));
            }
        });
    }

    /** Updates the search status label (always on EDT). */
    private void setSearchStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (searchStatusLabel != null) {
                searchStatusLabel.setText(text);
                searchStatusLabel.setForeground(color);
            }
        });
    }

    // ── Misc actions ──────────────────────────────────────────────────────────
    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://ps3.aldostools.org/ird.html"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir o navegador.");
        }
    }

    private void extractGameID() {
        String isoPath = isoField.getText().trim();
        if (isoPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione uma ISO primeiro.");
            return;
        }

        String match = findGameId(isoPath);

        if (match.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Game ID não encontrado no nome nem na leitura da ISO.");
        } else {
            showCopyDialog("Game ID Encontrado", match);
        }
    }

    /**
     * Attempts to find the 9-character PS3 Game ID (e.g., BLES01234).
     * 1. Checks the filename.
     * 2. If not found, scans the first 2MB of the ISO file itself (PARAM.SFO area).
     */
    private String findGameId(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) return "";
        
        java.util.regex.Pattern idPat = java.util.regex.Pattern.compile("([A-Z]{4})[-]?(\\d{5})");
        
        // 1. Check filename
        java.util.regex.Matcher m = idPat.matcher(f.getName().toUpperCase());
        if (m.find()) return m.group(1) + m.group(2); // return normalised (no dash)

        // 2. Scan internal ISO bytes (first 2MB)
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buffer = new byte[2 * 1024 * 1024]; // 2 MB
            int read = fis.read(buffer);
            if (read > 0) {
                // Convert to ASCII string, replacing non-printables with space to avoid regex issues
                String content = new String(buffer, 0, read, StandardCharsets.US_ASCII);
                m = idPat.matcher(content);
                if (m.find()) return m.group(1) + m.group(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private void showCopyDialog(String title, String value) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(380, 160);
        dialog.getContentPane().setBackground(Theme.BG_DARK);
        dialog.setLayout(new BorderLayout(0, 12));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTextField field = UIFactory.createTextField("");
        field.setText(value);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("Monospaced", Font.BOLD, 18));
        dialog.add(field, BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setOpaque(false);
        panel.add(UIFactory.createActionButton("Copiar", Color.WHITE, Theme.ACCENT, e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(value);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        }));
        panel.add(UIFactory.createActionButton("Fechar", Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> dialog.dispose()));

        dialog.add(panel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Single decrypt ────────────────────────────────────────────────────────
    private void runDecrypt() {
        String isoPath   = isoField.getText().trim();
        String dkeyInput = dkeyField.getText().trim();

        if (isoPath.isEmpty() || dkeyInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos!");
            return;
        }

        // Persist output field even if user typed it manually (Feature 3)
        String outDir = outputField.getText().trim();
        AppPreferences.setOutputDir(outDir);

        executeDecryption(isoPath, dkeyInput, outDir, true, null);
    }

    // ── Batch decrypt ─────────────────────────────────────────────────────────
    private void runBatchDecrypt() {
        if (batchTableModel == null || batchTableModel.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum arquivo na fila!");
            return;
        }

        String outputBase = batchOutputField.getText().trim();
        List<BatchItem> pending = new ArrayList<>();
        for (BatchItem item : batchTableModel.getItems()) {
            if (item.getKeyFile() != null && !item.getStatus().equals("Concluído")) {
                pending.add(item);
            }
        }

        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum item válido para processar. Verifique as chaves pendentes.");
            return;
        }

        processNextBatchItem(pending, 0, outputBase);
    }

    private void processNextBatchItem(List<BatchItem> queue, int index, String outputBase) {
        if (index >= queue.size()) {
            String msg = queue.size() + " ISO(s) convertida(s) com sucesso!";
            // Notifica ANTES do dialog para que apareça mesmo com app minimizado
            notifyTray("PS3 ISO Decryptor", msg);
            JOptionPane.showMessageDialog(this, msg, "Lote Concluído", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BatchItem item = queue.get(index);
        item.setStatus("Processando...");
        batchTable.repaint();

        executeDecryption(item.getIsoFile().getAbsolutePath(), item.getKeyFile().getAbsolutePath(),
                outputBase, false, new DecryptCallback() {
                    @Override
                    public void onComplete(boolean success, String errorMsg) {
                        item.setStatus(success ? "Concluído" : "Erro: " + errorMsg);
                        batchTable.repaint();
                        SwingUtilities.invokeLater(() -> processNextBatchItem(queue, index + 1, outputBase));
                    }
                });
    }

    interface DecryptCallback {
        void onComplete(boolean success, String errorMsg);
    }

    // ── Core decrypt execution ────────────────────────────────────────────────
    private void executeDecryption(String isoPath, String dkeyInput, String outputBaseDir,
                                   boolean isSingleMode, DecryptCallback callback) {
        File isoFile = new File(isoPath);
        if (!isoFile.exists()) {
            if (isSingleMode) JOptionPane.showMessageDialog(this, "Arquivo ISO não encontrado!");
            if (callback != null) callback.onComplete(false, "ISO ausente");
            return;
        }

        String ps3decBin = OSUtils.getPs3decPath();
        if (!new File(ps3decBin).exists()) {
            if (isSingleMode) JOptionPane.showMessageDialog(this,
                    "Binário ps3decrs não encontrado!\nProcurado em: " + ps3decBin);
            if (callback != null) callback.onComplete(false, "Binário ausente");
            return;
        }

        String dkeyHex;
        try {
            File dkeyFile = new File(dkeyInput);
            if (dkeyFile.exists() && dkeyFile.isFile()) {
                BufferedReader br = new BufferedReader(new FileReader(dkeyFile));
                dkeyHex = br.readLine().trim();
                br.close();
            } else {
                dkeyHex = dkeyInput;
            }
        } catch (Exception e) {
            if (isSingleMode) JOptionPane.showMessageDialog(this, "Erro ao ler a chave: " + e.getMessage());
            if (callback != null) callback.onComplete(false, "Chave inválida");
            return;
        }

        String outputDir = outputBaseDir + "/isos/";
        new File(outputDir).mkdirs();

        long totalSize = isoFile.length();
        String isoName = isoFile.getName();
        String stemName = isoName.contains(".") ? isoName.substring(0, isoName.lastIndexOf('.')) : isoName;
        String outputFileName = stemName + "_decrypted.iso";

        // Log header (Feature 5)
        appendLog("──────────────────────────────────────");
        appendLog("▶ Iniciando: " + isoName);
        appendLog("  Chave : " + dkeyInput);
        appendLog("  Saída : " + outputDir + outputFileName);

        ProgressDialog progressDialog = new ProgressDialog(this, isoName, totalSize, () -> decryptService.cancel());

        decryptService.startDecryption(ps3decBin, isoPath, dkeyHex, outputDir, outputFileName,
                new DecryptService.DecryptListener() {
                    @Override
                    public void onProgress(int targetPct, String statusText, String elapsedStr) {
                        progressDialog.updateProgress(targetPct, statusText, elapsedStr);
                    }

                    @Override
                    public void onSuccess(String outputFilePath) {
                        appendLog("✔ Concluído: " + outputFilePath);
                        if (isSingleMode) {
                            // Notifica ANTES de mostrar o dialog para disparar mesmo com app minimizado
                            notifyTray("PS3 ISO Decryptor", isoName + " convertida com sucesso!");
                            progressDialog.finishWithAnimation(() ->
                                    new SuccessDialog(MainFrame.this, outputFilePath).setVisible(true)
                            );
                        } else {
                            progressDialog.finishWithAnimation(() -> {
                                progressDialog.dispose();
                                if (callback != null) callback.onComplete(true, null);
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMsg) {
                        appendLog("✖ Erro: " + errorMsg);
                        progressDialog.dispose();
                        if (isSingleMode) JOptionPane.showMessageDialog(MainFrame.this, errorMsg);
                        if (callback != null) callback.onComplete(false, errorMsg);
                    }

                    @Override
                    public void onFinish() { /* handled above */ }
                },
                // Feature 5 — log consumer
                line -> appendLog("  " + line));

        progressDialog.setVisible(true);
    }
}
