package com.petbook.app.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.petbook.app.activities.LoginActivity;

public final class SessionUtils {

    private static Boolean testAuthenticatedOverride;

    private SessionUtils() {
    }

    public static void setTestAuthenticatedOverride(Boolean authenticated) {
        testAuthenticatedOverride = authenticated;
    }

    public static void clearTestAuthenticatedOverride() {
        testAuthenticatedOverride = null;
    }

    public static boolean isAuthenticated(Context context) {
        Long userId = UserProfileStorage.getUserId(context);
        if (userId == null) {
            return false;
        }

        if (testAuthenticatedOverride != null) {
            return testAuthenticatedOverride;
        }

        if (FirebaseChatConfig.isEnabled(context)) {
            return FirebaseAuth.getInstance().getCurrentUser() != null;
        }

        return true;
    }

    public static boolean requireAuthenticated(AppCompatActivity activity) {
        if (isAuthenticated(activity)) {
            return true;
        }

        UserProfileStorage.clearProfile(activity);
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return false;
    }

    public static void logout(AppCompatActivity activity) {
        if (FirebaseChatConfig.isEnabled(activity)) {
            FirebaseAuth.getInstance().signOut();
        }
        UserProfileStorage.clearProfile(activity);
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void openMainFlow(AppCompatActivity activity, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
