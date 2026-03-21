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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import com.ps3dec.util.AppPreferences;
import com.ps3dec.util.I18n;

public class MainFrame extends JFrame {

    // --- Single-tab fields ---
    private JTextField isoField;
    private JTextField dkeyField;
    private JTextField outputField;
    private DecryptService decryptService;
    private final DkeySearchService dkeySearchService = new DkeySearchService();

    // UI elements that need localized updates
    private JButton btnSingle;   // instance field — assigned in initUI(), NOT re-declared there
    private JButton btnBatch;    // same
    private JButton btnShowLogs; // log-toggle button
    private JButton siteBtn;
    private JLabel labelIso, labelKey, labelDest, labelBatchFolder, labelBatchDest;
    private JButton btnGameId, btnSearchDkey, btnConvert, btnStartBatch;
    private JTextField folderField;
    /** Status label shown below the button bar in single view */
    private JLabel searchStatusLabel;

    // --- Batch fields ---
    private JTable batchTable;
    private BatchTableModel batchTableModel;
    private JTextField batchOutputField;

    // --- Layout ---
    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // --- Log console ---
    private JTextArea logArea;
    private JPanel logPanel;
    private boolean logsVisible = false;

    // --- System Tray ---
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

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel titleLabel = new JLabel("PS3 ISO Decryptor", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        topRow.add(titleLabel, BorderLayout.CENTER);

        JButton settingsBtn = UIFactory.createActionButton(
                "\u2699", Theme.TEXT_SECONDARY, new Color(55, 57, 72),
                e -> showLanguageMenu((JButton) e.getSource()));
        settingsBtn.setPreferredSize(new Dimension(40, 40));
        topRow.add(settingsBtn, BorderLayout.EAST);

        header.add(topRow, BorderLayout.NORTH);

        // Tab toggle buttons — assign to INSTANCE FIELDS (no 'JButton' keyword here)
        JPanel tabButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        tabButtons.setOpaque(false);
        btnSingle = UIFactory.createActionButton(I18n.get("tab.single"), Color.WHITE, Theme.ACCENT, null);
        btnBatch  = UIFactory.createActionButton(I18n.get("tab.batch"), Theme.TEXT_SECONDARY, new Color(55, 57, 72), null);
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

        // Assign log-toggle button to instance field so updateLocalizedText() can reach it
        btnShowLogs = UIFactory.createActionButton(
                I18n.get("btn.show_logs"), Theme.TEXT_SECONDARY, new Color(40, 42, 54),
                e -> toggleLogs());
        btnShowLogs.setPreferredSize(new Dimension(160, 32));
        JPanel logToggleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logToggleRow.setOpaque(false);
        logToggleRow.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        logToggleRow.add(btnShowLogs);
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

    private void toggleLogs() {
        logsVisible = !logsVisible;
        logPanel.setVisible(logsVisible);
        updateLogToggleButton();
        pack();
    }

    private void updateLogToggleButton() {
        if (btnShowLogs != null) {
            btnShowLogs.setText(logsVisible ? I18n.get("btn.hide_logs") : I18n.get("btn.show_logs"));
        }
    }

    private void showLanguageMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(40, 42, 58));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));

        menu.add(createLangMenuItem("Portugu\u00EAs (BR)", e -> setLanguage(new java.util.Locale("pt", "BR"))));
        menu.add(createLangMenuItem("English (US)",      e -> setLanguage(java.util.Locale.ENGLISH)));
        menu.show(invoker, 0, invoker.getHeight());
    }

    private JMenuItem createLangMenuItem(String label, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        // Remove native OS styling (which causes the bright green bug on macOS)
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI());
        
        item.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        item.setForeground(Theme.TEXT_PRIMARY);
        item.setBackground(Theme.BG_PANEL);
        item.setOpaque(true);
        item.setBorderPainted(false);
        item.setPreferredSize(new Dimension(160, 36));
        item.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.addActionListener(action);
        
        // Custom hover effect
        item.addChangeListener(e -> {
            if (item.getModel().isArmed()) {
                item.setBackground(Theme.BG_FIELD);
            } else {
                item.setBackground(Theme.BG_PANEL);
            }
        });
        
        return item;
    }

    private void setLanguage(java.util.Locale locale) {
        java.util.Locale.setDefault(locale);
        I18n.loadBundle(locale);
        AppPreferences.setLanguage(locale.toLanguageTag()); // persist choice
        updateLocalizedText();
    }

    /** Updates the stored placeholder string in a field so it repaints with the new text. */
    private static void setPlaceholder(JTextField field, String text) {
        if (field == null) return;
        field.putClientProperty("placeholder", text);
        field.setToolTipText(text); // update tooltip as well
        field.repaint();
    }

    private void updateLocalizedText() {
        // Tabs
        if (btnSingle != null) btnSingle.setText(I18n.get("tab.single"));
        if (btnBatch  != null) btnBatch.setText(I18n.get("tab.batch"));

        // Single view labels
        if (labelIso  != null) labelIso.setText(I18n.get("label.iso_file"));
        if (labelKey  != null) labelKey.setText(I18n.get("label.dkey"));
        if (labelDest != null) labelDest.setText(I18n.get("label.dest_folder"));

        // Single view placeholders (updates both tooltip AND painted placeholder)
        setPlaceholder(isoField,    I18n.get("placeholder.iso"));
        setPlaceholder(dkeyField,   I18n.get("placeholder.dkey"));
        setPlaceholder(outputField, I18n.get("placeholder.dest"));
        setPlaceholder(folderField, I18n.get("placeholder.batch"));
        if (batchOutputField != null) setPlaceholder(batchOutputField, I18n.get("placeholder.dest"));

        // Single view buttons
        if (btnGameId    != null) btnGameId.setText(I18n.get("label.game_id"));
        if (btnSearchDkey!= null) btnSearchDkey.setText(I18n.get("btn.search_dkey"));
        if (btnConvert   != null) btnConvert.setText(I18n.get("btn.convert_iso"));

        // Batch view
        if (labelBatchFolder != null) labelBatchFolder.setText(I18n.get("label.batch_folder"));
        if (labelBatchDest   != null) labelBatchDest.setText(I18n.get("label.dest_folder"));
        if (btnStartBatch    != null) btnStartBatch.setText(I18n.get("btn.start_batch"));

        // Batch table columns
        if (batchTableModel != null) batchTableModel.fireTableStructureChanged();

        // Log toggle button
        updateLogToggleButton();

        revalidate();
        repaint();
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
        labelIso = UIFactory.createLabel(I18n.get("label.iso_file"));
        card.add(labelIso, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        isoField = UIFactory.createTextField(I18n.get("placeholder.iso"));
        enableFileDrop(isoField, true);
        card.add(isoField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseISO()), gbc);

        // DKEY row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        labelKey = UIFactory.createLabel(I18n.get("label.dkey"));
        card.add(labelKey, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        dkeyField = UIFactory.createTextField(I18n.get("placeholder.dkey"));
        enableFileDrop(dkeyField, false);
        card.add(dkeyField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseDkey()), gbc);

        // Output row — load saved preferences
        String savedIso = AppPreferences.getLastIsoPath();
        if (!savedIso.isEmpty() && new File(savedIso).exists()) isoField.setText(savedIso);

        String savedDkey = AppPreferences.getLastDkeyPath();
        if (!savedDkey.isEmpty()) {
            dkeyField.setText(savedDkey);
        } else if (!savedIso.isEmpty() && new File(savedIso).exists()) {
            javax.swing.SwingUtilities.invokeLater(this::searchDkeyOnline);
        }

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        labelDest = UIFactory.createLabel(I18n.get("label.dest_folder"));
        card.add(labelDest, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        outputField = UIFactory.createTextField(I18n.get("placeholder.dest"));
        outputField.setText(AppPreferences.getOutputDir());
        card.add(outputField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        card.add(UIFactory.createBrowseButton(e -> chooseOutput(outputField)), gbc);

        panel.add(card, BorderLayout.CENTER);

        // Bottom: button bar + search status
        JPanel southArea = new JPanel(new BorderLayout(0, 6));
        southArea.setOpaque(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonBar.setOpaque(false);
        siteBtn      = UIFactory.createActionButton("Site DKEY", Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> openWebsite());
        btnGameId    = UIFactory.createActionButton(I18n.get("label.game_id"), Theme.TEXT_PRIMARY, new Color(65, 67, 85), e -> extractGameID());
        btnSearchDkey= UIFactory.createActionButton(I18n.get("btn.search_dkey"), new Color(200, 220, 255), new Color(50, 70, 120), e -> searchDkeyOnline());
        btnConvert   = UIFactory.createActionButton(I18n.get("btn.convert_iso"), Color.WHITE, Theme.ACCENT, e -> runDecrypt());

        buttonBar.add(siteBtn);
        buttonBar.add(btnGameId);
        buttonBar.add(btnSearchDkey);
        buttonBar.add(btnConvert);
        southArea.add(buttonBar, BorderLayout.NORTH);

        searchStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        searchStatusLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
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
        enableFolderDrop(folderField);
        controls.add(folderField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        controls.add(UIFactory.createBrowseButton(e -> chooseBatchFolder(folderField)), gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        labelBatchDest = UIFactory.createLabel(I18n.get("label.dest_folder"));
        controls.add(labelBatchDest, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        batchOutputField = UIFactory.createTextField(I18n.get("placeholder.dest"));
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
        batchTable.getTableHeader().setFont(new Font(Font.DIALOG, Font.BOLD, 12));

        batchTableModel = new BatchTableModel(new ArrayList<>(), new ArrayList<>());
        batchTable.setModel(batchTableModel);

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        btnStartBatch = UIFactory.createActionButton(I18n.get("btn.start_batch"), Color.WHITE, Theme.ACCENT, e -> runBatchDecrypt());
        // Override the default 140px to let text breathe at any language
        btnStartBatch.setPreferredSize(null);
        btnStartBatch.setMargin(new Insets(0, 24, 0, 24));
        bottom.add(btnStartBatch);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ── Drag & Drop helpers ───────────────────────────────────────────────────
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

    private void enableFolderDrop(JTextField field) {
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
                        File folder = f.isDirectory() ? f : f.getParentFile();
                        field.setText(folder.getAbsolutePath());
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

    // ── File choosers ──────────────────────────────────────────────────────────
    private void chooseISO() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("chooser.iso.title"));
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
        chooser.setDialogTitle(I18n.get("chooser.dkey.title"));
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
        chooser.setDialogTitle(I18n.get("label.dest_folder"));
        chooser.setCurrentDirectory(new File(targetField.getText()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            targetField.setText(path);
            AppPreferences.setOutputDir(path);
        }
    }

    private void chooseBatchFolder(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(I18n.get("label.batch_folder"));
        String lastDir = AppPreferences.getBatchDir();
        chooser.setCurrentDirectory(new File(lastDir.isEmpty() ? System.getProperty("user.home") : lastDir));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            field.setText(folder.getAbsolutePath());
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

            String status = matchedKey != null
                    ? I18n.get("batch.status.ready")
                    : I18n.get("batch.status.missing_key");
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
            keyCombo.addItem(I18n.get("batch.key_select"));
            for (File k : keys) keyCombo.addItem(k.getName());
            batchTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(keyCombo));
        }

        final String keyReady       = I18n.get("batch.status.ready");
        final String keyReadyOnline = I18n.get("item.status.ready_online");
        final String keyDone        = I18n.get("item.status.done");
        final String keyProcessing  = I18n.get("batch.status.processing");
        final String keySearching   = I18n.get("item.status.online_search");
        final String keyMissing     = I18n.get("batch.status.missing_key");

        batchTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if (status.equals(keyMissing) || status.startsWith(I18n.get("item.status.error")) || status.startsWith(I18n.get("item.status.not_found_online")))
                    c.setForeground(new Color(255, 100, 100));
                else if (status.equals(keyDone) || status.equals(keyReady) || status.equals(keyReadyOnline))
                    c.setForeground(new Color(100, 255, 100));
                else if (status.equals(keyProcessing) || status.contains(keySearching.substring(0, Math.min(keySearching.length(), 6))))
                    c.setForeground(Theme.ACCENT);
                else
                    c.setForeground(Theme.TEXT_PRIMARY);
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
                    item.setStatus(I18n.get("item.status.online_search"));
                    publish(item);

                    String titleId = findGameId(item.getIsoFile().getAbsolutePath());
                    if (titleId.isEmpty()) {
                        item.setStatus(I18n.get("item.status.not_found_online"));
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
                            item.setStatus(I18n.get("item.status.ready_online"));
                            appendLog("\uD83D\uDD11 DKEY online (batch): " + titleId + " \u2192 " + res.getDkeyHex());
                        } else {
                            item.setStatus(I18n.get("item.status.not_found_online"));
                        }
                    } catch (Exception e) {
                        item.setStatus(I18n.get("item.status.search_error"));
                    }
                    publish(item);
                    Thread.sleep(1000);
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

    // ── Online DKEY search ────────────────────────────────────────────────────
    private void searchDkeyOnline() {
        String isoPath = isoField.getText().trim();
        if (isoPath.isEmpty()) {
            setSearchStatus(I18n.get("status.select_iso_first"), new Color(255, 180, 80));
            return;
        }

        String titleId = findGameId(isoPath);
        if (titleId.isEmpty()) {
            setSearchStatus(I18n.get("status.gameid_not_found"), new Color(255, 120, 120));
            return;
        }

        setSearchStatus(MessageFormat.format(I18n.get("status.searching_for"), titleId), Theme.TEXT_SECONDARY);

        dkeySearchService.searchAsync(titleId, new DkeySearchService.SearchCallback() {
            @Override
            public void onResult(DkeyResult result) {
                dkeyField.setText(result.getDkeyHex());
                AppPreferences.setLastDkeyPath(result.getDkeyHex());
                setSearchStatus(
                        MessageFormat.format(I18n.get("status.key_found"), result.getGameName()),
                        new Color(100, 220, 100));
                appendLog("\uD83D\uDD11 DKEY online: " + result.getTitleId() + " \u2192 " + result.getDkeyHex());
            }

            @Override
            public void onNotFound(String id) {
                setSearchStatus(
                        MessageFormat.format(I18n.get("status.not_found_for"), id),
                        new Color(255, 150, 80));
            }

            @Override
            public void onError(String errorMsg) {
                setSearchStatus(
                        MessageFormat.format(I18n.get("status.error_for"), errorMsg),
                        new Color(255, 100, 100));
            }
        });
    }

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
            JOptionPane.showMessageDialog(this, I18n.get("error.open_browser"));
        }
    }

    private void extractGameID() {
        String isoPath = isoField.getText().trim();
        if (isoPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("status.select_iso_first"));
            return;
        }

        String match = findGameId(isoPath);

        if (match.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("status.gameid_not_found"));
        } else {
            showCopyDialog(I18n.get("gameid.dialog_title"), match);
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
        if (m.find()) return m.group(1) + m.group(2);

        // 2. Scan internal ISO bytes (first 2MB)
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buffer = new byte[2 * 1024 * 1024];
            int read = fis.read(buffer);
            if (read > 0) {
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
        panel.add(UIFactory.createActionButton(I18n.get("btn.copy"), Color.WHITE, Theme.ACCENT, e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(value);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        }));
        panel.add(UIFactory.createActionButton(I18n.get("btn.close"), Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> dialog.dispose()));

        dialog.add(panel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Single decrypt ────────────────────────────────────────────────────────
    private void runDecrypt() {
        String isoPath   = isoField.getText().trim();
        String dkeyInput = dkeyField.getText().trim();

        if (isoPath.isEmpty() || dkeyInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("error.fill_fields"));
            return;
        }

        String outDir = outputField.getText().trim();
        AppPreferences.setOutputDir(outDir);

        executeDecryption(isoPath, dkeyInput, outDir, true, null);
    }

    // ── Batch decrypt ─────────────────────────────────────────────────────────
    private void runBatchDecrypt() {
        if (batchTableModel == null || batchTableModel.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("error.no_queue"));
            return;
        }

        String outputBase = batchOutputField.getText().trim();
        List<BatchItem> pending = new ArrayList<>();
        for (BatchItem item : batchTableModel.getItems()) {
            if (item.getKeyFile() != null && !item.getStatus().equals(I18n.get("item.status.done"))) {
                pending.add(item);
            }
        }

        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("batch.empty"));
            return;
        }

        processNextBatchItem(pending, 0, outputBase);
    }

    private void processNextBatchItem(List<BatchItem> queue, int index, String outputBase) {
        if (index >= queue.size()) {
            String msg = MessageFormat.format(I18n.get("batch.done.message"), queue.size());
            notifyTray(I18n.get("notify.title"), msg);
            JOptionPane.showMessageDialog(this, msg, I18n.get("batch.done.title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BatchItem item = queue.get(index);
        item.setStatus(I18n.get("batch.status.processing"));
        batchTable.repaint();

        executeDecryption(item.getIsoFile().getAbsolutePath(), item.getKeyFile().getAbsolutePath(),
                outputBase, false, new DecryptCallback() {
                    @Override
                    public void onComplete(boolean success, String errorMsg) {
                        item.setStatus(success
                                ? I18n.get("item.status.done")
                                : I18n.get("item.status.error") + ": " + errorMsg);
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
            if (isSingleMode) JOptionPane.showMessageDialog(this, I18n.get("error.iso_not_found"));
            if (callback != null) callback.onComplete(false, I18n.get("error.iso_not_found"));
            return;
        }

        String ps3decBin = OSUtils.getPs3decPath();
        if (!new File(ps3decBin).exists()) {
            String msg = MessageFormat.format(I18n.get("error.binary_missing"), ps3decBin);
            if (isSingleMode) JOptionPane.showMessageDialog(this, msg);
            if (callback != null) callback.onComplete(false, msg);
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
            String msg = MessageFormat.format(I18n.get("error.read_key"), e.getMessage());
            if (isSingleMode) JOptionPane.showMessageDialog(this, msg);
            if (callback != null) callback.onComplete(false, msg);
            return;
        }

        String outputDir = outputBaseDir + "/isos/";
        new File(outputDir).mkdirs();

        long totalSize = isoFile.length();
        String isoName = isoFile.getName();
        String stemName = isoName.contains(".") ? isoName.substring(0, isoName.lastIndexOf('.')) : isoName;
        String outputFileName = stemName + "_decrypted.iso";

        appendLog("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        appendLog("\u25B6 " + I18n.get("dialog.progress.starting") + " " + isoName);
        appendLog("  Key  : " + dkeyInput);
        appendLog("  Out  : " + outputDir + outputFileName);

        ProgressDialog progressDialog = new ProgressDialog(this, isoName, totalSize, () -> decryptService.cancel());

        decryptService.startDecryption(ps3decBin, isoPath, dkeyHex, outputDir, outputFileName,
                new DecryptService.DecryptListener() {
                    @Override
                    public void onProgress(int targetPct, String statusText, String elapsedStr) {
                        progressDialog.updateProgress(targetPct, statusText, elapsedStr);
                    }

                    @Override
                    public void onSuccess(String outputFilePath) {
                        appendLog("\u2714 " + I18n.get("item.status.done") + ": " + outputFilePath);
                        if (isSingleMode) {
                            notifyTray(I18n.get("notify.title"), isoName + " " + I18n.get("notify.done_iso"));
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
                        appendLog("\u2716 " + I18n.get("item.status.error") + ": " + errorMsg);
                        progressDialog.dispose();
                        if (isSingleMode) JOptionPane.showMessageDialog(MainFrame.this, errorMsg);
                        if (callback != null) callback.onComplete(false, errorMsg);
                    }

                    @Override
                    public void onFinish() { /* handled above */ }
                },
                line -> appendLog("  " + line));

        progressDialog.setVisible(true);
    }
}
