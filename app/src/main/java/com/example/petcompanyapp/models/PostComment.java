package com.petbook.app.models;

public class PostComment {

    private final long id;
    private final long postId;
    private final long authorUserId;
    private final String authorName;
    private final String message;
    private final long createdAtMillis;
    private final Long parentCommentId;
    private final String parentAuthorName;
    private final int depth;

    public PostComment(
            long id,
            long postId,
            long authorUserId,
            String authorName,
            String message,
            long createdAtMillis
    ) {
        this(id, postId, authorUserId, authorName, message, createdAtMillis, null, 0);
    }

    public PostComment(
            long id,
            long postId,
            long authorUserId,
            String authorName,
            String message,
            long createdAtMillis,
            Long parentCommentId,
            int depth
    ) {
        this(id, postId, authorUserId, authorName, message, createdAtMillis, parentCommentId, null, depth);
    }

    public PostComment(
            long id,
            long postId,
            long authorUserId,
            String authorName,
            String message,
            long createdAtMillis,
            Long parentCommentId,
            String parentAuthorName,
            int depth
    ) {
        this.id = id;
        this.postId = postId;
        this.authorUserId = authorUserId;
        this.authorName = authorName;
        this.message = message;
        this.createdAtMillis = createdAtMillis;
        this.parentCommentId = parentCommentId;
        this.parentAuthorName = parentAuthorName;
        this.depth = depth;
    }

    public long getId() {
        return id;
    }

    public long getPostId() {
        return postId;
    }

    public long getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getMessage() {
        return message;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public String getParentAuthorName() {
        return parentAuthorName;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isReply() {
        return parentCommentId != null;
    }
}
