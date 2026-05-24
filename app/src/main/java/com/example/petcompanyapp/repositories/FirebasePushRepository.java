package com.petbook.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.UserProfileStorage;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class FirebasePushRepository {

    private static final String PREFS_NAME = "petbook_push";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private FirebasePushRepository() {
    }

    public static void syncCurrentSessionToken(Context context) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }

        String currentUserEmail = UserProfileStorage.getEmail(context, "");
        if (currentUserEmail == null || currentUserEmail.trim().isEmpty()) {
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.trim().isEmpty()) {
                        return;
                    }
                    storeLatestToken(context, token);
                    syncTokenForEmail(context, currentUserEmail, token);
                });
    }

    public static void storeLatestToken(Context context, String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_FCM_TOKEN, token.trim()).apply();

        String currentUserEmail = UserProfileStorage.getEmail(context, "");
        if (currentUserEmail != null && !currentUserEmail.trim().isEmpty() && FirebaseChatConfig.isEnabled(context)) {
            syncTokenForEmail(context, currentUserEmail, token.trim());
        }
    }

    public static String getStoredToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, "");
    }

    private static void syncTokenForEmail(Context context, @NonNull String email, @NonNull String token) {
        Map<String, Object> values = new HashMap<>();
        values.put("userKey", ChatIdentityUtils.userKeyFromEmail(email));
        values.put("email", email.trim().toLowerCase(java.util.Locale.ROOT));
        values.put("lastFcmToken", token);
        values.put("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token));
        values.put("updatedAtMillis", System.currentTimeMillis());

        FirebaseChatConfig.getFirestore(context)
                .collection("users")
                .document(ChatIdentityUtils.userKeyFromEmail(email))
                .set(values, com.google.firebase.firestore.SetOptions.merge());
    }
}
