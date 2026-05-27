package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;
import com.petbook.app.models.AppNotification;

import java.util.ArrayList;
import java.util.List;

public final class NotificationRepository {

    private NotificationRepository() {
    }

    public static void addNotification(
            Context context,
            long recipientUserId,
            String type,
            String title,
            String message,
            Long relatedPostId,
            String relatedPostType,
            Long relatedUserId,
            String relatedUserName,
            String relatedUserEmail,
            Long relatedConversationId
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("recipient_user_id", recipientUserId);
            values.put("notification_type", type);
            values.put("title", title);
            values.put("message_text", message);
            putNullableLong(values, "related_post_id", relatedPostId);
            values.put("related_post_type", relatedPostType);
            putNullableLong(values, "related_user_id", relatedUserId);
            values.put("related_user_name", relatedUserName);
            values.put("related_user_email", relatedUserEmail);
            putNullableLong(values, "related_conversation_id", relatedConversationId);
            values.put("created_at_millis", System.currentTimeMillis());
            values.put("is_read", 0);
            db.insert(AppDatabaseHelper.TABLE_NOTIFICATIONS, null, values);
        } finally {
            db.close();
        }
    }

    public static List<AppNotification> getNotificationsForUser(Context context, long userId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                "recipient_user_id = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                "created_at_millis DESC, id DESC"
        );

        try {
            List<AppNotification> notifications = new ArrayList<>();
            while (cursor.moveToNext()) {
                notifications.add(mapNotification(cursor));
            }
            return notifications;
        } finally {
            cursor.close();
            db.close();
        }
    }

    public static int getUnreadCount(Context context, long userId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_NOTIFICATIONS
                        + " WHERE recipient_user_id = ? AND is_read = 0",
                new String[]{String.valueOf(userId)}
        );
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
            db.close();
        }
    }

    public static void markAsRead(Context context, long notificationId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("is_read", 1);
            db.update(
                    AppDatabaseHelper.TABLE_NOTIFICATIONS,
                    values,
                    "id = ?",
                    new String[]{String.valueOf(notificationId)}
            );
        } finally {
            db.close();
        }
    }

    public static void markAllAsRead(Context context, long userId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("is_read", 1);
            db.update(
                    AppDatabaseHelper.TABLE_NOTIFICATIONS,
                    values,
                    "recipient_user_id = ? AND is_read = 0",
                    new String[]{String.valueOf(userId)}
            );
        } finally {
            db.close();
        }
    }

    public static void deleteNotification(Context context, long notificationId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            db.delete(
                    AppDatabaseHelper.TABLE_NOTIFICATIONS,
                    "id = ?",
                    new String[]{String.valueOf(notificationId)}
            );
        } finally {
            db.close();
        }
    }

    public static void deleteAllForUser(Context context, long userId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            db.delete(
                    AppDatabaseHelper.TABLE_NOTIFICATIONS,
                    "recipient_user_id = ?",
                    new String[]{String.valueOf(userId)}
            );
        } finally {
            db.close();
        }
    }

    private static AppNotification mapNotification(Cursor cursor) {
        return new AppNotification(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("recipient_user_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("notification_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("message_text")),
                getNullableLong(cursor, "related_post_id"),
                cursor.getString(cursor.getColumnIndexOrThrow("related_post_type")),
                getNullableLong(cursor, "related_user_id"),
                cursor.getString(cursor.getColumnIndexOrThrow("related_user_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("related_user_email")),
                getNullableLong(cursor, "related_conversation_id"),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at_millis")),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1
        );
    }

    private static void putNullableLong(ContentValues values, String key, Long value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private static Long getNullableLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }
}
