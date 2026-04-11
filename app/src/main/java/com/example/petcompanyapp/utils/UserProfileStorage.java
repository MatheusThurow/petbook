package com.example.petcompanyapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserProfileStorage {

    private static final String PREF_NAME = "pet_company_profile";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TYPE = "type";

    private UserProfileStorage() {
        // Persistencia simples do perfil ativo.
    }

    public static void saveProfile(Context context, String name, String email, String userType) {
        saveProfile(context, null, name, email, userType);
    }

    public static void saveProfile(Context context, Long userId, String name, String email, String userType) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putLong(KEY_USER_ID, userId == null ? -1L : userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_TYPE, userType)
                .apply();
    }

    public static Long getUserId(Context context) {
        long userId = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_USER_ID, -1L);
        return userId >= 0 ? userId : null;
    }

    public static String getName(Context context, String fallback) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NAME, fallback);
    }

    public static String getEmail(Context context, String fallback) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, fallback);
    }

    public static String getUserType(Context context, String fallback) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TYPE, fallback);
    }

    public static void clearProfile(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
