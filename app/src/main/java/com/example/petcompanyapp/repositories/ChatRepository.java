package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;
import com.petbook.app.models.ChatMessage;
import com.petbook.app.models.ConversationSummary;
import com.petbook.app.models.User;

import java.util.ArrayList;
import java.util.List;

public final class ChatRepository {

    private ChatRepository() {
        // Repositorio local para inbox e mensagens.
    }

    public static List<ConversationSummary> getConversations(Context context, long currentUserId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, "
                        + "CASE WHEN c.user_one_id = ? THEN c.user_two_id ELSE c.user_one_id END AS partner_user_id, "
                        + "u.name AS partner_name, "
                        + "u.user_type AS partner_type, "
                        + "u.email AS partner_email, "
                        + "COALESCE(c.last_message_text, '') AS last_message_text, "
                        + "COALESCE(c.last_message_at_millis, c.created_at_millis) AS last_message_at_millis, "
                        + "(SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m "
                        + " WHERE m.conversation_id = c.id AND m.receiver_user_id = ? AND m.is_read = 0) AS unread_count "
                        + "FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c "
                        + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u "
                        + " ON u.id = CASE WHEN c.user_one_id = ? THEN c.user_two_id ELSE c.user_one_id END "
                        + "WHERE c.user_one_id = ? OR c.user_two_id = ? "
                        + "ORDER BY last_message_at_millis DESC",
                new String[]{
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId)
                }
        );

        try {
            List<ConversationSummary> conversations = new ArrayList<>();
            while (cursor.moveToNext()) {
                conversations.add(new ConversationSummary(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getLong(6),
                        cursor.getInt(7)
                ));
            }
            return conversations;
        } finally {
            cursor.close();
            db.close();
        }
    }

    public static List<ChatMessage> getMessages(Context context, long conversationId, long currentUserId) {
        markConversationAsRead(context, conversationId, currentUserId);

        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT m.id, m.conversation_id, m.sender_user_id, m.receiver_user_id, "
                        + "u.name AS sender_name, m.message_text, m.sent_at_millis, m.is_read "
                        + "FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m "
                        + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON u.id = m.sender_user_id "
                        + "WHERE m.conversation_id = ? "
                        + "ORDER BY m.sent_at_millis ASC, m.id ASC",
                new String[]{String.valueOf(conversationId)}
        );

        try {
            List<ChatMessage> messages = new ArrayList<>();
            while (cursor.moveToNext()) {
                messages.add(new ChatMessage(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getLong(3),
                        null,
                        null,
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getLong(6),
                        cursor.getInt(7) == 1
                ));
            }
            return messages;
        } finally {
            cursor.close();
            db.close();
        }
    }

    public static long findOrCreateConversation(Context context, long firstUserId, long secondUserId) {
        long userOneId = Math.min(firstUserId, secondUserId);
        long userTwoId = Math.max(firstUserId, secondUserId);

        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            Cursor cursor = db.query(
                    AppDatabaseHelper.TABLE_CONVERSATIONS,
                    new String[]{"id"},
                    "user_one_id = ? AND user_two_id = ?",
                    new String[]{String.valueOf(userOneId), String.valueOf(userTwoId)},
                    null,
                    null,
                    null
            );

            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }

            ContentValues values = new ContentValues();
            values.put("user_one_id", userOneId);
            values.put("user_two_id", userTwoId);
            values.put("created_at_millis", System.currentTimeMillis());
            values.put("last_message_text", "");
            values.put("last_message_at_millis", System.currentTimeMillis());
            return db.insert(AppDatabaseHelper.TABLE_CONVERSATIONS, null, values);
        } finally {
            db.close();
        }
    }

    public static ChatMessage sendMessage(
            Context context,
            long senderUserId,
            long receiverUserId,
            String messageText
    ) {
        String normalizedMessage = messageText == null ? "" : messageText.trim();
        if (normalizedMessage.isEmpty()) {
            return null;
        }

        long conversationId = findOrCreateConversation(context, senderUserId, receiverUserId);
        long sentAtMillis = System.currentTimeMillis();

        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("conversation_id", conversationId);
            values.put("sender_user_id", senderUserId);
            values.put("receiver_user_id", receiverUserId);
            values.put("message_text", normalizedMessage);
            values.put("sent_at_millis", sentAtMillis);
            values.put("is_read", 0);
            long messageId = db.insert(AppDatabaseHelper.TABLE_MESSAGES, null, values);

            ContentValues conversationValues = new ContentValues();
            conversationValues.put("last_message_text", normalizedMessage);
            conversationValues.put("last_message_at_millis", sentAtMillis);
            db.update(
                    AppDatabaseHelper.TABLE_CONVERSATIONS,
                    conversationValues,
                    "id = ?",
                    new String[]{String.valueOf(conversationId)}
            );

            User sender = UserRepository.findById(context, senderUserId);
            return new ChatMessage(
                    messageId,
                    conversationId,
                        senderUserId,
                        receiverUserId,
                        null,
                        null,
                        sender == null ? "" : sender.getName(),
                        normalizedMessage,
                        sentAtMillis,
                    false
            );
        } finally {
            db.close();
        }
    }

    public static void markConversationAsRead(Context context, long conversationId, long currentUserId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("is_read", 1);
            db.update(
                    AppDatabaseHelper.TABLE_MESSAGES,
                    values,
                    "conversation_id = ? AND receiver_user_id = ? AND is_read = 0",
                    new String[]{String.valueOf(conversationId), String.valueOf(currentUserId)}
            );
        } finally {
            db.close();
        }
    }

    public static ConversationSummary getConversationSummary(
            Context context,
            long currentUserId,
            long conversationId
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, "
                        + "CASE WHEN c.user_one_id = ? THEN c.user_two_id ELSE c.user_one_id END AS partner_user_id, "
                        + "u.name AS partner_name, "
                        + "u.user_type AS partner_type, "
                        + "u.email AS partner_email, "
                        + "COALESCE(c.last_message_text, '') AS last_message_text, "
                        + "COALESCE(c.last_message_at_millis, c.created_at_millis) AS last_message_at_millis, "
                        + "0 AS unread_count "
                        + "FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c "
                        + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u "
                        + " ON u.id = CASE WHEN c.user_one_id = ? THEN c.user_two_id ELSE c.user_one_id END "
                        + "WHERE c.id = ? AND (c.user_one_id = ? OR c.user_two_id = ?)",
                new String[]{
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId),
                        String.valueOf(conversationId),
                        String.valueOf(currentUserId),
                        String.valueOf(currentUserId)
                }
        );

        try {
            if (cursor.moveToFirst()) {
                return new ConversationSummary(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getLong(6),
                        cursor.getInt(7)
                );
            }
            return null;
        } finally {
            cursor.close();
            db.close();
        }
    }
}

