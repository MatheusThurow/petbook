package com.petbook.app.utils;

public final class ChatIdentityUtils {

    private ChatIdentityUtils() {
    }

    public static String userKeyFromEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase()
                .replace(".", ",")
                .replace("@", "_at_");
    }

    public static String conversationId(String firstUserKey, String secondUserKey) {
        if (firstUserKey.compareTo(secondUserKey) <= 0) {
            return firstUserKey + "__" + secondUserKey;
        }
        return secondUserKey + "__" + firstUserKey;
    }
}

