package com.example.petcompanyapp.network;

import android.content.Context;

import com.example.petcompanyapp.R;

public final class ApiConfig {

    private ApiConfig() {
    }

    public static String getBaseUrl(Context context) {
        String value = context.getString(R.string.api_base_url).trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
