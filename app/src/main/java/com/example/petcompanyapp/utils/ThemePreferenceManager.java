package com.petbook.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferenceManager {

    private static final String PREFS_NAME = "petbook_preferences";
    private static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";

    private ThemePreferenceManager() {
        // Persistencia simples da preferencia de tema.
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(
                isDarkModeEnabled(context)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_DARK_MODE_ENABLED, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        getPreferences(context)
                .edit()
                .putBoolean(KEY_DARK_MODE_ENABLED, enabled)
                .apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
