package com.petbook.app.utils;

import android.content.Context;

import com.petbook.app.R;

public final class FeatureFlags {

    private FeatureFlags() {
    }

    public static boolean useRemoteApi(Context context) {
        return context.getResources().getBoolean(R.bool.use_remote_api);
    }

    public static boolean useFirebaseChat(Context context) {
        return context.getResources().getBoolean(R.bool.use_firebase_chat);
    }
}

