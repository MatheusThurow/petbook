package com.petbook.app.models;

public class ChatMessage {
    private final Long id;
    private final Long conversationId;
    private final Long senderUserId;
    private final Long receiverUserId;
    private final String senderUserKey;
    private final String receiverUserKey;
    private final String senderName;
    private final String messageText;
    private final long sentAtMillis;
    private final boolean read;

    public ChatMessage(
            Long id,
            Long conversationId,
            Long senderUserId,
            Long receiverUserId,
            String senderUserKey,
            String receiverUserKey,
            String senderName,
            String messageText,
            long sentAtMillis,
            boolean read
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.senderUserKey = senderUserKey;
        this.receiverUserKey = receiverUserKey;
        this.senderName = senderName;
        this.messageText = messageText;
        this.sentAtMillis = sentAtMillis;
        this.read = read;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public String getSenderUserKey() {
        return senderUserKey;
    }

    public String getReceiverUserKey() {
        return receiverUserKey;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    public boolean isRead() {
        return read;
    }
}

