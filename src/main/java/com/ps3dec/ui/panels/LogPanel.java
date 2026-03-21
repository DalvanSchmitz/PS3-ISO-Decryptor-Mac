package com.ps3dec.ui.panels;

import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.util.I18n;
import com.ps3dec.util.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Encapsulates the log text area and its visibility toggle.
 */
public class LogPanel extends JPanel {

    private JTextArea logArea;
    private JScrollPane logScrollPane;
    private JButton btnShowLogs;
    private boolean logsVisible = false;

    public LogPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        initComponents();
    }

    private void initComponents() {
        JPanel logHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        logHeader.setOpaque(false);

        btnShowLogs = UIFactory.createActionButton(I18n.get("btn.show_logs"), Theme.TEXT_SECONDARY, Theme.BG_FIELD, e -> toggleLogsVisibility());
        btnShowLogs.setPreferredSize(new Dimension(160, 32));
        btnShowLogs.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        logHeader.add(btnShowLogs);
        add(logHeader, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(15, 15, 20));
        logArea.setForeground(new Color(180, 182, 200));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(10, 10, 10, 10));

        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR));
        logScrollPane.setPreferredSize(new Dimension(0, 150));
        logScrollPane.setVisible(false);
        add(logScrollPane, BorderLayout.CENTER);
    }

    private void toggleLogsVisibility() {
        logsVisible = !logsVisible;
        logScrollPane.setVisible(logsVisible);
        updateLogToggleButton();
        revalidate();
        repaint();
    }

    public void updateLogToggleButton() {
        if (btnShowLogs != null) {
            btnShowLogs.setText(logsVisible ? I18n.get("btn.hide_logs") : I18n.get("btn.show_logs"));
        }
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateLocalizedText() {
        updateLogToggleButton();
    }
}
