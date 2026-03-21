package com.ps3dec;

import com.ps3dec.ui.MainFrame;

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Set locale before anything else
        String savedTag = com.ps3dec.util.AppPreferences.getLanguage();
        if (!savedTag.isEmpty()) {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag(savedTag));
        } else {
            java.util.Locale.setDefault(new java.util.Locale("pt", "BR"));
        }

        // Handle --clear command to reset preferences logic
        if (args.length > 0 && args[0].equals("--clear")) {
            com.ps3dec.util.AppPreferences.clear();
            System.out.println("Preferências limpas com sucesso.");
        }

        System.setProperty("apple.awt.antialiasing", "on");
        System.setProperty("apple.awt.textantialiasing", "on");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("apple.awt.application.name", "PS3 ISO Decryptor");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PS3 ISO Decryptor");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

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
