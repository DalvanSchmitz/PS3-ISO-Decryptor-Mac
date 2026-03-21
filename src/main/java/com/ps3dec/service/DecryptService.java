package com.ps3dec.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.io.File;
import java.util.function.Consumer;

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

                String statusMsg = "Quase lá...";
                if (pctDisplay < 20) statusMsg = "Lendo setores...";
                else if (pctDisplay < 50) statusMsg = "Descriptografando...";
                else if (pctDisplay < 75) statusMsg = "Escrevendo dados...";

                listener.onProgress(pct, statusMsg, timeStr);
            }
        });

        worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(
                        ps3decBin, isoPath,
                        "--dk", dkeyHex,
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

    public void cancel() {
        cancelled = true;
        if (animationTimer != null) animationTimer.stop();
        if (process != null) process.destroyForcibly();
    }
}
