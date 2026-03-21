package com.ps3dec.ui;

import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.ui.managers.TrayManager;
import com.ps3dec.ui.panels.BatchTabPanel;
import com.ps3dec.ui.panels.LogPanel;
import com.ps3dec.ui.panels.SingleTabPanel;
import com.ps3dec.util.AppPreferences;
import com.ps3dec.util.I18n;
import com.ps3dec.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Main application window.
 * Acts as a container for modular panels (SingleTabPanel, BatchTabPanel, LogPanel).
 */
public class MainFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JButton btnSingle, btnBatch;
    private JLabel titleLabel;

    // Modular Components
    private SingleTabPanel singleTabPanel;
    private BatchTabPanel batchTabPanel;
    private LogPanel logPanel;
    private TrayManager trayManager;

    public MainFrame() {
        setTitle("PS3 ISO Decryptor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);
        
        initUI();
        
        // Setup tray manager
        trayManager = new TrayManager(this);
        trayManager.setup();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 1. Header (Title + Settings button)
        add(createHeader(), BorderLayout.NORTH);

        // 2. Center Content (Tabs + Panels)
        JPanel centerArea = new JPanel(new BorderLayout(0, 20));
        centerArea.setOpaque(false);

        // Tab Selector Row
        centerArea.add(createTabSelector(), BorderLayout.NORTH);

        // Panels Container (uses CardLayout for tab switching)
        logPanel = new LogPanel();
        singleTabPanel = new SingleTabPanel(this, logPanel);
        batchTabPanel = new BatchTabPanel(this, logPanel);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.add(createCardWrapper(singleTabPanel), "SINGLE");
        cardPanel.add(createCardWrapper(batchTabPanel), "BATCH");

        centerArea.add(cardPanel, BorderLayout.CENTER);
        add(centerArea, BorderLayout.CENTER);

        // 3. Footer (Persistent Log Panel)
        add(logPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the top header with logo, title, and settings gear.
     */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 50));

        // Left Section: Logo + Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        
        try {
            JLabel logo = new JLabel(new ImageIcon(getClass().getResource("/assets/AppIcon.png")));
            left.add(logo);
            left.add(Box.createHorizontalStrut(15));
        } catch (Exception e) {
            // Fallback if icon is missing
        }
        
        titleLabel = new JLabel("PS3 ISO Decryptor");
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        left.add(titleLabel);
        header.add(left, BorderLayout.WEST);

        // Right Section: Settings Gear (Language Switcher)
        JLabel settingsBtn = new JLabel("\u2699");
        settingsBtn.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
        settingsBtn.setForeground(Theme.TEXT_SECONDARY);
        settingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settingsBtn.setToolTipText("Language / Idioma");
        settingsBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { showLanguageMenu(settingsBtn); }
            @Override
            public void mouseEntered(MouseEvent e) { settingsBtn.setForeground(Color.WHITE); }
            @Override
            public void mouseExited(MouseEvent e) { settingsBtn.setForeground(Theme.TEXT_SECONDARY); }
        });
        header.add(settingsBtn, BorderLayout.EAST);

        return header;
    }

    /**
     * Creates the tab buttons row (Single / Batch).
     */
    private JPanel createTabSelector() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        tabs.setOpaque(false);

        btnSingle = createTabButton(I18n.get("tab.single"), true, "SINGLE");
        btnBatch = createTabButton(I18n.get("tab.batch"), false, "BATCH");

        tabs.add(btnSingle);
        tabs.add(btnBatch);
        return tabs;
    }

    private JButton createTabButton(String text, boolean active, String cardName) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!isEnabled()) { // Active state indicator (disabled button means it's the current tab)
                    g.setColor(Theme.ACCENT);
                    g.fillRect(10, getHeight() - 3, getWidth() - 20, 3);
                }
            }
        };
        btn.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        btn.setForeground(active ? Color.WHITE : Theme.TEXT_SECONDARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setEnabled(!active);

        btn.addActionListener(e -> {
            cardLayout.show(cardPanel, cardName);
            btnSingle.setEnabled(cardName.equals("BATCH"));
            btnBatch.setEnabled(cardName.equals("SINGLE"));
            btnSingle.setForeground(btnSingle.isEnabled() ? Theme.TEXT_SECONDARY : Color.WHITE);
            btnBatch.setForeground(btnBatch.isEnabled() ? Theme.TEXT_SECONDARY : Color.WHITE);
        });
        return btn;
    }

    /**
     * Wraps a panel in a styled card with rounded corners and border.
     */
    private JPanel createCardWrapper(JPanel content) {
        JPanel card = new JPanel(new BorderLayout()) {
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
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        card.add(content, BorderLayout.CENTER);
        return card;
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
        
        item.addChangeListener(e -> {
            if (item.getModel().isArmed()) item.setBackground(Theme.BG_FIELD);
            else item.setBackground(Theme.BG_PANEL);
        });
        return item;
    }

    private void setLanguage(java.util.Locale locale) {
        java.util.Locale.setDefault(locale);
        I18n.loadBundle(locale);
        AppPreferences.setLanguage(locale.toLanguageTag());
        updateLocalizedText();
    }

    /**
     * Propagates localization update calls to all sub-components.
     */
    private void updateLocalizedText() {
        if (btnSingle != null) btnSingle.setText(I18n.get("tab.single"));
        if (btnBatch != null) btnBatch.setText(I18n.get("tab.batch"));

        if (singleTabPanel != null) singleTabPanel.updateLocalizedText();
        if (batchTabPanel != null) batchTabPanel.updateLocalizedText();
        if (logPanel != null) logPanel.updateLocalizedText();
        if (trayManager != null) trayManager.updateLocalizedText();

        revalidate();
        repaint();
    }
}
