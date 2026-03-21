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
    private static final String KEY_INITIALIZED  = "initialized";
    private static final String KEY_LANGUAGE     = "language";

    static {
        // Garantir que o app comece "zerado" na primeira vez que for aberto em uma máquina nova.
        if (!PREFS.getBoolean(KEY_INITIALIZED, false)) {
            clear();
            PREFS.putBoolean(KEY_INITIALIZED, true);
        }
    }

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

    // ── Idioma escolhido pelo usuário (BCP-47 tag, ex: "pt-BR", "en") ────────
    public static String getLanguage() {
        return PREFS.get(KEY_LANGUAGE, "");   // "" = usar locale do sistema na primeira vez
    }
    public static void setLanguage(String languageTag) {
        if (languageTag != null) PREFS.put(KEY_LANGUAGE, languageTag);
    }

    public static void clear() {
        try {
            PREFS.clear();
            PREFS.flush();
        } catch (Exception ignored) {}
    }
}
