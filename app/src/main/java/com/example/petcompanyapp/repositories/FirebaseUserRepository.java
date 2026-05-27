package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.User;
import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FirebaseUserRepository {

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface BooleanCallback {
        void onSuccess(boolean value);
        void onError(String message);
    }

    public interface CompletionCallback {
        void onSuccess();
        void onError(String message);
    }

    private FirebaseUserRepository() {
    }

    public static boolean isEnabled(Context context) {
        return FirebaseChatConfig.isEnabled(context);
    }

    public static void bootstrapLocalUsers(Context context, @NonNull CompletionCallback callback) {
        if (!isEnabled(context)) {
            callback.onSuccess();
            return;
        }

        List<User> users = UserRepository.getActiveUsers(context);
        WriteBatch batch = usersCollection(context).getFirestore().batch();
        for (User user : users) {
            batch.set(
                    usersCollection(context).document(documentIdFromEmail(user.getEmail())),
                    buildUserMap(user),
                    SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void authenticate(
            Context context,
            String email,
            String password,
            @NonNull UserCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String normalizedEmail = normalizedEmail(email);
        auth.signInWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(result ->
                        findByEmail(context, normalizedEmail, callback)
                )
                .addOnFailureListener(exception ->
                        fallbackAuthenticateWithFirestore(context, normalizedEmail, password, callback)
                );
    }

    public static void register(
            Context context,
            String userType,
            String name,
            String email,
            String password,
            String document,
            @NonNull UserCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        String documentId = documentIdFromEmail(email);
        usersCollection(context)
                .document(documentId)
                .get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        callback.onError("Ja existe um usuario cadastrado com este email.");
                        return;
                    }

                    long generatedId = System.currentTimeMillis();
                    User user = new User(
                            generatedId,
                            userType,
                            name,
                            normalizedEmail(email),
                            password,
                            document,
                            true
                    );

                    usersCollection(context)
                            .document(documentId)
                            .set(buildUserMap(user), SetOptions.merge())
                            .addOnSuccessListener(unused ->
                                    FirebaseAuth.getInstance()
                                            .createUserWithEmailAndPassword(normalizedEmail(email), password)
                                            .addOnSuccessListener(authResult -> callback.onSuccess(user))
                                            .addOnFailureListener(exception -> {
                                                String message = safeMessage(exception);
                                                if (message != null && message.toLowerCase(Locale.ROOT).contains("already")) {
                                                    callback.onSuccess(user);
                                                    return;
                                                }
                                                callback.onError(message);
                                            })
                            )
                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void registerGoogleUser(
            Context context,
            String name,
            String email,
            @NonNull UserCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        String documentId = documentIdFromEmail(email);
        usersCollection(context)
                .document(documentId)
                .get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        User user = mapUser(existing.getData());
                        if (user == null) {
                            callback.onError("Nao foi possivel carregar a conta Google.");
                            return;
                        }
                        callback.onSuccess(user);
                        return;
                    }

                    User user = new User(
                            System.currentTimeMillis(),
                            "PERSON",
                            safeDisplayName(name, email),
                            normalizedEmail(email),
                            "",
                            "",
                            true
                    );

                    usersCollection(context)
                            .document(documentId)
                            .set(buildUserMap(user), SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void completeGoogleRegistration(
            Context context,
            String userType,
            String name,
            String email,
            String password,
            String document,
            @NonNull UserCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        String documentId = documentIdFromEmail(email);
        usersCollection(context)
                .document(documentId)
                .get()
                .addOnSuccessListener(existing -> {
                    if (existing.exists()) {
                        User user = mapUser(existing.getData());
                        if (user == null) {
                            callback.onError("Nao foi possivel concluir o cadastro.");
                            return;
                        }
                        callback.onSuccess(user);
                        return;
                    }

                    User user = new User(
                            System.currentTimeMillis(),
                            userType,
                            name,
                            normalizedEmail(email),
                            password,
                            document == null ? "" : document,
                            true
                    );

                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onError("Nao foi possivel concluir o cadastro com Google.");
                        return;
                    }

                    firebaseUser.updatePassword(password)
                            .addOnSuccessListener(unused ->
                                    usersCollection(context)
                                            .document(documentId)
                                            .set(buildUserMap(user), SetOptions.merge())
                                            .addOnSuccessListener(saved -> callback.onSuccess(user))
                                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)))
                            )
                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void findByEmail(Context context, String email, @NonNull UserCallback callback) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        usersCollection(context)
                .document(documentIdFromEmail(email))
                .get()
                .addOnSuccessListener(document -> {
                    User user = mapUser(document.getData());
                    if (user == null) {
                        callback.onError("Usuario nao encontrado.");
                        return;
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void findById(Context context, Long userId, @NonNull UserCallback callback) {
        if (!isEnabled(context) || userId == null) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        usersCollection(context)
                .whereEqualTo("localUserId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onError("Usuario nao encontrado.");
                        return;
                    }
                    User user = mapUser(result.getDocuments().get(0).getData());
                    if (user == null) {
                        callback.onError("Usuario nao encontrado.");
                        return;
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void updateProfile(
            Context context,
            long userId,
            String name,
            String email,
            @NonNull UserCallback callback
    ) {
        findById(context, userId, new UserCallback() {
            @Override
            public void onSuccess(User currentUser) {
                String oldDocumentId = documentIdFromEmail(currentUser.getEmail());
                String newDocumentId = documentIdFromEmail(email);

                if (!oldDocumentId.equals(newDocumentId)) {
                    usersCollection(context)
                            .document(newDocumentId)
                            .get()
                            .addOnSuccessListener(existing -> {
                                if (existing.exists()) {
                                    callback.onError("Ja existe um usuario cadastrado com este email.");
                                    return;
                                }
                                saveProfileDocument(context, currentUser, name, email, oldDocumentId, newDocumentId, callback);
                            })
                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                    return;
                }

                saveProfileDocument(context, currentUser, name, email, oldDocumentId, newDocumentId, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public static void resetPassword(
            Context context,
            String email,
            String document,
            String newPassword,
            @NonNull BooleanCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        usersCollection(context)
                .document(documentIdFromEmail(email))
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = mapUser(snapshot.getData());
                    if (user == null || !user.isActive() || user.getDocument() == null || !user.getDocument().equals(document)) {
                        callback.onSuccess(false);
                        return;
                    }

                    usersCollection(context)
                            .document(documentIdFromEmail(email))
                            .update("password", newPassword, "updatedAtMillis", System.currentTimeMillis())
                            .addOnSuccessListener(unused -> callback.onSuccess(true))
                            .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    public static void isValidActiveUser(Context context, Long userId, @NonNull BooleanCallback callback) {
        findById(context, userId, new UserCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user.isActive());
            }

            @Override
            public void onError(String message) {
                callback.onSuccess(false);
            }
        });
    }

    public static void sendPasswordResetEmail(
            Context context,
            String email,
            @NonNull CompletionCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        String normalizedEmail = normalizedEmail(email);
        findByEmail(context, normalizedEmail, new UserCallback() {
            @Override
            public void onSuccess(User user) {
                FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(normalizedEmail)
                        .addOnSuccessListener(unused -> callback.onSuccess())
                        .addOnFailureListener(exception ->
                                provisionAuthUserForReset(user, new CompletionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        FirebaseAuth.getInstance()
                                                .sendPasswordResetEmail(normalizedEmail)
                                                .addOnSuccessListener(ignore -> callback.onSuccess())
                                                .addOnFailureListener(secondException -> callback.onError(safeMessage(secondException)));
                                    }

                                    @Override
                                    public void onError(String message) {
                                        callback.onError(message);
                                    }
                                })
                        );
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public static void changePassword(
            Context context,
            String email,
            String currentPassword,
            String newPassword,
            @NonNull CompletionCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        authenticate(context, email, currentPassword, new UserCallback() {
            @Override
            public void onSuccess(User user) {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser == null || firebaseUser.getEmail() == null) {
                    callback.onError("Nao foi possivel validar a sessao atual.");
                    return;
                }

                AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword);
                firebaseUser.reauthenticate(credential)
                        .addOnSuccessListener(unused ->
                                firebaseUser.updatePassword(newPassword)
                                        .addOnSuccessListener(passwordUpdated ->
                                                updateStoredPassword(context, email, newPassword, callback)
                                        )
                                        .addOnFailureListener(exception -> callback.onError(safeMessage(exception)))
                        )
                        .addOnFailureListener(exception -> callback.onError("Senha atual incorreta."));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private static void saveProfileDocument(
            Context context,
            User currentUser,
            String name,
            String email,
            String oldDocumentId,
            String newDocumentId,
            @NonNull UserCallback callback
    ) {
        User updatedUser = new User(
                currentUser.getId(),
                currentUser.getUserType(),
                name,
                normalizedEmail(email),
                currentUser.getPassword(),
                currentUser.getDocument(),
                currentUser.isActive()
        );

        WriteBatch batch = usersCollection(context).getFirestore().batch();
        batch.set(
                usersCollection(context).document(newDocumentId),
                buildUserMap(updatedUser),
                SetOptions.merge()
        );
        if (!oldDocumentId.equals(newDocumentId)) {
            batch.delete(usersCollection(context).document(oldDocumentId));
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null && currentUser.getEmail() != null
                            && currentUser.getEmail().equalsIgnoreCase(firebaseUser.getEmail())
                            && !currentUser.getEmail().equalsIgnoreCase(email)) {
                        firebaseUser.verifyBeforeUpdateEmail(normalizedEmail(email))
                                .addOnSuccessListener(ignore -> callback.onSuccess(updatedUser))
                                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                        return;
                    }
                    callback.onSuccess(updatedUser);
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    private static void fallbackAuthenticateWithFirestore(
            Context context,
            String email,
            String password,
            @NonNull UserCallback callback
    ) {
        usersCollection(context)
                .document(documentIdFromEmail(email))
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Usuario ou senha invalidos.");
                        return;
                    }

                    User user = mapUser(document.getData());
                    if (user == null || !user.isActive() || user.getPassword() == null || !user.getPassword().equals(password)) {
                        callback.onError("Usuario ou senha invalidos.");
                        return;
                    }

                    ensureAuthUser(context, user, new CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            FirebaseAuth.getInstance()
                                    .signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener(result -> callback.onSuccess(user))
                                    .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
                })
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    private static void ensureAuthUser(
            Context context,
            User user,
            @NonNull CompletionCallback callback
    ) {
        String email = normalizedEmail(user.getEmail());
        String password = user.getPassword();
        if (email.isEmpty() || password == null || password.trim().isEmpty()) {
            callback.onError("Usuario sem credenciais validas para autenticacao.");
            return;
        }

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(signInException ->
                        FirebaseAuth.getInstance()
                                .createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> callback.onSuccess())
                                .addOnFailureListener(createException -> {
                                    String message = safeMessage(createException).toLowerCase(Locale.ROOT);
                                    if (message.contains("already")) {
                                        callback.onSuccess();
                                        return;
                                    }
                                    callback.onError(safeMessage(createException));
                                })
                );
    }

    private static void updateStoredPassword(
            Context context,
            String email,
            String newPassword,
            @NonNull CompletionCallback callback
    ) {
        usersCollection(context)
                .document(documentIdFromEmail(email))
                .update("password", newPassword, "updatedAtMillis", System.currentTimeMillis())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(exception -> callback.onError(safeMessage(exception)));
    }

    private static void provisionAuthUserForReset(
            User user,
            @NonNull CompletionCallback callback
    ) {
        String email = normalizedEmail(user.getEmail());
        String password = user.getPassword();
        if (email.isEmpty() || password == null || password.trim().isEmpty()) {
            callback.onError("Nao foi possivel preparar sua conta para recuperacao de senha.");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(exception -> {
                    String code = authErrorCode(exception);
                    if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code) || "email-already-in-use".equals(code)) {
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(safeMessage(exception));
                });
    }

    private static Map<String, Object> buildUserMap(User user) {
        Map<String, Object> values = new HashMap<>();
        values.put("userKey", ChatIdentityUtils.userKeyFromEmail(user.getEmail()));
        values.put("name", user.getName());
        values.put("email", normalizedEmail(user.getEmail()));
        values.put("userType", user.getUserType());
        values.put("active", user.isActive());
        values.put("localUserId", user.getId());
        values.put("password", user.getPassword());
        values.put("documentNumber", user.getDocument());
        values.put("updatedAtMillis", System.currentTimeMillis());
        return values;
    }

    private static User mapUser(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object idValue = data.get("localUserId");
        Long userId = idValue instanceof Number ? ((Number) idValue).longValue() : null;
        Object activeValue = data.get("active");
        boolean active = !(activeValue instanceof Boolean) || (Boolean) activeValue;
        return new User(
                userId,
                safeString(data.get("userType")),
                safeString(data.get("name")),
                safeString(data.get("email")),
                safeString(data.get("password")),
                safeString(data.get("documentNumber")),
                active
        );
    }

    private static CollectionReference usersCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("users");
    }

    private static String documentIdFromEmail(String email) {
        return ChatIdentityUtils.userKeyFromEmail(normalizedEmail(email));
    }

    private static String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String safeDisplayName(String name, String email) {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String normalizedEmail = normalizedEmail(email);
        int separatorIndex = normalizedEmail.indexOf('@');
        if (separatorIndex > 0) {
            return normalizedEmail.substring(0, separatorIndex);
        }
        return "Usuario Google";
    }

    private static String safeMessage(Exception exception) {
        String code = authErrorCode(exception);
        if ("ERROR_USER_NOT_FOUND".equals(code) || "user-not-found".equals(code)) {
            return "Nao encontramos uma conta com este e-mail.";
        }
        if ("ERROR_INVALID_EMAIL".equals(code) || "invalid-email".equals(code)) {
            return "Informe um e-mail valido.";
        }
        if ("ERROR_TOO_MANY_REQUESTS".equals(code) || "too-many-requests".equals(code)) {
            return "Muitas tentativas seguidas. Aguarde um pouco e tente novamente.";
        }
        if ("ERROR_NETWORK_REQUEST_FAILED".equals(code) || "network-request-failed".equals(code)) {
            return "Falha de rede ao falar com o Firebase. Verifique sua internet.";
        }
        if ("ERROR_INTERNAL_ERROR".equals(code) || "internal-error".equals(code)) {
            return "O Firebase nao conseguiu processar a recuperacao agora. Tente novamente em instantes.";
        }
        return exception == null || exception.getMessage() == null
                ? "Nao foi possivel concluir a operacao no Firebase."
                : exception.getMessage();
    }

    private static String authErrorCode(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            return ((FirebaseAuthException) exception).getErrorCode();
        }
        return null;
    }
}
