package com.petbook.app.models;

public class ConversationSummary {
    private final Long conversationId;
    private final Long partnerUserId;
    private final String partnerName;
    private final String partnerType;
    private final String partnerEmail;
    private final String lastMessageText;
    private final long lastMessageAtMillis;
    private final int unreadCount;

    public ConversationSummary(
            Long conversationId,
            Long partnerUserId,
            String partnerName,
            String partnerType,
            String partnerEmail,
            String lastMessageText,
            long lastMessageAtMillis,
            int unreadCount
    ) {
        this.conversationId = conversationId;
        this.partnerUserId = partnerUserId;
        this.partnerName = partnerName;
        this.partnerType = partnerType;
        this.partnerEmail = partnerEmail;
        this.lastMessageText = lastMessageText;
        this.lastMessageAtMillis = lastMessageAtMillis;
        this.unreadCount = unreadCount;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getPartnerUserId() {
        return partnerUserId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getPartnerType() {
        return partnerType;
    }

    public String getPartnerEmail() {
        return partnerEmail;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public long getLastMessageAtMillis() {
        return lastMessageAtMillis;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}

