package com.ps3dec.ui.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import com.ps3dec.util.Theme;
import com.ps3dec.util.I18n;
import com.ps3dec.ui.components.UIFactory;

/**
 * Shared UI utility methods for dialogs and choosers.
 */
public class UIUtils {

    public static File showFolderChooser(Component parent, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    public static File showFileChooser(Component parent, String title, String initialDir) {
        return showFileChooser(parent, title, initialDir, null);
    }

    public static File showFileChooser(Component parent, String title, String initialDir, String description,
            String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        if (initialDir != null && !initialDir.isEmpty()) {
            chooser.setCurrentDirectory(new File(initialDir));
        }
        if (extensions != null && extensions.length > 0) {
            chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
        }
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    public static void setPlaceholder(JTextField field, String placeholder) {
        if (field == null)
            return;
        field.putClientProperty("placeholder", placeholder);
        field.repaint();
    }

    public static String getPS3DecBinPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String binName = os.contains("win") ? "ps3dec.exe" : "ps3dec";

        // Try relative to project root
        File binFile = new File("bin/" + binName);
        if (binFile.exists())
            return binFile.getAbsolutePath();

        // Try user.dir
        binFile = new File(System.getProperty("user.dir") + "/bin/" + binName);
        if (binFile.exists())
            return binFile.getAbsolutePath();

        return binName; // Fallback to PATH
    }

    public static void showCopyDialog(Window parent, String title, String value) {
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
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
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
