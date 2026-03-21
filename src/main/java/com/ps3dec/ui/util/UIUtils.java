package com.ps3dec.ui.util;

import com.ps3dec.util.Theme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

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

    public static File showFileChooser(Component parent, String title, String initialDir, String description, String... extensions) {
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
        if (field == null) return;
        field.putClientProperty("placeholder", placeholder);
        field.repaint();
    }

    public static String getPS3DecBinPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String binName = os.contains("win") ? "ps3dec.exe" : "ps3dec";
        
        // Try relative to project root
        File binFile = new File("bin/" + binName);
        if (binFile.exists()) return binFile.getAbsolutePath();
        
        // Try user.dir
        binFile = new File(System.getProperty("user.dir") + "/bin/" + binName);
        if (binFile.exists()) return binFile.getAbsolutePath();
        
        return binName; // Fallback to PATH
    }
}
