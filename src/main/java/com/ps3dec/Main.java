package com.ps3dec;

import com.ps3dec.ui.MainFrame;

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

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
