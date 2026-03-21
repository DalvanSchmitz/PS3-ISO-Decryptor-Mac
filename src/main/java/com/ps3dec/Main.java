package com.ps3dec;

import com.ps3dec.ui.MainFrame;

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Fix for macOS character dropping/breaking
        System.setProperty("apple.awt.antialiasing", "on");
        System.setProperty("apple.awt.textantialiasing", "on");
        System.setProperty("file.encoding", "UTF-8");

        // Handle --clear command to reset preferences logic
        if (args.length > 0 && args[0].equals("--clear")) {
            com.ps3dec.util.AppPreferences.clear();
            System.out.println("Preferências limpas com sucesso.");
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored2) {}
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();

            try {
                java.net.URL iconURL = Main.class.getResource("/AppIcon.png");
                if (iconURL != null) {
                    Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
                    mainFrame.setIconImage(icon);
                    if (Taskbar.isTaskbarSupported()) {
                        Taskbar taskbar = Taskbar.getTaskbar();
                        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                            taskbar.setIconImage(icon);
                        }
                    }
                }
            } catch (Exception ignored) {}

            mainFrame.setVisible(true);
        });
    }
}
