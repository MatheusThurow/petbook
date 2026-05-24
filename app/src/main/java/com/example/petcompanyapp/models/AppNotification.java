package com.petbook.app.models;

public class AppNotification {

    private final long id;
    private final long recipientUserId;
    private final String type;
    private final String title;
    private final String message;
    private final Long relatedPostId;
    private final String relatedPostType;
    private final Long relatedUserId;
    private final String relatedUserName;
    private final String relatedUserEmail;
    private final Long relatedConversationId;
    private final long createdAtMillis;
    private final boolean read;

    public AppNotification(
            long id,
            long recipientUserId,
            String type,
            String title,
            String message,
            Long relatedPostId,
            String relatedPostType,
            Long relatedUserId,
            String relatedUserName,
            String relatedUserEmail,
            Long relatedConversationId,
            long createdAtMillis,
            boolean read
    ) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedPostId = relatedPostId;
        this.relatedPostType = relatedPostType;
        this.relatedUserId = relatedUserId;
        this.relatedUserName = relatedUserName;
        this.relatedUserEmail = relatedUserEmail;
        this.relatedConversationId = relatedConversationId;
        this.createdAtMillis = createdAtMillis;
        this.read = read;
    }

    public long getId() {
        return id;
    }

    public long getRecipientUserId() {
        return recipientUserId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Long getRelatedPostId() {
        return relatedPostId;
    }

    public String getRelatedPostType() {
        return relatedPostType;
    }

    public Long getRelatedUserId() {
        return relatedUserId;
    }

    public String getRelatedUserName() {
        return relatedUserName;
    }

    public String getRelatedUserEmail() {
        return relatedUserEmail;
    }

    public Long getRelatedConversationId() {
        return relatedConversationId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isRead() {
        return read;
    }
}
