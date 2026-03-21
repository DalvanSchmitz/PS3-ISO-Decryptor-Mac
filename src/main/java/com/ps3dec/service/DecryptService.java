package com.ps3dec.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import com.ps3dec.util.I18n;

public class DecryptService {

    public interface DecryptListener {
        void onProgress(int targetPct, String statusText, String elapsedStr);
        void onSuccess(String outputFileName);
        void onError(String errorMsg);
        void onFinish();
    }

    private Process process;
    private SwingWorker<Integer, Void> worker;
    private Timer animationTimer;
    private boolean cancelled = false;
    private boolean processFinished = false;
    private long startTime;

    /**
     * Starts the decryption process.
     *
     * @param logConsumer Optional consumer that receives stdout lines from the binary.
     *                    Pass null to discard logs (legacy behaviour).
     */
    public void startDecryption(String ps3decBin, String isoPath, String dkeyHex,
                                String outputDir, String outputFileName,
                                DecryptListener listener, Consumer<String> logConsumer) {
        cancelled = false;
        processFinished = false;
        startTime = System.currentTimeMillis();

        // Simulated progress up to 85 %
        final double TIME_CONSTANT = 30.0;
        final double MAX_SIMULATED = 850.0;

        animationTimer = new Timer(50, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double elapsedSec = elapsed / 1000.0;

            long totalSec = elapsed / 1000;
            String timeStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);

            if (!processFinished) {
                double progress = MAX_SIMULATED * (1.0 - Math.exp(-elapsedSec / TIME_CONSTANT));
                int pct = (int) progress;
                double pctDisplay = pct / 10.0;

                String statusMsg = I18n.get("decrypt.status.almost_there");
                if (pctDisplay < 20) statusMsg = I18n.get("decrypt.status.reading");
                else if (pctDisplay < 50) statusMsg = I18n.get("decrypt.status.decrypting");
                else if (pctDisplay < 75) statusMsg = I18n.get("decrypt.status.writing");

                listener.onProgress(pct, statusMsg, timeStr);
            }
        });

        worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                // Determine if dkeyHex is a file or a raw string
                String finalDkeyHex = dkeyHex;
                File dkeyFile = new File(dkeyHex);
                if (dkeyFile.exists() && dkeyFile.isFile()) {
                    try (BufferedReader br = new BufferedReader(new java.io.FileReader(dkeyFile))) {
                        finalDkeyHex = br.readLine().trim();
                    } catch (Exception ignored) {}
                }

                ProcessBuilder pb = new ProcessBuilder(
                        ps3decBin, isoPath,
                        "--dk", finalDkeyHex,
                        "--output-dir", outputDir,
                        "--skip");
                pb.redirectErrorStream(true);
                process = pb.start();

                // Drain stdout — forward each line to logConsumer if provided
                Thread outputDrain = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (logConsumer != null) {
                                final String logLine = line;
                                javax.swing.SwingUtilities.invokeLater(() -> logConsumer.accept(logLine));
                            }
                        }
                    } catch (Exception ignored) {}
                });
                outputDrain.setDaemon(true);
                outputDrain.start();

                return process.waitFor();
            }

            @Override
            protected void done() {
                processFinished = true;
                animationTimer.stop();
                listener.onFinish();

                try {
                    int exitCode = get();
                    if (cancelled) return;

                    if (exitCode == 0) {
                        listener.onSuccess(new File(outputDir, outputFileName).getAbsolutePath());
                    } else {
                        listener.onError("Erro no decrypt (código " + exitCode + ")");
                    }
                } catch (Exception e) {
                    if (!cancelled) listener.onError("Erro: " + e.getMessage());
                }
            }
        };

        animationTimer.start();
        worker.execute();
    }

    /** Backward-compatible overload (no log consumer). */
    public void startDecryption(String ps3decBin, String isoPath, String dkeyHex,
                                String outputDir, String outputFileName,
                                DecryptListener listener) {
        startDecryption(ps3decBin, isoPath, dkeyHex, outputDir, outputFileName, listener, null);
    }

    public String extractGameIdFromFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        
        File f = new File(filePath);
        if (!f.exists()) return "";

        java.util.regex.Pattern idPat = java.util.regex.Pattern.compile("([A-Z]{4})[- ]?(\\d{5})");

        // 1. Check filename
        java.util.regex.Matcher m = idPat.matcher(f.getName().toUpperCase());
        if (m.find()) return m.group(1) + m.group(2);

        // 2. Scan internal ISO bytes (first 2MB)
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buffer = new byte[2 * 1024 * 1024];
            int read = fis.read(buffer);
            if (read > 0) {
                String content = new String(buffer, 0, read, StandardCharsets.US_ASCII);
                m = idPat.matcher(content);
                if (m.find()) return m.group(1) + m.group(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public void cancel() {
        cancelled = true;
        if (animationTimer != null) animationTimer.stop();
        if (process != null) process.destroyForcibly();
    }
}
