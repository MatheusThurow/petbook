package com.example.petcompanyapp.utils;

import android.content.Context;

import com.example.petcompanyapp.R;

public final class FeatureFlags {

    private FeatureFlags() {
    }

    public static boolean useRemoteApi(Context context) {
        return context.getResources().getBoolean(R.bool.use_remote_api);
    }
}
