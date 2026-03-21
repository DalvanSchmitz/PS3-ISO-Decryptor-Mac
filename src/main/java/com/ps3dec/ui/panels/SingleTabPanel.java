package com.ps3dec.ui.panels;

import com.ps3dec.service.DecryptService;
import com.ps3dec.service.DkeySearchService;
import com.ps3dec.model.DkeyResult;
import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.ui.dialogs.ProgressDialog;
import com.ps3dec.ui.dialogs.SuccessDialog;
import com.ps3dec.ui.util.DndHelper;
import com.ps3dec.ui.util.UIUtils;
import com.ps3dec.util.AppPreferences;
import com.ps3dec.util.I18n;
import com.ps3dec.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import com.ps3dec.util.OSUtils;

/**
 * Panel for the Single ISO conversion tab.
 */
public class SingleTabPanel extends JPanel {

    private final JFrame parent;
    private final LogPanel logPanel;
    private final DecryptService decryptService;
    private final DkeySearchService dkeySearchService;

    private JLabel labelIso, labelDkey, labelDest, searchStatusLabel;
    private JTextField isoField, dkeyField, outputField;
    private JButton btnGameId, btnSearchDkey, btnConvert, btnSiteDkey;

    public SingleTabPanel(JFrame parent, LogPanel logPanel) {
        this.parent = parent;
        this.logPanel = logPanel;
        this.decryptService = new DecryptService();
        this.dkeySearchService = new DkeySearchService();

        setOpaque(false);
        setLayout(new BorderLayout(0, 15));
        initComponents();
        setupDragAndDrop();

        // Auto-search on startup if ISO is present but DKEY is missing
        String iso = isoField.getText().trim();
        String dkey = dkeyField.getText().trim();
        if (!iso.isEmpty() && dkey.isEmpty()) {
            Timer t = new Timer(100, e -> searchDkeyOnline());
            t.setRepeats(false);
            t.start();
        }
    }

