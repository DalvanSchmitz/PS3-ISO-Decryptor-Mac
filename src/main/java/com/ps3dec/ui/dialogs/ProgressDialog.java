package com.ps3dec.ui.dialogs;

import com.ps3dec.util.Theme;
import com.ps3dec.util.FormatUtils;
import com.ps3dec.ui.components.UIFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel elapsedLabel;
    private final JLabel statusLabel;
    private final Runnable onCancel;

    public ProgressDialog(JFrame parent, String isoName, long totalSize, Runnable onCancel) {
        super(parent, "Descriptografando...", false); // não-modal: janela principal permanece acessível
        this.onCancel = onCancel;
        
        setSize(540, 260);
        setResizable(false);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        JPanel topPanel = new JPanel(new BorderLayout(0, 4));
        topPanel.setOpaque(false);

        JLabel fileLabel = new JLabel(isoName);
        fileLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        fileLabel.setForeground(Theme.TEXT_PRIMARY);
        topPanel.add(fileLabel, BorderLayout.NORTH);

        JLabel sizeLabel = new JLabel("Tamanho: " + FormatUtils.formatSize(totalSize));
        sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sizeLabel.setForeground(Theme.TEXT_SECONDARY);
        topPanel.add(sizeLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 12, 0));

        progressBar = new JProgressBar(0, 1000);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Iniciando...");
        progressBar.setFont(new Font("SansSerif", Font.BOLD, 12));
        progressBar.setForeground(Theme.ACCENT);
        progressBar.setBackground(Theme.BG_FIELD);
        progressBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        progressBar.setPreferredSize(new Dimension(480, 30));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        centerPanel.add(progressBar);

        centerPanel.add(Box.createVerticalStrut(14));

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setOpaque(false);

        elapsedLabel = createStatLabel("Tempo Decorrido", "00:00");
        statusLabel = createStatLabel("Status", "Processando...");

        statsPanel.add(elapsedLabel);
        statsPanel.add(statusLabel);

        centerPanel.add(statsPanel);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.setOpaque(false);

        JButton cancelButton = UIFactory.createActionButton("Cancelar", Color.WHITE, Theme.ACCENT_RED, e -> cancel());
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        setLocationRelativeTo(parent);
    }

    public void updateProgress(int targetPct, String statusMsg, String timeStr) {
        progressBar.setValue(targetPct);
        progressBar.setString(String.format("%.1f%%", targetPct / 10.0));

        elapsedLabel.setText(
                "<html><center><font color='#9496A5' size='2'>Tempo Decorrido</font><br>" +
                        "<font color='#DCDDE6'>" + timeStr + "</font></center></html>");

        statusLabel.setText(statHtml("Status", statusMsg));
    }

    public void finishWithAnimation(Runnable onComplete) {
        final int startVal = progressBar.getValue();
        final int targetVal = 1000;
        final long animStart = System.currentTimeMillis();
        final int ANIM_DURATION = 800; // milissegundos

        statusLabel.setText(statHtml("Status", "Concluído!"));

        Timer completionAnim = new Timer(16, null); // ~60fps
        completionAnim.addActionListener(tick -> {
            long now = System.currentTimeMillis();
            double t = Math.min(1.0, (now - animStart) / (double) ANIM_DURATION);

            // Easing: ease-out cubic
            double eased = 1.0 - Math.pow(1.0 - t, 3);
            int value = startVal + (int) ((targetVal - startVal) * eased);

            progressBar.setValue(value);
            progressBar.setString(String.format("%.1f%%", value / 10.0));

            // Transição de cor: ACCENT → GREEN
            int r = (int) (Theme.ACCENT.getRed() + (Theme.ACCENT_GREEN.getRed() - Theme.ACCENT.getRed()) * eased);
            int g = (int) (Theme.ACCENT.getGreen() + (Theme.ACCENT_GREEN.getGreen() - Theme.ACCENT.getGreen()) * eased);
            int b = (int) (Theme.ACCENT.getBlue() + (Theme.ACCENT_GREEN.getBlue() - Theme.ACCENT.getBlue()) * eased);
            progressBar.setForeground(new Color(
                    Math.max(0, Math.min(255, r)),
                    Math.max(0, Math.min(255, g)),
                    Math.max(0, Math.min(255, b))));

            if (t >= 1.0) {
                ((Timer) tick.getSource()).stop();

                // Pausa breve pra mostrar o 100% antes de fechar
                Timer pause = new Timer(600, p -> {
                    ((Timer) p.getSource()).stop();
                    dispose();
                    if (onComplete != null) onComplete.run();
                });
                pause.setRepeats(false);
                pause.start();
            }
        });
        completionAnim.start();
    }

    private void cancel() {
        if (onCancel != null) onCancel.run();
        dispose();
    }

    private String statHtml(String title, String value) {
        return "<html><center><font color='#9496A5' size='2'>" + title + "</font><br>" +
                "<font color='#DCDDE6'>" + value + "</font></center></html>";
    }

    private JLabel createStatLabel(String title, String value) {
        JLabel label = new JLabel(statHtml(title, value), SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return label;
    }
}
