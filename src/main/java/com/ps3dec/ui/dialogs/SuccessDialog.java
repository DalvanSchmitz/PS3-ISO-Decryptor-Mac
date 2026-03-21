package com.ps3dec.ui.dialogs;

import com.ps3dec.ui.components.UIFactory;
import com.ps3dec.util.I18n;
import com.ps3dec.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SuccessDialog extends JDialog {

    public SuccessDialog(JFrame parent, String filePath) {
        super(parent, I18n.get("dialog.success.title"), true);
        setSize(480, 200);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(0, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JLabel icon = new JLabel(I18n.get("dialog.success.message"), SwingConstants.CENTER);
        icon.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        icon.setForeground(Theme.ACCENT_GREEN);
        add(icon, BorderLayout.NORTH);

        JTextField pathField = UIFactory.createTextField("");
        pathField.setText(filePath);
        pathField.setEditable(false);
        pathField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        add(pathField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        btnPanel.add(UIFactory.createActionButton(I18n.get("btn.open_folder"), Color.WHITE, Theme.ACCENT, e -> {
            try {
                Desktop.getDesktop().open(new File(filePath).getParentFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, I18n.get("alert.error_open_folder"));
            }
        }));
        btnPanel.add(UIFactory.createActionButton(I18n.get("btn.close"), Theme.TEXT_SECONDARY, new Color(55, 57, 72), e -> dispose()));

        add(btnPanel, BorderLayout.SOUTH);
        setLocationRelativeTo(parent);
    }
}
