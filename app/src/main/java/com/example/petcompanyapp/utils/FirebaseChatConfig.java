package com.petbook.app.utils;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public final class FirebaseChatConfig {

    private FirebaseChatConfig() {
    }

    public static boolean isConfigured(Context context) {
        FirebaseApp app = FirebaseApp.initializeApp(context);
        return app != null || !FirebaseApp.getApps(context).isEmpty();
    }

    public static boolean isEnabled(Context context) {
        return FeatureFlags.useFirebaseChat(context) && isConfigured(context);
    }

    public static FirebaseFirestore getFirestore(Context context) {
        FirebaseApp app = FirebaseApp.initializeApp(context);
        if (app != null) {
            return FirebaseFirestore.getInstance(app);
        }

        List<FirebaseApp> apps = FirebaseApp.getApps(context);
        if (!apps.isEmpty()) {
            return FirebaseFirestore.getInstance(apps.get(0));
        }

        throw new IllegalStateException("Firebase nao configurado.");
    }
}
