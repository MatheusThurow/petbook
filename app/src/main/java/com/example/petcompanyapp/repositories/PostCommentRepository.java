package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;
import com.petbook.app.models.PostComment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PostCommentRepository {

    private PostCommentRepository() {
    }

    public static void addComment(Context context, long postId, long authorUserId, String message, Long parentCommentId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("post_id", postId);
            values.put("author_user_id", authorUserId);
            values.put("message_text", message);
            if (parentCommentId == null) {
                values.putNull("parent_comment_id");
            } else {
                values.put("parent_comment_id", parentCommentId);
            }
            values.put("created_at_millis", System.currentTimeMillis());
            db.insert(AppDatabaseHelper.TABLE_POST_COMMENTS, null, values);
        } finally {
            db.close();
        }
    }

    public static List<PostComment> getCommentsForPost(Context context, long postId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.post_id, c.author_user_id, u.name, c.message_text, c.created_at_millis, c.parent_comment_id "
                        + "FROM " + AppDatabaseHelper.TABLE_POST_COMMENTS + " c "
                        + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON u.id = c.author_user_id "
                        + "WHERE c.post_id = ? "
                        + "ORDER BY c.created_at_millis ASC, c.id ASC",
                new String[]{String.valueOf(postId)}
        );

        try {
            List<PostComment> flatComments = new ArrayList<>();
            while (cursor.moveToNext()) {
                flatComments.add(new PostComment(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getLong(5),
                        cursor.isNull(6) ? null : cursor.getLong(6),
                        0
                ));
            }
            return buildThreadedComments(flatComments);
        } finally {
            cursor.close();
            db.close();
        }
    }

    private static List<PostComment> buildThreadedComments(List<PostComment> flatComments) {
        Map<Long, List<PostComment>> repliesByParent = new LinkedHashMap<>();
        Map<Long, PostComment> commentsById = new LinkedHashMap<>();
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
}
