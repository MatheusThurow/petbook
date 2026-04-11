package com.example.petcompanyapp.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.petcompanyapp.database.AppDatabaseHelper;
import com.example.petcompanyapp.models.User;

public final class UserRepository {

    private UserRepository() {
        // Repositorio persistido em SQLite local.
    }

    public static User authenticate(Context context, String email, String password) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_USERS,
                null,
                "LOWER(email) = LOWER(?) AND password = ? AND is_active = 1",
                new String[]{email, password},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return mapUser(cursor);
            }
        } finally {
            cursor.close();
            db.close();
        }
        return null;
    }

    public static User authenticate(Context context, String email, String password, String userType) {
        User authenticatedUser = authenticate(context, email, password);
        if (authenticatedUser == null) {
            return null;
        }

        if (userType == null || userType.trim().isEmpty()) {
            return authenticatedUser;
        }

        return userType.equals(authenticatedUser.getUserType()) ? authenticatedUser : null;
    }

    public static User register(
            Context context,
            String userType,
            String name,
            String email,
            String password,
            String document
    ) {
        if (findByEmail(context, email) != null) {
            return null;
        }

        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_type", userType);
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("document_number", document);
        values.put("is_active", 1);
        long userId = db.insert(AppDatabaseHelper.TABLE_USERS, null, values);
        db.close();

        if (userId < 0) {
            return null;
        }

        return new User(userId, userType, name, email, password, document, true);
    }

    public static User findById(Context context, Long userId) {
        if (userId == null) {
            return null;
        }

        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_USERS,
                null,
                "id = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return mapUser(cursor);
            }
        } finally {
            cursor.close();
            db.close();
        }
        return null;
    }

    public static User findByEmail(Context context, String email) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_USERS,
                null,
                "LOWER(email) = LOWER(?)",
                new String[]{email},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return mapUser(cursor);
            }
        } finally {
            cursor.close();
            db.close();
        }
        return null;
    }

    public static boolean isValidActiveUser(Context context, Long userId) {
        User user = findById(context, userId);
        return user != null && user.isActive();
    }

    public static boolean updateProfile(Context context, long userId, String name, String email) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        int updatedRows = db.update(
                AppDatabaseHelper.TABLE_USERS,
                values,
                "id = ?",
                new String[]{String.valueOf(userId)}
        );
        db.close();
        return updatedRows > 0;
    }

    public static boolean resetPassword(
            Context context,
            String email,
            String document,
            String newPassword
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        int updatedRows = db.update(
                AppDatabaseHelper.TABLE_USERS,
                values,
                "LOWER(email) = LOWER(?) AND document_number = ? AND is_active = 1",
                new String[]{email, document}
        );
        db.close();
        return updatedRows > 0;
    }

    public static User findOrCreateGoogleUser(
            Context context,
            String userType,
            String name,
            String email
    ) {
        User existingUser = findByEmail(context, email);
        if (existingUser != null) {
            return existingUser;
        }

        String fallbackName = (name == null || name.trim().isEmpty()) ? email : name;
        String fallbackDocument = email == null ? "google-user" : email;
        return register(
                context,
                userType,
                fallbackName,
                email,
                "google-auth",
                fallbackDocument
        );
    }

    private static User mapUser(Cursor cursor) {
        return new User(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("user_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("password")),
                cursor.getString(cursor.getColumnIndexOrThrow("document_number")),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
        );
    }
}
