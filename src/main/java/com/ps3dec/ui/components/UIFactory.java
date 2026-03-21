package com.ps3dec.ui.components;

import com.ps3dec.util.Theme;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class UIFactory {

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        label.setForeground(Theme.TEXT_SECONDARY);
        label.setPreferredSize(new Dimension(80, 40));
        return label;
    }

    /**
     * Creates a styled text field with a custom painted placeholder.
     * The placeholder is stored in the ClientProperty "placeholder" so it can be
     * updated at runtime (e.g., on language switch) via field.putClientProperty("placeholder", newText).
     */
    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Read current placeholder from client property so it survives language changes
                Object prop = getClientProperty("placeholder");
                String ph = (prop instanceof String) ? (String) prop : null;
                if (getText().isEmpty() && !isFocusOwner() && ph != null && !ph.isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 152, 170));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    Insets ins = getInsets();
                    int y = ins.top + (getHeight() - ins.top - ins.bottom - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(ph, ins.left + 4, y);
                    g2.dispose();
                }
            }
        };
        // Seed the client property with the initial value
        field.putClientProperty("placeholder", placeholder);
        field.setPreferredSize(new Dimension(0, 40));
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        field.setBackground(Theme.BG_FIELD);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        // Repaint on focus change so placeholder appears/disappears
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { field.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e)   { field.repaint(); }
        });
        return field;
    }

    public static JButton createActionButton(String text, Color fg, Color bg, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color currentBg = getBackground();
                if (getModel().isPressed()) {
                    g2.setColor(currentBg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(currentBg.brighter());
                } else {
                    g2.setColor(currentBg);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 40));

        if (action != null) {
            btn.addActionListener(action);
        }

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.repaint(); }
        });

        return btn;
    }

    public static JButton createBrowseButton(ActionListener action) {
        JButton btn = createActionButton("...", Theme.TEXT_PRIMARY, Theme.BG_FIELD, action);
        btn.setPreferredSize(new Dimension(50, 40));
        btn.setBorder(new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 12, 12));
                g2.dispose();
            }
        });
        return btn;
    }
}
