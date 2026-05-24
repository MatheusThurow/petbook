package com.petbook.app;

import android.app.Application;

import com.petbook.app.utils.ThemePreferenceManager;

public class PetBookApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferenceManager.applySavedTheme(this);
    }
}
