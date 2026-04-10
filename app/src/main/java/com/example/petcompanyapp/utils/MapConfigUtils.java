package com.example.petcompanyapp.utils;

import android.content.Context;

import com.example.petcompanyapp.R;

public final class MapConfigUtils {

    private static final String PLACEHOLDER_KEY = "ADD_YOUR_GOOGLE_MAPS_API_KEY";

    private MapConfigUtils() {
        // Utilitario para validar configuracao do Google Maps.
    }

    public static boolean hasConfiguredMapsKey(Context context) {
        String apiKey = context.getString(R.string.google_maps_key);
        return apiKey != null
                && !apiKey.trim().isEmpty()
                && !PLACEHOLDER_KEY.equals(apiKey.trim());
    }
}
