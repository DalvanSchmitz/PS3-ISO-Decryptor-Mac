package com.ps3dec.util;

import java.util.prefs.Preferences;

/**
 * Wrapper centralizado para preferências persistentes do usuário.
 * Usa o registro nativo do OS (macOS NSUserDefaults / Windows Registry / Linux XML).
 *
 * Todas as chaves de preferência do app devem ser definidas e acessadas aqui.
 */
public class AppPreferences {

    private static final Preferences PREFS =
            Preferences.userRoot().node("com/ps3dec/app");

    // --- Chaves internas ---
    private static final String KEY_OUTPUT_DIR   = "outputDir";
    private static final String KEY_ISO_DIR      = "isoDir";
    private static final String KEY_BATCH_DIR    = "batchDir";
    private static final String KEY_LAST_ISO     = "lastIsoPath";
    private static final String KEY_LAST_DKEY    = "lastDkeyPath";

    private AppPreferences() { /* classe utilitária, sem instância */ }

    // ── Pasta de destino (compartilhada entre abas) ──────────────────────────
    public static String getOutputDir() {
        return PREFS.get(KEY_OUTPUT_DIR, System.getProperty("user.home") + "/Desktop");
    }
    public static void setOutputDir(String path) {
        if (path != null && !path.trim().isEmpty()) PREFS.put(KEY_OUTPUT_DIR, path);
    }

    // ── Último diretório aberto no chooser de ISO ────────────────────────────
    public static String getIsoDir() {
        return PREFS.get(KEY_ISO_DIR, System.getProperty("user.home"));
    }
    public static void setIsoDir(String dirPath) {
        if (dirPath != null && !dirPath.trim().isEmpty()) PREFS.put(KEY_ISO_DIR, dirPath);
    }

    // ── Pasta do lote ────────────────────────────────────────────────────────
    public static String getBatchDir() {
        return PREFS.get(KEY_BATCH_DIR, "");
    }
    public static void setBatchDir(String path) {
        if (path != null && !path.trim().isEmpty()) PREFS.put(KEY_BATCH_DIR, path);
    }

    // ── Caminho completo do último ISO usado (aba Única) ─────────────────────
    public static String getLastIsoPath() {
        return PREFS.get(KEY_LAST_ISO, "");
    }
    public static void setLastIsoPath(String path) {
        if (path != null && !path.trim().isEmpty()) PREFS.put(KEY_LAST_ISO, path);
    }

    // ── Caminho completo do último DKEY usado (aba Única) ────────────────────
    public static String getLastDkeyPath() {
        return PREFS.get(KEY_LAST_DKEY, "");
    }
    public static void setLastDkeyPath(String path) {
        if (path != null && !path.trim().isEmpty()) PREFS.put(KEY_LAST_DKEY, path);
    }
}