    private void initComponents() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ISO File
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        labelIso = UIFactory.createLabel(I18n.get("label.iso_file"));
        form.add(labelIso, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        isoField = UIFactory.createTextField(I18n.get("placeholder.iso"));
        isoField.setText(AppPreferences.getLastIsoPath());
        form.add(isoField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(UIFactory.createBrowseButton(e -> {
            File f = UIUtils.showFileChooser(parent, I18n.get("chooser.iso.title"), AppPreferences.getIsoDir());
            if (f != null) {
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
                }
            }
        }), gbc);

        // DKEY / RAP
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        labelDkey = UIFactory.createLabel(I18n.get("label.dkey"));
        form.add(labelDkey, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        dkeyField = UIFactory.createTextField(I18n.get("placeholder.dkey"));
        dkeyField.setText(AppPreferences.getLastDkeyPath());
        form.add(dkeyField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(UIFactory.createBrowseButton(e -> {
            File f = UIUtils.showFileChooser(parent, I18n.get("chooser.dkey.title"), AppPreferences.getIsoDir());
            if (f != null) {
                dkeyField.setText(f.getAbsolutePath());
                AppPreferences.setLastDkeyPath(f.getAbsolutePath());
            }
        }), gbc);

        // Output Folder
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        labelDest = UIFactory.createLabel(I18n.get("label.dest_folder"));
        form.add(labelDest, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        outputField = UIFactory.createTextField(I18n.get("placeholder.dest"));
        outputField.setText(AppPreferences.getOutputDir());
        form.add(outputField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(UIFactory.createBrowseButton(e -> {
            File f = UIUtils.showFolderChooser(parent, I18n.get("label.dest_folder"));
            if (f != null) {
                outputField.setText(f.getAbsolutePath());
                AppPreferences.setOutputDir(f.getAbsolutePath());
            }
        }), gbc);

        add(form, BorderLayout.CENTER);

        // Bottom Actions
        JPanel southArea = new JPanel(new BorderLayout(0, 10));
        southArea.setOpaque(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonBar.setOpaque(false);

        btnGameId = UIFactory.createActionButton(I18n.get("label.game_id"), Theme.TEXT_PRIMARY, new Color(65, 67, 85), e -> {
            String id = decryptService.extractGameIdFromFileName(isoField.getText());
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(parent, I18n.get("status.gameid_not_found"));
            } else {
                UIUtils.showCopyDialog(parent, I18n.get("gameid.dialog_title"), id);
            }
        });
        btnSiteDkey = UIFactory.createActionButton(I18n.get("btn.site_dkey"), Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> openWebsite());
        btnSearchDkey = UIFactory.createActionButton(I18n.get("btn.search_dkey"), new Color(200, 220, 255), new Color(50, 70, 120), e -> searchDkeyOnline());
        btnConvert = UIFactory.createActionButton(I18n.get("btn.convert_iso"), Color.WHITE, Theme.ACCENT, e -> runSingleDecrypt());

        buttonBar.add(btnSiteDkey);
        buttonBar.add(btnGameId);
        buttonBar.add(btnSearchDkey);
        buttonBar.add(btnConvert);
        southArea.add(buttonBar, BorderLayout.NORTH);

        searchStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        searchStatusLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        searchStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        southArea.add(searchStatusLabel, BorderLayout.CENTER);

        add(southArea, BorderLayout.SOUTH);
    }

    private void setupDragAndDrop() {
        DndHelper.setup(isoField, f -> {
            boolean isNewIso = !f.getAbsolutePath().equals(isoField.getText().trim());
            isoField.setText(f.getAbsolutePath());
            AppPreferences.setLastIsoPath(f.getAbsolutePath());
            AppPreferences.setIsoDir(f.getParent());
            if (isNewIso) {
                dkeyField.setText("");
                searchDkeyOnline();
            }
        });
        DndHelper.setup(dkeyField, f -> dkeyField.setText(f.getAbsolutePath()));
        DndHelper.setup(outputField, f -> outputField.setText(f.getAbsolutePath()));
    }

    private void searchDkeyOnline() {
        String isoPath = isoField.getText().trim();
        if (isoPath.isEmpty()) {
            setSearchStatus(I18n.get("status.select_iso_first"), new Color(255, 180, 80));
            return;
        }

        String titleId = decryptService.extractGameIdFromFileName(isoPath);
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
                setSearchStatus(MessageFormat.format(I18n.get("status.key_found"), result.getGameName()), new Color(100, 220, 100));
                logPanel.log("\uD83D\uDD11 DKEY online: " + result.getTitleId() + " \u2192 " + result.getDkeyHex());
            }
            @Override
            public void onNotFound(String id) {
                setSearchStatus(I18n.get("item.status.not_found_online"), new Color(255, 120, 120));
            }
            @Override
            public void onError(String msg) {
                setSearchStatus(I18n.get("item.status.search_error"), new Color(255, 100, 100));
            }
        });
    }

    private void runSingleDecrypt() {
        String isoPath = isoField.getText().trim();
        String dkey = dkeyField.getText().trim();
        String outputDir = outputField.getText().trim();

        if (isoPath.isEmpty() || dkey.isEmpty() || outputDir.isEmpty()) {
            JOptionPane.showMessageDialog(parent, I18n.get("status.select_iso_first"));
            return;
        }

        String ps3decBin = OSUtils.getPs3decPath();
        String isoName = new File(isoPath).getName();
        String stemName = isoName.contains(".") ? isoName.substring(0, isoName.lastIndexOf('.')) : isoName;
        String outputFileName = stemName + "_decrypted.iso";

        ProgressDialog progress = new ProgressDialog(parent, isoName, new File(isoPath).length(), () -> decryptService.cancel());

        decryptService.startDecryption(ps3decBin, isoPath, dkey, outputDir, outputFileName, 
            new DecryptService.DecryptListener() {
                @Override
                public void onProgress(int targetPct, String statusText, String elapsedStr) {
                    progress.updateProgress(targetPct, statusText, elapsedStr);
                }
                @Override
                public void onSuccess(String outputFilePath) {
                    ((com.ps3dec.ui.MainFrame) parent).getTrayManager().showNotification(
                        com.ps3dec.util.I18n.get("notify.title"),
                        com.ps3dec.util.I18n.get("notify.done_iso")
                    );
                    logPanel.log("\u2714 " + I18n.get("item.status.done") + ": " + outputFilePath);
                    progress.finishWithAnimation(() -> {
                        SwingUtilities.invokeLater(() -> {
                            new SuccessDialog(parent, outputFilePath).setVisible(true);
                        });
                    });
                }
                @Override
                public void onError(String errorMsg) {
                    logPanel.log("\u2716 " + I18n.get("item.status.error") + ": " + errorMsg);
                    progress.dispose();
                    JOptionPane.showMessageDialog(parent, errorMsg);
                }
                @Override
                public void onFinish() {
                    // Handled in success/error
                }
            }, 
            line -> logPanel.log("  " + line)
        );

        progress.setVisible(true);
    }

    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://ps3.aldostools.org/ird.html"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, I18n.get("error.open_browser"));
        }
    }

    private void setSearchStatus(String text, Color color) {
        searchStatusLabel.setText(text);
        searchStatusLabel.setForeground(color);
    }

    public void updateLocalizedText() {
        if (labelIso != null) labelIso.setText(I18n.get("label.iso_file"));
        if (labelDkey != null) labelDkey.setText(I18n.get("label.dkey"));
        if (labelDest != null) labelDest.setText(I18n.get("label.dest_folder"));
        if (btnGameId != null) btnGameId.setText(I18n.get("label.game_id"));
        if (btnSiteDkey != null) btnSiteDkey.setText(I18n.get("btn.site_dkey"));
        if (btnSearchDkey != null) btnSearchDkey.setText(I18n.get("btn.search_dkey"));
        if (btnConvert != null) btnConvert.setText(I18n.get("btn.convert_iso"));

        UIUtils.setPlaceholder(isoField, I18n.get("placeholder.iso"));
        UIUtils.setPlaceholder(dkeyField, I18n.get("placeholder.dkey"));
        UIUtils.setPlaceholder(outputField, I18n.get("placeholder.dest"));
    }
}
