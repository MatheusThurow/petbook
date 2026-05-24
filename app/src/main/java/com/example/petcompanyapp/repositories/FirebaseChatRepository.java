package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.ChatMessage;
import com.petbook.app.models.ConversationSummary;
import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.NotificationType;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirebaseChatRepository {

    public interface ConversationsCallback {
        void onSuccess(List<ConversationSummary> conversations);
        void onError(String message);
    }

    public interface MessagesCallback {
        void onSuccess(List<ChatMessage> messages);
        void onError(String message);
    }

    private FirebaseChatRepository() {
    }

    public static ListenerRegistration listenConversations(
            Context context,
            String currentUserEmail,
            @NonNull ConversationsCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return null;
        }

        String currentUserKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        return conversationsCollection(context)
                .whereArrayContains("participantKeys", currentUserKey)
                .orderBy("lastMessageAtMillis", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<ConversationSummary> conversations = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            conversations.add(mapConversation(document, currentUserKey));
                        }
                    }
                    callback.onSuccess(conversations);
                });
    }

    public static ListenerRegistration listenMessages(
            Context context,
            String currentUserEmail,
            String targetUserEmail,
            @NonNull MessagesCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return null;
        }

        String conversationId = ChatIdentityUtils.conversationId(
                ChatIdentityUtils.userKeyFromEmail(currentUserEmail),
                ChatIdentityUtils.userKeyFromEmail(targetUserEmail)
        );

        markConversationAsRead(context, currentUserEmail, targetUserEmail);

        return conversationsCollection(context)
                .document(conversationId)
                .collection("messages")
                .orderBy("sentAtMillis", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<ChatMessage> messages = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            messages.add(mapMessage(document));
                        }
                    }
                    callback.onSuccess(messages);
                });
    }

    public static void openConversation(
            Context context,
            String currentUserName,
            String currentUserEmail,
            String currentUserType,
            String targetUserName,
            String targetUserEmail,
            String targetUserType
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }

        String currentKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        String targetKey = ChatIdentityUtils.userKeyFromEmail(targetUserEmail);
        String conversationId = ChatIdentityUtils.conversationId(currentKey, targetKey);

        String userOneKey = currentKey.compareTo(targetKey) <= 0 ? currentKey : targetKey;
        String userTwoKey = currentKey.compareTo(targetKey) <= 0 ? targetKey : currentKey;

        boolean currentIsUserOne = currentKey.equals(userOneKey);
        Map<String, Object> values = new HashMap<>();
        values.put("participantKeys", java.util.Arrays.asList(userOneKey, userTwoKey));
        values.put("userOneKey", userOneKey);
        values.put("userOneName", currentIsUserOne ? currentUserName : targetUserName);
        values.put("userOneEmail", currentIsUserOne ? currentUserEmail : targetUserEmail);
        values.put("userOneType", currentIsUserOne ? currentUserType : targetUserType);
        values.put("userTwoKey", userTwoKey);
        values.put("userTwoName", currentIsUserOne ? targetUserName : currentUserName);
        values.put("userTwoEmail", currentIsUserOne ? targetUserEmail : currentUserEmail);
        values.put("userTwoType", currentIsUserOne ? targetUserType : currentUserType);
        values.put("updatedAtMillis", System.currentTimeMillis());
        values.put("createdAtMillis", System.currentTimeMillis());
        values.put("lastMessageAtMillis", System.currentTimeMillis());
        values.put("lastMessageText", "");
        values.put("unreadCountUserOne", 0L);
        values.put("unreadCountUserTwo", 0L);

        conversationsCollection(context).document(conversationId).set(values, com.google.firebase.firestore.SetOptions.merge());
    }

    public static void sendMessage(
            Context context,
            String currentUserName,
            String currentUserEmail,
            String currentUserType,
            String targetUserName,
            String targetUserEmail,
            String targetUserType,
            String messageText
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }

        String currentKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        String targetKey = ChatIdentityUtils.userKeyFromEmail(targetUserEmail);
        String conversationId = ChatIdentityUtils.conversationId(currentKey, targetKey);
        long now = System.currentTimeMillis();

        openConversation(
                context,
                currentUserName,
                currentUserEmail,
                currentUserType,
                targetUserName,
                targetUserEmail,
                targetUserType
        );

        Map<String, Object> message = new HashMap<>();
        message.put("senderKey", currentKey);
        message.put("senderName", currentUserName);
        message.put("receiverKey", targetKey);
        message.put("messageText", messageText);
        message.put("sentAtMillis", now);

        DocumentReference conversationRef = conversationsCollection(context).document(conversationId);
        conversationRef.collection("messages").add(message);

        boolean currentIsUserOne = currentKey.compareTo(targetKey) <= 0;
        Map<String, Object> conversationUpdate = new HashMap<>();
        conversationUpdate.put("lastMessageText", messageText);
        conversationUpdate.put("lastMessageAtMillis", now);
        conversationUpdate.put("updatedAtMillis", now);
        conversationUpdate.put(currentIsUserOne ? "unreadCountUserTwo" : "unreadCountUserOne",
                com.google.firebase.firestore.FieldValue.increment(1));
        conversationUpdate.put(currentIsUserOne ? "unreadCountUserOne" : "unreadCountUserTwo", 0);
        conversationRef.set(conversationUpdate, com.google.firebase.firestore.SetOptions.merge());

        FirebaseNotificationRepository.addNotification(
                context,
                targetUserEmail,
                NotificationType.CHAT_MESSAGE,
                context.getString(com.petbook.app.R.string.notification_chat_title),
                context.getString(com.petbook.app.R.string.notification_chat_message, currentUserName),
                null,
                null,
                null,
                currentUserName,
                currentUserEmail,
                null
        );
    }

    public static void markConversationAsRead(
            Context context,
            String currentUserEmail,
            String targetUserEmail
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            return;
        }

        String currentKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        String targetKey = ChatIdentityUtils.userKeyFromEmail(targetUserEmail);
        boolean currentIsUserOne = currentKey.compareTo(targetKey) <= 0;
        String conversationId = ChatIdentityUtils.conversationId(currentKey, targetKey);

        Map<String, Object> update = new HashMap<>();
        update.put(currentIsUserOne ? "unreadCountUserOne" : "unreadCountUserTwo", 0);
        conversationsCollection(context).document(conversationId)
                .set(update, com.google.firebase.firestore.SetOptions.merge());
    }

    private static ConversationSummary mapConversation(DocumentSnapshot document, String currentUserKey) {
        boolean currentIsUserOne = currentUserKey.equals(document.getString("userOneKey"));
        String partnerName = currentIsUserOne ? safe(document.getString("userTwoName")) : safe(document.getString("userOneName"));
        String partnerType = currentIsUserOne ? safe(document.getString("userTwoType")) : safe(document.getString("userOneType"));
        String partnerEmail = currentIsUserOne ? safe(document.getString("userTwoEmail")) : safe(document.getString("userOneEmail"));
        String lastMessage = safe(document.getString("lastMessageText"));
        long lastMessageAt = safeLong(document.getLong("lastMessageAtMillis"));
        int unreadCount = (int) safeLong(document.getLong(currentIsUserOne ? "unreadCountUserOne" : "unreadCountUserTwo"));
        return new ConversationSummary(
                0L,
                null,
                partnerName,
                partnerType,
                partnerEmail,
                lastMessage,
                lastMessageAt,
                unreadCount
        );
    }

    private static ChatMessage mapMessage(DocumentSnapshot document) {
        return new ChatMessage(
                0L,
                0L,
                null,
                null,
                document.getString("senderKey"),
                document.getString("receiverKey"),
                safe(document.getString("senderName")),
                safe(document.getString("messageText")),
                safeLong(document.getLong("sentAtMillis")),
                true
        );
    }

    private static CollectionReference conversationsCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("chat_conversations");
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

