package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.AppNotification;
import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class FirebaseNotificationRepository {

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface NotificationsCallback {
        void onSuccess(List<AppNotification> notifications);
        void onError(String message);
    }

    public interface CountCallback {
        void onSuccess(int unreadCount);
        void onError(String message);
    }

    private FirebaseNotificationRepository() {
    }

    public static void addNotification(
            Context context,
            String recipientEmail,
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
        if (!FirebaseChatConfig.isEnabled(context) || recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        java.util.Map<String, Object> values = new java.util.HashMap<>();
        values.put("id", System.currentTimeMillis());
        values.put("recipientKey", ChatIdentityUtils.userKeyFromEmail(recipientEmail));
        values.put("recipientEmail", recipientEmail.trim().toLowerCase());
        values.put("notificationType", type);
        values.put("title", title);
        values.put("messageText", message);
        values.put("relatedPostId", relatedPostId);
        values.put("relatedPostType", relatedPostType);
        values.put("relatedUserId", relatedUserId);
        values.put("relatedUserName", relatedUserName);
        values.put("relatedUserEmail", relatedUserEmail);
        values.put("relatedConversationId", relatedConversationId);
        values.put("createdAtMillis", System.currentTimeMillis());
        values.put("read", false);

        notificationsCollection(context).add(values);
    }

    public static void getNotifications(
            Context context,
            String currentUserEmail,
            @NonNull NotificationsCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return;
        }

        notificationsCollection(context)
                .whereEqualTo("recipientKey", ChatIdentityUtils.userKeyFromEmail(currentUserEmail))
                .get()
                .addOnSuccessListener(result -> {
                    List<AppNotification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : result) {
                        notifications.add(mapNotification(document));
                    }
                    notifications.sort((first, second) -> Long.compare(second.getCreatedAtMillis(), first.getCreatedAtMillis()));
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void getUnreadCount(
            Context context,
            String currentUserEmail,
            @NonNull CountCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess(0);
            return;
        }

        notificationsCollection(context)
                .whereEqualTo("recipientKey", ChatIdentityUtils.userKeyFromEmail(currentUserEmail))
                .get()
                .addOnSuccessListener(result -> {
                    int unreadCount = 0;
                    for (QueryDocumentSnapshot document : result) {
                        if (!Boolean.TRUE.equals(document.getBoolean("read"))) {
                            unreadCount++;
                        }
                    }
                    callback.onSuccess(unreadCount);
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void markAsRead(Context context, long notificationId) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }
        notificationsCollection(context)
                .whereEqualTo("id", notificationId)
                .get()
                .addOnSuccessListener(result -> {
                    for (QueryDocumentSnapshot document : result) {
                        document.getReference().update("read", true);
                    }
                });
    }

    public static void markAllAsRead(Context context, String currentUserEmail) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }
        notificationsCollection(context)
                .whereEqualTo("recipientKey", ChatIdentityUtils.userKeyFromEmail(currentUserEmail))
                .get()
                .addOnSuccessListener(result -> {
                    for (QueryDocumentSnapshot document : result) {
                        if (!Boolean.TRUE.equals(document.getBoolean("read"))) {
                            document.getReference().update("read", true);
                        }
                    }
                });
    }

    public static void deleteNotification(
            Context context,
            long notificationId,
            @NonNull OperationCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess();
            return;
        }

        notificationsCollection(context)
                .whereEqualTo("id", notificationId)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    final int[] pending = {result.size()};
                    final boolean[] failed = {false};
                    for (QueryDocumentSnapshot document : result) {
                        document.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    pending[0]--;
                                    if (pending[0] == 0 && !failed[0]) {
                                        callback.onSuccess();
                                    }
                                })
                                .addOnFailureListener(exception -> {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        callback.onError(exception.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void deleteAllNotifications(
            Context context,
            String currentUserEmail,
            @NonNull OperationCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess();
            return;
        }

        notificationsCollection(context)
                .whereEqualTo("recipientKey", ChatIdentityUtils.userKeyFromEmail(currentUserEmail))
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    final int[] pending = {result.size()};
                    final boolean[] failed = {false};
                    for (QueryDocumentSnapshot document : result) {
                        document.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    pending[0]--;
                                    if (pending[0] == 0 && !failed[0]) {
                                        callback.onSuccess();
                                    }
                                })
                                .addOnFailureListener(exception -> {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        callback.onError(exception.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    private static AppNotification mapNotification(QueryDocumentSnapshot document) {
        Long notificationId = document.getLong("id");
        long id = notificationId == null ? Math.abs(document.getId().hashCode()) : notificationId;
        return new AppNotification(
                id,
                0L,
                safe(document.getString("notificationType")),
                safe(document.getString("title")),
                safe(document.getString("messageText")),
                document.getLong("relatedPostId"),
                safe(document.getString("relatedPostType")),
                document.getLong("relatedUserId"),
                safe(document.getString("relatedUserName")),
                safe(document.getString("relatedUserEmail")),
                document.getLong("relatedConversationId"),
                safeLong(document.getLong("createdAtMillis")),
                Boolean.TRUE.equals(document.getBoolean("read"))
        );
    }

    private static CollectionReference notificationsCollection(Context context) {
        return FirebaseChatConfig.getFirestore(context).collection("notifications");
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
