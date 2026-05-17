package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.User;
import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FirebaseUserDirectoryRepository {

    public interface UserListCallback {
        void onSuccess(List<User> users);
        void onError(String message);
    }

    private FirebaseUserDirectoryRepository() {
    }

    public static void syncUser(Context context, User user) {
        if (user == null || user.getEmail() == null || !FirebaseChatConfig.isEnabled(context)) {
            return;
        }

        Map<String, Object> values = new HashMap<>();
        values.put("userKey", ChatIdentityUtils.userKeyFromEmail(user.getEmail()));
        values.put("name", user.getName());
        values.put("email", user.getEmail().trim().toLowerCase(Locale.ROOT));
        values.put("userType", user.getUserType());
        values.put("active", user.isActive());
        values.put("localUserId", user.getId());
        values.put("updatedAtMillis", System.currentTimeMillis());

        usersCollection(context)
                .document(ChatIdentityUtils.userKeyFromEmail(user.getEmail()))
                .set(values);
    }

    public static void searchUsers(
            Context context,
            String query,
            String currentUserEmail,
            @NonNull UserListCallback callback
    ) {
        if (!FirebaseChatConfig.isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return;
        }

        usersCollection(context)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(result -> {
                    String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
                    String currentUserKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : result) {
                        String userKey = document.getString("userKey");
                        if (userKey != null && userKey.equals(currentUserKey)) {
                            continue;
                        }

                        String name = safe(document.getString("name"));
                        String email = safe(document.getString("email"));
                        if (!normalizedQuery.isEmpty()
                                && !name.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                                && !email.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                            continue;
                        }

                        Long localId = document.getLong("localUserId");
                        users.add(new User(
                                localId,
                                document.getString("userType"),
                                name,
                                email,
                                null,
                                null,
                                true
                        ));
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    private static CollectionReference usersCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("users");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

