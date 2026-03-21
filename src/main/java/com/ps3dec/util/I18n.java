package com.ps3dec.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utilitário para Internacionalização (I18n).
 * Evita strings hardcoded e previne problemas de encoding/fonte no macOS
 * ao utilizar escapes Unicode nos arquivos de propriedades.
 */
public class I18n {
    private static ResourceBundle bundle;

    static {
        // Carrega o idioma salvo pelo usuário; se não houver, usa o Locale do sistema
        String savedTag = AppPreferences.getLanguage();
        Locale locale = savedTag.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(savedTag);
        loadBundle(locale);
    }

    public static void loadBundle(Locale locale) {
        try {
            bundle = ResourceBundle.getBundle("messages", locale);
        } catch (Exception e) {
            // Fallback para inglês se der qualquer erro
            bundle = ResourceBundle.getBundle("messages", Locale.ROOT);
        }
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
}
