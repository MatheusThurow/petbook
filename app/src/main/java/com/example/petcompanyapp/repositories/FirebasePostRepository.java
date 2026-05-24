package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.FairAnimal;
import com.petbook.app.models.PostComment;
import com.petbook.app.utils.ChatIdentityUtils;
import com.petbook.app.utils.FeedFilter;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.ImageUtils;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.PostType;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirebasePostRepository {

    public interface PostsCallback {
        void onSuccess(List<AnimalPost> posts);
        void onError(String message);
    }

    public interface PostCallback {
        void onSuccess(AnimalPost post);
        void onError(String message);
    }

    public interface FairAnimalsCallback {
        void onSuccess(List<FairAnimal> fairAnimals);
        void onError(String message);
    }

    public interface CommentsCallback {
        void onSuccess(List<PostComment> comments);
        void onError(String message);
    }

    public interface CompletionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface BooleanCallback {
        void onSuccess(boolean created);
        void onError(String message);
    }

    private FirebasePostRepository() {
    }

    public static boolean isEnabled(Context context) {
        return FirebaseChatConfig.isEnabled(context);
    }

    public static void loadPosts(Context context, String filter, String currentUserEmail, @NonNull PostsCallback callback) {
        if (!isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return;
        }

        postsCollection(context)
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    List<AnimalPost> posts = new ArrayList<>();
                    String currentUserKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
                    for (QueryDocumentSnapshot document : result) {
                        AnimalPost post = mapPost(document, currentUserKey);
                        if (matchesFilter(post, filter)) {
                            posts.add(post);
                        }
                    }
                    callback.onSuccess(posts);
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void bootstrapLocalPostsIfNeeded(Context context, @NonNull CompletionCallback callback) {
        if (!isEnabled(context)) {
            callback.onSuccess();
            return;
        }

        postsCollection(context)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (!result.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    List<AnimalPost> localPosts = AnimalPostRepository.getPosts(context, FeedFilter.ALL);
                    WriteBatch batch = FirebaseChatConfig.getFirestore(context).batch();
                    for (AnimalPost localPost : localPosts) {
                        Map<String, Object> values = buildPostMap(
                                context,
                                localPost,
                                AnimalPostRepository.getFairAnimalsForPost(context, localPost.getId())
                        );
                        batch.set(postsCollection(context).document(String.valueOf(localPost.getId())), values, SetOptions.merge());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void savePost(
            Context context,
            AnimalPost post,
            List<FairAnimal> fairAnimals,
            @NonNull PostCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        Map<String, Object> values = buildPostMap(context, post, fairAnimals);

        postsCollection(context)
                .document(String.valueOf(post.getId()))
                .set(values, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(post))
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void getPostById(Context context, long postId, @NonNull PostCallback callback) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }
        postsCollection(context)
                .document(String.valueOf(postId))
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError("Post nao encontrado.");
                        return;
                    }
                    callback.onSuccess(mapPost(document, ""));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void getFairAnimals(Context context, long postId, @NonNull FairAnimalsCallback callback) {
        getPostById(context, postId, new PostCallback() {
            @Override
            public void onSuccess(AnimalPost post) {
                postsCollection(context)
                        .document(String.valueOf(postId))
                        .get()
                        .addOnSuccessListener(document -> callback.onSuccess(extractFairAnimals(document.get("fairAnimals"))))
                        .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public static void toggleLike(
            Context context,
            AnimalPost post,
            String currentUserEmail,
            String currentUserName,
            Long currentUserId,
            @NonNull CompletionCallback callback
    ) {
        String currentUserKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        postsCollection(context)
                .document(String.valueOf(post.getId()))
                .get()
                .addOnSuccessListener(document -> {
                    List<String> likedByKeys = (List<String>) document.get("likedByKeys");
                    if (likedByKeys == null) {
                        likedByKeys = new ArrayList<>();
                    }
                    boolean alreadyLiked = likedByKeys.contains(currentUserKey);

                    Map<String, Object> update = new HashMap<>();
                    update.put("likedByKeys", alreadyLiked
                            ? FieldValue.arrayRemove(currentUserKey)
                            : FieldValue.arrayUnion(currentUserKey));
                    update.put("likeCount", FieldValue.increment(alreadyLiked ? -1 : 1));

                    postsCollection(context)
                            .document(String.valueOf(post.getId()))
                            .update(update)
                            .addOnSuccessListener(unused -> {
                                if (!alreadyLiked
                                        && post.getAuthorEmail() != null
                                        && !post.getAuthorEmail().trim().isEmpty()
                                        && !post.getAuthorEmail().equalsIgnoreCase(currentUserEmail)) {
                                    FirebaseNotificationRepository.addNotification(
                                            context,
                                            post.getAuthorEmail(),
                                            NotificationType.LIKE,
                                            context.getString(com.petbook.app.R.string.notification_like_title),
                                            context.getString(com.petbook.app.R.string.notification_like_message, currentUserName, post.getAnimalName()),
                                            post.getId(),
                                            post.getPostType(),
                                            currentUserId,
                                            currentUserName,
                                            currentUserEmail,
                                            null
                                    );
                                }
                                callback.onSuccess();
                            })
                            .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void getComments(Context context, long postId, @NonNull CommentsCallback callback) {
        if (!isEnabled(context)) {
            callback.onSuccess(java.util.Collections.emptyList());
            return;
        }

        postsCollection(context)
                .document(String.valueOf(postId))
                .collection("comments")
                .orderBy("createdAtMillis", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    List<PostComment> flatComments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : result) {
                        flatComments.add(new PostComment(
                                safeLong(document.getLong("id")),
                                postId,
                                safeLong(document.getLong("authorUserId")),
                                safe(document.getString("authorName")),
                                safe(document.getString("messageText")),
                                safeLong(document.getLong("createdAtMillis")),
                                document.getLong("parentCommentId"),
                                0
                        ));
                    }
                    callback.onSuccess(buildThreadedComments(flatComments));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void addComment(
            Context context,
            AnimalPost post,
            long authorUserId,
            String authorName,
            String authorEmail,
            String message,
            Long parentCommentId,
            @NonNull CompletionCallback callback
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", System.currentTimeMillis());
        values.put("authorUserId", authorUserId);
        values.put("authorName", authorName);
        values.put("authorEmail", authorEmail);
        values.put("messageText", message);
        values.put("parentCommentId", parentCommentId);
        values.put("createdAtMillis", System.currentTimeMillis());

        postsCollection(context)
                .document(String.valueOf(post.getId()))
                .collection("comments")
                .add(values)
                .addOnSuccessListener(unused -> {
                    if (post.getAuthorEmail() != null
                            && !post.getAuthorEmail().trim().isEmpty()
                            && !post.getAuthorEmail().equalsIgnoreCase(authorEmail)) {
                        FirebaseNotificationRepository.addNotification(
                                context,
                                post.getAuthorEmail(),
                                NotificationType.COMMENT,
                                context.getString(com.petbook.app.R.string.notification_comment_title),
                                context.getString(com.petbook.app.R.string.notification_comment_message, authorName, post.getAnimalName()),
                                post.getId(),
                                post.getPostType(),
                                authorUserId,
                                authorName,
                                authorEmail,
                                null
                        );
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    private static List<PostComment> buildThreadedComments(List<PostComment> flatComments) {
        Map<Long, List<PostComment>> repliesByParent = new HashMap<>();
        Map<Long, PostComment> commentsById = new HashMap<>();
        List<PostComment> rootComments = new ArrayList<>();

        for (PostComment comment : flatComments) {
            commentsById.put(comment.getId(), comment);
            if (comment.getParentCommentId() == null) {
                rootComments.add(comment);
                continue;
            }
            repliesByParent
                    .computeIfAbsent(comment.getParentCommentId(), ignored -> new ArrayList<>())
                    .add(comment);
        }

        List<PostComment> threaded = new ArrayList<>();
        for (PostComment rootComment : rootComments) {
            appendComment(threaded, rootComment, repliesByParent, commentsById, 0);
        }
        return threaded;
    }

    private static void appendComment(
            List<PostComment> target,
            PostComment comment,
            Map<Long, List<PostComment>> repliesByParent,
            Map<Long, PostComment> commentsById,
            int depth
    ) {
        String parentAuthorName = null;
        if (comment.getParentCommentId() != null) {
            PostComment parentComment = commentsById.get(comment.getParentCommentId());
            parentAuthorName = parentComment == null ? null : parentComment.getAuthorName();
        }

        target.add(new PostComment(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorUserId(),
                comment.getAuthorName(),
                comment.getMessage(),
                comment.getCreatedAtMillis(),
                comment.getParentCommentId(),
                parentAuthorName,
                depth
        ));

        List<PostComment> replies = repliesByParent.get(comment.getId());
        if (replies == null) {
            return;
        }

        for (PostComment reply : replies) {
            appendComment(target, reply, repliesByParent, commentsById, depth + 1);
        }
    }

    public static void registerInterest(
            Context context,
            AnimalPost post,
            Long currentUserId,
            String currentUserName,
            String currentUserEmail,
            String animalName,
            @NonNull BooleanCallback callback
    ) {
        String interestedKey = ChatIdentityUtils.userKeyFromEmail(currentUserEmail);
        String interestDocId = interestedKey + "__" + (animalName == null ? "" : animalName.replace(" ", "_"));

        postsCollection(context)
                .document(String.valueOf(post.getId()))
                .collection("interests")
                .document(interestDocId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(false);
                        return;
                    }

                    Map<String, Object> values = new HashMap<>();
                    values.put("interestedUserId", currentUserId);
                    values.put("interestedUserName", currentUserName);
                    values.put("interestedUserEmail", currentUserEmail);
                    values.put("animalName", animalName);
                    values.put("createdAtMillis", System.currentTimeMillis());

                    postsCollection(context)
                            .document(String.valueOf(post.getId()))
                            .collection("interests")
                            .document(interestDocId)
                            .set(values)
                            .addOnSuccessListener(unused -> {
                                if (post.getAuthorEmail() != null && !post.getAuthorEmail().equalsIgnoreCase(currentUserEmail)) {
                                    FirebaseNotificationRepository.addNotification(
                                            context,
                                            post.getAuthorEmail(),
                                            NotificationType.ADOPTION_INTEREST,
                                            context.getString(com.petbook.app.R.string.notification_interest_title),
                                            context.getString(
                                                    PostType.isFair(post.getPostType())
                                                            ? com.petbook.app.R.string.notification_interest_fair_message
                                                            : com.petbook.app.R.string.notification_interest_message,
                                                    currentUserName,
                                                    animalName
                                            ),
                                            post.getId(),
                                            post.getPostType(),
                                            currentUserId,
                                            currentUserName,
                                            currentUserEmail,
                                            null
                                    );
                                }
                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    public static void deletePost(
            Context context,
            long postId,
            @NonNull CompletionCallback callback
    ) {
        if (!isEnabled(context)) {
            callback.onError("Firebase nao configurado.");
            return;
        }

        com.google.firebase.firestore.DocumentReference postRef =
                postsCollection(context).document(String.valueOf(postId));

        deleteSubcollection(postRef.collection("comments"), () ->
                deleteSubcollection(postRef.collection("interests"), () ->
                        postRef.delete()
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(exception -> callback.onError(exception.getMessage())),
                        callback
                ),
                callback
        );
    }

    private static boolean matchesFilter(AnimalPost post, String filter) {
        if (FeedFilter.ALL.equals(filter)) {
            return true;
        }
        if (FeedFilter.LOST.equals(filter)) {
            return PostType.isLost(post.getPostType());
        }
        if (FeedFilter.ADOPTION.equals(filter)) {
            return PostType.isAdoptionRelated(post.getPostType());
        }
        return filter.equals(post.getPostType());
    }

    private static AnimalPost mapPost(com.google.firebase.firestore.DocumentSnapshot document, String currentUserKey) {
        List<FairAnimal> fairAnimals = extractFairAnimals(document.get("fairAnimals"));
        List<String> likedByKeys = (List<String>) document.get("likedByKeys");
        int likeCount = document.getLong("likeCount") == null ? (likedByKeys == null ? 0 : likedByKeys.size()) : document.getLong("likeCount").intValue();
        int fairAnimalCount = document.getLong("fairAnimalCount") == null ? fairAnimals.size() : document.getLong("fairAnimalCount").intValue();
        boolean liked = likedByKeys != null && likedByKeys.contains(currentUserKey);
        return new AnimalPost(
                safeLong(document.getLong("id")),
                document.getLong("authorUserId"),
                safe(document.getString("postType")),
                safe(document.getString("animalName")),
                safe(document.getString("species")),
                safe(document.getString("breed")),
                safe(document.getString("age")),
                safe(document.getString("description")),
                safe(document.getString("contactPhone")),
                document.getDouble("latitude"),
                document.getDouble("longitude"),
                safe(document.getString("locationReference")),
                safe(document.getString("imageUri")),
                safe(document.getString("authorName")),
                safe(document.getString("authorEmail")),
                safeLong(document.getLong("createdAtMillis")),
                liked,
                likeCount,
                fairAnimalCount
        );
    }

    private static List<Map<String, Object>> mapFairAnimals(List<FairAnimal> fairAnimals) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (fairAnimals == null) {
            return items;
        }
        for (FairAnimal fairAnimal : fairAnimals) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fairAnimal.getId());
            map.put("name", fairAnimal.getName());
            map.put("species", fairAnimal.getSpecies());
            map.put("breed", fairAnimal.getBreed());
            map.put("ageDescription", fairAnimal.getAgeDescription());
            items.add(map);
        }
        return items;
    }

    private static Map<String, Object> buildPostMap(Context context, AnimalPost post, List<FairAnimal> fairAnimals) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", post.getId());
        values.put("authorUserId", post.getAuthorUserId());
        values.put("authorName", post.getAuthorName());
        values.put("authorEmail", post.getAuthorEmail());
        values.put("postType", post.getPostType());
        values.put("animalName", post.getAnimalName());
        values.put("species", post.getSpecies());
        values.put("breed", post.getBreed());
        values.put("age", post.getAge());
        values.put("description", post.getDescription());
        values.put("contactPhone", post.getContactPhone());
        values.put("latitude", post.getLatitude());
        values.put("longitude", post.getLongitude());
        values.put("locationReference", post.getLocationReference());
        values.put("imageUri", normalizeImageValue(context, post.getImageUri()));
        values.put("createdAtMillis", post.getCreatedAtMillis());
        values.put("fairAnimalCount", fairAnimals == null ? 0 : fairAnimals.size());
        values.put("fairAnimals", mapFairAnimals(fairAnimals));
        return values;
    }

    private static String normalizeImageValue(Context context, String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) {
            return "";
        }
        if (imageValue.startsWith("data:image")) {
            return imageValue;
        }
        try {
            return ImageUtils.encodeImageAsDataUrl(context, android.net.Uri.parse(imageValue));
        } catch (Exception exception) {
            return imageValue;
        }
    }

    private static List<FairAnimal> extractFairAnimals(Object rawFairAnimals) {
        List<FairAnimal> fairAnimals = new ArrayList<>();
        if (!(rawFairAnimals instanceof List)) {
            return fairAnimals;
        }

        List<?> rawList = (List<?>) rawFairAnimals;
        for (Object item : rawList) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) item;
            fairAnimals.add(new FairAnimal(
                    map.get("id") instanceof Number ? ((Number) map.get("id")).longValue() : null,
                    null,
                    safe(map.get("name")),
                    safe(map.get("species")),
                    safe(map.get("breed")),
                    safe(map.get("ageDescription"))
            ));
        }
        return fairAnimals;
    }

    private static CollectionReference postsCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("posts");
    }

    private static void deleteSubcollection(
            CollectionReference collection,
            Runnable onSuccess,
            @NonNull CompletionCallback callback
    ) {
        collection.get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        onSuccess.run();
                        return;
                    }

                    WriteBatch batch = collection.getFirestore().batch();
                    for (QueryDocumentSnapshot document : result) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> onSuccess.run())
                            .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
                })
                .addOnFailureListener(exception -> callback.onError(exception.getMessage()));
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
