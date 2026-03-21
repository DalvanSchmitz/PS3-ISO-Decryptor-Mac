package com.ps3dec.ui.managers;

import com.ps3dec.util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Manages the System Tray icon and minimize-to-tray behavior.
 */
public class TrayManager {

    private final JFrame frame;
    private TrayIcon trayIcon;
    private MenuItem openItem;
    private MenuItem exitItem;

    public TrayManager(JFrame frame) {
        this.frame = frame;
    }

    public void setup() {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/AppIcon.png"));
            
            trayIcon = new TrayIcon(icon, "PS3 ISO Decryptor");
            trayIcon.setImageAutoSize(true);

            updateMenu();

            trayIcon.addActionListener(e -> restoreFrame());
            tray.add(trayIcon);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowIconified(WindowEvent e) {
                    frame.setVisible(false);
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to setup system tray: " + e.getMessage());
        }
    }

    private void updateMenu() {
        PopupMenu popup = new PopupMenu();
        
        openItem = new MenuItem(I18n.get("tray.open"));
        openItem.addActionListener(e -> restoreFrame());
        
        exitItem = new MenuItem(I18n.get("tray.exit"));
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon.setPopupMenu(popup);
    }

    public void updateLocalizedText() {
        if (trayIcon != null) {
            updateMenu();
        }
    }

    private void restoreFrame() {
        frame.setVisible(true);
        frame.setExtendedState(JFrame.NORMAL);
        frame.toFront();
    }
}
